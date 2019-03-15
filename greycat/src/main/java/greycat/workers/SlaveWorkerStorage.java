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
package greycat.workers;

import greycat.Callback;
import greycat.Constants;
import greycat.Graph;
import greycat.internal.heap.HeapBuffer;
import greycat.plugin.Storage;
import greycat.struct.Buffer;
import greycat.utility.Base64;

import static greycat.workers.StorageMessageType.*;

/**
 * @ignore ts
 */
public class SlaveWorkerStorage implements Storage {

    private WorkerCallbacksRegistry callbacksRegistry;
    private int workerMailboxId;

    public void setWorkerMailboxId(int workerMailboxId, WorkerCallbacksRegistry callbacksRegistry) {
        this.workerMailboxId = workerMailboxId;
        this.callbacksRegistry = callbacksRegistry;
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
        throw new UnsupportedOperationException();
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
        int callbackId = callbacksRegistry.register(callback);

        Buffer taskBuffer = new HeapBuffer();
        //Type REQ_TASK
        taskBuffer.write(operationId);
        taskBuffer.write(Constants.BUFFER_SEP);
        //Resp. Channel
        taskBuffer.writeInt(workerMailboxId);
        taskBuffer.write(Constants.BUFFER_SEP);
        //Callback.id
        Base64.encodeIntToBuffer(callbackId, taskBuffer);
        if (payload != null) {
            taskBuffer.write(Constants.BUFFER_SEP);
            taskBuffer.writeAll(payload.data());
        }
        MailboxRegistry.getInstance().getMailbox(GraphWorkerPool.getInstance().getRootWorkerMailboxId()).submit(taskBuffer.data());
        taskBuffer.free();
    }
}
