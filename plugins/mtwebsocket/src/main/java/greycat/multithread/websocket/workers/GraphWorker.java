package greycat.multithread.websocket.workers;

import greycat.*;
import greycat.internal.heap.HeapBuffer;
import greycat.internal.task.CoreProgressReport;
import greycat.multithread.websocket.message.GraphExecutorMessage;
import greycat.multithread.websocket.message.GraphMessage;
import greycat.multithread.websocket.message.TaskMessage;
import greycat.struct.Buffer;
import greycat.utility.Base64;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static greycat.multithread.websocket.Constants.*;

public class GraphWorker extends Thread {
    private final BlockingQueue<GraphExecutorMessage> graphInput;
    private final BlockingQueue<GraphMessage> output;
    private final BlockingQueue<TaskMessage> taskQueue;
    private final BlockingQueue<Buffer> updateQueue;
    private final GraphBuilder graphBuilder;
    private final ConcurrentHashMap<Integer, TaskContextRegistry> registryConcurrentHashMap;
    private boolean running = false;


    public GraphWorker(BlockingQueue<GraphExecutorMessage> graphInput, BlockingQueue<GraphMessage> output, BlockingQueue<TaskMessage> taskQueue, GraphBuilder graphBuilder, ConcurrentHashMap<Integer, TaskContextRegistry> registryConcurrentHashMap) {
        this.graphInput = graphInput;
        this.output = output;
        this.taskQueue = taskQueue;
        this.graphBuilder = graphBuilder;
        this.registryConcurrentHashMap = registryConcurrentHashMap;
        updateQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {
        running = true;
        Graph graph = graphBuilder.build();

        graph.connect(on -> {
                    Buffer update;
                    while (running) {
                        try {
                            TaskMessage message = taskQueue.take();
                            while ((update = updateQueue.poll()) != null) {
                                graph.remoteNotify(update);
                                update.free();
                            }
                            executeTask(graph, message);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    graph.disconnect(done -> {
                        System.out.println("done disconnecting");
                    });
                }
        );

    }

    private void executeTask(Graph graph, TaskMessage message) {
        long timeStart =  System.currentTimeMillis();
        //loading the task
        Task recreatedTask = Tasks.newTask();
        recreatedTask.loadFromBuffer(message.getTask(), graph);

        //preparing context
        TaskContext ctx = recreatedTask.prepare(graph, null, new Callback<TaskResult>() {
            @Override
            public void on(TaskResult result) {
                if (result.notifications() != null && result.notifications().length() > 0) {
                    graph.remoteNotify(result.notifications());
                }
                //remove taskcontext as the task is done
                registryConcurrentHashMap.remove(message.getTaskId());
                if (message.getPrintcb() != null) {
                    message.getPrintcb().free();
                }
                if (message.getProgrescb() != null) {
                    message.getProgrescb().free();
                }
                Buffer res = new HeapBuffer();
                result.saveToBuffer(res);
                //disconnect the graph
                graph.save(on -> {
                    try {
                        //forward result
                        long timeStop =  System.currentTimeMillis();
                        graph.log().info("task executed in "+(timeStop-timeStart)+"ms");
                        output.put(new GraphMessage(RESP_TASK, message.getReturnId(), res, message.getCallback()));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

            }
        });

        ctx.silentSave();
        //Configure printhook
        if (message.getPrintcb() != null) {
            ctx.setPrintHook(result -> {
                Buffer buffer = new HeapBuffer();
                Base64.encodeStringToBuffer(result, buffer);
                Buffer print = new HeapBuffer();
                print.writeAll(message.getPrintcb().data());
                try {
                    output.put(new GraphMessage(NOTIFY_PRINT, message.getReturnId(), buffer, print));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        //configure progress hook
        if (message.getProgrescb() != null) {
            ctx.setProgressHook(result -> {
                Buffer buffer = new HeapBuffer();
                ((CoreProgressReport) result).saveToBuffer(buffer);
                Buffer reportProg = new HeapBuffer();
                reportProg.writeAll(message.getProgrescb().data());
                try {
                    output.put(new GraphMessage(NOTIFY_PROGRESS, message.getReturnId(), buffer, reportProg));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        //load context if necessary
        if (message.getContext() != null) {
            ctx.loadFromBuffer(message.getContext(), loaded -> {
                graph.taskContextRegistry().registerWith(ctx, message.getTaskId());
                message.getContext().free();
                registryConcurrentHashMap.put(message.getTaskId(), graph.taskContextRegistry());
                recreatedTask.executeUsing(ctx);
            });
        } else {
            graph.taskContextRegistry().registerWith(ctx, message.getTaskId());
            registryConcurrentHashMap.put(message.getTaskId(), graph.taskContextRegistry());
            recreatedTask.executeUsing(ctx);
        }
    }

    public BlockingQueue<Buffer> getUpdateQueue() {
        return updateQueue;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
