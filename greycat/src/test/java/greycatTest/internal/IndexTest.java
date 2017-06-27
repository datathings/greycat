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
package greycatTest.internal;

import greycat.*;
import greycat.ActionFunction;
import org.junit.Assert;
import org.junit.Test;
import greycat.scheduler.NoopScheduler;
import greycat.TaskContext;
import greycat.Tasks;

public class IndexTest {

    @Test
    public void heapTest() {
        //test(new GraphBuilder().withScheduler(new NoopScheduler()).build());
        //testRelation(new GraphBuilder().withScheduler(new NoopScheduler()).build());
        testIndexedRelation(new GraphBuilder().withScheduler(new NoopScheduler()).build());
    }

    /*
    private void testRelation(final Graph graph) {
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                final Node node_t0 = graph.newNode(0, 0);
                node_t0.setAttribute("name", Type.STRING, "MyName");

                final Node node_t1 = graph.newNode(0, 0);
                node_t1.setAttribute("name", Type.STRING, "MyName2");

                node_t1.add("children", node_t0);
                graph.index("bigram", node_t1, "children", null);

                graph.findAll(0, 0, "bigram", new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        Assert.assertEquals(result.length, 1);
                        Assert.assertEquals(result[0].id(), node_t1.id());
                    }
                });

                Query q = graph.newQuery();
                q.setIndexName("bigram");
                q.setTime(0);
                q.setWorld(0);
                q.add("children", "[" + node_t0.id() + "]");
                graph.findByQuery(q, new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        Assert.assertEquals(result.length, 1);
                        Assert.assertEquals(result[0].id(), node_t1.id());
                    }
                });

                graph.disconnect(null);
            }
        });
    }*/

