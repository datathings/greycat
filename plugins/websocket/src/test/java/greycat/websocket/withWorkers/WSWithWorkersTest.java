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
import greycat.websocket.WSClientForWorkers;
import greycat.websocket.WSServerWithWorkers;
import greycat.workers.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNull;

/**
 * Created by Gregory NAIN on 2019-03-15.
 */
public class WSWithWorkersTest {

    private static WSServerWithWorkers wsServer;

    @BeforeClass
    public static void setUp() {
        GraphBuilder graphBuilder = GraphBuilder.newBuilder().withPlugin(new PluginForWorkersTest());
        WorkerBuilderFactory defaultFactory = () -> DefaultWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder);
        WorkerBuilderFactory defaultRootFactory = () -> DefaultRootWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder);

        GraphWorkerPool workersPool = GraphWorkerPool.getInstance()
                .withRootWorkerBuilderFactory(defaultRootFactory)
                .withDefaultWorkerBuilderFactory(defaultFactory);
        workersPool.initialize();
        workersPool.createGraphWorker(WorkerAffinity.GENERAL_PURPOSE_WORKER);
        wsServer = new WSServerWithWorkers(1234);
        wsServer.start();
    }

    @Test
    public void connectionTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        GraphBuilder builder = GraphBuilder.newBuilder().withPlugin(new PluginForWorkersTest()).withStorage(new WSClientForWorkers("ws://localhost:1234/ws"));
        Graph graph = builder.build();
        graph.connect(graphConnected -> {

            Task createNode = Tasks.newTask()
                    .declareIndex("nodes", "name")
                    .createNode()
                    .setAttribute("name", Type.STRING, "Node 0")
                    .updateIndex("nodes")
                    .save();
            createNode.executeRemotely(graph, creationResult -> {
                if (creationResult.exception() != null) {
                    creationResult.exception().printStackTrace();
                }
                long ts1 = System.currentTimeMillis();
                assertNull(creationResult.exception());
                assertNull(creationResult.output());
                assertEquals(1, creationResult.size());
                assertTrue(creationResult.get(0) instanceof Node);
                assertEquals("Node 0", ((Node) creationResult.get(0)).get("name"));

                graph.disconnect(disconnected -> {
                    latch.countDown();
                });
            });
        });
        latch.await();
    }


    @AfterClass
    public static void tearDown() {
        wsServer.stop();
    }


}
