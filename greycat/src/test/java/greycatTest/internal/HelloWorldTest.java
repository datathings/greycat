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
package greycatTest.internal;

import greycat.*;
import greycat.struct.Relation;
import greycat.utility.HashHelper;
import org.junit.Assert;
import org.junit.Test;

public class HelloWorldTest {

    @Test
    public void heapTest() {
        test(new GraphBuilder()/*.withScheduler(new NoopScheduler())*/.build());
    }

    public void test(final Graph graph) {
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean o) {

                final long available = graph.space().available();

                final Node node0 = graph.newNode(0, 0);

                //do something selectWith the node
                graph.lookup(0, 0, node0.id(), new Callback<Node>() {
                    @Override
                    public void on(Node result) {
                        //check that the lookup return the same
                        Assert.assertTrue(result.id() == node0.id());
                        result.free();

                        node0.set("name", Type.STRING, "MyName");
                        Assert.assertTrue(HashHelper.equals("MyName", node0.get("name").toString()));

                        node0.remove("name");
                        Assert.assertTrue(node0.get("name") == null);
                        node0.set("name", Type.STRING, "MyName");

                        node0.set("value", Type.STRING, "MyValue");
                        Assert.assertTrue(HashHelper.equals("MyValue", node0.get("value").toString()));
                        //check that other attribute name is not affected
                        Assert.assertTrue(HashHelper.equals("MyName", node0.get("name").toString()));

                        node0.set("name", Type.STRING, "MyName2");
                        Assert.assertTrue(HashHelper.equals("MyName2", node0.get("name").toString()));
                        Assert.assertTrue(HashHelper.equals("MyValue", node0.get("value").toString()));

                        //check the simple json print

                        String flatNode0 = "{\"world\":0,\"time\":0,\"id\":1,\"group\":0,\"name\":\"MyName2\",\"value\":\"MyValue\"}";

                        Assert.assertTrue(flatNode0.length() == node0.toString().length());
                        Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":0,\"id\":1,\"group\":0,\"name\":\"MyName2\",\"value\":\"MyValue\"}", node0.toString()));

                        //Create a new node
                        Node node1 = graph.newNode(0, 0);
                        Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":0,\"id\":2,\"group\":0}", node1.toString()));

                        //attach the new node
                        node1.addToRelation("children", node0);
                        Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":0,\"id\":2,\"group\":0,\"children\":[1]}", node1.toString()));

                        node1.addToRelation("children", node0);
                        Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":0,\"id\":2,\"group\":0,\"children\":[1,1]}", node1.toString()));

                        Node node2 = graph.newNode(0, 0);
                        node1.addToRelation("children", node2);
                        Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":0,\"id\":2,\"group\":0,\"children\":[1,1,3]}", node1.toString()));

                        Relation refValuesThree = (Relation) node1.get("children");
                        Assert.assertTrue(refValuesThree.size() == 3);
                        Assert.assertTrue(refValuesThree.get(0) == 1);
                        Assert.assertTrue(refValuesThree.get(1) == 1);
                        Assert.assertTrue(refValuesThree.get(2) == 3);

                        node1.traverse("children", new Callback<Node[]>() {
                            @Override
                            public void on(Node[] resolvedNodes) {
                                Assert.assertTrue(resolvedNodes[0].id() == 1);
                                Assert.assertTrue(resolvedNodes[1].id() == 1);
                                Assert.assertTrue(resolvedNodes[2].id() == 3);

                                graph.freeNodes(resolvedNodes);

                                node1.removeFromRelation("children", node0);
                                Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":0,\"id\":2,\"group\":0,\"children\":[1,3]}", node1.toString()));

                                node1.removeFromRelation("children", node0);
                                Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":0,\"id\":2,\"group\":0,\"children\":[3]}", node1.toString()));

                                node1.removeFromRelation("children", node2);
                                Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":0,\"id\":2,\"group\":0,\"children\":[]}", node1.toString()));

                                //destroy the node explicitly without waiting GC
                                node0.free();
                                node1.free();
                                node2.free();


                                graph.save(new Callback<Boolean>() {
                                    @Override
                                    public void on(Boolean result) {
                                        long availableAfter = graph.space().available();
                                        Assert.assertEquals(available, availableAfter);

                                        graph.disconnect(new Callback<Boolean>() {
                                            @Override
                                            public void on(Boolean result) {
                                                //end of test

                                            }
                                        });
                                    }
                                });


                            }
                        });

                    }
                });

            }
        });

    }

}

