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
package greycatTest.internal.task;


import greycat.*;
import greycat.ActionFunction;
import greycat.TaskContext;
import greycat.TaskResult;
import org.junit.Assert;
import org.junit.Test;

import static greycat.internal.task.CoreActions.readIndex;
import static greycat.internal.task.CoreActions.attributes;
import static greycat.internal.task.CoreActions.attributesWithTypes;
import static greycat.Tasks.newTask;

public class ActionPropertiesTest {
    private Graph graph;

    public void initGraph() {
        graph = new GraphBuilder().build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                Node root = graph.newNode(0, Constants.BEGINNING_OF_TIME);
                root.set("id", Type.INT, 1);
                root.set("attribute", Type.BOOL, false);

                graph.declareIndex(0, "root", rootIndex -> {
                    rootIndex.update(root);
                }, "id");

                Node child1 = graph.newNode(0, Constants.BEGINNING_OF_TIME);
                child1.set("name", Type.STRING, "child1");
                root.addToRelation("rel1", child1);

                Index localIindex1 = (Index) root.getOrCreate("localIindex1", Type.INDEX);
                localIindex1.declareAttributes(null, "name");
                localIindex1.update(child1);

            }
        });
    }

    public void deleteGraph() {
        graph.disconnect(null);
    }

    @Test
    public void testNormalRelations() {
        initGraph();
        newTask()
                .then(readIndex("root"))
                .then(attributes())
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        TaskResult<String> result = ctx.result();
                        Assert.assertEquals(4, result.size());

                        Assert.assertEquals("id", result.get(0));
                        Assert.assertEquals("attribute", result.get(1));
                        Assert.assertEquals("rel1", result.get(2));
                        Assert.assertEquals("localIindex1", result.get(3));
                        ctx.continueTask();
                    }
                })
                .execute(graph, null);
        deleteGraph();
    }

    @Test
    public void testLocalIndex() {
        initGraph();
        newTask()
                .then(readIndex("root"))
                .pipe(
                        newTask().then(attributesWithTypes(Type.RELATION)),
                        newTask().then(attributesWithTypes(Type.INDEX))
                ).flat()
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        TaskResult<String> result = ctx.result();
                        Assert.assertEquals(2, result.size());

                        Assert.assertEquals("rel1", result.get(0));
                        Assert.assertEquals("localIindex1", result.get(1));
                    }
                })
                .execute(graph, null);
        deleteGraph();
    }
}
