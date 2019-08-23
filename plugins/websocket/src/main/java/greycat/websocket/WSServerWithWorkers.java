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
import greycat.internal.CoreGraphLog;
import greycat.utility.Base64;
import greycat.internal.heap.HeapBuffer;
import greycat.struct.Buffer;
import greycat.struct.BufferIterator;
import greycat.workers.*;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WSServerWithWorkers implements WebSocketConnectionCallback, Callback<Buffer> {

    private static final String SERVER_IP = "0.0.0.0";
    private static final String SERVER_PREFIX = "/ws";

    private final Log logger = new CoreGraphLog(null);

    private int wsMaxIdle = 3600 * 1000; //1h by default
    private final int port;
    private Undertow server;

    protected Set<WebSocketChannel> peers;
    protected Map<String, HttpHandler> handlers;
    protected HttpHandler defaultHandler;

    private WorkerCallbacksRegistry callbackRegistry = new WorkerCallbacksRegistry();

    public WSServerWithWorkers(int p_port) {
        this.port = p_port;
        peers = new HashSet<WebSocketChannel>();
        handlers = new HashMap<String, HttpHandler>();
        handlers.put(SERVER_PREFIX, Handlers.websocket(this));

    }

    public WSServerWithWorkers setDefaultHandler(HttpHandler httpHandler) {
        this.defaultHandler = httpHandler;
        return this;
    }

    public WSServerWithWorkers addHandler(String prefix, HttpHandler httpHandler) {
        handlers.put(prefix, httpHandler);
        return this;
    }

    public void setMaxIdle(int maxIdle) {
        this.wsMaxIdle = maxIdle;
    }


    public void start() {

        logger.debug("WSServer starting");
        PathHandler pathHandler;
        if (this.defaultHandler != null) {
            pathHandler = Handlers.path(defaultHandler);
        } else {
            pathHandler = Handlers.path();
        }
        for (String name : handlers.keySet()) {
            pathHandler.addPrefixPath(name, handlers.get(name));
        }

        String serverPath = "ws://" + SERVER_IP + ":" + port + SERVER_PREFIX;
        this.server = Undertow.builder()
                .addHttpListener(port, SERVER_IP, pathHandler)
                .setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT, wsMaxIdle)
                .setServerOption(UndertowOptions.IDLE_TIMEOUT, wsMaxIdle)
                .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, true)
                .build();

        server.start();
        logger.info("WSServer started on " + serverPath);
    }

    public void stop() {
        logger.debug("WSServer stopping connections");
        peers.forEach(peer -> {
            try {
                peer.setCloseCode(1012);
                peer.setCloseReason("Service restart");
                peer.send(WebSocketFrameType.CLOSE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        logger.debug("WSServer stopping server");
        server.stop();
        server = null;
        logger.info("WSServer\tStopped");
    }

    @Override
    public void onConnect(WebSocketHttpExchange webSocketHttpExchange, WebSocketChannel webSocketChannel) {
        webSocketChannel.getReceiveSetter().set(new PeerInternalListener(webSocketChannel));
        webSocketChannel.resumeReceives();
        peers.add(webSocketChannel);
    }

    @Override
    public final void on(final Buffer result) {
        logger.trace("WSServer\tNotifying update");
        //broadcast to anyone...
        WebSocketChannel[] others = peers.toArray(new WebSocketChannel[peers.size()]);
        Buffer notificationBuffer = new HeapBuffer();
        notificationBuffer.write(StorageMessageType.NOTIFY_UPDATE);
        notificationBuffer.write(Constants.BUFFER_SEP);
        notificationBuffer.writeAll(result.data());
        byte[] notificationMsg = notificationBuffer.data();
        notificationBuffer.free();
        for (int i = 0; i < others.length; i++) {
            WebSockets.sendBinary(ByteBuffer.wrap(notificationMsg), others[i], null);
        }
    }

    protected class PeerInternalListener extends AbstractReceiveListener {

        private final byte[] PONG_MESSAGE = new byte[]{StorageMessageType.HEART_BEAT_PONG};
        Thread mailboxReaper;
        WorkerMailbox localMailbox;
        int localMailboxId;
        protected GraphWorker sessionWorker;


        public PeerInternalListener(final WebSocketChannel channel) {
            localMailbox = new WorkerMailbox(false);
            localMailboxId = MailboxRegistry.getInstance().addMailbox(localMailbox);

            logger.info("WSServer\tNew peer connected (" + localMailboxId + ")");

            mailboxReaper = new Thread(() -> {

                final WebSocketCallback wsCallback = new WebSocketCallback<Void>() {
                    @Override
                    public void complete(WebSocketChannel webSocketChannel, Void aVoid) {
                        logger.trace("WSServer\tSent message to peer " + localMailboxId);
                    }

                    @Override
                    public void onError(WebSocketChannel webSocketChannel, Void aVoid, Throwable throwable) {
                        logger.error("Error occurred while sending to channel " + localMailboxId);
                        if (throwable != null) {
                            if(throwable instanceof ClosedChannelException) {
                                killSession(webSocketChannel);
                            } else {
                                throwable.printStackTrace();
                            }
                        }
                    }
                };

                try {
                    logger.debug("WSServer\tMailbox reaper ready for peer (" + localMailboxId + ")");
                    while (true && channel.isOpen()) {
                        byte[] newMessage = localMailbox.take();
                        /*
                        Buffer jobBuffer = new HeapBuffer();
                        jobBuffer.writeAll(newMessage);
                        BufferIterator it = jobBuffer.iterator();
                        Buffer bufferTypeBufferView = it.next();//Ignore
                        Buffer respChannelBufferView = it.next();//Ignore
                        Buffer callbackBufferView = it.next();
                        int callbackId = Base64.decodeToIntWithBounds(callbackBufferView, 0, callbackBufferView.length());
                        */

                        logger.trace("WSServer\tForwarding response type " + StorageMessageType.byteToString(newMessage[0]) + " to peer " + localMailboxId);
                        WebSockets.sendBinary(ByteBuffer.wrap(newMessage), channel, wsCallback);
                    }
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
                logger.debug("WSServer\tMailbox reaper exited for peer (" + localMailboxId + ")");
            }, "WSServerReaper_" + channel.getDestinationAddress().getPort());
            mailboxReaper.start();
        }


        public boolean submitActivityToSessionWorker(byte[] activity) {
            if (sessionWorker == null) {
                sessionWorker = GraphWorkerPool.getInstance().createWorker(WorkerAffinity.SESSION_WORKER, "Session " + localMailboxId + " Worker", null);
            }
            return sessionWorker.submit(activity);
        }


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
           killSession(webSocketChannel);

            super.onClose(webSocketChannel, channel);
        }

        private void killSession(WebSocketChannel webSocketChannel) {
            logger.info("WSServer\tPeer (" + localMailboxId + ") connection closed: " + webSocketChannel.getCloseCode() + "\t" + webSocketChannel.getCloseReason());
            if (sessionWorker != null) {
                logger.trace("WSServer\tDestroying session worker id:" + sessionWorker.getId());
                GraphWorkerPool.getInstance().destroyWorkerById(sessionWorker.getId());
            }
            peers.remove(webSocketChannel);
            logger.debug("WSServer\tClosing mailbox id:" + localMailboxId);
            MailboxRegistry.getInstance().removeMailbox(localMailboxId);
            mailboxReaper.interrupt();
        }

        private void process_rpc(final byte[] input, final WebSocketChannel channel) {
            if (input.length == 0) {
                return;
            }

            if(input[0] == StorageMessageType.HEART_BEAT_PING) {
                localMailbox.submit(PONG_MESSAGE);
                return;
            }

            Buffer jobBuffer = new HeapBuffer();
            jobBuffer.writeAll(input);

            if (Log.LOG_LEVEL >= Log.TRACE) {
                final BufferIterator it = jobBuffer.iterator();
                final Buffer frameTypeBufferView = it.next();
                final Buffer respChannelBufferView = it.next();
                final Buffer callbackBufferView = it.next();
                
                logger.trace("WSServer\t========= WSServer Sending to Workers =========");
                logger.trace("WSServer\tType: " + StorageMessageType.byteToString(frameTypeBufferView.read(0)));
                logger.trace("WSServer\tChannel: " + respChannelBufferView.readInt(0));
                logger.trace("WSServer\tCallback: " + Base64.decodeToIntWithBounds(callbackBufferView, 0, callbackBufferView.length()));
                logger.trace("WSServer\tRaw: " + jobBuffer.toString());
            }


            //read worker affinity
            byte workerAffinity = (byte) (0xFF & jobBuffer.readInt(2));
            logger.trace("WSServer\tSubmitting task to " + WorkerAffinity.byteToString(workerAffinity));

            //Override worker affinity with mailboxId for response
            jobBuffer.writeIntAt(this.localMailboxId, 2);

            PeerInternalListener internalListener = (PeerInternalListener) ((SimpleSetter<WebSocketChannel>) channel.getReceiveSetter()).get();

            submitToWorker(workerAffinity, jobBuffer, internalListener);

        }

        protected void submitToWorker(byte workerAffinity, Buffer jobBuffer, PeerInternalListener internalListener) {
            WorkerMailbox destMailbox;

            switch (workerAffinity) {
                case WorkerAffinity.GENERAL_PURPOSE_WORKER: {
                    destMailbox = MailboxRegistry.getInstance().getMailbox(MailboxRegistry.getInstance().getDefaultMailboxId());
                    destMailbox.submit(jobBuffer.data());
                    MailboxRegistry.getInstance().notifyMailboxes();
                }
                break;
                case WorkerAffinity.SESSION_WORKER: {
                    internalListener.submitActivityToSessionWorker(jobBuffer.data());
                }
                break;
                case WorkerAffinity.TASK_WORKER: {
                    BufferIterator it = jobBuffer.iterator();
                    Buffer bufferTypeBufferView = it.next();
                    Buffer respChannelBufferView = it.next();
                    Buffer callbackBufferView = it.next();

                    int callbackId = Base64.decodeToIntWithBounds(callbackBufferView, 0, callbackBufferView.length());
                    //Create and register a specific worker for the task, identified by the callbackID of teh task for this channel.
                    GraphWorker taskWorker = GraphWorkerPool.getInstance().createWorker(WorkerAffinity.TASK_WORKER, "TaskWorker(" + callbackId + ")", null);
                    taskWorker.submit(jobBuffer.data());
                }
                break;
                default: {
                    logger.warn("WSServer\tUnexpected value for WorkerAffinity: " + (int) workerAffinity);
                }
            }
        }
    }
}
