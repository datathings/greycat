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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static greycat.Tasks.newTask;
import static org.junit.Assert.*;


/**
 * @ignore ts
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GraphWorkerTest {

    private static Thread localThread;
    private static TestGraphWorker localWorker;
    private static int worker1Id;

    @BeforeClass
    public static void setUp() {
        GraphBuilder graphBuilder = GraphBuilder.newBuilder().withPlugin(new PluginForWorkersTest());
        WorkerBuilderFactory defaultFactory = () -> DefaultWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder);
        WorkerBuilderFactory defaultRootFactory = () -> DefaultRootWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder);

        GraphWorkerPool workersPool = GraphWorkerPool.getInstance()
                .withRootWorkerBuilderFactory(defaultRootFactory)
                .withDefaultWorkerBuilderFactory(defaultFactory);
        workersPool.initialize();
        for(int i = 0; i < 3; i++) {
         workersPool.createWorker(WorkerAffinity.GENERAL_PURPOSE_WORKER, "GeneralPurposeWorker_" + i, null);
        }

        localWorker = (TestGraphWorker) TestWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder).withName("TestWorker").withKind(WorkerAffinity.GENERAL_PURPOSE_WORKER).build();
        localThread = new Thread(localWorker, "TestWorker");
        localThread.start();
    }

    @AfterClass
    public static void tearDown() {
        try {
            //System.out.println("Halting local worker");
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

    @Test
    public void _00_addNodeToCoreGraph() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            long ts0 = System.currentTimeMillis();
            Task createNode = newTask()
                    .declareIndex("nodes", "name")
                    .createNode()
                    .setAttribute("name", Type.STRING, "Node 0")
                    .updateIndex("nodes");
            localWorker.submitTask(createNode, creationResult -> {
                if (creationResult.exception() != null) {
                    creationResult.exception().printStackTrace();
                }
                long ts1 = System.currentTimeMillis();
                assertEquals(1, creationResult.size());
                assertTrue(creationResult.get(0) instanceof Node);
                assertEquals("Node 0", ((Node) creationResult.get(0)).get("name"));
                assertNull(creationResult.exception());
                assertNull(creationResult.output());
                //System.out.println("Duration: " + (ts1 - ts0) + "ms");
                latch.countDown();
            }, GraphWorkerPool.getInstance().getRootWorkerMailboxId());

            latch.await();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    Task nodeLookup = newTask().readIndex("nodes", "Node 0");

    @Test
    public void _01_lookupNodeFromWorkerA() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            long ts0 = System.currentTimeMillis();
            localWorker.submitTask(nodeLookup, lookupResult -> {
                long ts1 = System.currentTimeMillis();
                assertEquals(1, lookupResult.size());
                assertTrue(lookupResult.get(0) instanceof Node);
                assertEquals("Node 0", ((Node) lookupResult.get(0)).get("name"));
                assertNull(lookupResult.exception());
                assertNull(lookupResult.output());
                //System.out.println("Duration: " + (ts1 - ts0) + "ms");
                latch.countDown();
            }, localWorker.getId());
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void _02_lookupNodeAgainFromWorkerA() {
        try {
            CountDownLatch latch = new CountDownLatch(1);

            long ts0 = System.currentTimeMillis();

            localWorker.submitTask(nodeLookup, lookupResult -> {
                long ts1 = System.currentTimeMillis();
                assertEquals(1, lookupResult.size());
                assertTrue(lookupResult.get(0) instanceof Node);
                assertEquals("Node 0", ((Node) lookupResult.get(0)).get("name"));
                assertNull(lookupResult.exception());
                assertNull(lookupResult.output());
                //System.out.println("Duration: " + (ts1 - ts0) + "ms");
                latch.countDown();
            }, localWorker.getId());
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void _10_actionCreateOnWorkerA() {
        try {
            CountDownLatch latch = new CountDownLatch(1);

            long ts0 = System.currentTimeMillis();

            localWorker.submitTask(newTask().action(PluginForWorkersTest.CREATE_NODE), actionAresult -> {
                long ts1 = System.currentTimeMillis();
                assertEquals(1, actionAresult.size());
                assertTrue(actionAresult.get(0) instanceof Node);
                assertNull(actionAresult.exception());
                assertNull(actionAresult.output());
                //System.out.println("Duration: " + (ts1 - ts0) + "ms");
                latch.countDown();
            }, localWorker.getId());

            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void _11_actionExceptionOnWorkerA() {
        try {
            CountDownLatch latch = new CountDownLatch(1);

            long ts0 = System.currentTimeMillis();

            localWorker.submitTask(newTask().action(PluginForWorkersTest.THROW_EXCEPTION), exceptionAresult -> {
                long ts1 = System.currentTimeMillis();
                assertNotNull(exceptionAresult.exception());
                //System.out.println("Duration: " + (ts1 - ts0) + "ms");
                latch.countDown();
            }, localWorker.getId());

            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void _12_actionException2OnWorkerA() {
        try {
            CountDownLatch latch = new CountDownLatch(1);

            long ts0 = System.currentTimeMillis();

            localWorker.submitTask(newTask().action(PluginForWorkersTest.THROW_EXCEPTION_2), exceptionAresult -> {
                long ts1 = System.currentTimeMillis();
                assertNotNull(exceptionAresult.exception());
                //System.out.println("Duration: " + (ts1 - ts0) + "ms");
                latch.countDown();
            }, localWorker.getId());

            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void _13_actionWithProgressReportsOnWorkerA() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger reportsCount = new AtomicInteger();

            long ts0 = System.currentTimeMillis();

            Task withProgress = newTask().action(PluginForWorkersTest.PROGRESS_REPORTS);
            TaskContext taskContext = withProgress.prepare(null, null, result -> {
                long ts1 = System.currentTimeMillis();
                assertEquals(10, reportsCount.get());
                //System.out.println("Duration: " + (ts1 - ts0) + "ms");
                latch.countDown();
            });
            taskContext.setProgressHook(result -> {
                //System.out.println("Progress: " + result.progress() + " Comment:" + result.comment());
                reportsCount.getAndIncrement();
            });

            localWorker.submitPreparedTask(withProgress, taskContext, localWorker.getId());
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void _14_actionWithPrintHookOnWorkerA() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger reportsCount = new AtomicInteger();

            long ts0 = System.currentTimeMillis();

            Task withProgress = newTask().action(PluginForWorkersTest.PRINT_HOOK);
            TaskContext taskContext = withProgress.prepare(null, null, result -> {
                long ts1 = System.currentTimeMillis();
                assertEquals(10, reportsCount.get());
                //System.out.println("Duration: " + (ts1 - ts0) + "ms");
                latch.countDown();
            });
            taskContext.setPrintHook(result -> {
                //System.out.println("Printing:" + result);
                reportsCount.getAndIncrement();
            });

            localWorker.submitPreparedTask(withProgress, taskContext, localWorker.getId());
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void _15_GPPool() {
        try {

            int nbTasks = 10;

            CountDownLatch latch = new CountDownLatch(nbTasks);
            AtomicInteger reportsCount = new AtomicInteger();

            long ts0 = System.currentTimeMillis();

            Task withProgress = newTask().action(PluginForWorkersTest.PROGRESS_REPORTS);
            TaskContext taskContext = withProgress.prepare(null, null, result -> {
                long ts1 = System.currentTimeMillis();
                //assertEquals(10, reportsCount.get());
                //System.out.println("Duration: " + (ts1 - ts0) + "ms");
                latch.countDown();
            });
            taskContext.setProgressHook(result -> {
                //System.out.println("Progress: " + result.progress() + " Comment:" + result.comment());
                reportsCount.incrementAndGet();
            });
            for (int i = 0; i < nbTasks; i++) {
                localWorker.submitPreparedTask(withProgress, taskContext, MailboxRegistry.getInstance().getDefaultMailboxId());
            }
            latch.await();
            assertEquals(10 * nbTasks, reportsCount.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    @Test
    public void _16_taskWorker() {
        try {

            CountDownLatch latch = new CountDownLatch(1);

            GraphWorker taskWorker = GraphWorkerPool.getInstance().createWorker(WorkerAffinity.TASK_WORKER, "TaskWorker", null);

            taskWorker.submitTask(
                    newTask().log("inTask","debug"),
            lookupResult -> {
                System.out.println("Got result");
               // assertEquals("1", lookupResult.get(0));
                latch.countDown();
            });

            boolean gotFreed = latch.await(5, TimeUnit.SECONDS);
            assertEquals(true, gotFreed);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


}
