package greycatTest.workers;

import greycat.*;
import greycat.plugin.Plugin;
import greycat.plugin.Storage;
import greycat.plugin.StorageFactory;
import greycat.struct.Buffer;
import greycat.workers.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNull;

/**
 * @ignore ts
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GraphWorkerSuspendTest {

    private static Thread localThread;
    private static TestGraphWorker localWorker;

    private static String suspendActionName = "suspend";

    @BeforeClass
    public static void setUp() {

        Plugin mockPlugin = new Plugin() {
            @Override
            public void start(Graph graph) {
                graph.actionRegistry().getOrCreateDeclaration(suspendActionName).setFactory(params -> new Action() {
                    @Override
                    public void eval(TaskContext ctx) {
                        int[] ids = ctx.suspendTask();
                        new Thread(() -> {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            GraphWorker.wakeup(ids,"hello from the other side");
                        }).start();
                    }

                    @Override
                    public void serialize(Buffer builder) {

                    }

                    @Override
                    public String name() {
                        return suspendActionName;
                    }
                });
            }

            @Override
            public void stop() {

            }
        };

        GraphBuilder graphBuilder = GraphBuilder.newBuilder().withPlugin(mockPlugin);
        WorkerBuilderFactory defaultRootFactory = () -> DefaultRootWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder);
        WorkerBuilderFactory defaultFactory = () -> DefaultWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder.clone().withStorageFactory(new StorageFactory() {
            @Override
            public Storage build() {
                return new SlaveWorkerStorage();
            }

            @Override
            public void listen(Callback<Buffer> synCallback) {

            }
        }));

        GraphWorkerPool workersPool = GraphWorkerPool.getInstance()
                .withRootWorkerBuilderFactory(defaultRootFactory)
                .withDefaultWorkerBuilderFactory(defaultFactory);
        workersPool.initialize();
        for (int i = 0; i < 3; i++) {
            workersPool.createWorker(WorkerAffinity.GENERAL_PURPOSE_WORKER, "GeneralPurposeWorker_" + i, null);
        }

        localWorker = (TestGraphWorker) TestWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder).withName("TestWorker").withKind(WorkerAffinity.TASK_WORKER).build();
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
    public void _00_suspend() {
        try {
            CountDownLatch latch = new CountDownLatch(1);

            Task createNode = Tasks.newTask().action(suspendActionName).log("{{result}}", "debug");
            localWorker.submitTask(createNode, creationResult -> {
                if (creationResult.exception() != null) {
                    creationResult.exception().printStackTrace();
                }
                assertEquals(1, creationResult.size());
                assertTrue(creationResult.get(0) instanceof String);
                assertEquals("hello from the other side", creationResult.get(0));
                assertNull(creationResult.exception());
                assertNull(creationResult.output());
                latch.countDown();
            }, localWorker.getId());

            latch.await();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
