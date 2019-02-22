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
package greycatTest.internal;

import greycat.*;
import greycat.scheduler.NoopScheduler;
import greycat.plugin.Job;

public class Benchmark2Test {

    /**
     * @ignore ts
     */
    // @Test
    public void heapTest() {
        final Graph graph = new GraphBuilder().withScheduler(new NoopScheduler()).withMemorySize(100).build();
        test(graph, new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                graph.disconnect(new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        System.out.println("Graph disconnected");
                    }
                });
            }
        });

    }

    /**
     * @ignore ts
     */
    //@Test
    /*
    public void offHeapTest() {
        OffHeapByteArray.alloc_counter = 0;
        OffHeapDoubleArray.alloc_counter = 0;
        OffHeapLongArray.alloc_counter = 0;
        OffHeapStringArray.alloc_counter = 0;
        final Graph graph = new GraphBuilder().withScheduler(new NoopScheduler()).withOffHeapMemory().withMemorySize(100).saveEvery(20).build();
        test(graph, new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                graph.disconnect(new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        System.out.println("Graph disconnected");

                        Assert.assertTrue(OffHeapByteArray.alloc_counter == 0);
                        Assert.assertTrue(OffHeapDoubleArray.alloc_counter == 0);
                        Assert.assertTrue(OffHeapLongArray.alloc_counter == 0);
                        Assert.assertTrue(OffHeapStringArray.alloc_counter == 0);
                    }
                });
            }
        });
    }*/

    //final int valuesToInsert = 10000000;
    final int valuesToInsert = 500;

    final long timeOrigin = 1000;

    /**
     * @ignore ts
     */
    private void test(final Graph graph, final Callback<Boolean> testEnd) {
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                final long before = System.currentTimeMillis();

                Node temp_node = graph.newNode(0, 0);
                long nodeID = temp_node.id();
                temp_node.free();

                //Node node = graph.newNode(0, 0);
                final DeferCounter counter = graph.newCounter(valuesToInsert);
                for (long i = 0; i < valuesToInsert; i++) {

                    if (i % 10 == 0) {
                        //node.free();
                        //node = graph.newNode(0, 0);

                        temp_node = graph.newNode(0, 0);
                        nodeID = temp_node.id();
                        temp_node.free();

                        System.out.println(i + " node>" + nodeID);
                    }

                    if (i % 1000000 == 0) {
                        System.out.println("<insert til " + i + " in " + (System.currentTimeMillis() - before) / 1000 + "s");
                    }

                    final double value = i * 0.3;
                    final long time = timeOrigin + i;
                    final long finalNodeID = nodeID;
                    graph.lookup(0, time, nodeID, new Callback<Node>() {
                        @Override
                        public void on(Node timedNode) {

                            if (timedNode == null) {
                                graph.lookup(0, time, finalNodeID, new Callback<Node>() {
                                    @Override
                                    public void on(Node timedNode) {
                                        timedNode.set("value", Type.DOUBLE, value);
                                        counter.count();
                                        timedNode.free();//free the node, for cache management
                                    }
                                });
                            }


                            timedNode.set("value", Type.DOUBLE, value);
                            counter.count();
                            timedNode.free();//free the node, for cache management
                        }
                    });
                }
                // node.free();


                counter.then(new Job() {
                    @Override
                    public void run() {

                        long beforeRead = System.currentTimeMillis();

                        //System.out.println("<end insert phase>" + " " + (System.currentTimeMillis() - before) / 1000 + "s");
                        //System.out.println("result: " + (valuesToInsert / ((System.currentTimeMillis() - before) / 1000) / 1000) + "kv/s");

                        /*
                        final CoreDeferCounter counterRead = graph.counter(valuesToInsert);
                        for (long i = 0; i < valuesToInsert; i++) {
                            final double value = i * 0.3;
                            final long time = timeOrigin + i;

                            graph.lookup(0, time, node.id(), new Callback<Node>() {
                                @Override
                                public void on(Node timedNode) {
                                    Assert.assertTrue((double) timedNode.get("value") == value);
                                    counterRead.count();
                                    timedNode.free();//free the node, for cache management
                                }
                            });
                        }
                        counterRead.then(new Callback() {
                            @Override
                            public void on(Object result) {
                                long afterRead = System.currentTimeMillis();
                                System.out.println("<end read phase>" + " " + (afterRead - beforeRead) / 1000 + "s ");
                                System.out.println("result: " + (valuesToInsert / ((afterRead - beforeRead) / 1000) / 1000) + "kv/s");

                            }
                        });
*/

                        testEnd.on(true);


                    }
                });

            }
        });
    }

}