    private void testIndexedRelation(final Graph graph) {
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                final Node node_t0 = graph.newNode(0, 0);

                final Node node_t1 = graph.newNode(0, 0);
                node_t1.set("name", Type.STRING, "MyName");

                Index irel = (Index) node_t0.getOrCreate("ichildren", Type.INDEX);
                irel.declareAttributes(null, "name");
                irel.update(node_t1);

                long[] flat = irel.all();
                Assert.assertEquals(1, flat.length);
                Assert.assertEquals(node_t1.id(), flat[0]);

                final int[] passed = {0};
                irel.find(new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        Assert.assertEquals(result.length, 1);
                        Assert.assertEquals(result[0].id(), node_t1.id());
                        passed[0]++;
                    }
                }, 0, 0, "MyName");

                irel.find(new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        Assert.assertEquals(result.length, 1);
                        Assert.assertEquals(result[0].id(), node_t1.id());
                        passed[0]++;
                    }
                }, 0, 0, "MyName");

                irel.findByQuery(graph.newQuery().add("name", "MyName").setTime(0).setWorld(0), new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        Assert.assertEquals(result.length, 1);
                        Assert.assertEquals(result[0].id(), node_t1.id());
                        passed[0]++;
                    }
                });

                Assert.assertEquals(3, passed[0]);

                graph.disconnect(null);
            }
        });
    }

    @Test
    public void testReadBeforeSet() {
        Graph graph = new GraphBuilder().build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                Tasks.newTask()
                        .travelInTime(System.currentTimeMillis() + "")
                        .travelInWorld("0")
                        .declareIndex("indexName", "name")
                        .readIndex("indexName")
                        .createNode()
                        .setAttribute("name", Type.STRING, "156ea1e_11-SNAPSHOT")
                        .updateIndex("indexName")
                        .readIndex("indexName")
                        .thenDo(new ActionFunction() {
                            @Override
                            public void eval(TaskContext ctx) {
                                Assert.assertEquals(1, ctx.result().size());
                            }
                        })
                        .readIndex("indexName", "name", "156ea1e_11-SNAPSHOT")
                        .thenDo(new ActionFunction() {
                            @Override
                            public void eval(TaskContext ctx) {
                                Assert.assertEquals(1, ctx.result().size());
                                ctx.continueTask();
                            }
                        })
                        .save()
                        .execute(graph, null);
            }
        });
    }

    /*
    @Test
    public void testModificationKeyAttribute() {
        Graph graph = new GraphBuilder().build();

        final String rootNode = "rootNode";
        final String kAtt = "name";
        final String fValue = "root";
        final String sValue = "newName";
        final String idxName = "indexName";

        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                Tasks.newTask()
                        .travelInTime("0")
                        .travelInWorld("0")
                        .declareTimedIndex(idxName, kAtt)
                        .createNode()
                        .setAttribute(kAtt, Type.STRING, fValue)
                        .setAsVar(rootNode)
                        .updateIndex(idxName) //add to index at time 0
                        .readVar(rootNode)
                        .travelInTime("10") //jump the context at time 10
                        //.removeFromGlobalTimedIndex(idxName, kAtt) //remove the node from the index
                        .setAttribute(kAtt, Type.STRING, sValue) //modify its key value

                        .updateIndex(idxName) //re-add to the index

                        //Check
                        .travelInTime("10")
                        .readIndex(sValue)
                        .thenDo(new ActionFunction() {
                            @Override
                            public void eval(TaskContext ctx) {
                                Assert.assertEquals(1, ctx.result().size());
                                ctx.continueTask();
                            }
                        })
                        .travelInTime("0") //jump the context at time 0
                        .readIndex(idxName)
                        .thenDo(new ActionFunction() {
                            @Override
                            public void eval(TaskContext ctx) {
                                //The index works perfectly without the query
                                Node node = (Node) ctx.result().get(0);
                                Assert.assertEquals(1, ctx.result().size());
                                Assert.assertEquals(fValue, node.get(kAtt));
                                ctx.continueTask();
                            }
                        })
                        .readIndex(idxName, fValue)
                        .thenDo(new ActionFunction() {
                            @Override
                            public void eval(TaskContext ctx) {
                                //But not with the query...
                                Assert.assertEquals(0, ctx.result().size());
                                ctx.continueTask();
                            }
                        })
                        .execute(graph, null);
            }
        });
    }
*/

    /*
    private void test(final Graph graph) {
        final int[] counter = {0};
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean o) {
                Node node_t0 = graph.newNode(0, 0);
                node_t0.setAttribute("name", Type.STRING, "MyName");

                graph.findAll(0, 0, "nodes", new Callback<Node[]>() {
                    @Override
                    public void on(Node[] allNodes) {
                        counter[0]++;
                        Assert.assertTrue(allNodes.length == 0);
                    }
                });

                graph.index("nodes", node_t0, "name", new Callback<Boolean>() {
                    @Override
                    public void on(Boolean o) {
                        counter[0]++;
                    }
                });

                graph.findAll(0, 0, "nodes", new Callback<Node[]>() {
                    @Override
                    public void on(Node[] allNodes) {
                        counter[0]++;
                        Assert.assertTrue(allNodes.length == 1);
                        Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":0,\"id\":1,\"name\":\"MyName\"}", allNodes[0].toString()));
                    }
                });

                graph.find(0, 0, "nodes", "name=MyName", new Callback<Node[]>() {
                    @Override
                    public void on(Node[] kNode) {
                        counter[0]++;
                        Assert.assertTrue(kNode != null);
                        Assert.assertEquals(1, kNode.length);
                        Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":0,\"id\":1,\"name\":\"MyName\"}", kNode[0].toString()));
                    }
                });

                //test a null index
                graph.findAll(0, 0, "unknownIndex", new Callback<Node[]>() {
                    @Override
                    public void on(Node[] allNodes) {
                        counter[0]++;
                        Assert.assertTrue(allNodes.length == 0);
                    }
                });


                Node node_t1 = graph.newNode(0, 0);
                node_t1.setAttribute("name", Type.STRING, "MyName");
                node_t1.setAttribute("version", Type.STRING, "1.0");

                graph.index("nodes", node_t1, "name,version", new Callback<Boolean>() {
                    @Override
                    public void on(Boolean o) {
                        counter[0]++;
                    }
                });

                //test the old indexed node
                graph.find(0, 0, "nodes", "name=MyName", new Callback<Node[]>() {
                    @Override
                    public void on(Node[] kNode) {
                        counter[0]++;
                        Assert.assertTrue(kNode != null);
                        Assert.assertTrue(kNode.length == 1);
                        Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":0,\"id\":1,\"name\":\"MyName\"}", kNode[0].toString()));
                    }
                });

                //test the new indexed node
                graph.find(0, 0, "nodes", "name=MyName,version=1.0", new Callback<Node[]>() {
                    @Override
                    public void on(Node[] kNode) {
                        counter[0]++;
                        Assert.assertTrue(kNode != null);
                        Assert.assertTrue(kNode.length == 1);
                        Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":0,\"id\":3,\"name\":\"MyName\",\"version\":\"1.0\"}", kNode[0].toString()));
                    }
                });


                //test potential inversion
                graph.find(0, 0, "nodes", "version=1.0,name=MyName", new Callback<Node[]>() {
                    @Override
                    public void on(Node[] kNode) {
                        counter[0]++;
                        Assert.assertTrue(kNode != null);
                        Assert.assertTrue(kNode.length == 1);
                        Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":0,\"id\":3,\"name\":\"MyName\",\"version\":\"1.0\"}", kNode[0].toString()));
                    }
                });


                //unIndex the node @t1
                graph.unindex("nodes", node_t1, "name,version", new Callback<Boolean>() {
                    @Override
                    public void on(Boolean o) {
                        counter[0]++;
                    }
                });


                graph.find(0, 0, "nodes", "version=1.0,name=MyName", new Callback<Node[]>() {
                    @Override
                    public void on(Node[] kNode) {
                        counter[0]++;
                        Assert.assertTrue(kNode != null);
                        Assert.assertTrue(kNode.length == 0);
                    }
                });


                //reIndex
                graph.index("nodes", node_t1, "name,version", new Callback<Boolean>() {
                    @Override
                    public void on(Boolean o) {
                        counter[0]++;
                    }
                });


                //should work again
                graph.find(0, 0, "nodes", "version=1.0,name=MyName", new Callback<Node[]>() {
                    @Override
                    public void on(Node[] kNode) {
                        counter[0]++;
                        Assert.assertTrue(kNode != null);
                        Assert.assertTrue(kNode.length == 1);
                        Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":0,\"id\":3,\"name\":\"MyName\",\"version\":\"1.0\"}", kNode[0].toString()));
                    }
                });


                //local index usage
                Node node_index = graph.newNode(0, 0);
                node_index.index("children", node_t1, "name,version", new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        counter[0]++;
                    }
                });

                node_index.find("children", "name=MyName,version=1.0", new Callback<Node[]>() {
                    @Override
                    public void on(Node[] kNode) {
                        counter[0]++;
                        Assert.assertTrue(kNode != null);
                        Assert.assertTrue(kNode.length == 1);
                        Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":0,\"id\":3,\"name\":\"MyName\",\"version\":\"1.0\"}", kNode[0].toString()));
                    }
                });

                graph.disconnect(new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        //end of the test
                    }
                });

            }
        });
        Assert.assertTrue(counter[0] == 15);
    }*/

}
