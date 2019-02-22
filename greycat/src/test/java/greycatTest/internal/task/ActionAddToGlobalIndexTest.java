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
package greycatTest.internal.task;

import greycat.*;
import greycat.ActionFunction;
import org.junit.Assert;
import org.junit.Test;
import greycat.TaskContext;

import static greycat.internal.task.CoreActions.*;
import static greycat.Tasks.newTask;

public class ActionAddToGlobalIndexTest {

    @Test
    public void testIndexOneNode() {
        Graph graph = new GraphBuilder().build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                newTask()
                        .declareIndex("indexName", "name")
                        .createNode()
                        .setAttribute("name", Type.STRING, "root")
                        .updateIndex("indexName")
                        .defineAsGlobalVar("nodeIndexed")
                        .readIndex("indexName")
                        .thenDo(new ActionFunction() {
                            @Override
                            public void eval(TaskContext ctx) {
                                Assert.assertNotNull(ctx.result());
                                Node indexedNode = (Node) ctx.variable("nodeIndexed").get(0);
                                Assert.assertEquals(1, ctx.result().size());
                                Assert.assertEquals(indexedNode.id(), ctx.resultAsNodes().get(0).id());
                                ctx.continueTask();
                            }
                        })
                        // .then(removeFromGlobalIndex("indexName", "name"))
                        .then(readIndex("indexName"))
                        /*
                        .thenDo(new ActionFunction() {
                            @Override
                            public void eval(TaskContext ctx) {
                                Assert.assertNotNull(ctx.result());
                                Assert.assertEquals(0, ctx.result().size());
                                ctx.continueWith(null);
                            }
                        })*/
                        .execute(graph, null);
            }
        });
    }


    @Test
    public void readGlobalIndexTest() {
        Graph graph = new GraphBuilder().build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                newTask()
                        .declareIndex("indexName", "name")
                        .createNode()
                        .setAttribute("name", Type.STRING, "root")
                        .updateIndex("indexName")
                        .then(defineAsGlobalVar("nodeIndexed"))
                        .readIndex("indexName", "root")
                        .thenDo(new ActionFunction() {
                            @Override
                            public void eval(TaskContext ctx) {
                                Assert.assertNotNull(ctx.result());
                                Node indexedNode = (Node) ctx.variable("nodeIndexed").get(0);
                                Assert.assertEquals(1, ctx.result().size());
                                Assert.assertEquals(indexedNode.id(), ctx.resultAsNodes().get(0).id());
                                ctx.continueTask();
                            }
                        })
                        .execute(graph, null);
            }
        });
    }


    /*
    @Test
    public void testIndexComplexArrayOfNodes() {
        Graph graph = new GraphBuilder().build();

        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                Object complexArray = new Object[3];

                for (int i = 0; i < 3; i++) {
                    Object[] inner = new Node[2];
                    for (int j = 0; j < 2; j++) {
                        inner[j] = graph.newNode(0, 0);
                        ((Node) inner[j]).set("name", "node" + i + j);
                    }
                    ((Object[]) complexArray)[i] = inner;
                }

                newTask()
                        .inject(complexArray)
                        .indexNode("indexName", "name")
                        .asVar("nodeIndexed")
                        .readIndexAll("indexName")
                        .then(new ActionFunction() {
                            @Override
                            public void eval(TaskContext context) {
                                Assert.assertNotNull(context.result());
                                Assert.assertEquals(6, context.result().size());
                                for (int i = 0; i < 3; i++) {
                                    Object inner = ((Object[]) complexArray)[i];
                                    for (int j = 0; j < 2; j++) {
                                        Assert.assertEquals(((Node[]) inner)[j].get("name"), "node" + i + j);
                                    }
                                }
                                context.continueTask();
                            }
                        })
                        .unindexNode("indexName", "name")
                        .readIndexAll("indexName")
                        .then(new ActionFunction() {
                            @Override
                            public void eval(TaskContext context) {
                                Assert.assertNotNull(context.result());
                                Assert.assertEquals(0, context.result().size());
                                context.continueWith(null);
                            }
                        })
                        .execute(graph, null);
            }
        });
    }


    @Test
    public void testIndexNodeIncorrectInput() {
        Graph graph = new GraphBuilder().build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                Task indexWithOneIncoorectInput = CoreActions.newTask()
                        .inject(55)
                        .indexNode("indexName", "name");

                Task unindexWithOneIncoorectInput = CoreActions.newTask()
                        .inject(55)
                        .unindexNode("indexName", "name");

                Object complexArray = new Object[3];

                for (int i = 0; i < 3; i++) {
                    Object[] inner = new Object[2];
                    for (int j = 0; j < 2; j++) {
                        if (i == 2 && j == 0) {
                            inner[j] = 55;
                        } else {
                            inner[j] = graph.newNode(0, 0);
                            ((Node) inner[j]).set("name", "node" + i + j);
                        }
                    }
                    ((Object[]) complexArray)[i] = inner;
                }


                Task indexwithIncorrectArray = newTask()
                        .inject(complexArray)
                        .indexNode("indexName", "name");

                Task unindexwithIncorrectArray = newTask()
                        .inject(complexArray)
                        .unindexNode("indexName", "name");

                boolean exceptionCaught = false;
                try {
                    indexWithOneIncoorectInput.execute(graph, null);
                } catch (RuntimeException ex) {
                    exceptionCaught = true;
                }
                Assert.assertTrue(exceptionCaught);

                exceptionCaught = false;
                try {
                    unindexWithOneIncoorectInput.execute(graph, null);
                } catch (RuntimeException ex) {
                    exceptionCaught = true;
                }
                Assert.assertTrue(exceptionCaught);

                exceptionCaught = false;
                try {
                    indexwithIncorrectArray.execute(graph, null);
                } catch (RuntimeException ex) {
                    exceptionCaught = true;
                }
                Assert.assertTrue(exceptionCaught);

                exceptionCaught = false;
                try {
                    unindexwithIncorrectArray.execute(graph, null);
                } catch (RuntimeException ex) {
                    exceptionCaught = true;
                }
                Assert.assertTrue(exceptionCaught);

            }
        });
    }
    */

}
