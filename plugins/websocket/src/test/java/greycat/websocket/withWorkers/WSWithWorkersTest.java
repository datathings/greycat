package greycat.websocket.withWorkers;

import greycat.*;
import greycat.websocket.WSClientForWorkers;
import greycat.websocket.WSServerWithWorkers;
import greycat.workers.GraphWorkerPool;
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
        Constants.enableDebug = false;
        GraphWorkerPool.getInstance().initialize(GraphBuilder.newBuilder());
        GraphWorkerPool.getInstance().createGraphWorker();
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
                    .updateIndex("nodes");
            createNode.executeRemotely(graph, creationResult -> {
                if (creationResult.exception() != null) {
                    creationResult.exception().printStackTrace();
                }
                long ts1 = System.currentTimeMillis();
                assertEquals(1, creationResult.size());
                assertTrue(creationResult.get(0) instanceof Node);
                assertEquals("Node 0", ((Node) creationResult.get(0)).get("name"));
                assertNull(creationResult.exception());
                assertNull(creationResult.output());

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
