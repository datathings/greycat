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

import greycat.Callback;
import greycat.Graph;
import greycat.Type;
import greycat.language.*;
import greycat.language.Class;
import greycat.language.Enum;
import greycat.struct.DoubleArray;
import greycat.struct.IntArray;
import greycat.struct.LongArray;
import greycat.struct.StringArray;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Visibility;
import org.jboss.forge.roaster.model.source.*;

class NodeTypeGenerator {

    static JavaSource[] generate(String packageName, String name, Model model) {
        JavaSource[] sources = new JavaSource[model.classifiers().length];

        for (int i = 0; i < model.classifiers().length; i++) {
            final Classifier classifier = model.classifiers()[i];

            if (classifier instanceof Enum) {
                sources[i] = generateEnum(packageName, (Enum) classifier);
            } else if (classifier instanceof Class) {
                sources[i] = generateClass(packageName, name, (Class) classifier);
            }
        }

        return sources;
    }


    private static JavaSource generateEnum(String packageName, Enum enumClassifier) {
        final JavaEnumSource javaEnum = Roaster.create(JavaEnumSource.class);
        javaEnum.setPackage(packageName);

        javaEnum.setName(enumClassifier.name());
        for (String literal : enumClassifier.literals()) {
            javaEnum.addEnumConstant(literal);
        }
        return javaEnum;
    }

