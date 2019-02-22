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
package greycatTest.internal;

import greycat.Constants;
import greycat.Graph;
import greycat.internal.CoreConstants;
import greycat.plugin.Storage;
import greycat.struct.Buffer;
import greycat.struct.BufferIterator;
import greycat.utility.Base64;
import greycat.Callback;
import greycat.utility.HashHelper;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockStorage implements Storage {

    private Graph _graph;
    private short prefix = 0;
    public final Map<String, byte[]> backend = new HashMap<String, byte[]>();
    private final List<Callback<Buffer>> updates = new ArrayList<Callback<Buffer>>();

    /**
     * @native ts
     * return p.toString();
     */
    private String keyToString(byte[] p) {
        return new String(p);
    }

    @Override
    public final void get(Buffer keys, Callback<Buffer> callback) {
        final Buffer result = _graph.newBuffer();
        final BufferIterator it = keys.iterator();
        boolean isFirst = true;
        while (it.hasNext()) {
            //do nothing with the view, redirect to BlackHole...
            byte[] key = it.next().data();
            final byte[] resolved = backend.get(keyToString(key));
            if (isFirst) {
                isFirst = false;
                if (resolved != null) {
                    result.writeAll(resolved);
                }
            } else {
                result.write(CoreConstants.BUFFER_SEP);
                if (resolved != null) {
                    result.writeAll(resolved);
                }
            }
        }
        callback.on(result);
    }

    @Override
    public final void put(Buffer stream, Callback<Boolean> callback) {
        if (callback != null) {
            Buffer result = null;
            if (updates.size() != 0) {
                result = _graph.newBuffer();
            }
            BufferIterator it = stream.iterator();
            boolean isFirst = true;
            while (it.hasNext()) {
                Buffer keyView = it.next();
                byte[] keyData = keyView.data();
                Buffer valueView = it.next();
                byte[] valueData = valueView.data();
                if (result != null) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        result.write(Constants.KEY_SEP);
                    }
                    result.writeAll(keyView.data());
                    result.write(Constants.KEY_SEP);
                    Base64.encodeLongToBuffer(HashHelper.hashBuffer(valueView, 0, valueView.length()), result);
                }
                backend.put(keyToString(keyData), valueData);
            }
            for (int i = 0; i < updates.size(); i++) {
                final Callback<Buffer> explicit = updates.get(i);
                explicit.on(result);
            }
            callback.on(true);
        }
    }

    @Override
    public final void putSilent(Buffer stream, Callback<Buffer> callback) {
        if (callback != null) {
            Buffer result = _graph.newBuffer();
            BufferIterator it = stream.iterator();
            boolean isFirst = true;
            while (it.hasNext()) {
                Buffer keyView = it.next();
                byte[] keyData = keyView.data();
                Buffer valueView = it.next();
                byte[] valueData = valueView.data();
                if (result != null) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        result.write(Constants.KEY_SEP);
                    }
                    result.writeAll(keyView.data());
                    result.write(Constants.KEY_SEP);
                    Base64.encodeLongToBuffer(HashHelper.hashBuffer(valueView, 0, valueView.length()), result);
                }
                backend.put(keyToString(keyData), valueData);
            }
            callback.on(result);
        }
    }

    @Override
    public final void remove(Buffer keys, Callback<Boolean> callback) {
        final BufferIterator it = keys.iterator();
        while (it.hasNext()) {
            //do nothing with the view, redirect to BlackHole...
            byte[] key = it.next().data();
            backend.remove(keyToString(key));
        }
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
        updates.add(synCallback);
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