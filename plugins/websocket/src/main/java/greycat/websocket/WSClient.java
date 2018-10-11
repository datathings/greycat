/**
 * Copyright 2017-2018 The GreyCat Authors.  All rights reserved.
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
import greycat.base.BaseTaskResult;
import greycat.chunk.ChunkType;
import greycat.chunk.WorldOrderChunk;
import greycat.internal.task.CoreProgressReport;
import greycat.plugin.TaskExecutor;
import greycat.struct.BufferIterator;
import greycat.utility.*;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.protocol.http.HttpOpenListener;
import io.undertow.websockets.client.WebSocketClient;
import io.undertow.websockets.core.*;
import greycat.chunk.Chunk;
import greycat.plugin.Storage;
import greycat.struct.Buffer;
import org.xnio.*;
import org.xnio.ssl.JsseXnioSsl;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class WSClient implements Storage, TaskExecutor {

    private final String _url;

    private WebSocketChannel _channel;

    private XnioWorker _worker;

    private Graph _graph;

    private Map<Integer, Callback> _callbacks;

    private final List<Callback<Buffer>> _listeners = new ArrayList<Callback<Buffer>>();

    public WSClient(String p_url) {
        this._url = p_url;
        this._callbacks = new ConcurrentHashMap<Integer, Callback>();
    }

    @Override
    public final void get(Buffer keys, Callback<Buffer> callback) {
        send_rpc_req(WSConstants.REQ_GET, keys, callback);
    }

    @Override
    public final void put(Buffer stream, Callback<Boolean> callback) {
        send_rpc_req(WSConstants.REQ_PUT, stream, callback);
    }

    @Override
    public final void putSilent(Buffer stream, Callback<Buffer> callback) {
        send_rpc_req(WSConstants.REQ_PUT, stream, new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                callback.on(null);
            }
        });
    }

    @Override
    public final void remove(Buffer keys, Callback<Boolean> callback) {
        send_rpc_req(WSConstants.REQ_REMOVE, keys, callback);
    }

    @Override
    public final void lock(Callback<Buffer> callback) {
        send_rpc_req(WSConstants.REQ_LOCK, null, callback);
    }

    @Override
    public final void unlock(Buffer previousLock, Callback<Boolean> callback) {
        send_rpc_req(WSConstants.REQ_UNLOCK, previousLock, callback);
    }

    @Override
    public final void taskStats(Callback<String> callback) {
        send_rpc_req(WSConstants.REQ_TASK_STATS, null, callback);
    }

    @Override
    public final void taskStop(Integer id, Callback<Boolean> callback) {
        Buffer buf = this._graph.newBuffer();
        Base64.encodeIntToBuffer(id, buf);
        send_rpc_req(WSConstants.REQ_TASK_STATS, buf, callback);
        buf.free();
    }

    @Override
    public final void connect(final Graph p_graph, final Callback<Boolean> callback) {
        if (_channel != null) {
            if (callback != null) {
                callback.on(true);//already connected
            }
        }
        this._graph = p_graph;
        try {
            final Xnio xnio = Xnio.getInstance(io.undertow.websockets.client.WebSocketClient.class.getClassLoader());
            _worker = xnio.createWorker(OptionMap.builder()
                    .set(Options.WORKER_IO_THREADS, 2)
                    .set(Options.CONNECTION_HIGH_WATER, 1000000)
                    .set(Options.CONNECTION_LOW_WATER, 1000000)
                    .set(Options.WORKER_TASK_CORE_THREADS, 30)
                    .set(Options.WORKER_TASK_MAX_THREADS, 30)
                    .set(Options.TCP_NODELAY, true)
                    .set(Options.CORK, true)
                    .getMap());


            SSLContext sc = null;
            if (this._url.startsWith("wss")) {
                // Create a trust manager that does not validate certificate chains
                TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }

                    public void checkServerTrusted(X509Certificate[] chain,
                                                   String authType) throws CertificateException {
                    }
                }};


                // Install the all-trusting trust manager
                try {
                    sc = SSLContext.getInstance("TLS");
                    sc.init(null, trustAllCerts, new java.security.SecureRandom());


                } catch (Exception e) {
                    e.printStackTrace();
                }

            }


            ByteBufferPool _buffer = new DefaultByteBufferPool(true, 1024 * 1024);
            WebSocketClient.ConnectionBuilder builder = io.undertow.websockets.client.WebSocketClient
                    .connectionBuilder(_worker, _buffer, new URI(_url));
            if (sc != null) {
                builder.setSsl(new JsseXnioSsl(xnio, OptionMap.create(Options.USE_DIRECT_BUFFERS, true), sc));
            }

            IoFuture<WebSocketChannel> futureChannel = builder.connect();
            futureChannel.await(5, TimeUnit.SECONDS); //Todo change this magic number!!!
            if (futureChannel.getStatus() != IoFuture.Status.DONE) {
                System.err.println("Error during connexion with webSocket");
                if (callback != null) {
                    callback.on(null);
                }
            }

            _channel = futureChannel.get();
            _channel.getReceiveSetter().set(new MessageReceiver());
            _channel.addCloseTask(channel -> {
                //TODO notify the graph if graph is not disconnecting
                this._channel = null;
            });
            _channel.resumeReceives();
            if (callback != null) {
                callback.on(true);
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.on(false);
            }
            e.printStackTrace();
        }
    }

    @Override
    public final void disconnect(Callback<Boolean> callback) {
        try {
            _channel.sendClose();
            _channel.close();
            _worker.shutdown();
            _channel = null;
            _worker = null;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            callback.on(true);
        }
    }

    @Override
    public final void listen(Callback<Buffer> synCallback) {
        _listeners.add(synCallback);
    }

    @Override
    public final void execute(final Callback<TaskResult> callback, final Task task, final TaskContext prepared) {
        final Buffer buffer = _graph.newBuffer();
        task.saveToBuffer(buffer);
        final int hashPrint;
        final int hashProgress;
        if (prepared != null) {
            buffer.write(Constants.BUFFER_SEP);
            final Callback<String> printHook = prepared.printHook();
            if (printHook != null) {
                hashPrint = printHook.hashCode();
                _callbacks.put(hashPrint, printHook);
                Base64.encodeIntToBuffer(hashPrint, buffer);
            } else {
                hashPrint = -1;
            }

            buffer.write(Constants.BUFFER_SEP);
            final Callback<TaskProgressReport> progressHook = prepared.progressHook();
            if (progressHook != null) {
                hashProgress = progressHook.hashCode();
                _callbacks.put(hashProgress, progressHook);
                Base64.encodeIntToBuffer(hashProgress, buffer);
            } else {
                hashProgress = -1;
            }
            buffer.write(Constants.BUFFER_SEP);
            prepared.saveToBuffer(buffer);
        } else {
            hashPrint = -1;
            hashProgress = -1;
        }
        send_rpc_req(WSConstants.REQ_TASK, buffer, new Callback<Buffer>() {
            @Override
            public void on(final Buffer bufferResult) {
                if (hashPrint != -1) {
                    _callbacks.remove(hashPrint);
                }
                if (hashProgress != -1) {
                    _callbacks.remove(hashProgress);
                }
                buffer.free();
                final BaseTaskResult baseTaskResult = new BaseTaskResult(null, false);
                final L3GMap<List<Tuple<Object[], Integer>>> collector = new L3GMap<List<Tuple<Object[], Integer>>>(true);
                baseTaskResult.load(bufferResult, 0, _graph, collector);
                _graph.remoteNotify(baseTaskResult.notifications());
                baseTaskResult.loadRefs(_graph, collector, new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        bufferResult.free();
                        callback.on(baseTaskResult);
                    }
                });
            }
        });
    }

    @Override
    public final void log(String msg) {
        Buffer buf = this._graph.newBuffer();
        Base64.encodeStringToBuffer(msg, buf);
        send_rpc_req(WSConstants.REQ_LOG, buf, new Callback() {
            @Override
            public void on(Object result) {
                buf.free();
                //noop
            }
        });

    }

    private class MessageReceiver extends AbstractReceiveListener {
        @Override
        protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
            ByteBuffer byteBuffer = WebSockets.mergeBuffers(message.getData().getResource());
            process_rpc_resp(byteBuffer.array());
            super.onFullBinaryMessage(channel, message);
        }

        @Override
        protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
            process_rpc_resp(message.getData().getBytes());
            super.onFullTextMessage(channel, message);
        }
    }

    private void send_rpc_req(final byte operationId, final Buffer payload, final Callback callback) {
        if (_channel == null) {
            throw new RuntimeException(WSConstants.DISCONNECTED_ERROR);
        }
        Buffer buffer = _graph.newBuffer();
        buffer.write(operationId);
        buffer.write(Constants.BUFFER_SEP);
        int hash = callback.hashCode();
        _callbacks.put(hash, callback);
        Base64.encodeIntToBuffer(hash, buffer);
        if (payload != null) {
            buffer.write(Constants.BUFFER_SEP);
            buffer.writeAll(payload.data());
        }
        final ByteBuffer wrapped = ByteBuffer.wrap(buffer.data());
        buffer.free();
        WebSockets.sendBinary(wrapped, _channel, new WebSocketCallback<Void>() {
            @Override
            public void complete(WebSocketChannel webSocketChannel, Void aVoid) {
            }

            @Override
            public void onError(WebSocketChannel webSocketChannel, Void aVoid, Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }

    /*
    private void process_notify(Buffer buffer) {
        Map<Long, Tuple<Listeners, LArray>> events = null;
        if (buffer != null) {
            byte type = 0;
            long world = 0;
            long time = 0;
            long id = 0;
            long hash = 0;
            int step = 0;
            long cursor = 0;
            long previous = 0;
            int end = (int) buffer.length();
            while (cursor < end) {
                byte current = buffer.read(cursor);
                if (current == Constants.KEY_SEP) {
                    switch (step) {
                        case 0:
                            type = (byte) Base64.decodeToIntWithBounds(buffer, previous, cursor);
                            break;
                        case 1:
                            world = Base64.decodeToLongWithBounds(buffer, previous, cursor);
                            break;
                        case 2:
                            time = Base64.decodeToLongWithBounds(buffer, previous, cursor);
                            break;
                        case 3:
                            id = Base64.decodeToLongWithBounds(buffer, previous, cursor);
                            break;
                        case 4:
                            hash = Base64.decodeToLongWithBounds(buffer, previous, cursor);
                            break;
                    }
                    previous = cursor + 1;
                    if (step == 4) {
                        step = 0;
                        final Chunk ch = _graph.space().getAndMark(type, world, time, id);
                        if (ch != null) {
                            if (!ch.sync(hash)) {
                                if (events != null && events.get(id) != null) {
                                    events.get(id).right().add(time);
                                } else {
                                    final WorldOrderChunk wo = (WorldOrderChunk) _graph.space().getAndMark(ChunkType.WORLD_ORDER_CHUNK, 0, 0, id);
                                    final Listeners l = wo.listeners();
                                    if (l != null) {
                                        LArray collector = new LArray();
                                        collector.add(time);
                                        events.put(id, new Tuple<Listeners, LArray>(l, collector));
                                    }
                                }
                            }
                            _graph.space().unmark(ch.index());
                        }
                    } else {
                        step++;
                    }
                }
                cursor++;
            }
            switch (step) {
                case 0:
                    type = (byte) Base64.decodeToIntWithBounds(buffer, previous, cursor);
                    break;
                case 1:
                    world = Base64.decodeToLongWithBounds(buffer, previous, cursor);
                    break;
                case 2:
                    time = Base64.decodeToLongWithBounds(buffer, previous, cursor);
                    break;
                case 3:
                    id = Base64.decodeToLongWithBounds(buffer, previous, cursor);
                    break;
                case 4:
                    hash = Base64.decodeToLongWithBounds(buffer, previous, cursor);
                    break;
            }
            if (step == 4) {
                //invalidate
                final Chunk ch = _graph.space().getAndMark(type, world, time, id);
                if (ch != null) {
                    if (!ch.sync(hash)) {
                        if (events != null && events.get(id) != null) {
                            events.get(id).right().add(time);
                        } else {
                            final WorldOrderChunk wo = (WorldOrderChunk) _graph.space().getAndMark(ChunkType.WORLD_ORDER_CHUNK, 0, 0, id);
                            final Listeners l = wo.listeners();
                            if (l != null) {
                                LArray collector = new LArray();
                                collector.add(time);
                                events.put(id, new Tuple<Listeners, LArray>(l, collector));
                            }
                        }
                    }
                    _graph.space().unmark(ch.index());
                }
            }
            //dispatch notification
            if (events != null) {
                events.values().forEach(tuple -> {
                    tuple.left().dispatch(tuple.right().all());
                });
            }
        }
    }
    */

    private void process_rpc_resp(byte[] payload) {
        Buffer payloadBuf = _graph.newBuffer();
        payloadBuf.writeAll(payload);
        BufferIterator it = payloadBuf.iterator();
        Buffer codeView = it.next();
        if (codeView != null && codeView.length() != 0) {
            final byte firstCode = codeView.read(0);
            switch (firstCode) {
                case WSConstants.NOTIFY_UPDATE:
                    //todo duplicate
                    while (it.hasNext()) {
                        _graph.remoteNotify(it.next());
                    }
                    //todo optimize this
                    if (_listeners.size() > 0) {
                        final Buffer notifyBuffer = _graph.newBuffer();
                        notifyBuffer.writeAll(payloadBuf.slice(1, payloadBuf.length() - 1));
                        for (int i = 0; i < _listeners.size(); i++) {
                            _listeners.get(i).on(notifyBuffer);
                        }
                        notifyBuffer.free();
                    }
                    break;
                case WSConstants.NOTIFY_PRINT:
                    final Buffer callbackPrintCodeView = it.next();
                    final Buffer printContentView = it.next();
                    final int callbackPrintCode = Base64.decodeToIntWithBounds(callbackPrintCodeView, 0, callbackPrintCodeView.length());
                    final String printContent = Base64.decodeToStringWithBounds(printContentView, 0, printContentView.length());
                    final Callback printCallback = _callbacks.get(callbackPrintCode);
                    printCallback.on(printContent);
                    break;
                case WSConstants.NOTIFY_PROGRESS:
                    final Buffer progressCallbackCodeView = it.next();
                    final Buffer progressCallbackView = it.next();
                    final int progressCallbackCode = Base64.decodeToIntWithBounds(progressCallbackCodeView, 0, progressCallbackCodeView.length());
                    final CoreProgressReport report = new CoreProgressReport();
                    report.loadFromBuffer(progressCallbackView);
                    Callback<TaskProgressReport> progressHook = _callbacks.get(progressCallbackCode);
                    progressHook.on(report);
                    break;
                case WSConstants.RESP_LOCK:
                case WSConstants.RESP_GET:
                case WSConstants.RESP_TASK:
                case WSConstants.RESP_LOG:
                    final Buffer callBackCodeView = it.next();
                    final int callbackCode = Base64.decodeToIntWithBounds(callBackCodeView, 0, callBackCodeView.length());
                    final Callback resolvedCallback = _callbacks.get(callbackCode);
                    final Buffer newBuf = _graph.newBuffer();//will be free by the core
                    boolean isFirst = true;
                    while (it.hasNext()) {
                        if (isFirst) {
                            isFirst = false;
                        } else {
                            newBuf.write(Constants.BUFFER_SEP);
                        }
                        newBuf.writeAll(it.next().data());
                    }
                    _callbacks.remove(callbackCode);
                    resolvedCallback.on(newBuf);
                    break;
                case WSConstants.RESP_TASK_STATS: {
                    final Buffer callbackStatsCodeView = it.next();
                    final Buffer statsContentView = it.next();
                    final int callbackStatsCode = Base64.decodeToIntWithBounds(callbackStatsCodeView, 0, callbackStatsCodeView.length());
                    final String statsContent = Base64.decodeToStringWithBounds(statsContentView, 0, statsContentView.length());
                    final Callback statCallback = _callbacks.get(callbackStatsCode);
                    statCallback.on(statsContent);
                    break;
                }
                case WSConstants.RESP_TASK_STOP: {
                    Buffer genericCodeView = it.next();
                    final int genericCode = Base64.decodeToIntWithBounds(genericCodeView, 0, genericCodeView.length());
                    Callback genericCallback = _callbacks.get(genericCode);
                    _callbacks.remove(genericCode);
                    genericCallback.on(true);
                    break;
                }
                default:
                    Buffer genericCodeView = it.next();
                    final int genericCode = Base64.decodeToIntWithBounds(genericCodeView, 0, genericCodeView.length());
                    Callback genericCallback = _callbacks.get(genericCode);
                    _callbacks.remove(genericCode);
                    genericCallback.on(true);
            }
        }
        payloadBuf.free();
    }
}
