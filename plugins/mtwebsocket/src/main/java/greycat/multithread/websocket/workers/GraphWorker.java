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
package greycat.multithread.websocket.workers;

import greycat.*;
import greycat.internal.heap.HeapBuffer;
import greycat.internal.task.CoreProgressReport;
import greycat.multithread.websocket.message.GraphExecutorMessage;
import greycat.multithread.websocket.message.GraphMessage;
import greycat.multithread.websocket.message.TaskMessage;
import greycat.plugin.Job;
import greycat.plugin.SchedulerAffinity;
import greycat.struct.Buffer;
import greycat.utility.Base64;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;

import static greycat.multithread.websocket.Constants.*;

public class GraphWorker extends Thread {
    private final BlockingQueue<GraphExecutorMessage> graphInput;
    private final BlockingQueue<GraphMessage> output;
    private final BlockingQueue<TaskMessage> taskQueue;
    private final BlockingQueue<Buffer> updateQueue;
    private final ConcurrentHashMap<Integer, TaskContextRegistry> registryConcurrentHashMap;
    private final Graph graph;
    private boolean running = false;


    public GraphWorker(BlockingQueue<GraphExecutorMessage> graphInput, BlockingQueue<GraphMessage> output, BlockingQueue<TaskMessage> taskQueue, GraphBuilder graphBuilder, ConcurrentHashMap<Integer, TaskContextRegistry> registryConcurrentHashMap) {
        this.graphInput = graphInput;
        this.output = output;
        this.taskQueue = taskQueue;
        this.registryConcurrentHashMap = registryConcurrentHashMap;
        updateQueue = new LinkedBlockingQueue<>();
        this.graph = graphBuilder.build();
    }

    @Override
    public void run() {
        graph.space().clear();
        running = true;
        graph.connect(on -> {
            listeningMessage();
        });

    }

    private void listeningMessage() {
        try {
            TaskMessage message = taskQueue.take();
            DeferCounter counter = graph.newCounter(1);
            executeTask(message, counter);
            counter.then(this::listeningMessage);
        } catch (InterruptedException e) {
            if (graph.isConnected()) {
                graph.disconnect(on -> {
                    e.printStackTrace();
                });
            }
        }
    }

    private void executeTask(TaskMessage message, DeferCounter counter) {
        long timeStart = System.currentTimeMillis();
        Buffer update;
        while ((update = updateQueue.poll()) != null) {
            graph.remoteNotify(update);
            update.free();
        }
        long spaceBefore = graph.space().available();


        //loading the task
        Task recreatedTask = Tasks.newTask();
        recreatedTask.loadFromBuffer(message.getTask(), graph);

        //preparing context
        TaskContext ctx = recreatedTask.prepare(graph, null, new Callback<TaskResult>() {
            @Override
            public void on(TaskResult result) {
                Buffer res = new HeapBuffer();
                result.saveToBuffer(res);
                result.free();
                try {
                    output.put(new GraphMessage(RESP_TASK, message.getReturnId(), res, message.getCallback()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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

                //disconnect the graph
                graph.save(on -> {
                    //forward result
                    long spaceAfter = graph.space().available();
                    long timeStop = System.currentTimeMillis();
                    if (spaceAfter != spaceBefore) {
                        graph.log().error("task executed in " + (timeStop - timeStart) + "ms contained a memory leak, before: " + spaceBefore + " after: " + spaceAfter + ". Graph Worker disconnected/reconnected");
                    } else {
                        graph.log().info("task executed in " + (timeStop - timeStart) + "ms");
                    }
                    updateQueue.clear();
                    counter.count();
                });

            }
        });

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

        DeferCounter counteru = graph.newCounter(1);

        //load context if necessary
        if (message.getContext() != null) {
            ctx.loadFromBuffer(message.getContext(), loaded -> {
                graph.taskContextRegistry().registerWith(ctx, message.getTaskId());
                message.getContext().free();
                counteru.count();
            });
        } else {
            counteru.count();
        }
        counteru.then(() -> {
            registryConcurrentHashMap.put(message.getTaskId(), graph.taskContextRegistry());
            try {
                recreatedTask.executeUsing(ctx);
            } catch (RuntimeException e) {
                graph.log().error(e.getMessage());
                try {
                    output.put(new GraphMessage(RESP_TASK, message.getReturnId(), new HeapBuffer(), message.getCallback()));
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                graph.disconnect(disconnected -> {
                    graph.connect(connected -> {
                        counter.count();
                    });
                });
            }
        });


    }

    public BlockingQueue<Buffer> getUpdateQueue() {
        return updateQueue;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
