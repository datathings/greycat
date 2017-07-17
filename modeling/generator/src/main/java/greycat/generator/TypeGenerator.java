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

import com.squareup.javapoet.*;
import greycat.base.BaseCustomTypeSingle;
import greycat.base.BaseNode;
import greycat.language.*;
import greycat.language.Class;
import greycat.language.Index;
import greycat.language.Relation;
import greycat.language.Type;
import greycat.struct.*;

import java.util.List;

import static greycat.generator.Helper.*;
import static com.squareup.javapoet.TypeName.*;
import static greycat.utility.HashHelper.hash;
import static javax.lang.model.element.Modifier.*;

class TypeGenerator {

    static void generate(String packageName, Model model, List<JavaFile> collector) {
        for (int i = 0; i < model.classes().length; i++) {
            generateClass(packageName, model.classes()[i], collector);
        }
        for (int i = 0; i < model.customTypes().length; i++) {
            generateClass(packageName, model.customTypes()[i], collector);
        }
    }

    private static void generateClass(String packageName, Type gType, List<JavaFile> collector) {
        if (gType.name().equals("Node") || gType.name().equals("greycat.Node")) {
            return;
        }
        TypeSpec.Builder javaClass = TypeSpec.classBuilder(gType.name());
        javaClass.addModifiers(PUBLIC);
        if (gType.parent() != null) {
            javaClass.superclass(ClassName.get(packageName, gType.parent().name()));
        } else {
            if (gType instanceof CustomType) {
                javaClass.superclass(ClassName.get(BaseCustomTypeSingle.class));
            } else {
                javaClass.superclass(ClassName.get(BaseNode.class));
            }
        }
        CodeBlock.Builder tsGetSet = CodeBlock.builder();
        tsGetSet.addStatement("{@extend ts");
        gType.properties().forEach(o -> {
            if (o instanceof Attribute) {
                Attribute attribute = (Attribute) o;
                if (isPrimitive(attribute.type())) {
                    tsGetSet.addStatement("get $L(): $L { return this.get$L();}", attribute.name(), classTsName(attribute.type()), Generator.upperCaseFirstChar(attribute.name()));
                    tsGetSet.addStatement("set $L(p: $L){ this.set$L(p);}", attribute.name(), classTsName(attribute.type()), Generator.upperCaseFirstChar(attribute.name()));
                }
            }
        });
        tsGetSet.addStatement("}");
        javaClass.addJavadoc(tsGetSet.build());

        // constructor
        if (gType instanceof Class) {
            javaClass.addMethod(MethodSpec.constructorBuilder()
                    .addModifiers(PUBLIC)
                    .addParameter(LONG, "p_world")
                    .addParameter(LONG, "p_time")
                    .addParameter(LONG, "p_id")
                    .addParameter(gGraph, "p_graph")
                    .addStatement("super(p_world, p_time, p_id, p_graph)")
                    .build());
        } else if (gType instanceof CustomType) {
            javaClass.addMethod(MethodSpec.constructorBuilder()
                    .addModifiers(PUBLIC)
                    .addParameter(ClassName.get(EStructArray.class), "e")
                    .addStatement("super(e)")
                    .build());
        }

        // init method
        if (gType instanceof Class) {
            javaClass.addMethod(MethodSpec.methodBuilder("create")
                    .addModifiers(PUBLIC, STATIC)
                    .addParameter(LONG, "p_world")
                    .addParameter(LONG, "p_time")
                    .addParameter(gGraph, "p_graph")
                    .returns(ClassName.get(packageName, gType.name()))
                    .addStatement("return ($1T) p_graph.newTypedNode(p_world, p_time, $1T.META.name)", ClassName.get(packageName, gType.name()))
                    .build());
        }
        javaClass.addField(FieldSpec.builder(gMeta, "META")
                .addModifiers(PUBLIC, STATIC, FINAL)
                .initializer("new $T($S,$L,$L)", gMeta, gType.name(), hash(gType.name()), hash(gType.name()))
                .build());

        // properties
        gType.properties().forEach(o -> {
            // constants
            if (o instanceof Constant) {
                Constant constant = (Constant) o;
                String value = constant.value();
                if (constant.type().equals("Task")) {
                    if (value != null) {
                        value = "greycat.Tasks.newTask().parse(\"" + value.replaceAll("\"", "'").trim() + "\",null)";
                    }
                } else if (constant.type().equals("String") && value != null) {
                    value = "\"" + value + "\"";
                }
                javaClass.addField(FieldSpec.builder(gMetaConst, constant.name().toUpperCase())
                        .addModifiers(PUBLIC, STATIC, FINAL)
                        .initializer("new $T($S, $L,$L, $L)", gMetaConst, constant.name(), typeName(constant.type()), hash(constant.name()), value)
                        .build());
            } else if (o instanceof Attribute) {
                final Attribute att = (Attribute) o;
                javaClass.addField(FieldSpec.builder(gMeta, att.name().toUpperCase())
                        .addModifiers(PUBLIC, STATIC, FINAL)
                        .initializer("new $T($S, $L,$L)", gMeta, att.name(), typeName(att.type()), hash(att.name()))
                        .build());

                javaClass.addMethod(MethodSpec.methodBuilder("get" + Generator.upperCaseFirstChar(att.name()))
                        .addModifiers(PUBLIC, FINAL)
                        .returns(clazz(att.type()))
                        .addStatement("return ($T) super.getAt($L.hash)", clazz(att.type()), att.name().toUpperCase())
                        .build());

                if (isPrimitive(att.type())) {
                    javaClass.addMethod(MethodSpec.methodBuilder("set" + Generator.upperCaseFirstChar(att.name()))
                            .addModifiers(PUBLIC, FINAL)
                            .addParameter(clazz(att.type()), "value")
                            .returns(clazz(gType.name()))
                            .addStatement("setAt($1L.hash,$1L.type, value)", att.name().toUpperCase())
                            .addStatement("return this")
                            .build());
                } else {
                    javaClass.addMethod(MethodSpec.methodBuilder("getOrCreate" + Generator.upperCaseFirstChar(att.name()))
                            .addModifiers(PUBLIC, FINAL)
                            .returns(clazz(att.type()))
                            .addStatement("return ($T) super.getOrCreateAt($L.hash,$L.type)", clazz(att.type()), att.name().toUpperCase(), att.name().toUpperCase())
                            .build());
                }
            } else if (o instanceof Relation) {
                Relation rel = (Relation) o;
                String metaRelation = rel.name().toUpperCase();
                String resultType = rel.type();
                javaClass.addField(FieldSpec.builder(gMeta, metaRelation)
                        .addModifiers(PUBLIC, STATIC, FINAL)
                        .initializer("new $T($S, $L, $L)", gMeta, rel.name(), "greycat.Type.RELATION", hash(rel.name()))
                        .build());

                javaClass.addMethod(MethodSpec.methodBuilder("get" + Generator.upperCaseFirstChar(rel.name()))
                        .addModifiers(PUBLIC, FINAL)
                        .returns(VOID)
                        .addParameter(ParameterizedTypeName.get(gCallback, ArrayTypeName.of(ClassName.get(packageName, resultType))), "callback")
                        .addStatement("traverseAt($L.hash, $L)", metaRelation, TypeSpec.anonymousClassBuilder("")
                                .addSuperinterface(ParameterizedTypeName.get(gCallback, gNodeArray))
                                .addMethod(MethodSpec.methodBuilder("on")
                                        .addAnnotation(Override.class)
                                        .addModifiers(PUBLIC)
                                        .addParameter(gNodeArray, "result")
                                        .returns(VOID)
                                        .beginControlFlow("if(result != null)")
                                        .addStatement("$1T[] typedResult = new $1T[result.length]", ClassName.get(packageName, resultType))
                                        .addStatement("$T.arraycopy(result, 0, typedResult, 0, result.length)", jlSystem)
                                        .addStatement("callback.on(typedResult)")
                                        .nextControlFlow("else")
                                        .addStatement("callback.on(new $T[0])", ClassName.get(packageName, resultType))
                                        .endControlFlow()
                                        .build())
                                .build())
                        .build());

                MethodSpec.Builder addTo = MethodSpec.methodBuilder("addTo" + Generator.upperCaseFirstChar(rel.name()))
                        .addModifiers(PUBLIC, FINAL)
                        .returns(ClassName.get(packageName, gType.name()))
                        .addParameter(clazz(rel.type()), "value")
                        .addStatement("addToRelationAt($L.hash,value)", rel.name().toUpperCase());
                if (rel.opposite() != null) {
                    addTo.addStatement(createAddOppositeBody(rel.type(), rel).toString());
                }
                addTo.addStatement("return this");
                javaClass.addMethod(addTo.build());

                MethodSpec.Builder removeFrom = MethodSpec.methodBuilder("removeFrom" + Generator.upperCaseFirstChar(rel.name()))
                        .addModifiers(PUBLIC, FINAL)
                        .returns(ClassName.get(packageName, gType.name()))
                        .addParameter(clazz(rel.type()), "value")
                        .addStatement("removeFromRelationAt($L.hash,value)", rel.name().toUpperCase());
                if (rel.opposite() != null) {
                    removeFrom.addStatement(createRemoveOppositeBody(rel.type(), rel).toString());
                }
                removeFrom.addStatement("return this");
                javaClass.addMethod(removeFrom.build());

            } else if (o instanceof Reference) {
                Reference ref = (Reference) o;
                TypeName referenceType = clazz(ref.type());
                javaClass.addField(FieldSpec.builder(gMeta, ref.name().toUpperCase())
                        .addModifiers(PUBLIC, STATIC, FINAL)
                        .initializer("new $T($S, $L, $L)", gMeta, ref.name(), "greycat.Type.RELATION", hash(ref.name()))
                        .build());

                javaClass.addMethod(MethodSpec.methodBuilder("get" + Generator.upperCaseFirstChar(ref.name()))
                        .addModifiers(PUBLIC, FINAL)
                        .returns(VOID)
                        .addParameter(ParameterizedTypeName.get(gCallback, referenceType), "callback")
                        .addStatement("traverseAt($L.hash, $L)", ref.name().toUpperCase(), TypeSpec.anonymousClassBuilder("")
                                .addSuperinterface(ParameterizedTypeName.get(gCallback, gNodeArray))
                                .addMethod(MethodSpec.methodBuilder("on")
                                        .addAnnotation(Override.class)
                                        .addModifiers(PUBLIC)
                                        .addParameter(gNodeArray, "result")
                                        .returns(VOID)
                                        .beginControlFlow("if(result != null && result.length == 1)")
                                        .addStatement("callback.on(($T)result[0])", referenceType)
                                        .nextControlFlow("else")
                                        .addStatement("callback.on(null)")
                                        .endControlFlow()
                                        .build())
                                .build())
                        .build());

                MethodSpec.Builder addTo = MethodSpec.methodBuilder("set" + Generator.upperCaseFirstChar(ref.name()))
                        .addModifiers(PUBLIC, FINAL)
                        .returns(ClassName.get(packageName, gType.name()))
                        .addParameter(clazz(ref.type()), "value")
                        .addStatement("(($T)this.getOrCreateAt($L.hash, greycat.Type.RELATION)).clear().add(value.id())", ClassName.get(greycat.struct.Relation.class), ref.name().toUpperCase());
                if (ref.opposite() != null) {
                    addTo.addStatement(createAddOppositeBody(ref.type(), ref).toString());
                }
                addTo.addStatement("return this");
                javaClass.addMethod(addTo.build());

                MethodSpec.Builder removeFrom = MethodSpec.methodBuilder("removeFrom" + Generator.upperCaseFirstChar(ref.name()))
                        .addModifiers(PUBLIC, FINAL)
                        .returns(ClassName.get(packageName, gType.name()))
                        .addParameter(clazz(ref.type()), "value")
                        .addStatement("removeFromRelationAt($L.hash,value)", ref.name().toUpperCase());
                if (ref.opposite() != null) {
                    removeFrom.addStatement(createRemoveOppositeBody(ref.type(), ref).toString());
                }
                removeFrom.addStatement("return this");
                javaClass.addMethod(removeFrom.build());

            } else if (o instanceof Index) {
                greycat.language.Index li = (greycat.language.Index) o;

                //Index field
                javaClass.addField(FieldSpec.builder(gMeta, li.name().toUpperCase())
                        .addModifiers(PUBLIC, STATIC, FINAL)
                        .initializer("new $T($S, $L, $L)", gMeta, li.name(), "greycat.Type.INDEX", hash(li.name()))
                        .build());

                //Index method
                MethodSpec.Builder indexMethod = MethodSpec.methodBuilder("index" + Generator.upperCaseFirstChar(li.name()))
                        .addModifiers(PUBLIC, FINAL)
                        .returns(ClassName.get(packageName, gType.name()))
                        .addParameter(ClassName.get(packageName, li.type()), "value");

                StringBuilder indexedAttBuilder = new StringBuilder();
                for (AttributeRef attRef : li.attributes()) {
                    indexedAttBuilder.append(li.type() + "." + attRef.ref().name().toUpperCase() + ".name");
                    indexedAttBuilder.append(",");
                }
                indexedAttBuilder.deleteCharAt(indexedAttBuilder.length() - 1);

                indexMethod.addStatement("$T index = ($T) this.getAt($L.hash)", gIndex, gIndex, li.name().toUpperCase());
                indexMethod.beginControlFlow("if(index == null)");
                indexMethod.addStatement("index = ($T) this.getOrCreateAt($L.hash,greycat.Type.INDEX)", gIndex, li.name().toUpperCase());
                indexMethod.addStatement("index.declareAttributes(null, $L)", indexedAttBuilder.toString());
                indexMethod.endControlFlow();
                indexMethod.addStatement("index.update(value)");
                if (li.opposite() != null) {
                    indexMethod.addCode(createAddOppositeBody(li.type(), li).toString());
                }
                indexMethod.addStatement("return this");
                javaClass.addMethod(indexMethod.build());

                //UnIndex method
                MethodSpec.Builder unindexMethod = MethodSpec.methodBuilder("unindex" + Generator.upperCaseFirstChar(li.name()))
                        .addModifiers(PUBLIC, FINAL)
                        .returns(ClassName.get(packageName, gType.name()))
                        .addParameter(ClassName.bestGuess(Generator.upperCaseFirstChar(li.type())), "value");

                unindexMethod.addStatement("$T index = ($T) this.getAt($L.hash)", gIndex, gIndex, li.name().toUpperCase());
                unindexMethod.beginControlFlow("if(index != null)");
                unindexMethod.addStatement("index.unindex(value)");
                if (li.opposite() != null) {
                    unindexMethod.addCode(createRemoveOppositeBody(li.type(), li).toString());
                }
                unindexMethod.endControlFlow();
                unindexMethod.addStatement("return this");
                javaClass.addMethod(unindexMethod.build());

                //Find method
                MethodSpec.Builder findMethod = MethodSpec.methodBuilder("find" + Generator.upperCaseFirstChar(li.name()))
                        .addModifiers(PUBLIC, FINAL)
                        .returns(VOID);
                for (AttributeRef indexedAtt : li.attributes()) {
                    findMethod.addParameter(ClassName.get(String.class), indexedAtt.ref().name());
                }
                findMethod.addParameter(ParameterizedTypeName.get(gCallback, ArrayTypeName.of(ClassName.get(packageName, li.type()))), "callback");
                findMethod.addStatement("$T index = ($T) this.getAt($L.hash)", gIndex, gIndex, li.name().toUpperCase());
                findMethod.beginControlFlow("if(index != null)");

                TypeSpec findCallback = TypeSpec.anonymousClassBuilder("")
                        .addSuperinterface(ParameterizedTypeName.get(gCallback, gNodeArray))
                        .addMethod(MethodSpec.methodBuilder("on")
                                .addAnnotation(Override.class)
                                .addModifiers(PUBLIC)
                                .addParameter(gNodeArray, "result")
                                .returns(VOID)
                                .addStatement("$1T[] typedResult = new $1T[result.length]", ClassName.get(packageName, li.type()))
                                .addStatement("$T.arraycopy(result, 0, typedResult, 0, result.length)", jlSystem)
                                .addStatement("callback.on(typedResult)")
                                .build())
                        .build();

                final StringBuilder params = new StringBuilder();
                li.attributes().forEach(attributeRef -> params.append(",").append(attributeRef.ref().name()));
                findMethod.addStatement("index.find($L, this.world(), this.time() $L)", findCallback, params.toString());
                findMethod.nextControlFlow("else");
                findMethod.addStatement("callback.on(null)");
                findMethod.endControlFlow();
                javaClass.addMethod(findMethod.build());

                //Find all method
                javaClass.addMethod(MethodSpec.methodBuilder("findAll" + Generator.upperCaseFirstChar(li.name()))
                        .addModifiers(PUBLIC, FINAL)
                        .addParameter(ParameterizedTypeName.get(gCallback, ArrayTypeName.of(ClassName.get(packageName, li.type()))), "callback")
                        .returns(VOID)
                        .addStatement("$T index = ($T) this.getAt($L.hash)", gIndex, gIndex, li.name().toUpperCase())
                        .beginControlFlow("if(index != null)")
                        .addStatement("index.find($L, this.world(), this.time())",
                                TypeSpec.anonymousClassBuilder("")
                                        .addSuperinterface(ParameterizedTypeName.get(gCallback, gNodeArray))
                                        .addMethod(MethodSpec.methodBuilder("on")
                                                .addAnnotation(Override.class)
                                                .addModifiers(PUBLIC)
                                                .addParameter(gNodeArray, "result")
                                                .returns(VOID)
                                                .addStatement("$1T[] typedResult = new $1T[result.length]", ClassName.get(packageName, li.type()))
                                                .addStatement("$T.arraycopy(result, 0, typedResult, 0, result.length)", jlSystem)
                                                .addStatement("callback.on(typedResult)")
                                                .build())
                                        .build())
                        .nextControlFlow("else")
                        .addStatement("callback.on(null)")
                        .endControlFlow().build());
            }
        });

        //Init method
        MethodSpec.Builder initMethod = MethodSpec.methodBuilder("init")
                .addAnnotation(Override.class)
                .returns(VOID)
                .addModifiers(PUBLIC);
        gType.allProperties().forEach((s, o) -> {
            if (o instanceof Attribute) {
                final Attribute att = (Attribute) o;
                if (att.value() != null) {
                    if (isPrimitive(att.type())) {
                        if (att.type().equals("String")) {
                            initMethod.addStatement("setAt($L.hash,$L.type, $S)", att.name().toUpperCase(), att.name().toUpperCase(), att.value().get(0).get(0));
                        } else {
                            initMethod.addStatement("setAt($1L.hash,$1L.type, $2L)", att.name().toUpperCase(), att.value().get(0).get(0));
                        }

                    } else if (isPrimitiveArray(att.type())) {
                        final CodeBlock.Builder paramBuilder = CodeBlock.builder();
                        att.value().forEach(objects -> {
                            switch (att.type()) {
                                case "StringArray":
                                    paramBuilder.add(".addElement($S)", objects.get(0));
                                    break;
                                default:
                                    paramBuilder.add(".addElement($L)", objects.get(0));
                            }
                        });
                        initMethod.addStatement("(($T)getOrCreateAt($L.hash,$L.type))$L", clazz(att.type()), att.name().toUpperCase(), att.name().toUpperCase(), paramBuilder.build());
                    } else if (isMap(att.type())) {
                        final CodeBlock.Builder paramBuilder = CodeBlock.builder();
                        att.value().forEach(objects -> {
                            switch (att.type()) {
                                case "StringToIntMap":
                                    paramBuilder.add(".put($S,$L)", objects.get(0), objects.get(1));
                                    break;
                                case "IntToStringMap":
                                    paramBuilder.add(".put($L,$S)", objects.get(0), objects.get(1));
                                    break;
                                default:
                                    paramBuilder.add(".put($L,$L)", objects.get(0), objects.get(1));
                            }
                        });
                        initMethod.addStatement("(($T)getOrCreateAt($L.hash,$L.type))$L", clazz(att.type()), att.name().toUpperCase(), att.name().toUpperCase(), paramBuilder.build());
                    } else if (isMatrix(att.type())) {
                        final CodeBlock.Builder paramBuilder = CodeBlock.builder();
                        att.value().forEach(objects -> paramBuilder.add(".add($L,$L,$L)", objects.get(0), objects.get(1), objects.get(2)));
                        initMethod.addStatement("(($T)getOrCreateAt($L.hash,$L.type))$L", clazz(att.type()), att.name().toUpperCase(), att.name().toUpperCase(), paramBuilder.build());
                    }
                }
            } else if (o instanceof Annotation) {
                final Annotation annot = (Annotation) o;
                switch (annot.name()) {
                    case "timeSensitivity":
                        long parsedSensitivity = Long.parseLong(annot.value().get(0).get(0).toString());
                        long offset = 0;
                        if (annot.value().get(0).size() == 2) {
                            offset = Long.parseLong(annot.value().get(0).get(1).toString());
                        }
                        initMethod.addStatement("setTimeSensitivity($L, $L)", parsedSensitivity, offset);
                        break;
                }
            }
        });
        javaClass.addMethod(initMethod.build());

        //TypeAt method
        MethodSpec.Builder typeAtMethod = MethodSpec.methodBuilder("typeAt")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(INT, "index")
                .returns(INT);
        final boolean[] isFirst = {true};
        gType.allProperties().forEach((s, o) -> {
            if (o instanceof Constant) {
                Constant constant = (Constant) o;
                if (isFirst[0]) {
                    typeAtMethod.beginControlFlow("if(index == $L)", hash(constant.name()));
                    isFirst[0] = false;
                } else {
                    typeAtMethod.nextControlFlow("else if(index == $L)", hash(constant.name()));
                }
                typeAtMethod.addStatement("return $L.type", constant.name().toUpperCase());
            }
        });
        if (!isFirst[0]) {
            typeAtMethod.endControlFlow();
        }
        typeAtMethod.addStatement("return super.typeAt(index)");
        javaClass.addMethod(typeAtMethod.build());

        //GetAt method
        MethodSpec.Builder getAtMethod = MethodSpec.methodBuilder("getAt")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(INT, "index")
                .returns(OBJECT);
        isFirst[0] = true;
        gType.allProperties().forEach((s, o) -> {
            if (o instanceof Constant) {
                Constant constant = (Constant) o;
                if (isFirst[0]) {
                    getAtMethod.beginControlFlow("if(index == $L)", hash(constant.name()));
                    isFirst[0] = false;
                } else {
                    getAtMethod.nextControlFlow("else if(index == $L)", hash(constant.name()));
                }
                getAtMethod.addStatement("return $L.value", constant.name().toUpperCase());
            }
        });
        if (!isFirst[0]) {
            getAtMethod.endControlFlow();
        }
        getAtMethod.addStatement("return super.getAt(index)");
        javaClass.addMethod(getAtMethod.build());

        collector.add(JavaFile.builder(packageName, javaClass.build()).build());
    }

