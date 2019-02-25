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

import greycat.Graph;
import greycat.internal.heap.HeapBuffer;
import greycat.struct.Buffer;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import static greycat.multithread.websocket.Constants.*;

/**
 * Thread handling all write from the Websocket server based on a message Queue
 */
public class ResolverWorker extends Thread {
    private final BlockingQueue<GraphMessage> toResolve;
    private final Map<Integer, WebSocketChannel> channels;
    protected boolean running = true;
    protected Graph sideGraph = null;

    /**
     * @param toResolve message Queue holding all message to send
     * @param channels  map of existing channels
     */
    public ResolverWorker(BlockingQueue<GraphMessage> toResolve, Map<Integer, WebSocketChannel> channels) {
        this.toResolve = toResolve;
        this.channels = channels;
    }

    @Override
    public void run() {
        while (running) {
            GraphMessage message = null;
            try {
                message = toResolve.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (message != null) {
                if (message.getOperationId() == NOTIFY_UPDATE) {
                    if (sideGraph != null) {
                        sideGraph.remoteNotify(message.getContent());
                    }
                    Buffer notificationBuffer = new HeapBuffer();
                    notificationBuffer.write(NOTIFY_UPDATE);
                    notificationBuffer.write(greycat.Constants.BUFFER_SEP);
                    notificationBuffer.writeAll(message.getContent().data());
                    WebSocketChannel[] channelsToNotify = channels.values().toArray(new WebSocketChannel[channels.size()]);
                    for (int i = 0; i < channelsToNotify.length; i++) {
                        ByteBuffer finalBuf = ByteBuffer.wrap(notificationBuffer.data());
                        WebSockets.sendBinary(finalBuf, channelsToNotify[i], null);
                    }
                    message.getContent().free();
                    notificationBuffer.free();
                } else {
                    if (channels.containsKey(message.getReturnID())) {
                        WebSocketChannel channel = channels.get(message.getReturnID());
                        final Buffer newBuf = new HeapBuffer();
                        newBuf.write(message.getOperationId());
                        newBuf.write(greycat.Constants.BUFFER_SEP);
                        newBuf.writeAll(message.getOriginalCallBack().data());
                        message.getOriginalCallBack().free();
                        newBuf.write(greycat.Constants.BUFFER_SEP);
                        if (message.getContent() != null) {
                            newBuf.writeAll(message.getContent().data());
                            message.getContent().free();
                        }
                        ByteBuffer finalBuf = ByteBuffer.wrap(newBuf.data());
                        newBuf.free();
                        WebSockets.sendBinary(finalBuf, channel, null);
                    } else {
                        if (message.getContent() != null) {
                            message.getContent().free();
                        }
                        message.getOriginalCallBack().free();
                    }
                }
            }
        }

    }

}
