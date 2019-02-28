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
package greycat.multithread.websocket;

import greycat.*;
import greycat.Constants;
import greycat.internal.CoreDeferCounterSync;
import greycat.internal.heap.HeapBuffer;
import greycat.multithread.websocket.buffergraph.BufferStorage;
import greycat.multithread.websocket.buffergraph.BufferScheduler;
import greycat.multithread.websocket.message.GraphExecutorMessage;
import greycat.multithread.websocket.message.GraphMessage;
import greycat.multithread.websocket.message.TaskMessage;
import greycat.multithread.websocket.workers.GraphConsumer;
import greycat.multithread.websocket.workers.GraphExecutor;
import greycat.multithread.websocket.workers.GraphWorker;
import greycat.multithread.websocket.workers.ResolverWorker;
import greycat.struct.Buffer;
import greycat.struct.BufferIterator;
import greycat.utility.Base64;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static greycat.multithread.websocket.Constants.*;

@SuppressWarnings("Duplicates")
public class WSServer implements WebSocketConnectionCallback {
    private final int port;
    private Undertow server;

    protected Map<Integer, WebSocketChannel> peers;
    protected Map<String, HttpHandler> handlers;

    private final BlockingQueue<GraphMessage> resultsToResolve;
    private final ConcurrentHashMap<Integer, TaskContextRegistry> registryConcurrentHashMap;
    private AtomicInteger counter = new AtomicInteger(0);
    private AtomicInteger channelCounter = new AtomicInteger(0);


    private final BlockingQueue<GraphExecutorMessage> graphInput;
    private final BlockingQueue<TaskMessage> taskQueue;



    private final GraphBuilder builder;
    private final int thread;
    private HttpHandler defaultHandler;

    private GraphExecutor graphExec;
    private ResolverWorker resolver;
    private GraphWorker[] graphWorkers;

    public static void attach(int port, GraphBuilder builder, int thread) {
        WSServer srv = new WSServer(port, builder, thread);
        srv.start();
        Runtime.getRuntime().addShutdownHook(new Thread(srv::stop));
    }

    public WSServer(int p_port, GraphBuilder builder, int thread) {
        this.port = p_port;
        this.resultsToResolve = new LinkedBlockingQueue<>();
        this.builder = builder;
        peers = Collections.synchronizedMap(new HashMap<>());
        handlers = new HashMap<>();
        handlers.put(PREFIX, Handlers.websocket(this));
        registryConcurrentHashMap = new ConcurrentHashMap<>();
        this.thread = thread;
        graphInput = new LinkedBlockingQueue<>();
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        taskQueue = new LinkedBlockingQueue<>();
    }

    public WSServer addHandler(String prefix, HttpHandler httpHandler) {
        handlers.put(prefix, httpHandler);
        return this;
    }

    public WSServer setDefaultHandler(HttpHandler httpHandler) {
        this.defaultHandler = httpHandler;
        return this;
    }


    private static final String PREFIX = "/ws";

    public void start() {
        PathHandler pathHandler;
        if (this.defaultHandler != null) {
            pathHandler = Handlers.path(defaultHandler);
        } else {
            pathHandler = Handlers.path();
        }
        for (String name : handlers.keySet()) {
            pathHandler.addPrefixPath(name, handlers.get(name));
        }
        this.server = Undertow.builder().addHttpListener(port, "0.0.0.0", pathHandler).build();
        DeferCounterSync deferCounterSync = new CoreDeferCounterSync(1);

        this.graphWorkers = new GraphWorker[Math.max(1, thread)];
        BlockingQueue<Buffer>[] workerupdateQueue = new BlockingQueue[graphWorkers.length];
        for (int i = 0; i < graphWorkers.length; i++) {
            graphWorkers[i] = new GraphWorker(graphInput, resultsToResolve, taskQueue, toBufferGraphBuilder(builder), registryConcurrentHashMap);
            workerupdateQueue[i] = graphWorkers[i].getUpdateQueue();
        }
        this.resolver = new ResolverWorker(resultsToResolve, peers,workerupdateQueue);
        this.graphExec = new GraphExecutor(builder, graphInput, deferCounterSync, resultsToResolve);
        server.start();
        resolver.start();
        graphExec.start();
        for (int i = 0; i < graphWorkers.length; i++) {
            graphWorkers[i].start();
        }
        deferCounterSync.waitResult();
    }

    public void stop() {
        server.stop();
        for (int i = 0; i < graphWorkers.length; i++) {
            graphWorkers[i].setRunning(false);
        }
        resolver.setRunning(false);
        graphExec.setRunning(false);
        server = null;
    }

    @Override
    public void onConnect(WebSocketHttpExchange webSocketHttpExchange, WebSocketChannel webSocketChannel) {
        webSocketChannel.getReceiveSetter().set(new InternalListener());
        webSocketChannel.resumeReceives();
        webSocketChannel.setAttribute("id", channelCounter.get());
        peers.put(channelCounter.getAndIncrement(), webSocketChannel);
        if (channelCounter.get() == Integer.MAX_VALUE) {
            channelCounter.set(0);
        }
    }

    protected void onChannelClosed(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) {
        //NOOP
    }

    protected class InternalListener extends AbstractReceiveListener {

        @Override
        protected final void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
            ByteBuffer byteBuffer = WebSockets.mergeBuffers(message.getData().getResource());
            process_rpc(byteBuffer.array(), channel);
            super.onFullBinaryMessage(channel, message);
        }