    private static JavaSource generateClass(String packageName, String name, Class classClassifier) {
        final JavaClassSource javaClass = Roaster.create(JavaClassSource.class);

        javaClass.setPackage(packageName);
        javaClass.setName(classClassifier.name());

        if (classClassifier.parent() != null) {
            javaClass.setSuperType(classClassifier.parent().name());
        } else {
            javaClass.setSuperType("greycat.base.BaseNode");
        }


        // create method
        MethodSource<JavaClassSource> create = javaClass.addMethod()
                .setName("create")
                .setVisibility(Visibility.PUBLIC)
                .setStatic(true);
        create.addParameter("long", "p_world");
        create.addParameter("long", "p_time");
        create.addParameter(Graph.class, "p_graph");
        create.setReturnType(classClassifier.name());
        create.setBody("return (" + javaClass.getName() + ") p_graph.newTypedNode(p_world, p_time, " + javaClass.getName() + ".NODE_NAME);");


        // constructor
        MethodSource<JavaClassSource> constructor = javaClass.addMethod().setConstructor(true);
        constructor.addParameter("long", "p_world");
        constructor.addParameter("long", "p_time");
        constructor.addParameter("long", "p_id");
        constructor.addParameter(Graph.class, "p_graph");
        constructor.setBody("super(p_world, p_time, p_id, p_graph);");
        constructor.setVisibility(Visibility.PUBLIC);

        // helper name
        javaClass.addField()
                .setVisibility(Visibility.PUBLIC)
                .setFinal(true)
                .setName("NODE_NAME")
                .setType(String.class)
                .setStringInitializer(javaClass.getCanonicalName())
                .setStatic(true);


        for (Property prop : classClassifier.properties()) {

            //add helper name
            javaClass.addField()
                    .setVisibility(Visibility.PUBLIC)
                    .setFinal(true)
                    .setName(prop.name().toUpperCase())
                    .setType(String.class)
                    .setStringInitializer(prop.name())
                    .setStatic(true);


            if (prop instanceof Attribute) {
                Attribute casted = (Attribute) prop;
                javaClass.addImport(Type.class);
                FieldSource<JavaClassSource> typeHelper = javaClass.addField()
                        .setVisibility(Visibility.PUBLIC)
                        .setFinal(true)
                        .setName(casted.name().toUpperCase() + "_TYPE")
                        .setType(byte.class)
                        .setStatic(true);
                typeHelper.setLiteralInitializer(typeToString((Attribute) prop));

                //generate getter
                MethodSource<JavaClassSource> getter = javaClass.addMethod();
                getter.setVisibility(Visibility.PUBLIC).setFinal(true);
                getter.setReturnType(typeToClassName((Attribute) prop));
                getter.setName("get" + upperCaseFirstChar(prop.name()));
                if (casted.isArray()) {
                    getter.setBody("return (" + typeToClassName(casted) + ") super.getOrCreate(" + casted.name().toUpperCase() + ", " + casted.name().toUpperCase() + "_TYPE);");
                } else {
                    getter.setBody("return (" + typeToClassName(casted) + ") super.get(" + casted.name().toUpperCase() + ");");
                }


                //generate setter
                javaClass.addMethod()
                        .setVisibility(Visibility.PUBLIC).setFinal(true)
                        .setName("set" + upperCaseFirstChar(prop.name()))
                        .setReturnType(classClassifier.name())
                        .setBody("super.set(" + prop.name().toUpperCase() + ", " + prop.name().toUpperCase()
                                + "_TYPE,value);\nreturn this;"
                        )
                        .addParameter(typeToClassName(casted), "value");


            } else if (prop instanceof Relation) {
                Relation rel = (Relation) prop;

                //generate getter
                if (rel.isToOne()) {
                    String resultType = rel.type();
                    MethodSource<JavaClassSource> getter = javaClass.addMethod();
                    getter.setVisibility(Visibility.PUBLIC);
                    getter.setFinal(true);
                    getter.setReturnTypeVoid();
                    getter.setName("get" + upperCaseFirstChar(rel.name()));
                    getter.addParameter("greycat.Callback<" + resultType + ">", "callback");
                    getter.setBody(
                            "this.relation(" + prop.name().toUpperCase() + ",new greycat.Callback<greycat.Node[]>() {\n" +
                                    "@Override\n" +
                                    "public void on(greycat.Node[] nodes) {\n" +
                                    resultType + " result = (" + resultType + ") nodes[0];\n" +
                                    "callback.on(result);" +
                                    "}\n" +
                                    "});"
                    );
                } else {
                    String resultType = rel.type();
                    MethodSource<JavaClassSource> getter = javaClass.addMethod();
                    getter.setVisibility(Visibility.PUBLIC);
                    getter.setFinal(true);
                    getter.setReturnTypeVoid();
                    getter.setName("get" + upperCaseFirstChar(rel.name()));
                    getter.addParameter("greycat.Callback<" + resultType + "[]>", "callback");
                    getter.setBody(
                            "this.relation(" + prop.name().toUpperCase() + ",new greycat.Callback<greycat.Node[]>() {\n" +
                                    "@Override\n" +
                                    "public void on(greycat.Node[] nodes) {\n" +
                                    resultType + "[] result = new " + resultType + "[nodes.length];\n" +
                                    "for(int i=0;i<result.length;i++) {\n" +
                                    "result[i] = (" + resultType + ") nodes[i];\n" +
                                    "}\n" +
                                    "callback.on(result);" +
                                    "}\n" +
                                    "});"
                    );
                }

                // relation indexes
                StringBuilder relIdxBuilder = new StringBuilder();
                if (rel.isIndexedRelation()) {
                    for (String att : rel.indexedAttributes()) {
                        relIdxBuilder.append(rel.type() + "." + att.toUpperCase());
                        relIdxBuilder.append(",");
                    }
                    relIdxBuilder.deleteCharAt(relIdxBuilder.length() - 1);
                } else {
                    relIdxBuilder.append("null");
                }


                if (rel.isToOne()) {
                    //generate set
                    StringBuilder addToBodyBuilder = new StringBuilder();
                    MethodSource<JavaClassSource> add = javaClass.addMethod();
                    add.setVisibility(Visibility.PUBLIC).setFinal(true);
                    add.setName("set" + upperCaseFirstChar(prop.name()));
                    add.setReturnType(classClassifier.name());
                    add.addParameter(rel.type(), "value");
                    addToBodyBuilder.append("if(value != null) {");
                    addToBodyBuilder.append("super.removeFromRelation(").append(prop.name().toUpperCase()).append(",(greycat.Node)value );");
                    addToBodyBuilder.append("super.addToRelation(").append(prop.name().toUpperCase()).append(",(greycat.Node)value );");
                    addToBodyBuilder.append("}");
                    addToBodyBuilder.append("return this;");
                    add.setBody(addToBodyBuilder.toString());

                } else {
                    //generate addTo
                    StringBuilder addToBodyBuilder = new StringBuilder();
                    MethodSource<JavaClassSource> add = javaClass.addMethod();
                    add.setVisibility(Visibility.PUBLIC).setFinal(true);
                    add.setName("addTo" + upperCaseFirstChar(prop.name()));
                    add.setReturnType(classClassifier.name());
                    add.addParameter(rel.type(), "value");
                    addToBodyBuilder.append("super.addToRelation(").append(prop.name().toUpperCase()).append(",(greycat.Node)value, " + relIdxBuilder + ");");
                    addToBodyBuilder.append("return this;");
                    add.setBody(addToBodyBuilder.toString());
                }


                if (rel.isToOne()) {
                    //generate remove
                    StringBuilder removeFromBodyBuilder = new StringBuilder();
                    MethodSource<JavaClassSource> remove = javaClass.addMethod();
                    remove.setVisibility(Visibility.PUBLIC).setFinal(true);
                    String refType = upperCaseFirstChar(prop.name());
                    remove.setName("remove" + refType);
                    remove.setReturnTypeVoid();
                    remove.addParameter("greycat.Callback<Boolean>", "callback");
                    removeFromBodyBuilder.append(classClassifier.name() + " self = this;");
                    removeFromBodyBuilder.append("get" + refType + "(new greycat.Callback<" + refType + ">() {");
                    removeFromBodyBuilder.append("@Override\n");
                    removeFromBodyBuilder.append("public void on(" + refType + " result) {");
                    removeFromBodyBuilder.append("self.removeFromRelation(").append(prop.name().toUpperCase()).append(", result);");
                    removeFromBodyBuilder.append("callback.on(true);");
                    removeFromBodyBuilder.append("}");
                    removeFromBodyBuilder.append("});");
                    remove.setBody(removeFromBodyBuilder.toString());
                } else {
                    //generate removeFrom
                    StringBuilder removeFromBodyBuilder = new StringBuilder();
                    MethodSource<JavaClassSource> remove = javaClass.addMethod();
                    remove.setVisibility(Visibility.PUBLIC).setFinal(true);
                    remove.setName("removeFrom" + upperCaseFirstChar(prop.name()));
                    remove.setReturnType(classClassifier.name());
                    remove.addParameter(rel.type(), "value");
                    removeFromBodyBuilder.append("super.removeFromRelation(").append(prop.name().toUpperCase()).append(",(greycat.Node)value, " + relIdxBuilder + ");");
                    removeFromBodyBuilder.append("return this;");
                    remove.setBody(removeFromBodyBuilder.toString());
                }

            }
        }


        // keys
        if (classClassifier.keys().length > 0) {
            StringBuilder indexMethodBody = new StringBuilder();
            StringBuilder unindexMethodBody = new StringBuilder();

            String deferPart = "\t\tfinal " + classClassifier.name() + " self = this;\n" +
                    "\t\tgreycat.DeferCounter deferCounter = this.graph().newCounter(" + classClassifier.keys().length + ");\n" +
                    "\t\tdeferCounter.then(new greycat.plugin.Job() {\n" +
                    "\t\t\t@Override\n" +
                    "\t\t\tpublic void run() {\n" +
                    "\t\t\t\tif (callback != null) {\n" +
                    "\t\t\t\t\tcallback.on(true);\n" +
                    "\t\t\t\t}\n" +
                    "\t\t\t}\n" +
                    "\t\t});\n";

            indexMethodBody.append(deferPart);
            unindexMethodBody.append(deferPart);

            for (Key key : classClassifier.keys()) {
                String keyName = key.name();

                // index constants
                javaClass.addField()
                        .setVisibility(Visibility.PUBLIC)
                        .setFinal(true)
                        .setName(keyName.toUpperCase())
                        .setType(String.class)
                        .setStringInitializer(keyName)
                        .setStatic(true);

                StringBuilder indexedProperties = new StringBuilder();
                for (Property property : key.attributes()) {
                    indexedProperties.append(property.name().toUpperCase());
                    indexedProperties.append(",");
                }
                indexedProperties.deleteCharAt(indexedProperties.length() - 1);
                String time = key.isWithTime() ? "time()" : "greycat.Constants.BEGINNING_OF_TIME";

                indexMethodBody.append(
                        "\t\tthis.graph().index(world(), " + time + "," + keyName.toUpperCase() + " , new greycat.Callback<greycat.NodeIndex>() {\n" +
                                "\t\t\t@Override\n" +
                                "\t\t\tpublic void on(greycat.NodeIndex indexNode) {\n" +
                                "\t\t\t\tindexNode.removeFromIndex(self, " + indexedProperties + " );\n" +
                                "\t\t\t\tindexNode.addToIndex(self," + indexedProperties + ");\n" +
                                "\t\t\t\tdeferCounter.count();\n" +
                                "\t\t\t}\n" +
                                "\t\t});"
                );

                unindexMethodBody.append(
                        "\t\tthis.graph().index(world(), " + time + "," + keyName.toUpperCase() + " , new greycat.Callback<greycat.NodeIndex>() {\n" +
                                "\t\t\t@Override\n" +
                                "\t\t\tpublic void on(greycat.NodeIndex indexNode) {\n" +
                                "\t\t\t\tindexNode.removeFromIndex(self, " + indexedProperties + " );\n" +
                                "\t\t\t\tdeferCounter.count();\n" +
                                "\t\t\t}\n" +
                                "\t\t});"
                );
            }

            javaClass.addMethod()
                    .setName("index" + classClassifier.name())
                    .setVisibility(Visibility.PUBLIC)
                    .setFinal(true)
                    .setReturnTypeVoid()
                    .setBody(indexMethodBody.toString())
                    .addParameter("Callback<Boolean>", "callback");
            javaClass.addImport(Callback.class);

            javaClass.addMethod()
                    .setName("unindex" + classClassifier.name())
                    .setVisibility(Visibility.PUBLIC)
                    .setFinal(true)
                    .setReturnTypeVoid()
                    .setBody(unindexMethodBody.toString())
                    .addParameter("Callback<Boolean>", "callback");
            javaClass.addImport(Callback.class);
        }


        return javaClass;
    }

