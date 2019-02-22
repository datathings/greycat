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

import greycat.Graph;
import greycat.plugin.Storage;
import greycat.struct.Buffer;
import greycat.struct.BufferIterator;
import greycat.utility.Base64;
import greycat.Callback;

public class BlackHoleStorage implements Storage {

    private Graph _graph;

    private short prefix = 0;

    @Override
    public final void get(Buffer keys, Callback<Buffer> callback) {
        Buffer result = _graph.newBuffer();
        BufferIterator it = keys.iterator();
        boolean isFirst = true;

        while (it.hasNext()) {
            Buffer tempView = it.next();
            //do nothing with the view, redirect to BlackHole...
            if (isFirst) {
                isFirst = false;
            } else {
                result.write(CoreConstants.BUFFER_SEP);
            }
        }
        callback.on(result);
    }

    @Override
    public final void put(Buffer stream, Callback<Boolean> callback) {
        //System.err.println("WARNING: POTENTIAL DATA LOSSES, NOOP STORAGE don't save");
        if (callback != null) {
            callback.on(true);
        }
    }

    @Override
    public void putSilent(Buffer stream, Callback<Buffer> callback) {
        if (callback != null) {
            callback.on(_graph.newBuffer());
        }
    }

    @Override
    public final void remove(Buffer keys, Callback<Boolean> callback) {
        callback.on(true);
    }

    @Override
    public final void connect(Graph graph, Callback<Boolean> callback) {
        _graph = graph;
        callback.on(true);
    }

    @Override
    public final void lock(Callback<Buffer> callback) {
        Buffer buffer = _graph.newBuffer();
        Base64.encodeIntToBuffer(prefix, buffer);
        prefix++;
        callback.on(buffer);
    }

    @Override
    public final void unlock(Buffer previousLock, Callback<Boolean> callback) {
        callback.on(true);
    }

    @Override
    public final void disconnect(Callback<Boolean> callback) {
        _graph = null;
        callback.on(true);
    }

    @Override
    public void listen(Callback<Buffer> synCallback) {
        //TODO
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
