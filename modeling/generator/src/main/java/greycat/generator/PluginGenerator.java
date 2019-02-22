/**
 * Copyright 2017-2019 The GreyCat Authors.  All rights reserved.
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
import greycat.*;
import greycat.language.*;
import greycat.language.Class;
import greycat.plugin.Job;
import greycat.plugin.NodeFactory;
import greycat.plugin.Plugin;
import greycat.plugin.TypeFactory;
import greycat.struct.EStructArray;

import java.util.List;
import java.util.stream.Collectors;

import static greycat.generator.Helper.*;
import static com.squareup.javapoet.TypeName.*;
import static javax.lang.model.element.Modifier.*;

class PluginGenerator {

    static void generate(final String packageName, final String pluginName, final Model model, final List<JavaFile> collector) {
        TypeSpec.Builder javaClass = TypeSpec.classBuilder(pluginName);
        javaClass.addModifiers(PUBLIC);
        javaClass.addSuperinterface(ClassName.get(Plugin.class));

        javaClass.addField(FieldSpec.builder(TypeName.BOOLEAN, "_doInitialization")
                .addModifiers(PRIVATE)
                .initializer("$L", "true")
                .build());

        javaClass.addMethod(MethodSpec.methodBuilder("doNotInitialize")
                .addModifiers(PUBLIC, FINAL)
                .addStatement("this._doInitialization = false")
                .addStatement("return this")
                .returns(ClassName.get(packageName, pluginName))
                .build());

        javaClass.addMethod(MethodSpec.methodBuilder("stop")
                .addModifiers(PUBLIC, FINAL)
                .addAnnotation(Override.class)
                .build());

        MethodSpec.Builder startMethod = MethodSpec.methodBuilder("start")
                .addModifiers(PUBLIC, FINAL)
                .addParameter(gGraph, "graph")
                .addAnnotation(Override.class);
        //Register NodeTypes
        for (Class aClass : model.classes()) {
            startMethod.addStatement("graph.nodeRegistry().getOrCreateDeclaration($L.META.name).setFactory($L)", aClass.name(), TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(ClassName.get(NodeFactory.class))
                    .addMethod(MethodSpec.methodBuilder("create")
                            .addAnnotation(Override.class)
                            .addModifiers(PUBLIC)
                            .addParameter(LONG, "world")
                            .addParameter(LONG, "time")
                            .addParameter(LONG, "id")
                            .addParameter(gGraph, "graph")
                            .returns(gNode)
                            .addStatement("return new $T(world,time,id,graph)", ClassName.get(packageName, aClass.name()))
                            .build())
                    .build());
        }
        //Register CustomTypes
        for (CustomType aType : model.customTypes()) {
            startMethod.addStatement("graph.typeRegistry().getOrCreateDeclaration($L.META.name).setFactory($L)", aType.name(), TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(ClassName.get(TypeFactory.class))
                    .addMethod(MethodSpec.methodBuilder("wrap")
                            .addAnnotation(Override.class)
                            .addModifiers(PUBLIC)
                            .addParameter(ClassName.get(EStructArray.class), "backend")
                            .returns(OBJECT)
                            .addStatement("return new $T(backend)", ClassName.get(packageName, aType.name()))
                            .build())
                    .build());
        }
        //Declare indexes
        if (!model.indexes().isEmpty()) {
            MethodSpec.Builder onMethod = MethodSpec.methodBuilder("on")
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(ParameterizedTypeName.get(gCallback, BOOLEAN.box()), "endIndexes")
                    .returns(VOID)
                    .addStatement("$T waiter = graph.newCounter($L)", ClassName.get(DeferCounter.class), model.indexes().size());
            model.indexes().forEach(index -> {
                CodeBlock.Builder param = CodeBlock.builder();
                index.attributes().forEach(attributeRef -> param.add(",$T.$L.name", ClassName.get(packageName, (attributeRef.ref().parent()).name()), attributeRef.ref().name().toUpperCase()));
                onMethod.addStatement("graph.declareIndex(0,$L.META.name,$L$L)", index.name(), TypeSpec.anonymousClassBuilder("")
                        .addSuperinterface(ParameterizedTypeName.get(gCallback, gNodeIndex))
                        .addMethod(MethodSpec.methodBuilder("on")
                                .addAnnotation(Override.class)
                                .addModifiers(PUBLIC)
                                .addParameter(gNodeIndex, "idx")
                                .addStatement("idx.free()")
                                .addStatement("waiter.count()")
                                .returns(VOID).build())
                        .build(), param.build());
            });


            MethodSpec.Builder waiterRun = MethodSpec.methodBuilder("run")
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .returns(VOID);
            for (Class aClass : model.classes()) {
                waiterRun.addStatement("graph.resolver().stringToHash($L.META.name,true)", aClass.name());
                aClass.properties.forEach((propName, value) -> {
                    if (!(value instanceof Annotation)) {
                        waiterRun.addStatement("graph.resolver().stringToHash($L.$L.name,true)", aClass.name(), propName.toUpperCase());
                    }
                });

            }
            for (CustomType aType : model.customTypes()) {
                waiterRun.addStatement("graph.resolver().stringToHash($L.META.name,true)", aType.name());
                aType.properties.forEach((propName, value) -> {
                    if (!(value instanceof Annotation)) {
                        waiterRun.addStatement("graph.resolver().stringToHash($L.$L.name,true)", aType.name(), propName.toUpperCase());
                    }
                });
            }
            waiterRun.addStatement("graph.save(endIndexes)");
            //.addStatement("endIndexes.on(true)")

            onMethod.addStatement("waiter.then($L)", TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(ClassName.get(Job.class))
                    .addMethod(waiterRun.build())
                    .build());

            startMethod
                    .beginControlFlow("if (this._doInitialization)")
                    .addStatement("graph.addConnectHook($L)", TypeSpec.anonymousClassBuilder("")
                            .addSuperinterface(ParameterizedTypeName.get(gCallback, ParameterizedTypeName.get(gCallback, BOOLEAN.box())))
                            .addMethod(onMethod.build())
                            .build())
                    .endControlFlow();

        }
        javaClass.addMethod(startMethod.build());

        collector.add(JavaFile.builder(packageName, javaClass.build()).build());
    }

}