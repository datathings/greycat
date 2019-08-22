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
package greycatTest.workers;

import greycat.*;
import greycat.workers.*;
import greycatTest.internal.MockStorage;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @ignore ts
 */
public class GraphWorkerSpeedTest {

    public static final Task createAndIndex = Tasks.newTask()
            .declareVar("inserted")
            .declareIndex("nodes", "name")
            .loop("0", "999",
                    Tasks.newTask()
                            .createNode()
                            .setAttribute("name", Type.STRING, "node_{{i}}")
                            .updateIndex("nodes")
                            .readVar("i")
                            .setAsVar("inserted")
            )
            .save()
            .readVar("inserted");

    public static final Task lookupNodes = Tasks.newTask()
            .readIndex("nodes");

    private static final MockStorage baseStorage = new MockStorage();
    private static final MockStorage queueStorage = new MockStorage();

    public static void main(String[] args) {
        baseSpeedTest();
        baseColdLoadSpeedTest();
        queueSpeedTest();
        queueSpeedColdTest();
    }

    private static void baseSpeedTest() {
        try {
            CountDownLatch allDoneLatch = new CountDownLatch(1);
            Graph graph = GraphBuilder.newBuilder().withStorage(baseStorage).build();
            graph.connect(connected -> {

                long ts0 = System.currentTimeMillis();
                createAndIndex.execute(graph, insertionResult -> {
                    long ts1 = System.currentTimeMillis();

                    assertNull(insertionResult.exception());
                    assertEquals(999, (int) insertionResult.get(0));

                    long ts2 = System.currentTimeMillis();
                    lookupNodes.execute(graph, lookupResult -> {
                        long ts3 = System.currentTimeMillis();

                        assertNull(lookupResult.exception());
                        assertEquals(1000, lookupResult.size());

                        long ts4 = System.currentTimeMillis();
                        lookupNodes.execute(graph, lookup2Result -> {
                            long ts5 = System.currentTimeMillis();

                            assertNull(lookup2Result.exception());
                            assertEquals(1000, lookup2Result.size());

                            System.out.println("------- Native Speed --------");
                            System.out.println("Insertion: \t" + (ts1 - ts0) + "ms");
                            System.out.println("Lookup1: \t" + (ts3 - ts2) + "ms");
                            System.out.println("Lookup2: \t" + (ts5 - ts4) + "ms");
                            allDoneLatch.countDown();

                        });
                    });
                });
            });
            allDoneLatch.await();
            graph.save(saved -> {
                graph.disconnect(disconnected -> {

                });
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private static void baseColdLoadSpeedTest() {
        try {
            CountDownLatch allDoneLatch = new CountDownLatch(1);
            Graph graph = GraphBuilder.newBuilder().withStorage(baseStorage).build();
            graph.connect(connected -> {
                long ts0 = System.currentTimeMillis();
                lookupNodes.execute(graph, lookupResult -> {
                    long ts1 = System.currentTimeMillis();

                    assertNull(lookupResult.exception());
                    assertEquals(1000, lookupResult.size());

                    long ts2 = System.currentTimeMillis();
                    lookupNodes.execute(graph, lookup2Result -> {
                        long ts3 = System.currentTimeMillis();

                        assertNull(lookup2Result.exception());
                        assertEquals(1000, lookup2Result.size());

                        System.out.println("------- Native Cold Speed --------");
                        System.out.println("Lookup1: \t" + (ts1 - ts0) + "ms");
                        System.out.println("Lookup2: \t" + (ts3 - ts2) + "ms");
                        graph.disconnect(disconnected -> {
                            allDoneLatch.countDown();
                        });

                    });
                });
            });
            allDoneLatch.await();
            graph.save(saved -> {
                graph.disconnect(disconnected -> {

                });
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private static void queueSpeedTest() {
        try {

            CountDownLatch allDoneLatch = new CountDownLatch(1);

            GraphBuilder graphBuilder = GraphBuilder.newBuilder().withStorage(queueStorage);
            WorkerBuilderFactory defaultFactory = () -> DefaultWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder);
            WorkerBuilderFactory defaultRootFactory = () -> DefaultRootWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder);

            GraphWorkerPool workersPool = GraphWorkerPool.getInstance()
                    .withRootWorkerBuilderFactory(defaultRootFactory)
                    .withDefaultWorkerBuilderFactory(defaultFactory);
            workersPool.initialize();

            TestGraphWorker localWorker = (TestGraphWorker) TestWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder).withName("TestWorker").withWorkerKind(WorkerAffinity.GENERAL_PURPOSE_WORKER).build();
            Thread localThread = new Thread(localWorker, "TestWorker");
            localThread.start();

            long ts0 = System.currentTimeMillis();
            localWorker.submitTask(createAndIndex, insertionResult -> {
                long ts1 = System.currentTimeMillis();

                assertNull(insertionResult.exception());
                assertEquals(999, (int) insertionResult.get(0));

                long ts2 = System.currentTimeMillis();
                localWorker.submitTask(lookupNodes, lookupResult -> {
                    long ts3 = System.currentTimeMillis();

                    assertNull(lookupResult.exception());
                    assertEquals(1000, lookupResult.size());

                    long ts4 = System.currentTimeMillis();
                    localWorker.submitTask(lookupNodes, lookup2Result -> {
                        long ts5 = System.currentTimeMillis();

                        assertNull(lookup2Result.exception());
                        assertEquals(1000, lookup2Result.size());

                        System.out.println("------- Queues Speed --------");
                        System.out.println("Insertion: \t" + (ts1 - ts0) + "ms");
                        System.out.println("Lookup1: \t" + (ts3 - ts2) + "ms");
                        System.out.println("Lookup2: \t" + (ts5 - ts4) + "ms");
                        allDoneLatch.countDown();

                    }, localWorker.getId());

                }, localWorker.getId());

            }, localWorker.getId());


            allDoneLatch.await();
           // System.out.println("Halting local worker");
            localWorker.halt();
           // System.out.println("Joining local thread");
            localThread.interrupt();
            localThread.join();
           // System.out.println("Halting GraphWorkersPool");
            GraphWorkerPool.getInstance().halt();
           // System.out.println("Bye.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static void queueSpeedColdTest() {
        try {

            CountDownLatch allDoneLatch = new CountDownLatch(1);


            GraphBuilder graphBuilder = GraphBuilder.newBuilder().withStorage(queueStorage);
            WorkerBuilderFactory defaultFactory = () -> DefaultWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder);
            WorkerBuilderFactory defaultRootFactory = () -> DefaultRootWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder);

            GraphWorkerPool workersPool = GraphWorkerPool.getInstance()
                    .withRootWorkerBuilderFactory(defaultRootFactory)
                    .withDefaultWorkerBuilderFactory(defaultFactory);
            workersPool.initialize();

            TestGraphWorker localWorker = (TestGraphWorker) TestWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder).withName("TestWorker").withWorkerKind(WorkerAffinity.GENERAL_PURPOSE_WORKER).build();
            Thread localThread = new Thread(localWorker, "TestWorker");
            localThread.start();

            long ts0 = System.currentTimeMillis();
            localWorker.submitTask(lookupNodes, lookupResult -> {
                long ts1 = System.currentTimeMillis();

                assertNull(lookupResult.exception());
                assertEquals(1000, lookupResult.size());

                long ts2 = System.currentTimeMillis();
                localWorker.submitTask(lookupNodes, lookup2Result -> {
                    long ts3 = System.currentTimeMillis();

                    assertNull(lookup2Result.exception());
                    assertEquals(1000, lookup2Result.size());

                    System.out.println("------- Queues Cold Speed --------");
                    System.out.println("Lookup1: \t" + (ts1 - ts0) + "ms");
                    System.out.println("Lookup2: \t" + (ts3 - ts2) + "ms");
                    allDoneLatch.countDown();

                }, localWorker.getId());
            }, localWorker.getId());


            allDoneLatch.await();
           // System.out.println("Halting local worker");
            localWorker.halt();
            //System.out.println("Joining local thread");
            localThread.interrupt();
            localThread.join();
            //System.out.println("Halting GraphWorkersPool");
            GraphWorkerPool.getInstance().halt();
            //System.out.println("Bye.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }


}
