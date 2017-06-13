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
import greycat.scheduler.NoopScheduler;
import greycat.utility.HashHelper;
import org.junit.Assert;
import org.junit.Test;

public class TimelineTest {

    @Test
    public void heapTest() {
        test(new GraphBuilder().withScheduler(new NoopScheduler()).build());
        test2(new GraphBuilder().withScheduler(new NoopScheduler()).build());
    }

    private void test2(final Graph g) {
        g.connect(result -> {
            Node n = g.newNode(0, 0);
            n.set("name", Type.STRING, "name");

            n.travelInTime(1, n_t1 -> {
                //should be effect less
                n_t1.set("name", Type.STRING, "name");
                Assert.assertEquals(n_t1.timeDephasing(), 1);
                n_t1.set("name", Type.STRING, "newName");
                Assert.assertEquals(n_t1.timeDephasing(), 0);
            });
        });
    }

    private void test(final Graph graph) {
        final int[] counter = {0};
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean o) {


                final Node node_t0 = graph.newNode(0, 0);
                //timeTree should be already filled
                node_t0.timepoints(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, new Callback<long[]>() {
                    @Override
                    public void on(long[] longs) {
                        counter[0]++;
                        Assert.assertTrue(longs.length == 1);
                        Assert.assertTrue(longs[0] == 0);
                    }
                });
                //do a simple modification
                node_t0.set("name", Type.STRING, "MyName");
                Assert.assertTrue(node_t0.timeDephasing() == 0);
                //check the unmodified time tree
                node_t0.timepoints(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, new Callback<long[]>() {
                    @Override
                    public void on(long[] longs) {
                        counter[0]++;
                        Assert.assertTrue(longs.length == 1);
                        Assert.assertTrue(longs[0] == 0);
                    }
                });

                graph.lookup(node_t0.world(), 1, node_t0.id(), new Callback<Node>() {
                    @Override
                    public void on(final Node node_t1) {
                        counter[0]++;
                        Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":1,\"id\":1,\"name\":\"MyName\"}", node_t1.toString()));
                        Assert.assertTrue(node_t1.timeDephasing() == 1); //node hasField a dephasing of 1 with last known state
                        node_t1.rephase(); // force the object to move to timepoint 1
                        Assert.assertTrue(node_t1.timeDephasing() == 0); //node should be in phase now
                        Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":1,\"id\":1,\"name\":\"MyName\"}", node_t1.toString()));

                        node_t1.set("name", Type.STRING, "MyName@t1");
                        Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":1,\"id\":1,\"name\":\"MyName@t1\"}", node_t1.toString()));
                        Assert.assertTrue(HashHelper.equals("{\"world\":0,\"time\":0,\"id\":1,\"name\":\"MyName\"}", node_t0.toString()));

                        node_t1.timepoints(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, new Callback<long[]>() {
                            @Override
                            public void on(long[] longs) {
                                counter[0]++;
                                Assert.assertTrue(longs.length == 2);
                                Assert.assertTrue(longs[0] == 1);
                                Assert.assertTrue(longs[1] == 0);
                            }
                        });

                        node_t1.timepoints(1, Constants.END_OF_TIME, new Callback<long[]>() {
                            @Override
                            public void on(long[] longs) {
                                counter[0]++;
                                Assert.assertTrue(longs.length == 1);
                                Assert.assertTrue(longs[0] == 1);
                            }
                        });

                        //now try to diverge the world
                        long newWorld = graph.fork(0);
                        graph.lookup(newWorld, 2, node_t0.id(), new Callback<Node>() {
                            @Override
                            public void on(Node node_t1_w0) {
                                Assert.assertTrue(HashHelper.equals("{\"world\":1,\"time\":2,\"id\":1,\"name\":\"MyName@t1\"}", node_t1_w0.toString()));
                                Assert.assertTrue(node_t1_w0.timeDephasing() == 1);

                                node_t1_w0.timepoints(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, new Callback<long[]>() {
                                    @Override
                                    public void on(long[] longs) {
                                        counter[0]++;
                                        Assert.assertTrue(longs.length == 2);
                                        Assert.assertTrue(longs[0] == 1);
                                        Assert.assertTrue(longs[1] == 0);
                                    }
                                });
                                node_t1_w0.set("name", Type.STRING, "MyName@t1@w1");
                                Assert.assertTrue(HashHelper.equals("{\"world\":1,\"time\":2,\"id\":1,\"name\":\"MyName@t1@w1\"}", node_t1_w0.toString()));
                                //test the new timeline
                                node_t1_w0.timepoints(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, new Callback<long[]>() {
                                    @Override
                                    public void on(long[] longs) {
                                        counter[0]++;
                                        Assert.assertTrue(longs.length == 3);
                                        Assert.assertTrue(longs[0] == 2);
                                        Assert.assertTrue(longs[1] == 1);
                                        Assert.assertTrue(longs[2] == 0);
                                    }
                                });
                                //test the old timeline
                                node_t1.timepoints(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, new Callback<long[]>() {
                                    @Override
                                    public void on(long[] longs) {
                                        counter[0]++;
                                        Assert.assertTrue(longs.length == 2);
                                        Assert.assertTrue(longs[0] == 1);
                                        Assert.assertTrue(longs[1] == 0);
                                    }
                                });


                            }
                        });


                    }
                });


                //end of the test
                graph.disconnect(new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        //test end
                    }
                });

            }
        });
        Assert.assertTrue(counter[0] == 8);
    }


    @Test
    public void test3() {
        Graph g = GraphBuilder.newBuilder().build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                Node f = g.newNode(0, 5);
                f.set("temp", Type.DOUBLE, 5.0);

                f.travelInTime(10, new Callback<Node>() {
                    @Override
                    public void on(Node f_t10) {
                        f_t10.set("temp", Type.DOUBLE, 10.0);
                        Task readprev = Tasks.newTask().traverseTimeline("5", Constants.BEGINNING_OF_TIME_STR, "1");
                        TaskContext ctx = readprev.prepare(g, f_t10, new Callback<TaskResult>() {
                            @Override
                            public void on(TaskResult result) {
                                Assert.assertEquals(1,result.size());
                                Assert.assertEquals(5,((Node)((TaskResult)result.get(0)).get(0)).time());
                                f_t10.free();
                                result.free();
                            }
                        });
                        readprev.executeUsing(ctx);
                    }
                });
            }
        });
    }


}
