package greycatTest.workers;

import greycat.*;
import greycat.plugin.*;
import greycat.struct.Buffer;
import greycat.workers.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import static greycat.Tasks.newTask;
import static org.junit.Assert.fail;

/**
 * @ignore ts
 */
public class SubTaskTest {
    private static String ROOT_ACTION = "RootAction";
    private static String SUB_ACTION = "SubAction";

    private static Graph graph;

    private static Task subTask;
    private static TaskContext subCtx;
    private static Task subTask2;
    private static TaskContext subCtx2;

    @BeforeClass
    public static void setUp() {
        CountDownLatch latch = new CountDownLatch(1);

        Log.LOG_LEVEL = Log.TRACE;

        GraphWorkerPool.NUMBER_OF_TASK_WORKER = 2;
        GraphWorkerPool.MAXIMUM_TASK_QUEUE_SIZE = 100;

        GraphBuilder graphBuilder = GraphBuilder.newBuilder();
        graph = graphBuilder.build();

        graphBuilder.withPlugin(new Plugin() {
            @Override
            public void start(Graph graph) {
                graph.actionRegistry().getOrCreateDeclaration(ROOT_ACTION)
                        .setFactory(params -> new Action() {
                            @Override
                            public void eval(TaskContext ctx) {
                                subTask = Tasks.newTask().action(SUB_ACTION);
                                subCtx = subTask.prepare(ctx.graph(), null, null);

                                subTask2 = Tasks.newTask().action(SUB_ACTION);
                                subCtx2 = subTask2.prepare(ctx.graph(), null, null);

                                ctx.continueWhenAllFinished(
                                        Arrays.asList(subTask, subTask2),
                                        Arrays.asList(subCtx, subCtx2),
                                        Arrays.asList("subTask", "subTask2")
                                );
                            }

                            @Override
                            public void serialize(Buffer buffer) {
                            }

                            @Override
                            public String name() {
                                return ROOT_ACTION;
                            }
                        });

                graph.actionRegistry().getOrCreateDeclaration(SUB_ACTION).setFactory(params -> new Action() {
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
                                })
                                .thenDo(taskContext -> {
                                    System.out.println("done with waiting");
                                    taskContext.continueWith(taskContext.newResult().add("5"));
                                })
                                ;
                        internalSubTask.executeFrom(ctx, ctx.result(), SchedulerAffinity.ANY_LOCAL_THREAD, ctx::continueWith);
                    }

                    @Override
                    public void serialize(Buffer builder) {
                    }

                    @Override
                    public String name() {
                        return SUB_ACTION;
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
    public void subTaskFinishTest() {
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


    @Test
    public void subTaskKillTest() {
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
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        subCtx2.graph().taskContextRegistry().forceStop(0);

        System.out.println(GraphWorkerPool.getInstance().tasksStats());
        System.out.println(subCtx2.graph().taskContextRegistry().stats());


        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
