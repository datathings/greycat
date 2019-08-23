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
package greycat.websocket.withWorkers;

import greycat.*;
import greycat.plugin.Job;
import greycat.websocket.WSClientForWorkers;
import greycat.websocket.WSServerWithWorkers;
import greycat.workers.*;
import org.junit.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Created by Gregory NAIN on 2019-03-15.
 */
public class WSWithWorkersMultiCLientTest {

    private WSServerWithWorkers wsServer;

    @Before
    public void setUp() {
        GraphBuilder graphBuilder = GraphBuilder.newBuilder().withPlugin(new PluginForWorkersTest());
        WorkerBuilderFactory defaultFactory = () -> DefaultWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder);
        WorkerBuilderFactory defaultRootFactory = () -> DefaultRootWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder);

        GraphWorkerPool workersPool = GraphWorkerPool.getInstance()
                .withRootWorkerBuilderFactory(defaultRootFactory)
                .withDefaultWorkerBuilderFactory(defaultFactory);
        workersPool.initialize();

        for (int i = 0; i < 5; i++) {
            GraphWorkerPool.getInstance().createWorker(WorkerAffinity.GENERAL_PURPOSE_WORKER, "GeneralPurposeWorker_" + i, null);
        }
        wsServer = new WSServerWithWorkers(1234);
        wsServer.start();
    }

    @Test
    public void multiTaskOneClientTest() {
        try {

            int nbTasks = 10;

            AtomicInteger reportsCounter = new AtomicInteger();
            CountDownLatch latch = new CountDownLatch(1);
            runClient(nbTasks, latch, reportsCounter);
            latch.await();

            assertEquals(10 * nbTasks, reportsCounter.get());

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void runClient(int nbTasks, CountDownLatch clientsLatch, AtomicInteger reportsCounter) {
        try {

            CountDownLatch latch = new CountDownLatch(nbTasks * 11);
            GraphBuilder builder = GraphBuilder.newBuilder().withPlugin(new PluginForWorkersTest()).withStorage(new WSClientForWorkers("ws://localhost:1234/ws"));
            Graph graph = builder.build();
            graph.connect(graphConnected -> {

                long ts0 = System.currentTimeMillis();

                Task withProgress = Tasks.newTask().action(greycatTest.workers.PluginForWorkersTest.PROGRESS_REPORTS);
                TaskContext taskContext = withProgress.prepare(graph, null, result -> {
                    if (result.exception() != null) {
                        result.exception().printStackTrace();
                    }
                    long ts1 = System.currentTimeMillis();
                    //assertEquals(10, reportsCount.get());
                    //System.out.println("Duration: " + (ts1 - ts0) + "ms");
                    latch.countDown();
                });
                taskContext.setProgressHook(result -> {
                    //System.out.println("Progress: " + result.progress() + " Comment:" + result.comment());
                    reportsCounter.incrementAndGet();
                    latch.countDown();
                });
                for (int i = 0; i < nbTasks; i++) {
                    taskContext.setVariable("taskId", "" + i);
                    withProgress.executeRemotelyUsing(taskContext);
                }
            });

            latch.await();
            graph.disconnect((e) -> {
                if (clientsLatch != null) {
                    clientsLatch.countDown();
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void multiTaskMultiClientTest() {

        try {
            int nbTasks = 4;
            int nbClients = 3;

            CountDownLatch clientsLatch = new CountDownLatch(nbClients);
            AtomicInteger reportsCounter = new AtomicInteger();
            for (int i = 0; i < nbClients; i++) {
                new Thread(() -> runClient(nbTasks, clientsLatch, reportsCounter)).start();
            }

            clientsLatch.await();
            assertEquals(10 * nbTasks * nbClients, reportsCounter.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        wsServer.stop();
    }


}
