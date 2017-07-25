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
package greycatTest;

import greycat.*;
import greycat.internal.CoreNodeValue;
import greycat.scheduler.NoopScheduler;
import org.junit.Assert;
import org.junit.Test;

import static greycat.Tasks.newTask;

public class NodeValueTest {

    /**
     * @ignore ts
     */

    /*
    @Test
    public void test() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                NodeValue nodeValue = (NodeValue) g.newTypedNode(0, 0, CoreNodeValue.NAME);
                long before = System.currentTimeMillis();
                long size = 5000000;
                for (int i = 0; i < size; i++) {
                    int finalI = i;
                    g.lookup(0, i, nodeValue.id(), new Callback<NodeValue>() {
                        @Override
                        public void on(NodeValue result) {
                            result.setValue(finalI * 2.0d);
                            result.free();
                        }
                    });
                }
                long after = System.currentTimeMillis();
                double timeSecond = (after - before) / 1000d;
                System.out.println(timeSecond);
                System.out.println(size / timeSecond);
            }
        });
    }*/
    @Test
    public void testRelation() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                NodeValue nodeValue = (NodeValue) g.newTypedNode(0, 0, CoreNodeValue.NAME);
                nodeValue.setValue(42.5d);

                Node parent = g.newNode(0, 0);
                parent.addToRelation("sub", nodeValue);

                g.lookup(0, 0, nodeValue.id(), new Callback<Node>() {
                    @Override
                    public void on(Node result) {
                        Assert.assertNotNull(result);
                    }
                });

                g.lookupAll(0, 0, new long[]{nodeValue.id()}, new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        Assert.assertNotNull(result[0]);
                        NodeValue nd = (NodeValue) result[0];
                        Assert.assertEquals(nd.getValue() + "", "42.5");
                        Assert.assertNotNull(nd.getValue());
                    }
                });

                g.lookupBatch(new long[]{0}, new long[]{0}, new long[]{nodeValue.id()}, new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        Assert.assertNotNull(result[0]);
                        NodeValue nd = (NodeValue) result[0];
                        Assert.assertEquals(nd.getValue() + "", "42.5");
                        Assert.assertNotNull(nd.getValue());
                    }
                });

                parent.traverse("sub", new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        Assert.assertNotNull(result[0]);
                        NodeValue nd = (NodeValue) result[0];
                        Assert.assertEquals(nd.getValue() + "", "42.5");
                        Assert.assertNotNull(nd);
                    }
                });

                /*
                nodeValue.timepoints(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, new Callback<long[]>() {
                    @Override
                    public void on(long[] result) {
                        System.out.println(result);
                    }
                });
*/

                Task traverse = newTask().traverse("sub").setAsVar("v1").readVar("v1").timepoints(Constants.BEGINNING_OF_TIME_STR, Constants.END_OF_TIME_STR);
                TaskContext ctx = traverse.prepare(g, parent, new Callback<TaskResult>() {
                    @Override
                    public void on(TaskResult result) {
                        Assert.assertEquals(result.size(), 1);
                    }
                });
                traverse.executeUsing(ctx);

            }
        });
    }

    @Test
    public void testNull() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                NodeValue nodeValue = (NodeValue) g.newTypedNode(0, 0, CoreNodeValue.NAME);
                long size = 100;
                for (int i = 0; i < size; i++) {
                    int finalI = i;
                    g.lookup(0, i, nodeValue.id(), new Callback<NodeValue>() {
                        @Override
                        public void on(NodeValue result) {
                            if ((finalI % 10) == 0) {
                                result.setValue(null);
                            } else {
                                result.setValue(finalI * 2.0d);

                            }
                            result.free();
                        }
                    });
                }
                nodeValue.free();
                g.save(null);

                g.lookup(0, 0, nodeValue.id(), new Callback<NodeValue>() {
                    @Override
                    public void on(NodeValue result) {
                        for (int i = 0; i < size; i++) {
                            int finalI = i;
                            g.lookup(0, i, nodeValue.id(), new Callback<NodeValue>() {
                                @Override
                                public void on(NodeValue result) {
                                    if ((finalI % 10) == 0) {
                                        Assert.assertNull(result.getValue());
                                    } else {
                                        Assert.assertTrue((result.getValue() + "").equals((finalI * 2.0d) + ""));
                                    }
                                    result.free();
                                }
                            });
                        }
                    }
                });


            }
        });
    }

}
