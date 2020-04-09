package greycatTest.workers;

import greycat.*;
import greycat.plugin.*;
import greycat.struct.Buffer;
import greycat.workers.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static greycat.Tasks.newTask;

/**
 * @ignore ts
 */
public class SubTaskTest {
    private static String ROOT_ACTION = "RootAction";
    private static String SUB_TASK = "SubTask";

    @BeforeClass
    public static void setUp() {

        CountDownLatch latch = new CountDownLatch(1);

        GraphWorkerPool.NUMBER_OF_TASK_WORKER = 4;
        GraphWorkerPool.MAXIMUM_TASK_QUEUE_SIZE = 100;

        GraphBuilder graphBuilder = GraphBuilder.newBuilder();

        graphBuilder.withPlugin(new Plugin() {
            @Override
            public void start(Graph graph) {

                graph.actionRegistry().getOrCreateDeclaration(ROOT_ACTION)
                        .setFactory(params -> new Action() {
                            @Override
                            public void eval(TaskContext ctx) {
                                final GraphWorker subWorker = GraphWorkerPool.getInstance().createWorker(WorkerAffinity.TASK_WORKER, "subWorker", null);
                                subWorker.submitTask(newTask().action(SUB_TASK), result -> {
                                    System.out.println("subTask done");
                                    if (result.exception() != null) {
                                        result.exception().printStackTrace();
                                    }
                                    ctx.continueTask();
                                });

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
                        });

                graph.actionRegistry().getOrCreateDeclaration(SUB_TASK).setFactory(params -> new Action() {

                    @Override
                    public void eval(TaskContext ctx) {
                        Task internalSubTask = newTask()
                                .thenDo(taskContext -> {
                                    boolean stop = false;
                                    while (!stop) {
                                        try {
                                            System.out.println("waiting...");
                                            Thread.sleep(2000);
                                            stop = true;
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        taskContext.continueTask();
                                    }
                                });
                        internalSubTask.executeFrom(ctx, ctx.result(), SchedulerAffinity.ANY_LOCAL_THREAD,ctx::continueWith);
                    }

                    @Override
                    public void serialize(Buffer builder) {
                    }

                    @Override
                    public String name() {
                        return null;
                    }
                });
            }

            @Override
            public void stop() {

            }
        });

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
        CountDownLatch latch = new CountDownLatch(1);

        final GraphWorker rootWorker = GraphWorkerPool.getInstance().createWorker(WorkerAffinity.TASK_WORKER, "rootWorker", null);
        rootWorker.submitTask(newTask().action(ROOT_ACTION), result -> {
            System.out.println("rootTask finished");
            if (result.exception() != null) {
                result.exception().printStackTrace();
            }
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