        @Override
        protected final void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
            process_rpc(message.getData().getBytes(), channel);
            super.onFullTextMessage(channel, message);
        }

        @Override
        protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) throws IOException {
            peers.remove(webSocketChannel.getAttribute("id"));
            onChannelClosed(webSocketChannel, channel);
            super.onClose(webSocketChannel, channel);
        }
    }


    protected void forward(final byte operationId, final Buffer payload, final Buffer originalCallback, final int channelId) {
        try {
            graphInput.put(new GraphExecutorMessage(resultsToResolve, operationId, channelId, payload, originalCallback));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void process_rpc(final byte[] input, final WebSocketChannel channel) {
        if (input.length == 0) {
            return;
        }
        final int channelHash = (int) channel.getAttribute("id");
        final Buffer payload = new HeapBuffer();
        payload.writeAll(input);
        final BufferIterator it = payload.iterator();
        final Buffer codeView = it.next();
        final Buffer callbackCodeView = it.next();
        Buffer content;
        if (codeView != null && callbackCodeView != null && codeView.length() != 0) {
            byte firstCodeView = codeView.read(0);
            final Buffer callbackCode = new HeapBuffer();
            callbackCode.writeAll(callbackCodeView.data());
            //compute resp prefix
            switch (firstCodeView) {
                case REQ_LOG:
                    content = null;
                    if (it.hasNext()) {
                        content = new HeapBuffer();
                        payload.writeAll(it.next().data());
                    }
                    payload.free();
                    forward(REQ_LOG, content, callbackCode, channelHash);
                    break;
                case REQ_TASK_STATS:
                    StringBuilder builder = new StringBuilder();
                    builder.append('[');
                    boolean is_first = true;
                    for (Map.Entry<Integer, TaskContextRegistry> entry : registryConcurrentHashMap.entrySet()) {
                        if (is_first) {
                            is_first = false;
                        } else {
                            builder.append(',');
                        }
                        builder.append(entry.getValue().statsOf(entry.getKey()));
                    }
                    builder.append(']');

                    final Buffer task_stats_buf = new HeapBuffer();
                    Base64.encodeStringToBuffer(builder.toString(), task_stats_buf);
                    payload.free();
                    try {
                        resultsToResolve.put(new GraphMessage(RESP_TASK_STATS, channelHash, task_stats_buf, callbackCode));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;

                case REQ_TASK_STOP:

                    if (it.hasNext()) {
                        Buffer view = it.next();
                        int taskCode = Base64.decodeToIntWithBounds(view, 0, view.length());
                        registryConcurrentHashMap.get(taskCode).forceStop(taskCode);
                    }
                    payload.free();
                    try {
                        resultsToResolve.put(new GraphMessage(RESP_TASK_STOP, channelHash, null, callbackCode));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                case HEART_BEAT_PING:
                    final Buffer concat = new HeapBuffer();
                    concat.write(HEART_BEAT_PONG);
                    concat.writeString("ok");
                    ByteBuffer finalBuf = ByteBuffer.wrap(concat.data());
                    concat.free();
                    WebSockets.sendBinary(finalBuf, channel, null);
                    break;
                case HEART_BEAT_PONG:
                    //Ignore
                    payload.free();
                    break;
                case REQ_REMOVE:
                    content = new HeapBuffer();
                    boolean is_fir = true;
                    while (it.hasNext()) {
                        if (is_fir) {
                            is_fir = false;
                        } else {
                            content.write(Constants.BUFFER_SEP);
                        }
                        content.writeAll(it.next().data());
                    }
                    payload.free();
                    forward(REQ_REMOVE, content, callbackCode, channelHash);
                    break;
                case REQ_GET:
                    content = new HeapBuffer();
                    boolean is_firs = true;
                    while (it.hasNext()) {
                        if (is_firs) {
                            is_firs = false;
                        } else {
                            content.write(Constants.BUFFER_SEP);
                        }
                        content.writeAll(it.next().data());
                    }
                    payload.free();
                    forward(REQ_GET, content, callbackCode, channelHash);
                    break;
                case REQ_TASK:
                    if (it.hasNext()) {
                        Buffer task = new HeapBuffer();
                        task.writeAll(it.next().data());
                        TaskMessage taskm = new TaskMessage(task,callbackCode,channelHash, counter.getAndIncrement());
                        if (counter.get() == Integer.MAX_VALUE) {
                            counter.set(0);
                        }
                        if (it.hasNext()) {
                            Buffer printHookCB = new HeapBuffer();
                            printHookCB.writeAll(it.next().data());
                            Buffer progressHookCB = new HeapBuffer();
                            progressHookCB.writeAll(it.next().data());
                            Buffer context = new HeapBuffer();
                            context.writeAll(it.next().data());
                            if (context.length() != 0) {
                                taskm.setContext(context);
                            }
                            if (progressHookCB.length() != 0) {
                                taskm.setHookProgress(progressHookCB);
                            }
                            if (printHookCB.length() != 0) {
                                taskm.setHookPrint(printHookCB);
                            }
                        }
                        try {
                            taskQueue.put(taskm);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        payload.free();
                    }
                    break;
                case REQ_LOCK:
                    payload.free();
                    forward(REQ_LOCK, null, callbackCode, channelHash);
                    break;
                case REQ_UNLOCK:
                    content = new HeapBuffer();
                    content.writeAll(it.next().data());
                    payload.free();
                    forward(REQ_UNLOCK, content, callbackCode, channelHash);
                    break;
                case REQ_PUT:
                    content = new HeapBuffer();
                    boolean is_f = true;
                    while (it.hasNext()) {
                        if (is_f) {
                            is_f = false;
                        } else {
                            content.write(Constants.BUFFER_SEP);
                        }
                        content.writeAll(it.next().data());
                    }
                    forward(REQ_PUT, content, callbackCode, channelHash);
                    break;
            }
        }
    }

    public GraphBuilder toBufferGraphBuilder(GraphBuilder builder) {
        return builder.clone().withScheduler(new BufferScheduler()).withStorage(new BufferStorage(graphInput));
    }
}
