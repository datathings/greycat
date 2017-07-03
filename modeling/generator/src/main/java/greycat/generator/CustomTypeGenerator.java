/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package greycat.generator;

import greycat.language.Attribute;
import greycat.language.Constant;
import greycat.language.CustomType;
import greycat.language.Model;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Visibility;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MethodSource;

public class CustomTypeGenerator {

    static JavaSource[] generate(String packageName, Model model) {
        JavaSource[] sources = new JavaSource[model.classes().length];

        for (int i = 0; i < model.customTypes().length; i++) {
            sources[i] = generateCustomType(packageName, model.customTypes()[i]);
        }

        return sources;
    }

    private static JavaClassSource generateCustomType(String packageName, CustomType customType) {
        final JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
        javaClass.setPackage(packageName);
        javaClass.setName(customType.name());

        if (customType.parent() != null) {
            javaClass.setSuperType(packageName + "." + customType.parent().name());
        } else {
            javaClass.setSuperType("greycat.base.BaseCustomTypeSingle");
        }

        StringBuilder TS_GET_SET = new StringBuilder();
        customType.properties().forEach(o -> {
            if (o instanceof Attribute) {
                Attribute attribute = (Attribute) o;
                if (TypeManager.isPrimitive(attribute.type())) {
                    TS_GET_SET.append("get " + attribute.name() + "() : " + TypeManager.classTsName(attribute.type()) + " {return this.get" + Generator.upperCaseFirstChar(attribute.name()) + "();}\n");
                    TS_GET_SET.append("set " + attribute.name() + "(p : " + TypeManager.classTsName(attribute.type()) + "){ this.set" + Generator.upperCaseFirstChar(attribute.name()) + "(p);}\n");
                }
            }
        });
        //generate TS getter and setter
        javaClass.getJavaDoc().setFullText("<pre>{@extend ts\n" + TS_GET_SET + "\n}\n</pre>");


        // field for type name
        javaClass.addField()
                .setVisibility(Visibility.PUBLIC)
                .setFinal(true)
                .setStatic(true)
                .setType(String.class)
                .setName("TYPE_NAME")
                .setStringInitializer(customType.name());

        // field for type hash
        javaClass.addField()
                .setVisibility(Visibility.PUBLIC)
                .setFinal(true)
                .setStatic(true)
                .setType(int.class)
                .setName("TYPE_HASH")
                .setLiteralInitializer("greycat.utility.HashHelper.hash(TYPE_NAME)");

        // init method
        MethodSource<JavaClassSource> init = javaClass.addMethod()
                .setName("init")
                .setVisibility(Visibility.PUBLIC)
                .setReturnTypeVoid();
        StringBuilder initBodyBuilder = new StringBuilder();

        customType.properties().forEach(o -> {
            // constants
            if (o instanceof Constant) {
                Constant constant = (Constant) o;
                String value = constant.value();
                if (constant.type().equals("Task") && value != null) {
                    value = "greycat.Tasks.newTask().parse(\"" + value.replaceAll("\"", "'").trim() + "\",null);";
                } else if (!constant.type().equals("String") && value != null) {
                    value = value.replaceAll("\"", "");
                }
                javaClass.addField()
                        .setVisibility(Visibility.PUBLIC)
                        .setFinal(true)
                        .setName(constant.name())
                        .setType(TypeManager.className(constant.type()))
                        .setLiteralInitializer(value)
                        .setStatic(true);
            } else if (o instanceof Attribute) {
                Attribute att = (Attribute) o;
                // field attribute name
                javaClass.addField()
                        .setVisibility(Visibility.PUBLIC)
                        .setFinal(true)
                        .setName(att.name().toUpperCase())
                        .setType(String.class)
                        .setStringInitializer(att.name())
                        .setStatic(true);

                // field attribute type
                FieldSource<JavaClassSource> typeField = javaClass.addField()
                        .setVisibility(Visibility.PUBLIC)
                        .setFinal(true)
                        .setName(att.name().toUpperCase() + "_TYPE")
                        .setType(int.class)
                        .setStatic(true);
                typeField.setLiteralInitializer("greycat." + TypeManager.typeName(att.type()));

                // field attribute hash
                javaClass.addField()
                        .setVisibility(Visibility.PUBLIC)
                        .setFinal(true)
                        .setName(att.name().toUpperCase() + "_H")
                        .setType(int.class)
                        .setLiteralInitializer("greycat.utility.HashHelper.hash(" + att.name().toUpperCase() + ")")
                        .setStatic(true);

                // getter
                javaClass.addMethod()
                        .setName("get" + Generator.upperCaseFirstChar(att.name()))
                        .setVisibility(Visibility.PUBLIC)
                        .setFinal(true)
                        .setReturnType(TypeManager.className(att.type()))
                        .setBody("return (" + TypeManager.className(att.type()) + ") getAt(" + att.name().toUpperCase() + "_H" + ");");

                // setter
                javaClass.addMethod()
                        .setName("set" + Generator.upperCaseFirstChar(att.name()))
                        .setVisibility(Visibility.PUBLIC)
                        .setFinal(true)
                        .setReturnType(customType.name())
                        .setBody("setAt(" + att.name().toUpperCase() + "_H," +
                                "greycat." + TypeManager.typeName(att.type()) + "," + att.name() + ");\nreturn this;")
                        .addParameter(TypeManager.className(att.type()), att.name());

                // init
                if (att.value() != null) {
                    initBodyBuilder.append(DefaultValueGenerator.createMethodBody(att).toString());
                }

            }
        });


        // constructor
        MethodSource<JavaClassSource> constructor = javaClass.addMethod().setConstructor(true);
        constructor.addParameter("greycat.struct.EStructArray", "e");
        StringBuilder constructorBody = new StringBuilder();
        constructorBody.append("super(e);");
        constructor.setBody(constructorBody.toString());

        // toString
        MethodSource toString = javaClass.addMethod()
                .setName("toString")
                .setPublic()
                .setReturnType(String.class);
        final StringBuilder toStringBody = new StringBuilder();
        toStringBody.append("return \"" + customType.name() + "(\"");
        customType.properties().forEach(o -> {
            if (o instanceof Attribute) {
                Attribute att = (Attribute) o;
                toStringBody.append(" + " + "\"" + att.name() + ": \" " + "+ get" + Generator.upperCaseFirstChar(att.name()) + "()");
                toStringBody.append("+ \",\"");
            }
        });
        String finalToString = "";
        if (toStringBody.length() > 0) {
            finalToString = toStringBody.toString().substring(0, toStringBody.length() - 2);
        }
        finalToString += ")\";";
        toString.setBody(finalToString);

        init.setBody(initBodyBuilder.toString());

        return javaClass;
    }

}
