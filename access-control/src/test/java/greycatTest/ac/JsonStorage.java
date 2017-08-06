/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
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
package greycatTest.ac;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;
import greycat.Callback;
import greycat.Constants;
import greycat.Graph;
import greycat.internal.CoreConstants;
import greycat.plugin.Storage;
import greycat.struct.Buffer;
import greycat.struct.BufferIterator;
import greycat.utility.Base64;
import greycat.utility.HashHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class JsonStorage implements Storage {

    private Graph _graph;
    private short prefix = 0;
    private JsonObject backend;
    private final List<Callback<Buffer>> updates = new ArrayList<Callback<Buffer>>();
    private String _fileLocation;

    public JsonStorage(String location) {
        this._fileLocation = location;
    }


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
            JsonValue resolved = backend.get(keyToString(key));
            if (isFirst) {
                isFirst = false;
                if (resolved != null) {
                    result.writeAll(resolved.asString().getBytes());
                }
            } else {
                result.write(CoreConstants.BUFFER_SEP);
                if (resolved != null) {
                    result.writeAll(resolved.asString().getBytes());
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
                backend.set(keyToString(keyData), keyToString(valueData));
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
                backend.set(keyToString(keyData), keyToString(valueData));
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
        File f = new File(_fileLocation);
        if(f.exists()) {
            try {
                backend = Json.parse(new FileReader(f)).asObject();
            } catch (IOException e) {
                e.printStackTrace();
                backend = new JsonObject();
            }
        } else {
            backend = new JsonObject();
        }
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
        System.out.println("JsonStorage disconnection");
        _graph = null;
        try {
            File f = new File(this._fileLocation);
            System.out.println("Storage: " + f.getAbsolutePath());
            FileWriter fr = new FileWriter(f);
            backend.writeTo(fr, WriterConfig.PRETTY_PRINT);
            fr.flush();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        callback.on(true);
    }

    @Override
    public void listen(Callback<Buffer> synCallback) {
        updates.add(synCallback);
    }

}