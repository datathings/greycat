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
import greycat.multithread.websocket.buffergraph.BufferStorage;
import greycat.multithread.websocket.buffergraph.BufferScheduler;
import greycat.multithread.websocket.message.GraphExecutorMessage;
import greycat.multithread.websocket.message.GraphMessage;
import greycat.struct.Buffer;
import greycat.utility.Base64;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import static greycat.multithread.websocket.Constants.*;

/**
 * The graph consumer is created for the sole purpose of executing a task, ususally a remote execution task.
 * The graph is connected to the main graph through message queues. The task is thus executed in a different thread and will not impact the availibilty of the main graph.
 */
public class GraphConsumer implements Runnable {

    private final BlockingQueue<GraphExecutorMessage> graphInput;
    private final GraphBuilder graphBuilder;

    private final Buffer task;
    private final Buffer callback;
    private final BlockingQueue<GraphMessage> output;
    private final int returnId;
    private Graph graph;
    private final int taskId;
    private final ConcurrentHashMap<Integer, TaskContextRegistry> registryConcurrentHashMap;
    private Buffer context = null;
    private Buffer printcb = null;
    private Buffer progrescb = null;

    /**
     * @param graphInput                message queue of the main graph
     * @param output                    websocket output message queue (print and progress hook)
     * @param returnId                  channel hash of the websocket from which the task emanated
     * @param graphBuilder              graph builder
     * @param taskId                    that was picked by the websocket server
     * @param registryConcurrentHashMap task registry map to be able to force stop from the websocket server
     * @param task                      to execute
     * @param origincallback            callback id on the client side
     */
    public GraphConsumer(BlockingQueue<GraphExecutorMessage> graphInput, BlockingQueue<GraphMessage> output, int returnId,
                         GraphBuilder graphBuilder,
                         int taskId, ConcurrentHashMap<Integer, TaskContextRegistry> registryConcurrentHashMap, //todo refactor this
                         Buffer task, Buffer origincallback) {
        this.graphInput = graphInput;
        this.graphBuilder = graphBuilder;
        this.callback = origincallback;
        this.task = task;
        this.taskId = taskId;
        this.registryConcurrentHashMap = registryConcurrentHashMap;
        this.output = output;
        this.returnId = returnId;
    }

    @Override
    public void run() {
        //Switch to the right scheduler and storage, should have been cloned before
        graphBuilder.withScheduler(new BufferScheduler())
                .withStorage(new BufferStorage(graphInput));
        graph = graphBuilder.build();

        graph.connect(on -> {
            //loading the task
            Task recreatedTask = Tasks.newTask();
            recreatedTask.loadFromBuffer(task, graph);

            //preparing context
            TaskContext ctx = recreatedTask.prepare(graph, null, new Callback<TaskResult>() {
                @Override
                public void on(TaskResult result) {
                    if (result.notifications() != null && result.notifications().length() > 0) {
                        graph.remoteNotify(result.notifications());
                    }
                    //remove taskcontext as the task is done
                    registryConcurrentHashMap.remove(taskId);
                    if (printcb != null) {
                        printcb.free();
                    }
                    if (progrescb != null) {
                        progrescb.free();
                    }
                    Buffer res = new HeapBuffer();
                    result.saveToBuffer(res);
                    //disconnect the graph
                    graph.disconnect(on -> {
                        try {
                            //forward result
                            output.put(new GraphMessage(RESP_TASK, returnId, res, callback));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });

                }
            });

            ctx.silentSave();
            //Configure printhook
            if (printcb != null) {
                ctx.setPrintHook(result -> {
                    Buffer buffer = new HeapBuffer();
                    Base64.encodeStringToBuffer(result, buffer);
                    Buffer print = new HeapBuffer();
                    print.writeAll(printcb.data());
                    try {
                        output.put(new GraphMessage(NOTIFY_PRINT, returnId, buffer, print));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
            //configure progress hook
            if (progrescb != null) {
                ctx.setProgressHook(result -> {
                    Buffer buffer = new HeapBuffer();
                    ((CoreProgressReport) result).saveToBuffer(buffer);
                    Buffer reportProg = new HeapBuffer();
                    reportProg.writeAll(progrescb.data());
                    try {
                        output.put(new GraphMessage(NOTIFY_PROGRESS, returnId, buffer, reportProg));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
            //load context if necessary
            if (context != null) {
                ctx.loadFromBuffer(context, loaded -> {
                    graph.taskContextRegistry().registerWith(ctx, taskId);
                    context.free();
                    registryConcurrentHashMap.put(taskId, graph.taskContextRegistry());
                    recreatedTask.executeUsing(ctx);
                });
            } else {
                graph.taskContextRegistry().registerWith(ctx, taskId);
                registryConcurrentHashMap.put(taskId, graph.taskContextRegistry());
                recreatedTask.executeUsing(ctx);
            }
        });

    }

    public void setContext(Buffer context) {
        this.context = context;
    }

    public void setHookPrint(Buffer hookcodehash) {
        this.printcb = hookcodehash;
    }

    public void setHookProgress(Buffer progresscodehash) {
        this.progrescb = progresscodehash;
    }


}