    //TODO refactoring below using JavaPoet addStatement method
    private static StringBuilder createAddOppositeBody(String edgeType, Edge edge) {
        StringBuilder oppositeBodyBuilder = new StringBuilder();
        String oppositeName;
        if (edge.opposite() != null) {
            if (edge.opposite().edge() instanceof Relation) {
                oppositeName = ((Relation) edge.opposite().edge()).name();
                oppositeBodyBuilder.append("value.addToRelation(").append(edgeType).append(".").append(oppositeName.toUpperCase()).append(".name, this);");

            } else if (edge.opposite().edge() instanceof Reference) {
                oppositeName = ((Reference) edge.opposite().edge()).name();
                oppositeBodyBuilder.append("value.removeFromRelation(").append(edgeType).append(".").append(oppositeName.toUpperCase()).append(".name, this);");
                oppositeBodyBuilder.append("value.addToRelation(").append(edgeType).append(".").append(oppositeName.toUpperCase()).append(".name, this);");
            } else if (edge.opposite().edge() instanceof Index) {
                Index idx = ((Index) edge.opposite().edge());
                oppositeName = idx.name();
                oppositeBodyBuilder.append("greycat.Index index = value.getIndex(").
                        append(Generator.upperCaseFirstChar(edgeType)).append(".").append(oppositeName.toUpperCase()).append(".name);");
                oppositeBodyBuilder.append("if (index == null) {");
                oppositeBodyBuilder.append("index = (greycat.Index) value.getOrCreate(").
                        append(Generator.upperCaseFirstChar(edgeType)).append(".").append(oppositeName.toUpperCase()).append(".name, greycat.Type.INDEX);");
                StringBuilder indexedAttBuilder = new StringBuilder();
                for (AttributeRef attRef : idx.attributes()) {
                    indexedAttBuilder.append(idx.type() + "." + attRef.ref().name().toUpperCase() + ".name");
                    indexedAttBuilder.append(",");
                }
                indexedAttBuilder.deleteCharAt(indexedAttBuilder.length() - 1);
                oppositeBodyBuilder.append("index.declareAttributes(null, " + indexedAttBuilder.toString() + ");");
                oppositeBodyBuilder.append("}");
                oppositeBodyBuilder.append("index.update(").append("this").append(");");
            }
        }
        return oppositeBodyBuilder;
    }

    private static StringBuilder createRemoveOppositeBody(String edgeType, Edge edge) {
        StringBuilder oppositeBodyBuilder = new StringBuilder();
        oppositeBodyBuilder.append("Node self = this;\n");
        String oppositeName;
        if ((edge.opposite().edge() instanceof Relation) || (edge.opposite().edge() instanceof Reference)) {
            oppositeName = (edge.opposite().edge() instanceof Relation) ? ((Relation) edge.opposite().edge()).name() : ((Reference) edge.opposite().edge()).name();
            oppositeBodyBuilder.append("value.removeFromRelation(").append(edgeType).append(".").append(oppositeName.toUpperCase()).append(".name, self);");

        } else if (edge.opposite().edge() instanceof Index) {
            oppositeName = ((Index) edge.opposite().edge()).name();
            oppositeBodyBuilder.append("greycat.Index index = value.getIndex(").append(edgeType).append(".").append(oppositeName.toUpperCase()).append(".name);");
            oppositeBodyBuilder.append(" if (index != null) {");
            oppositeBodyBuilder.append("index.unindex(self);");
            oppositeBodyBuilder.append("}");
        }
        return oppositeBodyBuilder;
    }


}



