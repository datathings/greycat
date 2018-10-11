/**
 * Copyright 2017-2018 The GreyCat Authors.  All rights reserved.
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
import greycat.language.Index;
import greycat.language.Model;
import greycat.utility.HashHelper;

import java.util.List;

import static greycat.generator.Helper.*;
import static com.squareup.javapoet.TypeName.*;
import static javax.lang.model.element.Modifier.*;

class IndexGenerator {

    static void generate(String packageName, Model model, List<JavaFile> collector) {
        model.indexes().forEach(index -> generate(packageName, index, collector));
    }

    static void generate(final String packageName, final Index index, final List<JavaFile> collector) {
        TypeSpec.Builder javaClass = TypeSpec.classBuilder(index.name());
        javaClass.addModifiers(PUBLIC);
        //Meta field
        javaClass.addField(FieldSpec.builder(gMeta, "META")
                .addModifiers(PUBLIC, STATIC, FINAL)
                .initializer("new $T($S,$L,$L)", gMeta, index.name(), Helper.typeName("Index"), HashHelper.hash(index.name()))
                .build());
        //FindAll method
        javaClass.addMethod(MethodSpec.methodBuilder("findAll")
                .addModifiers(PUBLIC, STATIC, FINAL)
                .returns(TypeName.VOID)
                .addParameter(gGraph, "graph")
                .addParameter(LONG, "world")
                .addParameter(LONG, "time")
                .addParameter(ParameterizedTypeName.get(gCallback, ArrayTypeName.of(ClassName.get(packageName, index.type()))), "callback")
                .addStatement("graph.index(world, time, META.name, $L)", TypeSpec.anonymousClassBuilder("")
                        .addSuperinterface(ParameterizedTypeName.get(gCallback, gNodeIndex))
                        .addMethod(MethodSpec.methodBuilder("on")
                                .addAnnotation(Override.class)
                                .addModifiers(PUBLIC)
                                .addParameter(gNodeIndex, "idx")
                                .returns(VOID)
                                .beginControlFlow("if(idx != null)")
                                .addStatement("idx.findFrom($L)", TypeSpec.anonymousClassBuilder("")
                                        .addSuperinterface(ParameterizedTypeName.get(gCallback, gNodeArray))
                                        .addMethod(MethodSpec.methodBuilder("on")
                                                .addAnnotation(Override.class)
                                                .addModifiers(PUBLIC)
                                                .addParameter(gNodeArray, "result")
                                                .returns(VOID)
                                                .addStatement("$1T[] typedResult = new $1T[result.length]", ClassName.get(packageName, index.type()))
                                                .addStatement("$T.arraycopy(result, 0, typedResult, 0, result.length)", jlSystem)
                                                .addStatement("idx.free()")
                                                .addStatement("callback.on(typedResult)")
                                                .build())
                                        .build())
                                .nextControlFlow("else")
                                .addStatement("callback.on(new $T[0])", ClassName.get(packageName, index.type()))
                                .endControlFlow()
                                .build())
                        .build())
                .build());
        //Find method
        MethodSpec.Builder find = MethodSpec.methodBuilder("find")
                .addModifiers(PUBLIC, STATIC, FINAL)
                .returns(TypeName.VOID)
                .addParameter(gGraph, "graph")
                .addParameter(LONG, "world")
                .addParameter(LONG, "time");
        StringBuilder params = new StringBuilder();
        index.attributes().forEach(attributeRef -> {
            find.addParameter(ClassName.get(String.class), attributeRef.ref().name());
            params.append(",").append(attributeRef.ref().name());
        });
        find.addParameter(ParameterizedTypeName.get(gCallback, ArrayTypeName.of(ClassName.get(packageName, index.type()))), "callback")
                .addStatement("graph.index(world, time, META.name, $L)", TypeSpec.anonymousClassBuilder("")
                        .addSuperinterface(ParameterizedTypeName.get(gCallback, gNodeIndex))
                        .addMethod(MethodSpec.methodBuilder("on")
                                .addAnnotation(Override.class)
                                .addModifiers(PUBLIC)
                                .addParameter(gNodeIndex, "idx")
                                .returns(VOID)
                                .beginControlFlow("if(idx != null)")
                                .addStatement("idx.findFrom($L$L)", TypeSpec.anonymousClassBuilder("")
                                        .addSuperinterface(ParameterizedTypeName.get(gCallback, gNodeArray))
                                        .addMethod(MethodSpec.methodBuilder("on")
                                                .addAnnotation(Override.class)
                                                .addModifiers(PUBLIC)
                                                .addParameter(gNodeArray, "result")
                                                .returns(TypeName.VOID)
                                                .addStatement("$1T[] typedResult = new $1T[result.length]", ClassName.get(packageName, index.type()))
                                                .addStatement("$T.arraycopy(result, 0, typedResult, 0, result.length)", jlSystem)
                                                .addStatement("idx.free()")
                                                .addStatement("callback.on(typedResult)")
                                                .build())
                                        .build(), params)
                                .nextControlFlow("else")
                                .addStatement("callback.on(new $T[0])", ClassName.get(packageName, index.type()))
                                .endControlFlow()
                                .build())
                        .build());
        javaClass.addMethod(find.build());
        //Update method
        javaClass.addMethod(MethodSpec.methodBuilder("update")
                .addModifiers(PUBLIC, STATIC, FINAL)
                .returns(VOID)
                .addParameter(ClassName.get(packageName, index.type()), "toIndex")
                .addParameter(ParameterizedTypeName.get(gCallback, BOOLEAN.box()), "callback")
                .addStatement("toIndex.graph().index(toIndex.world(), toIndex.time(), META.name, $L)", TypeSpec.anonymousClassBuilder("")
                        .addSuperinterface(ParameterizedTypeName.get(gCallback, gNodeIndex))
                        .addMethod(MethodSpec.methodBuilder("on")
                                .addAnnotation(Override.class)
                                .addModifiers(PUBLIC)
                                .addParameter(gNodeIndex, "idx")
                                .returns(VOID)
                                .beginControlFlow("if(idx != null)")
                                .addStatement("idx.update(toIndex)")
                                .addStatement("idx.free()")
                                .beginControlFlow("if(callback != null)")
                                .addStatement("callback.on(true)")
                                .endControlFlow()
                                .nextControlFlow("else")
                                .addStatement("System.err.println($S)", "undeclared index " + index.name())
                                .beginControlFlow("if(callback != null)")
                                .addStatement("callback.on(false)")
                                .endControlFlow()
                                .endControlFlow()
                                .build())
                        .build())
                .build());
        //Remove method
        javaClass.addMethod(MethodSpec.methodBuilder("remove")
                .addModifiers(PUBLIC, STATIC, FINAL)
                .returns(VOID)
                .addParameter(ClassName.get(packageName, index.type()), "toUnIndex")
                .addParameter(ParameterizedTypeName.get(gCallback, BOOLEAN.box()), "callback")
                .addStatement("toUnIndex.graph().index(toUnIndex.world(), toUnIndex.time(), META.name, $L)", TypeSpec.anonymousClassBuilder("")
                        .addSuperinterface(ParameterizedTypeName.get(gCallback, gNodeIndex))
                        .addMethod(MethodSpec.methodBuilder("on")
                                .addAnnotation(Override.class)
                                .addModifiers(PUBLIC)
                                .addParameter(gNodeIndex, "idx")
                                .returns(VOID)
                                .beginControlFlow("if(idx != null)")
                                .addStatement("idx.unindex(toUnIndex)")
                                .addStatement("idx.free()")
                                .beginControlFlow("if(callback != null)")
                                .addStatement("callback.on(true)")
                                .endControlFlow()
                                .nextControlFlow("else")
                                .addStatement("System.err.println($S)", "undeclared index " + index.name())
                                .beginControlFlow("if(callback != null)")
                                .addStatement("callback.on(false)")
                                .endControlFlow()
                                .endControlFlow()
                                .build())
                        .build())
                .build());
        collector.add(JavaFile.builder(packageName, javaClass.build()).build());
    }


}
