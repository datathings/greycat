package greycatTest.workers;

import greycat.*;
import greycat.plugin.Storage;
import greycat.plugin.StorageFactory;
import greycat.struct.Buffer;
import greycat.workers.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * @ignore ts
 */
public class SubTaskTest {
    private static String ROOT_ACTION = "RootAction";

    @BeforeClass
    public static void setUp() {

        CountDownLatch latch = new CountDownLatch(1);

        GraphWorkerPool.NUMBER_OF_TASK_WORKER = 4;
        GraphWorkerPool.MAXIMUM_TASK_QUEUE_SIZE = 100;

        GraphBuilder graphBuilder = GraphBuilder.newBuilder();
        Graph graph = graphBuilder.build();
        graph.actionRegistry().getOrCreateDeclaration(ROOT_ACTION)
                .setFactory(params -> new RootAction());

        WorkerBuilderFactory defaultRootFactory = () -> DefaultRootWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder);
        WorkerBuilderFactory defaultFactory = () -> DefaultWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder);

        GraphWorkerPool workersPool = GraphWorkerPool.getInstance()
                .withRootWorkerBuilderFactory(defaultRootFactory)
                .withDefaultWorkerBuilderFactory(defaultFactory);

        workersPool.setOnPoolReady((worker, allSet) -> {
           latch.countDown();
            allSet.run();
        });
        workersPool.initialize();

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @AfterClass
    public static void tearDown() {
        GraphWorkerPool.getInstance().halt();
    }


    @Test
    public void createSubTaskTest() {
        final GraphWorker rootWorker = GraphWorkerPool.getInstance().createWorker(WorkerAffinity.TASK_WORKER, "rootWorker", null);
        rootWorker.submitTask(Tasks.newTask().action(ROOT_ACTION), result -> {
            System.out.println("rootTask finished");
        });
    }

    private static Task subTask = Tasks.newTask()
            .thenDo(ctx -> {
                while (true) {
                    try {
                        Thread.sleep(5000);
                        System.out.println("waiting...");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //ctx.continueTask();
            });

    private static class RootAction implements Action {

        @Override
        public void eval(TaskContext ctx) {
            final GraphWorker subWorker = GraphWorkerPool.getInstance().createWorker(WorkerAffinity.TASK_WORKER, "subWorker", null);
            subWorker.submitTask(subTask, result -> {
                System.out.println("subTask done");
            });

            ctx.continueTask();
        }

        @Override
        public void serialize(Buffer buffer) {
            buffer.writeString(name());
            buffer.writeChar(Constants.TASK_PARAM_OPEN);
            buffer.writeChar(Constants.TASK_PARAM_CLOSE);
        }

        @Override
        public String name() {
            return ROOT_ACTION;
        }
    }
}
