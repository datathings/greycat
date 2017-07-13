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
import greycat.*;
import greycat.language.*;
import greycat.language.Class;
import greycat.plugin.Job;
import greycat.plugin.NodeFactory;
import greycat.plugin.Plugin;
import greycat.plugin.TypeFactory;
import greycat.struct.EStructArray;

import java.util.List;

import static greycat.generator.Helper.*;
import static com.squareup.javapoet.TypeName.*;
import static javax.lang.model.element.Modifier.*;

class PluginGenerator {

    static void generate(final String packageName, final String pluginName, final Model model, final List<JavaFile> collector) {
        TypeSpec.Builder javaClass = TypeSpec.classBuilder(pluginName);
        javaClass.addModifiers(PUBLIC);
        javaClass.addSuperinterface(ClassName.get(Plugin.class));

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
                onMethod.addStatement("graph.declareIndex(0,$L.META.name,$L)", index.name(), TypeSpec.anonymousClassBuilder("")
                        .addSuperinterface(ParameterizedTypeName.get(gCallback, gNodeIndex))
                        .addMethod(MethodSpec.methodBuilder("on")
                                .addAnnotation(Override.class)
                                .addModifiers(PUBLIC)
                                .addParameter(gNodeIndex, "idx")
                                .addStatement("idx.free()")
                                .addStatement("waiter.count()")
                                .returns(VOID).build())
                        .build());
            });
            onMethod.addStatement("waiter.then($L)", TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(ClassName.get(Job.class))
                    .addMethod(MethodSpec.methodBuilder("run")
                            .addAnnotation(Override.class)
                            .addModifiers(PUBLIC)
                            .addStatement("endIndexes.on(true)")
                            .returns(VOID).build())
                    .build());
            startMethod.addStatement("graph.addConnectHook($L)", TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(ParameterizedTypeName.get(gCallback, ParameterizedTypeName.get(gCallback, BOOLEAN.box())))
                    .addMethod(onMethod.build())
                    .build());
        }
        javaClass.addMethod(startMethod.build());

        collector.add(JavaFile.builder(packageName, javaClass.build()).build());
    }

}
