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

/**
 * @ignore ts
 */
public class Benchmark3Test {

    //@Test
    public void heapTest() {
        final Graph graph = new GraphBuilder().withScheduler(new NoopScheduler()).withMemorySize(100000).build();
        test("heap ", graph, new Callback<Boolean>() {
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

    //@Test
    public void offHeapTest() {
        /*
        OffHeapByteArray.alloc_counter = 0;
        OffHeapDoubleArray.alloc_counter = 0;
        OffHeapLongArray.alloc_counter = 0;
        OffHeapStringArray.alloc_counter = 0;

        Unsafe.DEBUG_MODE = true;

        final Graph graph = new GraphBuilder().withScheduler(new NoopScheduler()).withOffHeapMemory().withMemorySize(100000).saveEvery(10000).build();

        test("offheap ", graph, new Callback<Boolean>() {
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
        */
    }

    //final int valuesToInsert = 10000000;
    final int valuesToInsert = 10000000;

    final long timeOrigin = 1000;

    private void test(final String name, final Graph graph, final Callback<Boolean> testEnd) {
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                final long before = System.currentTimeMillis();
                Node node = graph.newNode(0, 0);
                final DeferCounter counter = graph.newCounter(valuesToInsert);
                for (long i = 0; i < valuesToInsert; i++) {

                    if (i % 1000000 == 0) {
                        System.out.println("<insert til " + i + " in " + (System.currentTimeMillis() - before) / 1000 + "s");
                    }

                    final double value = i * 0.3;
                    final long time = timeOrigin + i;
                    graph.lookup(0, time, node.id(), new Callback<Node>() {
                        @Override
                        public void on(Node timedNode) {
                            timedNode.set("value", Type.DOUBLE, value);
                            counter.count();
                            timedNode.free();//free the node, for cache management
                        }
                    });
                }
                node.free();

                counter.then(new Job() {
                    @Override
                    public void run() {

                        long beforeRead = System.currentTimeMillis();

                        System.out.println("<end insert phase>" + " " + (System.currentTimeMillis() - before) / 1000 + "s");
                        System.out.println(name + " result: " + (valuesToInsert / ((System.currentTimeMillis() - before) / 1000) / 1000) + "kv/s");

                        testEnd.on(true);
                    }
                });

            }
        });
    }

}