    private static String upperCaseFirstChar(String init) {
        return init.substring(0, 1).toUpperCase() + init.substring(1);
    }


    private static String typeToString(final Attribute attribute) {
        StringBuilder typeBuilder = new StringBuilder();
        switch (attribute.type()) {
            case "String":
                typeBuilder.append("Type.STRING");
                break;
            case "Double":
                typeBuilder.append("Type.DOUBLE");
                break;
            case "Long":
                typeBuilder.append("Type.LONG");
                break;
            case "Integer":
                typeBuilder.append("Type.INT");
                break;
            case "Boolean":
                typeBuilder.append("Type.BOOL");
                break;
            default:
                throw new RuntimeException("type " + attribute.type() + " is unknown");
        }

        if (attribute.isArray()) {
            typeBuilder.append("_ARRAY");
        }

        return typeBuilder.toString();
    }


    private static String typeToClassName(final Attribute attribute) {
        if (attribute.isArray()) {
            switch (attribute.type()) {
                case "String":
                    return StringArray.class.getName();
                case "Double":
                    return DoubleArray.class.getName();
                case "Long":
                    return LongArray.class.getName();
                case "Integer":
                    return IntArray.class.getName();
                case "Boolean":
                    throw new RuntimeException("boolean arrays are not supported yet!");
                default:
                    throw new RuntimeException("type " + attribute.type() + " is unknown");
            }
        } else {
            switch (attribute.type()) {
                case "String":
                    return String.class.getCanonicalName();
                case "Double":
                    return double.class.getCanonicalName();
                case "Long":
                    return long.class.getCanonicalName();
                case "Integer":
                    return int.class.getCanonicalName();
                case "Boolean":
                    return boolean.class.getCanonicalName();
                default:
                    throw new RuntimeException("type " + attribute.type() + " is unknown");
            }
        }
    }


}
