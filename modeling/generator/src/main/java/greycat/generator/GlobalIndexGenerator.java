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

import greycat.language.AttributeRef;
import greycat.language.Index;
import greycat.language.Model;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Visibility;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MethodSource;

public class GlobalIndexGenerator {

    static JavaSource[] generate(String packageName, Model model) {
        JavaSource[] sources = new JavaSource[model.globalIndexes().length];

        for (int i = 0; i < model.globalIndexes().length; i++) {
            sources[i] = generateGlobalIndexes(packageName, model.globalIndexes()[i]);
        }

        return sources;
    }

    private static JavaSource generateGlobalIndexes(String packageName, Index index) {
        final JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
        javaClass.setPackage(packageName);
        javaClass.setName(index.name());

        String indexName = index.name();
        String indexConstant = "INDEX_NAME";

        // index name constant
        javaClass.addField()
                .setVisibility(Visibility.PUBLIC)
                .setFinal(true)
                .setName(indexConstant)
                .setType(String.class)
                .setStringInitializer(indexName)
                .setStatic(true);

        // attribute constants
        for (AttributeRef att : index.attributes()) {
            javaClass.addField()
                    .setVisibility(Visibility.PUBLIC)
                    .setFinal(true)
                    .setName(att.ref().name().toUpperCase())
                    .setType(String.class)
                    .setStringInitializer(att.ref().name())
                    .setStatic(true);
        }

        StringBuilder indexedAttributes = new StringBuilder();
        for (AttributeRef att : index.attributes()) {
            indexedAttributes.append(att.ref().name().toUpperCase());
            indexedAttributes.append(",");
        }
        indexedAttributes.deleteCharAt(indexedAttributes.length() - 1);

        // declare index
        /*
        if (index.isWithTime()) {
            MethodSource<JavaClassSource> declareIndex = javaClass.addMethod()
                    .setName("declareTimedIndex")
                    .setReturnTypeVoid()
                    .setFinal(true)
                    .setVisibility(Visibility.PUBLIC)
                    .setStatic(true);
            declareIndex.addParameter("greycat.Graph", "graph");
            declareIndex.addParameter("long", "world");
            declareIndex.addParameter("long", "time");
            declareIndex.addParameter("greycat.Callback<Boolean>", "callback");

            StringBuilder declareIndexBody = new StringBuilder();
            declareIndexBody.append("graph.declareTimedIndex(world, time, " + indexConstant + ", new greycat.Callback<greycat.NodeIndex>() {");
            declareIndexBody.append("@Override\n");
            declareIndexBody.append("public void on(greycat.NodeIndex result) {");
            declareIndexBody.append("if (callback != null) {");
            declareIndexBody.append("callback.on(true);");
            declareIndexBody.append("}");
            declareIndexBody.append("}");
            declareIndexBody.append("}, " + indexedAttributes.toString() + ");");

            declareIndex.setBody(declareIndexBody.toString());
        } else {
        */
        MethodSource<JavaClassSource> declareIndex = javaClass.addMethod()
                .setName("declareIndex")
                .setReturnTypeVoid()
                .setFinal(true)
                .setVisibility(Visibility.PUBLIC)
                .setStatic(true);
        declareIndex.addParameter("greycat.Graph", "graph");
        declareIndex.addParameter("long", "world");
        declareIndex.addParameter("greycat.Callback<Boolean>", "callback");

        StringBuilder declareIndexBody = new StringBuilder();
        declareIndexBody.append("graph.declareIndex(world, " + indexConstant + ", new greycat.Callback<greycat.NodeIndex>() {");
        declareIndexBody.append("@Override\n");
        declareIndexBody.append("public void on(greycat.NodeIndex result) {");
        declareIndexBody.append("if (callback != null) {");
        declareIndexBody.append("callback.on(true);");
        declareIndexBody.append("}");
        declareIndexBody.append("}");
        declareIndexBody.append("}, " + indexedAttributes.toString() + ");");

        declareIndex.setBody(declareIndexBody.toString());
        //}

        // find method
        MethodSource find = javaClass.addMethod()
                .setName("find")
                .setVisibility(Visibility.PUBLIC)
                .setReturnTypeVoid()
                .setFinal(true)
                .setStatic(true);
        find.addParameter("Graph", "graph");
        find.addParameter("long", "world");
        find.addParameter("long", "time");

        StringBuilder paramsBuilder = new StringBuilder();
        for (AttributeRef att : index.attributes()) {
            find.addParameter("String", att.ref().name());
            paramsBuilder.append(att.ref().name());
            paramsBuilder.append(",");
        }
        paramsBuilder.deleteCharAt(paramsBuilder.length() - 1);
        find.addParameter("greycat.Callback<" + index.type() + "[]>", "callback");
        StringBuilder findBody = createFindMethodBody(index, indexConstant, paramsBuilder);
        find.setBody(findBody.toString());

        // findAll method
        MethodSource findAll = javaClass.addMethod()
                .setName("findAll")
                .setVisibility(Visibility.PUBLIC)
                .setReturnTypeVoid()
                .setFinal(true)
                .setStatic(true);
        findAll.addParameter("Graph", "graph");
        findAll.addParameter("long", "world");
        findAll.addParameter("long", "time");
        findAll.addParameter("greycat.Callback<" + index.type() + "[]>", "callback");
        StringBuilder findAllBody = createFindMethodBody(index, indexConstant, null);
        findAll.setBody(findAllBody.toString());


        // update index method
        MethodSource updateIndex = javaClass.addMethod()
                .setName("updateIndex")
                .setVisibility(Visibility.PUBLIC)
                .setReturnTypeVoid()
                .setFinal(true)
                .setStatic(true);
        updateIndex.addParameter("greycat.Graph", "graph");
        updateIndex.addParameter("long", "world");
        updateIndex.addParameter("long", "time");
        updateIndex.addParameter(index.type(), "toIndex");
        updateIndex.addParameter("greycat.Callback<Boolean>", "callback");

        StringBuilder updateIndexBody = new StringBuilder();
        updateIndexBody.append("graph.index(world, time, " + indexConstant + ", new Callback<greycat.NodeIndex>() {");
        updateIndexBody.append("@Override\n");
        updateIndexBody.append("public void on(greycat.NodeIndex result) {");
        updateIndexBody.append("result.update(toIndex);");
        updateIndexBody.append("result.free();");
        updateIndexBody.append("if (callback != null) {");
        updateIndexBody.append("callback.on(true);");
        updateIndexBody.append("}");
        updateIndexBody.append("}");
        updateIndexBody.append("});");

        updateIndex.setBody(updateIndexBody.toString());


        // unindex
        MethodSource unindex = javaClass.addMethod()
                .setName("unindex")
                .setVisibility(Visibility.PUBLIC)
                .setReturnTypeVoid()
                .setFinal(true)
                .setStatic(true);
        unindex.addParameter("greycat.Graph", "graph");
        unindex.addParameter("long", "world");
        unindex.addParameter("long", "time");
        unindex.addParameter(index.type(), "toUnIndex");
        unindex.addParameter("greycat.Callback<Boolean>", "callback");

        StringBuilder unindexBody = new StringBuilder();
        unindexBody.append("graph.index(world, time, " + indexConstant + ", new Callback<greycat.NodeIndex>() {");
        unindexBody.append("@Override\n");
        unindexBody.append("public void on(greycat.NodeIndex result) {");
        unindexBody.append("result.unindex(toUnIndex);");
        unindexBody.append("result.free();");
        unindexBody.append("if (callback != null) {");
        unindexBody.append("callback.on(true);");
        unindexBody.append("}");
        unindexBody.append("}");
        unindexBody.append("});");

        unindex.setBody(unindexBody.toString());

        return javaClass;
    }

    private static StringBuilder createFindMethodBody(Index index, String indexConstant, StringBuilder paramsBuilder) {
        StringBuilder findBody = new StringBuilder();
        findBody.append(" graph.index(world, time, " + indexConstant + ", new Callback<greycat.NodeIndex>() {");
        findBody.append("@Override\n");
        findBody.append("public void on(greycat.NodeIndex result) {");
        findBody.append("result.findFrom(new Callback<greycat.Node[]>() {");
        findBody.append("@Override\n");
        findBody.append("public void on(greycat.Node[] result) {");
        findBody.append(index.type() + "[] typedResult = new " + index.type() + "[result.length];");
        findBody.append("java.lang.System.arraycopy(result, 0, typedResult, 0, result.length);");
        findBody.append("callback.on(typedResult);");
        findBody.append("}");
        if (paramsBuilder == null) {
            findBody.append("});");
        } else {
            findBody.append("}, " + paramsBuilder.toString() + ");");
        }
        findBody.append("}");
        findBody.append("});");
        return findBody;
    }


}
