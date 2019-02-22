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
package org.mwg;

import org.mwg.plugin.Storage;
import org.mwg.struct.Buffer;
import org.mwg.struct.BufferIterator;
import voldemort.client.*;
import voldemort.client.protocol.RequestFormatType;
import voldemort.server.VoldemortConfig;
import voldemort.store.CompositePutVoldemortRequest;
import voldemort.store.routed.action.PerformParallelPutRequests;
import voldemort.utils.ByteArray;
import voldemort.utils.ByteUtils;

// TODO check for configuration params
// TODO do we create a client per request or keep one for the storage


public class VoldemortStorage implements Storage {
    private static final String _connectedError = "PLEASE CONNECT YOUR DATABASE FIRST";


    private boolean _isConnected;

    private final String _bootstrapUrl;
    private final String _storeName;

    private StoreClientFactory _factory;
    private StoreClient<ByteArray, byte[]> _client;
    private Graph _graph;

    private VoldemortConfig _config;

    public VoldemortStorage(String bootstrapUrl, String storeName) {
        this._bootstrapUrl = bootstrapUrl;
        this._storeName = storeName;

        this._factory = new SocketStoreClientFactory(new ClientConfig()
                .setEnableSerializationLayer(false)
//                .setEnableLazy(false)
//                .setRequestFormatType(RequestFormatType.VOLDEMORT_V3)
                .setBootstrapUrls(bootstrapUrl)
        );
    }

    @Override
    public void get(Buffer keys, Callback<Buffer> callback) {
        if (!_isConnected) {
            throw new RuntimeException(_connectedError);
        }
        Buffer result = _graph.newBuffer();
        BufferIterator it = keys.iterator();
        boolean isFirst = true;
        while (it.hasNext()) {
            Buffer view = it.next();
            try {
                if (!isFirst) {
                    result.write(Constants.BUFFER_SEP);
                } else {
                    isFirst = false;
                }
                ByteArray key = new ByteArray(view.data());
                byte[] res = _client.getValue(key);
                if (res != null) {
                    result.writeAll(res);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (callback != null) {
            callback.on(result);
        }
    }

    @Override
    public void put(Buffer stream, Callback<Boolean> p_callback) {

        if (!_isConnected) {
            throw new RuntimeException(_connectedError);
        }

        BufferIterator it = stream.iterator();
        while (it.hasNext()) {
            Buffer keyView = it.next();
            Buffer valueView = it.next();
            if (valueView != null) {
                 ByteArray key = new ByteArray(keyView.data());
                _client.put(key, valueView.data());
            }
        }


        if (p_callback != null) {
            p_callback.on(true);
        }



    }

    @Override
    public void remove(Buffer keys, Callback<Boolean> callback) {
        if (!_isConnected) {
            throw new RuntimeException(_connectedError);
        }
        try {
            BufferIterator it = keys.iterator();
            while (it.hasNext()) {
                Buffer view = it.next();
                _client.delete(new ByteArray(view.data()));
            }
            if (callback != null) {
                callback.on(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) {
                callback.on(false);
            }
        }
    }

    @Override
    public void connect(Graph graph, Callback<Boolean> callback) {
        if (_isConnected) {
            if (callback != null) {
                callback.on(null);
            }
            return;
        }

        this._graph = graph;

        // create a client that executes operations on a single store
        _client = _factory.getStoreClient(this._storeName);

        _isConnected = true;

        callback.on(true);
    }

    @Override
    public void lock(Callback<Buffer> callback) {
        byte[] current = null;//db.get(prefixKey);
        if (current == null) {
            current = new String("0").getBytes();
        }
        Short currentPrefix = Short.parseShort(new String(current));
        // db.put(prefixKey, ((currentPrefix + 1) + "").getBytes());
        if (callback != null) {
            Buffer newBuf = _graph.newBuffer();
            org.mwg.utility.Base64.encodeIntToBuffer(currentPrefix, newBuf);
            callback.on(newBuf);
        }
    }

    @Override
    public void unlock(Buffer previousLock, Callback<Boolean> callback) {
        callback.on(true);

    }

    @Override
    public void disconnect(Callback<Boolean> callback) {
        _client = null;
        if (callback != null) {
            callback.on(true);
        }
    }
}
