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
import greycat.utility.Base64;
import greycat.internal.CoreGraphLogFile;
import greycat.internal.heap.HeapBuffer;
import greycat.struct.Buffer;
import greycat.struct.BufferIterator;
import greycat.workers.MailboxRegistry;
import greycat.workers.StorageMessageType;
import greycat.workers.WorkerCallbacksRegistry;
import greycat.workers.WorkerMailbox;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WSServerWithWorkers implements WebSocketConnectionCallback, Callback<Buffer> {

    private final Log logger = new CoreGraphLogFile(null, "WSServer", "2MB");

    private final int port;
    private Undertow server;

    protected Set<WebSocketChannel> peers;
    protected Map<String, HttpHandler> handlers;

    private WorkerCallbacksRegistry callbackRegistry = new WorkerCallbacksRegistry();

    public WSServerWithWorkers(int p_port) {
        this.port = p_port;
        peers = new HashSet<WebSocketChannel>();
        handlers = new HashMap<String, HttpHandler>();
        handlers.put(SERVER_PREFIX, Handlers.websocket(this));

    }

    public WSServerWithWorkers addHandler(String prefix, HttpHandler httpHandler) {
        handlers.put(prefix, httpHandler);
        return this;
    }

    private static final String SERVER_PREFIX = "/ws";
    private static final String SERVER_IP = "0.0.0.0";

    public void start() {
        logger.debug("WSServer starting");
        final PathHandler pathHandler = Handlers.path();
        for (String name : handlers.keySet()) {
            pathHandler.addPrefixPath(name, handlers.get(name));
        }
        String serverPath = "ws://" + SERVER_IP + ":" + port + SERVER_PREFIX;
        this.server = Undertow.builder().addHttpListener(port, SERVER_IP, pathHandler).build();
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
        logger.debug("WSServer\tNotifying update");
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

        Thread mailboxReaper;
        WorkerMailbox localMailbox;
        int localMailboxId;

        public PeerInternalListener(WebSocketChannel channel) {
            localMailbox = new WorkerMailbox(false);
            localMailboxId = MailboxRegistry.getInstance().addMailbox(localMailbox);

            logger.info("WSServer\tNew peer connected (" + localMailboxId + ")");

            mailboxReaper = new Thread(() -> {
                try {
                    logger.debug("WSServer\tMailbox reaper ready for peer (" + localMailboxId + ")");
                    while (true) {
                        byte[] newMessage = localMailbox.take();
                        logger.debug("WSServer\tForwarding response to peer "+ localMailboxId);
                        WebSockets.sendBinary(ByteBuffer.wrap(newMessage), channel, null);
                    }
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
                logger.debug("WSServer\tMailbox reaper exited for peer ("+localMailboxId+")");
            }, "WSServerReaper_" + channel.getDestinationAddress().getPort());
            mailboxReaper.start();
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
            logger.info("WSServer\tPeer ("+localMailboxId+") connection closed: " + webSocketChannel.getCloseCode() + "\t" + webSocketChannel.getCloseReason());
            peers.remove(webSocketChannel);
            MailboxRegistry.getInstance().removeMailbox(localMailboxId);
            mailboxReaper.interrupt();
            super.onClose(webSocketChannel, channel);
        }

        private void process_rpc(final byte[] input, final WebSocketChannel channel) {
            if (input.length == 0) {
                return;
            }

            Buffer jobBuffer = new HeapBuffer();
            jobBuffer.writeAll(input);
            //Override mailboxId for response
            jobBuffer.writeIntAt(this.localMailboxId, 2);

            if(Constants.enableDebug) {
                final BufferIterator it = jobBuffer.iterator();
                final Buffer bufferTypeBufferView = it.next();
                final Buffer respChannelBufferView = it.next();
                final Buffer callbackBufferView = it.next();

                logger.debug("WSServer\t========= WSServer Sending to Workers =========");
                logger.debug("WSServer\tType: " + StorageMessageType.byteToString(bufferTypeBufferView.read(0)));
                logger.debug("WSServer\tChannel: " + respChannelBufferView.readInt(0));
                logger.debug("WSServer\tCallback: " + Base64.decodeToIntWithBounds(callbackBufferView, 0, callbackBufferView.length()));
                logger.debug("WSServer\tRaw: " + jobBuffer.toString());
            }

            WorkerMailbox destMailbox = MailboxRegistry.getInstance().getMailbox(MailboxRegistry.getInstance().getDefaultMailboxId());

            destMailbox.submit(jobBuffer.data());
            MailboxRegistry.getInstance().notifyMailboxes();

        }

    }

}
