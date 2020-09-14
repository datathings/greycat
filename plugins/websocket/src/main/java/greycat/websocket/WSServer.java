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
package greycat.websocket;

import greycat.*;
import greycat.chunk.Chunk;
import greycat.internal.CoreGraphLog;
import greycat.internal.heap.HeapBuffer;
import greycat.internal.task.CoreProgressReport;
import greycat.plugin.Job;
import greycat.struct.Buffer;
import greycat.struct.BufferIterator;
import greycat.utility.Base64;
import greycat.chunk.ChunkKey;
import greycat.chunk.KeyHelper;
import greycat.workers.StorageMessageType;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.xnio.ChannelListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class WSServer implements WebSocketConnectionCallback, Callback<Buffer> {

    protected final GraphBuilder builder;
    private final int port;
    private Undertow server;

    protected Set<WebSocketChannel> peers;
    protected Map<String, HttpHandler> handlers;

    public static void attach(GraphBuilder storage, int port) {
        WSServer srv = new WSServer(storage, port);
        srv.start();
        Runtime.getRuntime().addShutdownHook(new Thread(srv::stop));
    }

    public WSServer(GraphBuilder p_builder, int p_port) {
        this.builder = p_builder;
        this.port = p_port;
        peers = new HashSet<WebSocketChannel>();
        handlers = new HashMap<String, HttpHandler>();
        handlers.put(PREFIX, Handlers.websocket(this));
    }

    public WSServer addHandler(String prefix, HttpHandler httpHandler) {
        handlers.put(prefix, httpHandler);
        return this;
    }

    private static final String PREFIX = "/ws";

    public void start() {
        final PathHandler pathHandler = Handlers.path();
        for (String name : handlers.keySet()) {
            pathHandler.addPrefixPath(name, handlers.get(name));
        }
        this.server = Undertow.builder().addHttpListener(port, "0.0.0.0", pathHandler).build();
        server.start();
        if (builder.storage != null) {
            builder.storage.listen(this);
        } else if (builder.storageFactory != null) {
            builder.storageFactory.listen(this);
        }
    }

    public void stop() {
        server.stop();
        server = null;
    }

    @Override
    public void onConnect(WebSocketHttpExchange webSocketHttpExchange, WebSocketChannel webSocketChannel) {
        final Graph graph = builder.build();
        graph.setProperty("ws.source", webSocketChannel.getSourceAddress().getAddress());
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                webSocketChannel.getReceiveSetter().set(new PeerInternalListener(graph));
                webSocketChannel.resumeReceives();
                peers.add(webSocketChannel);
            }
        });
    }

    @Override
    public final void on(final Buffer result) {
        //broadcast to anyone...
        WebSocketChannel[] others = peers.toArray(new WebSocketChannel[peers.size()]);
        Buffer notificationBuffer = new HeapBuffer();
        notificationBuffer.write(StorageMessageType.NOTIFY_UPDATE);
        notificationBuffer.write(Constants.BUFFER_SEP);
        notificationBuffer.writeAll(result.data());
        byte[] notificationMsg = notificationBuffer.data();
        notificationBuffer.free();
        for (int i = 0; i < others.length; i++) {
            PeerInternalListener listener = (PeerInternalListener) ((ChannelListener.SimpleSetter) others[i].getReceiveSetter()).get();
            //locally invalidate cache
            listener.graph.remoteNotify(result);
            //notify to connected peer
            send_flat_resp(notificationMsg, others[i]);
        }
    }

    protected class PeerInternalListener extends AbstractReceiveListener {

        private final Graph graph;

        PeerInternalListener(Graph p_graph) {
            graph = p_graph;
        }

        public Graph graph() {
            return graph;
        }

        @Override
        protected final void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
            ByteBuffer byteBuffer = WebSockets.mergeBuffers(message.getData().getResource());
            process_rpc(graph, byteBuffer.array(), channel);
            super.onFullBinaryMessage(channel, message);
        }

        @Override
        protected final void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
            process_rpc(graph, message.getData().getBytes(), channel);
            super.onFullTextMessage(channel, message);
        }

        @Override
        protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) throws IOException {
            peers.remove(webSocketChannel);
            graph.disconnect(new Callback<Boolean>() {
                @Override
                public void on(Boolean result) {
                    //noop
                }
            });
            super.onClose(webSocketChannel, channel);
        }
    }

    protected void process_rpc(final Graph graph, final byte[] input, final WebSocketChannel channel) {
        if (input.length == 0) {
            return;
        }
        final Buffer payload = graph.newBuffer();
        payload.writeAll(input);
        final BufferIterator it = payload.iterator();
        final Buffer codeView = it.next();
        final Buffer callbackCodeView = it.next();
        if (codeView != null && callbackCodeView != null && codeView.length() != 0) {
            byte firstCodeView = codeView.read(0);
            //compute resp prefix
            switch (firstCodeView) {
                case StorageMessageType.REQ_LOG:
                    graph.setProperty("ws.last", System.currentTimeMillis());
                    if (it.hasNext()) {
                        Buffer b = it.next();
                        StringBuilder buf = new StringBuilder();
                        buf.append(Base64.decodeToStringWithBounds(b, 0, b.length()));
                        ((CoreGraphLog) graph.log()).writeMessage(buf);
                    }
                    final Buffer task_log_buf = graph.newBuffer();
                    task_log_buf.write(StorageMessageType.RESP_LOG);
                    task_log_buf.write(Constants.BUFFER_SEP);
                    task_log_buf.writeAll(callbackCodeView.data());
                    payload.free();
                    WSServer.this.send_resp(task_log_buf, channel);
                    break;
                case StorageMessageType.REQ_TASK_STATS:
                    graph.setProperty("ws.last", System.currentTimeMillis());
                    final Buffer task_stats_buf = graph.newBuffer();
                    task_stats_buf.write(StorageMessageType.RESP_TASK_STATS);
                    task_stats_buf.write(Constants.BUFFER_SEP);
                    task_stats_buf.writeAll(callbackCodeView.data());
                    task_stats_buf.write(Constants.BUFFER_SEP);
                    Base64.encodeStringToBuffer(graph.taskContextRegistry().stats(), task_stats_buf);
                    payload.free();
                    WSServer.this.send_resp(task_stats_buf, channel);
                    break;
                case StorageMessageType.REQ_TASK_STOP:
                    graph.setProperty("ws.last", System.currentTimeMillis());
                    if (it.hasNext()) {
                        Buffer view = it.next();
                        int taskCode = Base64.decodeToIntWithBounds(view, 0, view.length());
                        graph.taskContextRegistry().forceStop(taskCode);
                    }
                    final Buffer task_stop_buf = graph.newBuffer();
                    task_stop_buf.write(StorageMessageType.RESP_TASK_STOP);
                    task_stop_buf.write(Constants.BUFFER_SEP);
                    task_stop_buf.writeAll(callbackCodeView.data());
                    payload.free();
                    WSServer.this.send_resp(task_stop_buf, channel);
                    break;
                case StorageMessageType.HEART_BEAT_PING:
                    final Buffer concat = graph.newBuffer();
                    concat.write(StorageMessageType.HEART_BEAT_PONG);
                    concat.writeString("ok");
                    WSServer.this.send_resp(concat, channel);
                    break;
                case StorageMessageType.HEART_BEAT_PONG:
                    //Ignore
                    break;
                case StorageMessageType.REQ_REMOVE:
                    graph.setProperty("ws.last", System.currentTimeMillis());
                    final List<ChunkKey> rkeys = new ArrayList<ChunkKey>();
                    while (it.hasNext()) {
                        rkeys.add(KeyHelper.bufferToKey(it.next()));
                    }
                    process_remove(graph, rkeys.toArray(new ChunkKey[rkeys.size()]), new Callback() {
                        @Override
                        public void on(Object o) {
                            final Buffer concatRM = graph.newBuffer();
                            concatRM.write(StorageMessageType.RESP_REMOVE);
                            concatRM.write(Constants.BUFFER_SEP);
                            concatRM.writeAll(callbackCodeView.data());
                            payload.free();
                            WSServer.this.send_resp(concatRM, channel);
                        }
                    });
                    break;
                case StorageMessageType.REQ_GET:
                    graph.setProperty("ws.last", System.currentTimeMillis());
                    //build keys list
                    final List<ChunkKey> keys = new ArrayList<ChunkKey>();
                    while (it.hasNext()) {
                        keys.add(KeyHelper.bufferToKey(it.next()));
                    }
                    process_get(graph, keys.toArray(new ChunkKey[keys.size()]), new Callback<Buffer>() {
                        @Override
                        public void on(Buffer streamResult) {
                            final Buffer concatGet = graph.newBuffer();
                            concatGet.write(StorageMessageType.RESP_GET);
                            concatGet.write(Constants.BUFFER_SEP);
                            concatGet.writeAll(callbackCodeView.data());
                            concatGet.write(Constants.BUFFER_SEP);
                            concatGet.writeAll(streamResult.data());
                            streamResult.free();
                            payload.free();
                            WSServer.this.send_resp(concatGet, channel);
                        }
                    });
                    break;
                case StorageMessageType.REQ_TASK:
                    graph.setProperty("ws.last", System.currentTimeMillis());
                    if (it.hasNext()) {
                        final Callback<TaskResult> end = new Callback<TaskResult>() {
                            @Override
                            public void on(TaskResult result) {
                                final Buffer concatTask = graph.newBuffer();
                                concatTask.write(StorageMessageType.RESP_TASK);
                                concatTask.write(Constants.BUFFER_SEP);
                                concatTask.writeAll(callbackCodeView.data());
                                concatTask.write(Constants.BUFFER_SEP);
                                result.saveToBuffer(concatTask);
                                result.free();
                                payload.free();
                                WSServer.this.send_resp(concatTask, channel);
                            }
                        };
                        Task t = Tasks.newTask();
                        try {
                            t.loadFromBuffer(it.next(), graph);
                            TaskContext ctx = t.prepare(graph, null, new Callback<TaskResult>() {
                                @Override
                                public void on(TaskResult result) {
                                    //we also dispatch locally
                                    if (result.notifications() != null && result.notifications().length() > 0) {
                                        graph.remoteNotify(result.notifications());
                                    }
                                    end.on(result);
                                }
                            });
                            ctx.silentSave();
                            if (it.hasNext()) {
                                final int printHookCode;
                                Buffer hookCodeView = it.next();
                                if (hookCodeView.length() > 0) {
                                    printHookCode = Base64.decodeToIntWithBounds(hookCodeView, 0, hookCodeView.length());
                                    ctx.setPrintHook(new Callback<String>() {
                                        @Override
                                        public void on(String result) {
                                            final Buffer concat = graph.newBuffer();
                                            concat.write(StorageMessageType.NOTIFY_PRINT);
                                            concat.write(Constants.BUFFER_SEP);
                                            Base64.encodeIntToBuffer(printHookCode, concat);
                                            concat.write(Constants.BUFFER_SEP);
                                            Base64.encodeStringToBuffer(result, concat);
                                            WSServer.this.send_resp(concat, channel);
                                        }
                                    });
                                }
                                final int progressHookCode;
                                Buffer progressHookCodeView = it.next();
                                if (progressHookCodeView.length() > 0) {
                                    progressHookCode = Base64.decodeToIntWithBounds(progressHookCodeView, 0, progressHookCodeView.length());
                                    ctx.setProgressHook(report -> {
                                        final Buffer concatProgress = graph.newBuffer();
                                        concatProgress.write(StorageMessageType.NOTIFY_PROGRESS);
                                        concatProgress.write(Constants.BUFFER_SEP);
                                        Base64.encodeIntToBuffer(progressHookCode, concatProgress);
                                        concatProgress.write(Constants.BUFFER_SEP);
                                        ((CoreProgressReport) report).saveToBuffer(concatProgress);
                                        WSServer.this.send_resp(concatProgress, channel);
                                    });
                                }
                                ctx.loadFromBuffer(it.next(), loaded -> {
                                    graph.taskContextRegistry().register(ctx);
                                    t.executeUsing(ctx);
                                });
                            } else {
                                graph.taskContextRegistry().register(ctx);
                                t.executeUsing(ctx);
                            }
                        } catch (Exception e) {
                            end.on(Tasks.emptyResult().setException(e));
                        }
                    }
                    break;
                case StorageMessageType.REQ_LOCK:
                    graph.setProperty("ws.last", System.currentTimeMillis());
                    process_lock(graph, new Callback<Buffer>() {
                        @Override
                        public void on(Buffer result) {
                            Buffer concat = graph.newBuffer();
                            concat.write(StorageMessageType.RESP_LOCK);
                            concat.write(Constants.BUFFER_SEP);
                            concat.writeAll(callbackCodeView.data());
                            concat.write(Constants.BUFFER_SEP);
                            concat.writeAll(result.data());
                            result.free();
                            payload.free();
                            WSServer.this.send_resp(concat, channel);
                        }
                    });
                    break;
                case StorageMessageType.REQ_UNLOCK:
                    graph.setProperty("ws.last", System.currentTimeMillis());
                    process_unlock(graph, it.next(), new Callback<Boolean>() {
                        @Override
                        public void on(Boolean result) {
                            Buffer concat = graph.newBuffer();
                            concat.write(StorageMessageType.RESP_UNLOCK);
                            concat.write(Constants.BUFFER_SEP);
                            concat.writeAll(callbackCodeView.data());
                            payload.free();
                            WSServer.this.send_resp(concat, channel);
                        }
                    });
                    break;
                case StorageMessageType.REQ_PUT:
                    graph.setProperty("ws.last", System.currentTimeMillis());
                    final List<ChunkKey> flatKeys = new ArrayList<ChunkKey>();
                    final List<Buffer> flatValues = new ArrayList<Buffer>();
                    while (it.hasNext()) {
                        final Buffer keyView = it.next();
                        final Buffer valueView = it.next();
                        if (valueView != null) {
                            flatKeys.add(KeyHelper.bufferToKey(keyView));
                            flatValues.add(valueView);
                        }
                    }
                    final ChunkKey[] collectedKeys = flatKeys.toArray(new ChunkKey[flatKeys.size()]);
                    process_put(graph, collectedKeys, flatValues.toArray(new Buffer[flatValues.size()]), new Job() {
                        @Override
                        public void run() {
                            graph.save(new Callback<Boolean>() {
                                @Override
                                public void on(Boolean result) {
                                    Buffer concat = graph.newBuffer();
                                    concat.write(StorageMessageType.RESP_PUT);
                                    concat.write(Constants.BUFFER_SEP);
                                    concat.writeAll(callbackCodeView.data());
                                    payload.free();
                                    WSServer.this.send_resp(concat, channel);
                                }
                            });
                        }
                    });
                    break;
            }
        }
    }

    private void process_lock(Graph graph, Callback<Buffer> callback) {
        graph.storage().lock(callback);
    }

    private void process_unlock(Graph graph, Buffer toUnlock, Callback<Boolean> callback) {
        graph.storage().unlock(toUnlock, callback);
    }

    private void process_put(final Graph graph, final ChunkKey[] keys, final Buffer[] values, Job job) {
        final DeferCounter defer = graph.newCounter(keys.length);
        defer.then(job);
        for (int i = 0; i < keys.length; i++) {
            final int finalI = i;
            ChunkKey tuple = keys[i];
            graph.space().getOrLoadAndMark(tuple.type, tuple.world, tuple.time, tuple.id, new Callback<Chunk>() {
                @Override
                public void on(Chunk memoryChunk) {
                    if (memoryChunk != null) {
                        memoryChunk.loadDiff(values[finalI]);
                        graph.space().unmark(memoryChunk.index());
                    } else {
                        Chunk newChunk = graph.space().createAndMark(tuple.type, tuple.world, tuple.time, tuple.id);
                        if (newChunk != null) {
                            newChunk.loadDiff(values[finalI]);
                            graph.space().unmark(newChunk.index());
                        }
                    }
                    defer.count();
                }
            });
        }
    }

    private void process_remove(final Graph graph, ChunkKey[] keys, final Callback callback) {
        Buffer buffer = graph.newBuffer();
        for (int i = 0; i < keys.length; i++) {
            if (i != 0) {
                buffer.write(Constants.BUFFER_SEP);
            }
            ChunkKey tuple = keys[i];
            KeyHelper.keyToBuffer(buffer, tuple.type, tuple.world, tuple.time, tuple.id);
            graph.space().delete(tuple.type, tuple.world, tuple.time, tuple.id);
        }
        graph.storage().remove(buffer, new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                buffer.free();
                callback.on(null);
            }
        });
    }

    private void process_get(final Graph graph, ChunkKey[] keys, final Callback<Buffer> callback) {
        final DeferCounter defer = graph.newCounter(keys.length);
        final Buffer[] buffers = new Buffer[keys.length];
        defer.then(new Job() {
            @Override
            public void run() {
                Buffer stream = graph.newBuffer();
                for (int i = 0; i < buffers.length; i++) {
                    if (i != 0) {
                        stream.write(Constants.BUFFER_SEP);
                    }
                    if (buffers[i] != null) {
                        stream.writeAll(buffers[i].data());
                        buffers[i].free();
                    }
                }
                callback.on(stream);
            }
        });
        for (int i = 0; i < keys.length; i++) {
            final int fixedI = i;
            ChunkKey tuple = keys[i];
            graph.space().getOrLoadAndMark(tuple.type, tuple.world, tuple.time, tuple.id, new Callback<Chunk>() {
                @Override
                public void on(Chunk memoryChunk) {
                    if (memoryChunk != null) {
                        final Buffer toSaveBuffer = graph.newBuffer();
                        memoryChunk.save(toSaveBuffer);
                        graph.space().unmark(memoryChunk.index());
                        buffers[fixedI] = toSaveBuffer;
                    } else {
                        buffers[fixedI] = null;
                    }
                    defer.count();
                }
            });
        }
    }

    private void send_resp(final Buffer stream, final WebSocketChannel channel) {
        ByteBuffer finalBuf = ByteBuffer.wrap(stream.data());
        stream.free();
        WebSockets.sendBinary(finalBuf, channel, null);
    }

    private void send_flat_resp(final byte[] flat, final WebSocketChannel channel) {
        WebSockets.sendBinary(ByteBuffer.wrap(flat), channel, null);
    }

}
