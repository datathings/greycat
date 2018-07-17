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
                if (value != null) {
                    switch (constant.type()) {
                        case "Task":
                            value = "greycat.Tasks.newTask().parse(\"" + value.replaceAll("\"", "'").trim() + "\",null)";
                            break;
                        case "Long":
                            value = value + "L";
                            break;
                        case "Double":
                            value = value + "d";
                            break;
                        case "String":
                            value = "\"" + value + "\"";
                            break;
                    }
                }
                javaClass.addField(FieldSpec.builder(ParameterizedTypeName.get(gMetaConst, clazz(constant.type())), constant.name().toUpperCase())
                        .addModifiers(PUBLIC, STATIC, FINAL)
                        .initializer("new $T($S, $L,$L, $L)", ParameterizedTypeName.get(gMetaConst, clazz(constant.type())), constant.name(), typeName(constant.type()), hash(constant.name()), value)
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

                if (rel.opposite() != null) {
                    MethodSpec.Builder addTo = MethodSpec.methodBuilder("addTo" + Generator.upperCaseFirstChar(rel.name()))
                            .addModifiers(PUBLIC, FINAL)
                            .addParameter(clazz(rel.type()), "value")
                            .addParameter(ParameterizedTypeName.get(gCallback, ClassName.get(packageName, gType.name())), "callback")
                            .addStatement("$T self = this", ClassName.get(packageName, gType.name()))
                            .addStatement("addToRelationAt($L.hash,value)", rel.name().toUpperCase());
                    addTo.addCode(createAddOppositeBlock(rel.type(), rel, ClassName.get(packageName, gType.name())).build());
                    javaClass.addMethod(addTo.build());
                } else {
                    MethodSpec.Builder addTo = MethodSpec.methodBuilder("addTo" + Generator.upperCaseFirstChar(rel.name()))
                            .addModifiers(PUBLIC, FINAL)
                            .returns(ClassName.get(packageName, gType.name()))
                            .addParameter(clazz(rel.type()), "value")
                            .addStatement("addToRelationAt($L.hash,value)", rel.name().toUpperCase())
                            .addStatement("return this");
                    javaClass.addMethod(addTo.build());
                }

                MethodSpec.Builder removeFrom = MethodSpec.methodBuilder("removeFrom" + Generator.upperCaseFirstChar(rel.name()))
                        .addModifiers(PUBLIC, FINAL)
                        .returns(ClassName.get(packageName, gType.name()))
                        .addParameter(clazz(rel.type()), "value")
                        .addStatement("removeFromRelationAt($L.hash,value)", rel.name().toUpperCase());
                if (rel.opposite() != null) {
                    removeFrom.addCode(createRemoveOppositeBlock(rel.type(), rel).build());
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

                if (ref.opposite() != null) {
                    MethodSpec.Builder addTo = MethodSpec.methodBuilder("set" + Generator.upperCaseFirstChar(ref.name()))
                            .addModifiers(PUBLIC, FINAL)
                            .addParameter(clazz(ref.type()), "value")
                            .addParameter(ParameterizedTypeName.get(gCallback, ClassName.get(packageName, gType.name())), "callback")
                            .addStatement("$T self = this", ClassName.get(packageName, gType.name()))
                            .addStatement("(($T)this.getOrCreateAt($L.hash, greycat.Type.RELATION)).clear().add(value.id())", ClassName.get(greycat.struct.Relation.class), ref.name().toUpperCase());
                    addTo.addCode(createAddOppositeBlock(ref.type(), ref, ClassName.get(packageName, gType.name())).build());
                    javaClass.addMethod(addTo.build());
                } else {
                    MethodSpec.Builder addTo = MethodSpec.methodBuilder("set" + Generator.upperCaseFirstChar(ref.name()))
                            .addModifiers(PUBLIC, FINAL)
                            .returns(ClassName.get(packageName, gType.name()))
                            .addParameter(clazz(ref.type()), "value")
                            .addStatement("(($T)this.getOrCreateAt($L.hash, greycat.Type.RELATION)).clear().add(value.id())", ClassName.get(greycat.struct.Relation.class), ref.name().toUpperCase())
                            .addStatement("return this");
                    javaClass.addMethod(addTo.build());
                }

                MethodSpec.Builder removeFrom = MethodSpec.methodBuilder("removeFrom" + Generator.upperCaseFirstChar(ref.name()))
                        .addModifiers(PUBLIC, FINAL)
                        .returns(ClassName.get(packageName, gType.name()))
                        .addParameter(clazz(ref.type()), "value")
                        .addStatement("removeFromRelationAt($L.hash,value)", ref.name().toUpperCase());
                if (ref.opposite() != null) {
                    removeFrom.addCode(createRemoveOppositeBlock(ref.type(), ref).build());
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
                if (li.opposite() != null) {
                    MethodSpec.Builder indexMethod = MethodSpec.methodBuilder("index" + Generator.upperCaseFirstChar(li.name()))
                            .addModifiers(PUBLIC, FINAL)
                            .addParameter(ClassName.get(packageName, li.type()), "value")
                            .addParameter(ParameterizedTypeName.get(gCallback, ClassName.get(packageName, gType.name())), "callback")

                            .addStatement("$T self = this", ClassName.get(packageName, gType.name()))
                            .addStatement("$T index = ($T) this.getAt($L.hash)", gIndex, gIndex, li.name().toUpperCase())
                            .beginControlFlow("if(index == null)")
                            .addStatement("index = ($T) this.getOrCreateAt($L.hash,greycat.Type.INDEX)", gIndex, li.name().toUpperCase());
                    if (li.attributes().size() > 0) {
                        CodeBlock.Builder declareIndexinit = CodeBlock.builder();
                        li.attributes().forEach(attributeRef -> {
                            declareIndexinit.add(", $T.$L.name", clazz(li.type()), attributeRef.ref().name().toUpperCase());
                        });
                        indexMethod.addStatement("index.declareAttributes(null$L)", declareIndexinit.build());
                    }
                    indexMethod.endControlFlow()
                            .addStatement("index.update(value)")
                            .addCode(createAddOppositeBlock(li.type(), li, ClassName.get(packageName, gType.name())).build());
                    javaClass.addMethod(indexMethod.build());
                } else {
                    MethodSpec.Builder indexMethod = MethodSpec.methodBuilder("index" + Generator.upperCaseFirstChar(li.name()))
                            .addModifiers(PUBLIC, FINAL)
                            .returns(ClassName.get(packageName, gType.name()))
                            .addParameter(ClassName.get(packageName, li.type()), "value")
                            .addStatement("$T index = ($T) this.getAt($L.hash)", gIndex, gIndex, li.name().toUpperCase())
                            .beginControlFlow("if(index == null)")
                            .addStatement("index = ($T) this.getOrCreateAt($L.hash,greycat.Type.INDEX)", gIndex, li.name().toUpperCase());
                    if (li.attributes().size() > 0) {
                        CodeBlock.Builder declareIndexinit = CodeBlock.builder();
                        li.attributes().forEach(attributeRef -> {
                            declareIndexinit.add(", $T.$L.name", clazz(li.type()), attributeRef.ref().name().toUpperCase());
                        });
                        indexMethod.addStatement("index.declareAttributes(null$L)", declareIndexinit.build());
                    }
                    indexMethod.endControlFlow()
                            .addStatement("index.update(value)")
                            .addStatement("return this");
                    javaClass.addMethod(indexMethod.build());
                }

                //UnIndex method
                MethodSpec.Builder unindexMethod = MethodSpec.methodBuilder("unindex" + Generator.upperCaseFirstChar(li.name()))
                        .addModifiers(PUBLIC, FINAL)
                        .returns(ClassName.get(packageName, gType.name()))
                        .addParameter(ClassName.bestGuess(Generator.upperCaseFirstChar(li.type())), "value");

                unindexMethod.addStatement("$T index = ($T) this.getAt($L.hash)", gIndex, gIndex, li.name().toUpperCase());
                unindexMethod.beginControlFlow("if(index != null)");
                unindexMethod.addStatement("index.unindex(value)");
                if (li.opposite() != null) {
                    unindexMethod.addCode(createRemoveOppositeBlock(li.type(), li).build());
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
                findMethod.addStatement("callback.on(new $T[0])", ClassName.get(packageName, li.type()));
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
                        .addStatement("callback.on(new $T[0])", ClassName.get(packageName, li.type()))
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
                        if (annot.value().size() == 2) {
                            offset = Long.parseLong(annot.value().get(1).get(0).toString());
                        }
                        initMethod.addStatement("setTimeSensitivity($L, $L)", parsedSensitivity, offset);
                        break;
                }
            } else if (o instanceof Index) {
                Index idx = (Index) o;
                initMethod.addStatement("$1T $2LIndex = ($1T) this.getOrCreateAt($3L.hash,greycat.Type.INDEX)", gIndex, idx.name(), idx.name().toUpperCase());
                if (idx.attributes().size() > 0) {
                    CodeBlock.Builder declareIndexinit = CodeBlock.builder();
                    idx.attributes().forEach(attributeRef -> {
                        declareIndexinit.add(", $T.$L.name", clazz(idx.type()), attributeRef.ref().name().toUpperCase());
                    });
                    initMethod.addStatement("$LIndex.declareAttributes(null$L)", idx.name(), declareIndexinit.build());
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


    public static CodeBlock.Builder createAddOppositeBlock(String edgeType, Edge edge, ClassName className) {
        CodeBlock.Builder addBlock = CodeBlock.builder();
        if (edge.opposite() != null) {
            if (edge.opposite().edge() instanceof Relation) {
                String oppositeName = edge.opposite().edge().name();
                addBlock.addStatement("value.addToRelation($L.$L.name, $L)", edgeType, oppositeName.toUpperCase(), "this");
                addBlock.beginControlFlow("if(callback != null)");
                addBlock.addStatement("callback.on(self)");
                addBlock.endControlFlow();

            } else if (edge.opposite().edge() instanceof Reference) {
                String oppositeName = edge.opposite().edge().name();
                String metaRelation = edge.name().toUpperCase();

                CodeBlock.Builder removeFromOther = CodeBlock.builder();
                if (edge instanceof Index) {
                    removeFromOther.addStatement("$T index = ($T) result.getAt($L.hash)", gIndex, gIndex, metaRelation)
                            .beginControlFlow("if(index != null)")
                            .addStatement("index.unindex(value)")
                            .endControlFlow();
                } else {
                    removeFromOther.addStatement("result.removeFromRelationAt($L.hash, $L)", metaRelation, "value");
                }

                addBlock.addStatement("value.get$L($L)", Generator.upperCaseFirstChar(oppositeName), TypeSpec.anonymousClassBuilder("")
                        .addSuperinterface(ParameterizedTypeName.get(gCallback, className))
                        .addMethod(MethodSpec.methodBuilder("on")
                                .addAnnotation(Override.class)
                                .addModifiers(PUBLIC)
                                .addParameter(className, "result")
                                .returns(VOID)
                                .beginControlFlow("if(result != null)")
                                .addCode(removeFromOther.build())
                                .endControlFlow()
                                .addStatement("$T rel = value.getRelation($L.$L.name)", gRelation, edgeType, oppositeName.toUpperCase())
                                .beginControlFlow("if(rel != null)")
                                .addStatement("rel.clear()")
                                .endControlFlow()
                                .addStatement("value.addToRelation($L.$L.name, $L)", edgeType, oppositeName.toUpperCase(), "self")
                                .beginControlFlow("if(callback != null)")
                                .addStatement("callback.on(self)")
                                .endControlFlow()
                                .build())
                        .build());

            } else if (edge.opposite().edge() instanceof Index) {
                Index idx = ((Index) edge.opposite().edge());
                String oppositeName = idx.name();
                addBlock.addStatement("$T index = value.getIndex($L.$L.name)", Helper.gIndex, Generator.upperCaseFirstChar(edgeType), oppositeName.toUpperCase());
                addBlock.beginControlFlow("if (index == null)");
                addBlock.addStatement("index = ($T) value.getOrCreate($L.$L.name, $L)", Helper.gIndex, Generator.upperCaseFirstChar(edgeType), oppositeName.toUpperCase(), "greycat.Type.INDEX");
                final StringBuilder params = new StringBuilder();
                idx.attributes().forEach(attributeRef -> params.append(",").append(attributeRef.ref().name().toUpperCase() + ".name"));
                addBlock.addStatement("index.declareAttributes(null $L)", params.toString());
                addBlock.endControlFlow();
                addBlock.addStatement("index.update($L)", "this");
                addBlock.beginControlFlow("if(callback != null)");
                addBlock.addStatement("callback.on(self)");
                addBlock.endControlFlow();
            }
        }
        return addBlock;
    }

    private static CodeBlock.Builder createRemoveOppositeBlock(String edgeType, Edge edge) {
        CodeBlock.Builder removeBlock = CodeBlock.builder();
        removeBlock.addStatement("$T self = this", Helper.gNode);

        if ((edge.opposite().edge() instanceof Relation) || (edge.opposite().edge() instanceof Reference)) {
            String oppositeName = (edge.opposite().edge() instanceof Relation) ? ((Relation) edge.opposite().edge()).name() : ((Reference) edge.opposite().edge()).name();
            removeBlock.addStatement("value.removeFromRelation($L.$L.name,$L)", edgeType, oppositeName.toUpperCase(), "self");

        } else if (edge.opposite().edge() instanceof Index) {
            String oppositeName = ((Index) edge.opposite().edge()).name();
            removeBlock.addStatement("$T index = value.getIndex($L.$L.name)", Helper.gIndex, edgeType, oppositeName.toUpperCase());
            removeBlock.beginControlFlow("if(index != null)");
            removeBlock.addStatement("index.unindex(self)");
            removeBlock.endControlFlow();
        }

        return removeBlock;
    }


}



