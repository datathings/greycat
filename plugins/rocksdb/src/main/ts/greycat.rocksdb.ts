///
/// Copyright 2017 The GreyCat Authors.  All rights reserved.
/// <p>
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
/// <p>
/// http://www.apache.org/licenses/LICENSE-2.0
/// <p>
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///


import * as greycat from "@greycat/greycat";
import { Graph, Callback, Constants } from "@greycat/greycat";

const rocksdb = require('rocksdb-node')

export class RocksStorage implements greycat.plugin.Storage {
    db: any;
    graph: Graph = null;
    RocksStorage(path: string) {
        this.db = rocksdb.open({ create_if_missing: true }, path);
    }
    get(keys: greycat.struct.Buffer, callback: Callback<greycat.struct.Buffer>): void {
        let result: greycat.struct.Buffer = this.graph.newBuffer();
        let it: greycat.struct.BufferIterator = keys.iterator();
        var isFirst = true;
        while (it.hasNext()) {
            let view: greycat.struct.Buffer = it.next();
            if (!isFirst) {
                result.write(Constants.BUFFER_SEP);
            } else {
                isFirst = false;
            }
            var res = this.db.get(view.data());
            if (res != null) {
                result.writeAll(res);
            }
        }
        if (callback != null) {
            callback(result);
        }
    }
    put(stream: greycat.struct.Buffer, callback: greycat.Callback<boolean>): void {
        if (!_isConnected) {
            throw new RuntimeException(_connectedError);
        }
        Buffer result = null;
        if (updates.size() != 0) {
            result = _graph.newBuffer();
        }
        WriteBatch batch = new WriteBatch();
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
        WriteOptions options = new WriteOptions();
        options.setSync(false);
        try {
            _db.write(options, batch);
            for (int i = 0; i < updates.size(); i++) {
                final Callback<Buffer> explicit = updates.get(i);
                explicit.on(result);
            }
            if (p_callback != null) {
                p_callback.on(true);
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
            if (p_callback != null) {
                p_callback.on(false);
            }
        }
    }
    putSilent(stream: greycat.struct.Buffer, callback: greycat.Callback<greycat.struct.Buffer>): void {
                if (!_isConnected) {
            throw new RuntimeException(_connectedError);
        }
        Buffer result = _graph.newBuffer();
        WriteBatch batch = new WriteBatch();
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
        WriteOptions options = new WriteOptions();
        options.setSync(false);
        try {
            _db.write(options, batch);
            for (int i = 0; i < updates.size(); i++) {
                final Callback<Buffer> explicit = updates.get(i);
                explicit.on(result);
            }
            callback.on(result);
        } catch (RocksDBException e) {
            e.printStackTrace();
            callback.on(null);
        }
    }
    remove(keys: greycat.struct.Buffer, callback: greycat.Callback<boolean>): void {
        if (!_isConnected) {
            throw new RuntimeException(_connectedError);
        }
        try {
            BufferIterator it = keys.iterator();
            while (it.hasNext()) {
                Buffer view = it.next();
                _db.delete(view.data());
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
    connect(graph: greycat.Graph, callback: greycat.Callback<boolean>): void {
        if (_isConnected) {
            if (callback != null) {
                callback.on(true);
            }
            return;
        }
        _graph = graph;
        //by default activate snappy compression of bytes
        _options = new Options()
                .setCreateIfMissing(true)
                .setCompressionType(CompressionType.SNAPPY_COMPRESSION);
        File location = new File(_storagePath);
        if (!location.exists()) {
            location.mkdirs();
        }
        File targetDB = new File(location, "data");
        targetDB.mkdirs();
        try {
            _db = RocksDB.open(_options, targetDB.getAbsolutePath());
            _isConnected = true;
            if (callback != null) {
                callback.on(true);
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
            if (callback != null) {
                callback.on(false);
            }
        }
    }
    lock(callback: greycat.Callback<greycat.struct.Buffer>): void {
         try {
            byte[] current = _db.get(prefixKey);
            if (current == null) {
                current = new String("0").getBytes();
            }
            Short currentPrefix = Short.parseShort(new String(current));
            _db.put(prefixKey, ((currentPrefix + 1) + "").getBytes());
            if (callback != null) {
                Buffer newBuf = _graph.newBuffer();
                Base64.encodeIntToBuffer(currentPrefix, newBuf);
                callback.on(newBuf);
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
            if (callback != null) {
                callback.on(null);
            }
        }
    }

    private static final prefixKey = "prefix".getBytes();

    unlock(previousLock: greycat.struct.Buffer, callback: greycat.Callback<boolean>): void {
        callback.on(true);
    }
    disconnect(callback: greycat.Callback<boolean>): void {
       //TODO write the prefix
        try {
            WriteOptions options = new WriteOptions();
            options.sync();
            _db.write(options, new WriteBatch());
            _db.close();
            _options.close();
            _options = null;
            _db = null;
            _isConnected = false;
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
    listen(synCallback: greycat.Callback<greycat.struct.Buffer>): void {
        throw new Error("Method not implemented.");
    }
}

