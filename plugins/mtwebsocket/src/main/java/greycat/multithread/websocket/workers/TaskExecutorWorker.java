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
import greycat.multithread.websocket.buffergraph.BufferScheduler;
import greycat.multithread.websocket.buffergraph.BufferStorage;
import greycat.multithread.websocket.message.GraphExecutorMessage;
import greycat.multithread.websocket.message.GraphMessage;
import greycat.multithread.websocket.message.TaskMessage;
import greycat.struct.Buffer;
import greycat.utility.Base64;

import java.util.*;
import java.util.concurrent.*;

import static greycat.multithread.websocket.Constants.*;

public class TaskExecutorWorker extends Thread {
    private final BlockingQueue<GraphExecutorMessage> graphInput;
    private final BlockingQueue<GraphMessage> output;
    private final BlockingQueue<TaskMessage> taskQueue;


    private final BlockingQueue<Buffer> updateQueue;
    private final ConcurrentHashMap<Integer, TaskContextRegistry> registryConcurrentHashMap;


    private final BlockingQueue<Graph> freeworkers;
    private final Set<Graph> busyWorkers;
    private final int nbWorkers;
    private final GraphBuilder graphBuilder;
    private final ExecutorService executors;
    private boolean running = false;


    public TaskExecutorWorker(BlockingQueue<GraphExecutorMessage> graphInput, BlockingQueue<GraphMessage> output, BlockingQueue<TaskMessage> taskQueue, GraphBuilder graphBuilder, ConcurrentHashMap<Integer, TaskContextRegistry> registryConcurrentHashMap, int nbThread) {
        this.graphInput = graphInput;
        this.output = output;
        this.taskQueue = taskQueue;
        this.registryConcurrentHashMap = registryConcurrentHashMap;
        this.freeworkers = new LinkedBlockingQueue<>();
        this.nbWorkers = nbThread;
        updateQueue = new LinkedBlockingQueue<>();
        this.graphBuilder = graphBuilder;
        busyWorkers = Collections.synchronizedSet(new HashSet<>());
        executors = Executors.newFixedThreadPool(nbThread);
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            TaskMessage task = taskQueue.poll();
            if (task != null) {
                if (freeworkers.size() > 0) {
                    Graph worker = freeworkers.poll();
                    executors.submit(executeTask(worker, task));
                } else if (freeworkers.size() + busyWorkers.size() < nbWorkers) {
                    Graph graph = graphBuilder.clone().withScheduler(new BufferScheduler()).withStorage(new BufferStorage(graphInput)).build();
                    graph.connect(on -> executors.submit(executeTask(graph, task)));
                } else {
                    try {
                        Graph worker = freeworkers.take();
                        executors.submit(executeTask(worker, task));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            Buffer buffer;
            while ((buffer = updateQueue.poll()) != null) {
                Queue<Graph> freeworkersToUpdate = new LinkedList<>();
                Graph worker;
                while ((worker = freeworkers.poll()) != null) {
                    worker.remoteNotify(buffer);
                    freeworkersToUpdate.add(worker);
                }
                for (int i = 0; i < freeworkersToUpdate.size(); i++) {
                    try {
                        freeworkers.put(freeworkersToUpdate.poll());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                buffer.free();
            }

        }
        executors.shutdown();
    }

    public BlockingQueue<Buffer> getUpdateQueue() {
        return updateQueue;
    }

    public Runnable executeTask(Graph graph, TaskMessage message) {
        return (() -> {
            long timeStart = System.currentTimeMillis();
            long spaceBefore = graph.space().available();

            busyWorkers.add(graph);

            Callback<TaskResult> callback = new Callback<TaskResult>() {
                @Override
                public void on(TaskResult result) {

                    boolean error = result.exception() != null;
                    Buffer res = graph.newBuffer();
                    if (!error) {
                        result.saveToBuffer(res);
                        result.free();
                    }else{
                        Tasks.emptyResult().setException(result.exception()).saveToBuffer(res);
                        result.free();
                    }
                    try {
                        output.put(new GraphMessage(RESP_TASK, message.getReturnId(), res, message.getCallback()));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //remove taskcontext as the task is done
                    registryConcurrentHashMap.remove(message.getTaskId());
                    if (message.getPrintcb() != null) {
                        message.getPrintcb().free();
                    }
                    if (message.getProgrescb() != null) {
                        message.getProgrescb().free();
                    }

                    if (!error) {
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
                            busyWorkers.remove(graph);
                            try {
                                freeworkers.put(graph);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                    } else {
                        graph.scheduler().stop();
                        busyWorkers.remove(graph);
                        System.err.println(" graph destroyed");
                        Graph graph = graphBuilder.clone().withScheduler(new BufferScheduler()).withStorage(new BufferStorage(graphInput)).build();
                        graph.connect(on -> {
                            try {
                                freeworkers.put(graph);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                    }

                }
            };

            //loading the task
            Task recreatedTask = Tasks.newTask();
            try {
                recreatedTask.loadFromBuffer(message.getTask(), graph);

                //preparing context
                TaskContext ctx = recreatedTask.prepare(graph, null, callback);

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
                    recreatedTask.executeUsing(ctx);
                });
            } catch (Exception e) {
                callback.on(Tasks.emptyResult().setException(e));
            }
        });
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
