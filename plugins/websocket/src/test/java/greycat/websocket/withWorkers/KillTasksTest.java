package greycat.websocket.withWorkers;

import greycat.*;
import greycat.websocket.WSClientForWorkers;
import greycat.websocket.WSServerWithWorkers;
import greycat.workers.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static greycat.Tasks.newTask;

public class KillTasksTest {

    private static WSServerWithWorkers wsServer;

    @BeforeClass
    public static void setUp() {
        CountDownLatch latch = new CountDownLatch(1);

        //Log.LOG_LEVEL = Log.TRACE;

        GraphWorkerPool.NUMBER_OF_TASK_WORKER = 3;
        GraphWorkerPool.MAXIMUM_TASK_QUEUE_SIZE = 100;

        GraphBuilder graphBuilder = GraphBuilder.newBuilder().withPlugin(new PluginForWorkersTest());
        WorkerBuilderFactory defaultRootFactory = () -> DefaultRootWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder);
        WorkerBuilderFactory defaultFactory = () -> DefaultWorkerBuilder.newBuilder().withGraphBuilder(graphBuilder);

        GraphWorkerPool workersPool = GraphWorkerPool.getInstance()
                .withRootWorkerBuilderFactory(defaultRootFactory)
                .withDefaultWorkerBuilderFactory(defaultFactory);

        workersPool.setOnPoolReady((worker, allSet) -> {
            allSet.run();
            latch.countDown();
        });
        workersPool.initialize();

        GraphWorkerPool.getInstance().createWorker(WorkerAffinity.GENERAL_PURPOSE_WORKER, "GP1", null);

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        wsServer = new WSServerWithWorkers(3003);
        wsServer.start();
    }

    @Test
    public void subTasksTest() {
        CountDownLatch latch = new CountDownLatch(1);

        Graph clientGraph = GraphBuilder.newBuilder().withStorage(new WSClientForWorkers("ws://localhost:3003/ws")).build();
        clientGraph.connect(connected -> {
            System.out.println("Client Graph connected:" + connected);
            Task task = newTask().action(PluginForWorkersTest.PARENT_TASK_LAUNCHER);
            TaskContext taskContext = task.prepare(clientGraph, null, result -> {
                System.out.println("rootTask finished");
                if (result.exception() != null) {
                    result.exception().printStackTrace();
                }
                clientGraph.disconnect(disconnected -> {
                    System.out.println("Client Graph disconnected: " + disconnected);

                    latch.countDown();
                });
            });
            taskContext.setWorkerAffinity(WorkerAffinity.TASK_WORKER);
            System.out.println("Task submitted");
            task.executeRemotelyUsing(taskContext);

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

        WSClientForWorkers wsClient = new WSClientForWorkers("ws://localhost:3003/ws");
        Graph clientGraph = GraphBuilder.newBuilder().withStorage(wsClient).build();
        clientGraph.connect(connected -> {
            System.out.println("Client Graph connected:" + connected);

            Task task = newTask().action(PluginForWorkersTest.PARENT_TASK_LAUNCHER);
            TaskContext taskContext = task.prepare(clientGraph, null, result -> {
                System.out.println("rootTask finished");
                if (result.exception() != null) {
                    result.exception().printStackTrace();
                }
                clientGraph.disconnect(disconnected -> {
                    System.out.println("Client Graph disconnected: " + disconnected);

                    latch.countDown();
                });
            });
            taskContext.setWorkerAffinity(WorkerAffinity.TASK_WORKER);
            System.out.println("Root task submitted");
            task.executeRemotelyUsing(taskContext);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            wsClient.workerTaskStop("subTask3", 0, registered -> {
                System.out.println("subTask3 kill registered");
            });

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            wsClient.workerTaskStop("subTask2", 0, registered -> {
                System.out.println("subTask2 kill registered");
            });


        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void rootTaskKillTest() {
        CountDownLatch latch = new CountDownLatch(1);

        WSClientForWorkers wsClient = new WSClientForWorkers("ws://localhost:3003/ws");
        Graph clientGraph = GraphBuilder.newBuilder().withStorage(wsClient).build();
        clientGraph.connect(connected -> {
            System.out.println("Client Graph connected:" + connected);

            Task task = newTask().action(PluginForWorkersTest.PARENT_TASK_LAUNCHER);
            TaskContext taskContext = task.prepare(clientGraph, null, result -> {
                System.out.println("rootTask finished");
                if (result.exception() != null) {
                    result.exception().printStackTrace();
                }
                clientGraph.disconnect(disconnected -> {
                    System.out.println("Client Graph disconnected: " + disconnected);

                    latch.countDown();
                });
            });
            taskContext.setWorkerAffinity(WorkerAffinity.TASK_WORKER);
            System.out.println("Root task submitted");
            task.executeRemotelyUsing(taskContext);

            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            wsClient.workerTaskStop("TaskWorker(-2147483646)", 0, registered -> {
                System.out.println("root kill registered");
            });

        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @AfterClass
    public static void tearDown() {

        if (wsServer != null) {
            wsServer.stop();
        }
        GraphWorkerPool.getInstance().halt();

    }


}
