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
package greycat.multithread.websocket.buffergraph;

import greycat.Callback;
import greycat.Graph;
import greycat.multithread.websocket.message.GraphExecutorMessage;
import greycat.plugin.Storage;
import greycat.struct.Buffer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static greycat.multithread.websocket.Constants.*;

/**
 * Storage using queue to communicate with the main graph
 */
public class BufferStorage implements Storage {

    private final BlockingQueue<GraphExecutorMessage> graphInput;
    private final List<Callback<Buffer>> _listeners = new ArrayList<Callback<Buffer>>();
    private Graph graph;
    private int counter = 0;

    public BufferStorage(BlockingQueue<GraphExecutorMessage> graphInput) {
        this.graphInput = graphInput;
    }

    @Override
    public void get(Buffer keys, Callback<Buffer> callback) {
        bufferize(REQ_GET, keys, callback);
    }

    @Override
    public void put(Buffer stream, Callback<Boolean> callback) {
        bufferize(REQ_PUT, stream, callback);
    }

    @Override
    public void putSilent(Buffer stream, Callback<Buffer> callback) {
        this.put(stream, new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                callback.on(null);
            }
        });
    }

    @Override
    public void remove(Buffer keys, Callback<Boolean> callback) {
        bufferize(REQ_REMOVE, keys, callback);
    }

    @Override
    public void connect(Graph graph, Callback<Boolean> callback) {
        this.graph = graph;
        if (callback != null) {
            callback.on(true);//already connected
        }
    }

    @Override
    public void lock(Callback<Buffer> callback) {
        bufferize(REQ_LOCK, null, callback);
    }

    @Override
    public void unlock(Buffer previousLock, Callback<Boolean> callback) {
        bufferize(REQ_UNLOCK, previousLock, callback);
    }

    @Override
    public void disconnect(Callback<Boolean> callback) {
        if (callback != null) {
            callback.on(true);//already connected
        }
    }

    @Override
    public void listen(Callback<Buffer> synCallback) {
        _listeners.add(synCallback);
    }

    @Override
    public void backup(String path) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(String path) throws Exception {
        throw new UnsupportedOperationException();
    }


    private void bufferize(final byte operationId, final Buffer payload, final Callback callback) {
        ((BufferScheduler) this.graph.scheduler()).getCallbackMap().put(counter, callback);

        try {
            graphInput.put(new GraphExecutorMessage(((BufferScheduler) this.graph.scheduler()).getIncomingMessages(), operationId, counter, payload, null));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (counter == Integer.MAX_VALUE) {
            counter = 0;
        } else {
            counter++;
        }
    }
}
