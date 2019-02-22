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
package greycat.internal;

import greycat.Callback;
import greycat.Graph;
import greycat.plugin.Storage;
import greycat.struct.Buffer;

public class ReadOnlyStorage implements Storage {

    private final Storage wrapped;

    private Graph graph;

    public ReadOnlyStorage(final Storage toWrap) {
        wrapped = toWrap;
    }

    @Override
    public void get(Buffer keys, Callback<Buffer> callback) {
        wrapped.get(keys, callback);
    }

    @Override
    public void put(Buffer stream, Callback<Boolean> callback) {
        if (callback != null) {
            callback.on(false);
        }
    }

    @Override
    public void putSilent(Buffer stream, Callback<Buffer> callback) {
        if (callback != null) {
            callback.on(graph.newBuffer());
        }
    }

    @Override
    public void remove(Buffer keys, Callback<Boolean> callback) {
        callback.on(false);
    }

    @Override
    public void connect(Graph p_graph, Callback<Boolean> callback) {
        this.graph = p_graph;
        wrapped.connect(graph, callback);
    }

    @Override
    public void disconnect(Callback<Boolean> callback) {
        wrapped.disconnect(callback);
    }

    @Override
    public void listen(Callback<Buffer> synCallback) {
        wrapped.listen(synCallback);
    }

    @Override
    public void lock(Callback<Buffer> callback) {
        wrapped.lock(callback);
    }

    @Override
    public void unlock(Buffer previousLock, Callback<Boolean> callback) {
        wrapped.unlock(previousLock, callback);
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
