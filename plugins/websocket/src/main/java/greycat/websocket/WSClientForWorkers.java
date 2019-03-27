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
import greycat.base.BaseTaskResult;
import greycat.internal.task.CoreProgressReport;
import greycat.plugin.Storage;
import greycat.plugin.TaskExecutor;
import greycat.struct.Buffer;
import greycat.struct.BufferIterator;
import greycat.utility.Base64;
import greycat.utility.L3GMap;
import greycat.utility.Tuple;
import greycat.workers.StorageMessageType;
import greycat.workers.WorkerAffinity;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.client.WebSocketClient;
import io.undertow.websockets.core.*;
import org.xnio.*;
import org.xnio.ssl.JsseXnioSsl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class WSClientForWorkers implements Storage, TaskExecutor {

    private final String _url;

    private WebSocketChannel _channel;

    private XnioWorker _worker;

    private Graph _graph;

    private static final int MIN_INTEGER = -2147483648;
    private static final int MAX_INTEGER = 2147483647;
    private Map<Integer, Callback> _callbacks;
    private AtomicInteger callbackCounters = new AtomicInteger(MIN_INTEGER);

    private final List<Callback<Buffer>> _listeners = new ArrayList<Callback<Buffer>>();

    public WSClientForWorkers(String p_url) {
        this._url = p_url;
        this._callbacks = new ConcurrentHashMap<Integer, Callback>();
    }

    private int registerCallback(Callback c) {
        int callbackId = callbackCounters.getAndIncrement();
        if(callbackCounters.get() == MAX_INTEGER) {
            callbackCounters.set(MIN_INTEGER);
        }
        _callbacks.put(callbackId, c);
        return callbackId;
    }

    @Override
    public final void get(Buffer keys, Callback<Buffer> callback) {
        send_rpc_req(StorageMessageType.REQ_GET, keys, callback);
    }

    @Override
    public final void put(Buffer stream, Callback<Boolean> callback) {
        send_rpc_req(StorageMessageType.REQ_PUT, stream, callback);
    }

    @Override
    public final void putSilent(Buffer stream, Callback<Buffer> callback) {
        send_rpc_req(StorageMessageType.REQ_PUT, stream, new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                callback.on(null);
            }
        });
    }

    @Override
    public final void remove(Buffer keys, Callback<Boolean> callback) {
        send_rpc_req(StorageMessageType.REQ_REMOVE, keys, callback);
    }

    @Override
    public final void lock(Callback<Buffer> callback) {
        send_rpc_req(StorageMessageType.REQ_LOCK, null, callback);
    }

    @Override
    public final void unlock(Buffer previousLock, Callback<Boolean> callback) {
        send_rpc_req(StorageMessageType.REQ_UNLOCK, previousLock, callback);
    }

    @Override
    public final void taskStats(Callback<String> callback) {
        send_rpc_req(StorageMessageType.REQ_TASK_STATS, null, callback);
    }

    @Override
    public final void taskStop(Integer id, Callback<Boolean> callback) {
        Buffer buf = this._graph.newBuffer();
        Base64.encodeIntToBuffer(id, buf);
        send_rpc_req(StorageMessageType.REQ_TASK_STATS, buf, callback);
        buf.free();
    }

    @Override
    public final void connect(final Graph p_graph, final Callback<Boolean> callback) {
        p_graph.log().info("WSClient connecting to " + this._url);
        if (_channel != null) {
            if (callback != null) {
                callback.on(true);//already connected
            }
        }
        this._graph = p_graph;
        try {
            final Xnio xnio = Xnio.getInstance(WebSocketClient.class.getClassLoader());
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

                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
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
            WebSocketClient.ConnectionBuilder builder = WebSocketClient
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
    public final void execute(final Callback<TaskResult> taskResultCallback, final Task task, final TaskContext prepared) {

        final Buffer buffer = _graph.newBuffer();

        final int hashPrint;
        final int hashProgress;
        if (prepared != null) {
            final Callback<String> printHook = prepared.printHook();
            if (printHook != null) {
                hashPrint = registerCallback(printHook);
            } else {
                hashPrint = -1;
            }
            final Callback<TaskProgressReport> progressHook = prepared.progressHook();
            if (progressHook != null) {
                hashProgress = registerCallback(progressHook);
            } else {
                hashProgress = -1;
            }
        } else {
            hashPrint = -1;
            hashProgress = -1;
        }


        Callback<Buffer> onResult = new Callback<Buffer>() {
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
                        taskResultCallback.on(baseTaskResult);
                    }
                });
            }
        };


        //Header
        buffer.write(StorageMessageType.REQ_TASK);
        buffer.write(Constants.BUFFER_SEP);
        //Using workerMailbox place to send worker affinity for remote execution
        if(prepared != null) {
            buffer.writeInt(prepared.getWorkerAffinity());
        } else {
            buffer.writeInt(WorkerAffinity.GENERAL_PURPOSE_WORKER);
        }
        buffer.write(Constants.BUFFER_SEP);
        int onResultCallback = registerCallback(onResult);
        Base64.encodeIntToBuffer(onResultCallback, buffer);
        //Payload
        buffer.write(Constants.BUFFER_SEP);
        task.saveToBuffer(buffer);

        if (prepared != null) {
            buffer.write(Constants.BUFFER_SEP);
            if (hashPrint != -1) {
                Base64.encodeIntToBuffer(hashPrint, buffer);
            }

            buffer.write(Constants.BUFFER_SEP);
            if (hashProgress != -1) {
                Base64.encodeIntToBuffer(hashProgress, buffer);
            }
            buffer.write(Constants.BUFFER_SEP);
            prepared.saveToBuffer(buffer);
        }

        if (_channel == null) {
            throw new RuntimeException(StorageMessageType.DISCONNECTED_ERROR);
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

    @Override
    public final void log(String msg) {
        Buffer buf = this._graph.newBuffer();
        Base64.encodeStringToBuffer(msg, buf);
        send_rpc_req(StorageMessageType.REQ_LOG, buf, new Callback() {
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
            throw new RuntimeException(StorageMessageType.DISCONNECTED_ERROR);
        }
        Buffer buffer = _graph.newBuffer();
        buffer.write(operationId);
        buffer.write(Constants.BUFFER_SEP);
        //Reserving 4 bytes for the channels
        buffer.writeInt(WorkerAffinity.GENERAL_PURPOSE_WORKER);
        buffer.write(Constants.BUFFER_SEP);
        int hash = registerCallback(callback);
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


    private void process_rpc_resp(byte[] payload) {
        Buffer payloadBuf = _graph.newBuffer();
        payloadBuf.writeAll(payload);
        BufferIterator it = payloadBuf.iterator();
        Buffer codeView = it.next();
        if (codeView != null && codeView.length() != 0) {

            final byte firstCode = codeView.read(0);
            final Buffer messageQueueBufferView;//Ignored
            final Buffer callbackIdBufferView;
            if (firstCode != StorageMessageType.NOTIFY_UPDATE) {
                messageQueueBufferView = it.next();
                callbackIdBufferView = it.next();
            } else {
                messageQueueBufferView = null;
                callbackIdBufferView = null;
            }

            switch (firstCode) {
                case StorageMessageType.NOTIFY_UPDATE: {
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
                }
                break;
                case StorageMessageType.NOTIFY_PROGRESS: {
                    final Buffer progressCallbackView = it.next();
                    final int progressCallbackCode = Base64.decodeToIntWithBounds(callbackIdBufferView, 0, callbackIdBufferView.length());
                    final CoreProgressReport report = new CoreProgressReport();
                    report.loadFromBuffer(progressCallbackView);
                    Callback<TaskProgressReport> progressHook = _callbacks.get(progressCallbackCode);
                    if(progressHook != null) {
                        progressHook.on(report);
                    } else {
                        System.err.println();
                    }
                }
                break;
                case StorageMessageType.RESP_LOCK:
                case StorageMessageType.RESP_GET:
                case StorageMessageType.RESP_TASK:
                case StorageMessageType.RESP_LOG: {
                    final int callbackCode = Base64.decodeToIntWithBounds(callbackIdBufferView, 0, callbackIdBufferView.length());
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
                }
                    break;
                case StorageMessageType.RESP_TASK_STATS:
                case StorageMessageType.NOTIFY_PRINT: {
                    final Buffer contentView = it.next();
                    final int callbackCode = Base64.decodeToIntWithBounds(callbackIdBufferView, 0, callbackIdBufferView.length());
                    final String content = Base64.decodeToStringWithBounds(contentView, 0, contentView.length());
                    final Callback localCallback = _callbacks.get(callbackCode);
                    localCallback.on(content);
                }
                break;
                default: {
                    final int genericCode = Base64.decodeToIntWithBounds(callbackIdBufferView, 0, callbackIdBufferView.length());
                    Callback genericCallback = _callbacks.get(genericCode);
                    _callbacks.remove(genericCode);
                    genericCallback.on(true);
                }
            }
        }
        payloadBuf.free();
    }

    @Override
    public void backup(String path) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(String path) throws Exception {
        throw new UnsupportedOperationException();
    }
}
