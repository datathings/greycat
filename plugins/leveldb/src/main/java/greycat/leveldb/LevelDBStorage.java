/**
 * Copyright 2017-2018 The GreyCat Authors.  All rights reserved.
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
package greycat.leveldb;

import greycat.Callback;
import greycat.Constants;
import greycat.Graph;
import greycat.plugin.Storage;
import greycat.struct.Buffer;
import greycat.struct.BufferIterator;
import greycat.utility.Base64;
import greycat.utility.HashHelper;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LevelDBStorage implements Storage {

    private static final String _connectedError = "PLEASE CONNECT YOUR DATABASE FIRST";
    private static final byte[] prefixKey = "prefix".getBytes();
    private final String storagePath;

    private final List<Callback<Buffer>> updates = new ArrayList<Callback<Buffer>>();

    private DB db;
    private boolean isConnected;
    private Graph graph;
    private boolean useNative = true;

    public LevelDBStorage(String storagePath) {
        this.isConnected = false;
        this.storagePath = storagePath;
    }

    public LevelDBStorage useNative(boolean p_useNative) {
        this.useNative = p_useNative;
        return this;
    }

    @Override
    public void get(Buffer keys, Callback<Buffer> callback) {
        if (!isConnected) {
            throw new RuntimeException(_connectedError);
        }
        Buffer result = graph.newBuffer();
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
                byte[] res = db.get(view.data());
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
    public void put(Buffer stream, Callback<Boolean> callback) {
        if (!isConnected) {
            throw new RuntimeException(_connectedError);
        }
        try {
            Buffer result = null;
            if (updates.size() != 0) {
                result = graph.newBuffer();
            }
            WriteBatch batch = db.createWriteBatch();
            BufferIterator it = stream.iterator();
            boolean isFirst = true;
            while (it.hasNext()) {
                Buffer keyView = it.next();
                Buffer valueView = it.next();
                if (valueView != null) {
                    batch.put(keyView.data(), valueView.data());
                }
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
            }
            db.write(batch);
            batch.close();
            for (int i = 0; i < updates.size(); i++) {
                final Callback<Buffer> explicit = updates.get(i);
                explicit.on(result);
            }
            if (callback != null) {
                callback.on(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) {
                callback.on(false);
            }
        }
    }

    @Override
    public final void putSilent(Buffer stream, Callback<Buffer> callback) {
        if (!isConnected) {
            throw new RuntimeException(_connectedError);
        }
        try {
            Buffer result = graph.newBuffer();
            WriteBatch batch = db.createWriteBatch();
            BufferIterator it = stream.iterator();
            boolean isFirst = true;
            while (it.hasNext()) {
                Buffer keyView = it.next();
                Buffer valueView = it.next();
                if (valueView != null) {
                    batch.put(keyView.data(), valueView.data());
                }
                if (isFirst) {
                    isFirst = false;
                } else {
                    result.write(Constants.KEY_SEP);
                }
                result.writeAll(keyView.data());
                result.write(Constants.KEY_SEP);
                Base64.encodeLongToBuffer(HashHelper.hashBuffer(valueView, 0, valueView.length()), result);
            }
            db.write(batch);
            for (int i = 0; i < updates.size(); i++) {
                final Callback<Buffer> explicit = updates.get(i);
                explicit.on(result);
            }
            callback.on(result);
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) {
                callback.on(null);
            }
        }
    }


    @Override
    public void remove(Buffer keys, Callback<Boolean> callback) {
        if (!isConnected) {
            throw new RuntimeException(_connectedError);
        }
        try {
            BufferIterator it = keys.iterator();
            while (it.hasNext()) {
                Buffer view = it.next();
                db.delete(view.data());
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
    public void disconnect(Callback<Boolean> callback) {
        try {
            db.close();
            db = null;
            isConnected = false;
            if (callback != null) {
                callback.on(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) {
                callback.on(false);
            }
        }
    }

    @Override
    public void listen(Callback<Buffer> synCallback) {
        updates.add(synCallback);
    }

    @Override
    public void connect(Graph graph, Callback<Boolean> callback) {
        if (isConnected) {
            if (callback != null) {
                callback.on(null);
            }
            return;
        }
        this.graph = graph;
        //by default activate snappy compression of bytes
        Options options = new Options()
                .createIfMissing(true)
                .compressionType(CompressionType.SNAPPY);
        File location = new File(storagePath);
        if (!location.exists()) {
            location.mkdirs();
        }
        File targetDB = new File(location, "data");
        targetDB.mkdirs();
        try {
            if (useNative) {
                db = JniDBFactory.factory.open(targetDB, options);
            } else {
                db = Iq80DBFactory.factory.open(targetDB, options);
            }
            isConnected = true;
            if (callback != null) {
                callback.on(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) {
                callback.on(null);
            }
        }
    }

    @Override
    public void lock(Callback<Buffer> callback) {
        if (!isConnected) {
            throw new RuntimeException(_connectedError);
        }
        byte[] current = db.get(prefixKey);
        if (current == null) {
            current = new String("0").getBytes();
        }
        Short currentPrefix = Short.parseShort(new String(current));
        db.put(prefixKey, ((currentPrefix + 1) + "").getBytes());
        if (callback != null) {
            Buffer newBuf = graph.newBuffer();
            Base64.encodeIntToBuffer(currentPrefix, newBuf);
            callback.on(newBuf);
        }
    }

    @Override
    public void unlock(Buffer previousLock, Callback<Boolean> callback) {
        if (!isConnected) {
            throw new RuntimeException(_connectedError);
        }
        callback.on(true);
    }
}
