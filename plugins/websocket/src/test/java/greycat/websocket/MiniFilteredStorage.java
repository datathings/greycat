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
package greycat.websocket;

import greycat.Callback;
import greycat.Constants;
import greycat.Graph;
import greycat.internal.heap.HeapBuffer;
import greycat.plugin.Storage;
import greycat.struct.Buffer;
import greycat.utility.Base64;

public class MiniFilteredStorage implements Storage {

    private final Storage backend;
    private final int[] negFilters;

    MiniFilteredStorage(final Storage proxy, int[] negFilters) {
        this.backend = proxy;
        this.negFilters = negFilters;
    }

    @Override
    public final void get(Buffer keys, Callback<Buffer> callback) {
        backend.get(keys, new Callback<Buffer>() {
            @Override
            public void on(final Buffer proxyResult) {
                final Buffer filtered = filter_get(proxyResult);
                proxyResult.free();
                callback.on(filtered);
            }
        });
    }

    @Override
    public final void put(Buffer stream, Callback<Boolean> callback) {
        final Buffer filtered = filter_put(stream);
        backend.put(filtered, new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                filtered.free();
                callback.on(result);
            }
        });
    }

    @Override
    public final void putSilent(Buffer stream, Callback<Buffer> callback) {
        final Buffer filtered = filter_put(stream);
        backend.putSilent(filtered, new Callback<Buffer>() {
            @Override
            public void on(Buffer result) {
                filtered.free();
                callback.on(result);
            }
        });
    }

    @Override
    public final void remove(Buffer keys, Callback<Boolean> callback) {
        backend.remove(keys, callback);
    }

    @Override
    public final void connect(Graph graph, Callback<Boolean> callback) {
        backend.connect(graph, callback);
    }

    @Override
    public final void lock(Callback<Buffer> callback) {
        backend.lock(callback);
    }

    @Override
    public final void unlock(Buffer previousLock, Callback<Boolean> callback) {
        backend.unlock(previousLock, callback);
    }

    @Override
    public final void disconnect(Callback<Boolean> callback) {
        backend.disconnect(callback);
    }

    @Override
    public final void listen(Callback<Buffer> synCallback) {
        backend.listen(synCallback);
    }


    private Buffer filter_get(Buffer in) {
        final Buffer result = new HeapBuffer();
        long max = in.length();
        long cursor = 0;
        int group = 0;
        long previous = 0;
        while (cursor < max) {
            byte elem = in.read(cursor);
            switch (elem) {
                case Constants.CHUNK_META_SEP:
                    group = Base64.decodeToIntWithBounds(in, previous, cursor);
                    break;
                case Constants.BUFFER_SEP:
                    if (filterChunk(group)) {
                        result.writeAll(in.slice(previous, cursor));
                    } else {
                        result.write(Constants.BUFFER_SEP);
                    }
                    group = 0;
                    previous = cursor + 1;
                    break;
            }
            cursor++;
        }
        if (filterChunk(group)) {
            result.writeAll(in.slice(previous, cursor - 1));
        } else {
            result.write(Constants.BUFFER_SEP);
        }
        return result;
    }

    private Buffer filter_put(Buffer in) {
        final Buffer result = new HeapBuffer();
        long max = in.length();
        long cursor = 0;
        int group = 0;
        long previous = 0;

        long previous_key = -1;

        while (cursor < max) {
            byte elem = in.read(cursor);
            switch (elem) {
                case Constants.CHUNK_META_SEP:
                    group = Base64.decodeToIntWithBounds(in, previous, cursor);
                    break;
                case Constants.BUFFER_SEP:
                    if (previous_key == -1) {
                        previous_key = previous;
                    } else {

                        if (filterChunk(group)) {
/*
                            HeapBuffer kbuf = new HeapBuffer();
                            kbuf.writeAll(in.slice(previous_key, previous-2));
                            ChunkKey key = ChunkKey.build(kbuf);
                            System.out.println("(" + group + ")" + key.type + "/" + key.world + "/" + key.time + "/" + key.id);
*/
                            result.writeAll(in.slice(previous_key, cursor));
                        }
                        previous_key = -1;
                    }
                    group = 0;
                    previous = cursor + 1;
                    break;
            }
            cursor++;
        }

        if (previous_key != -1 && filterChunk(group)) {
/*
            HeapBuffer kbuf = new HeapBuffer();
            kbuf.writeAll(in.slice(previous_key, previous-2));
            ChunkKey key = ChunkKey.build(kbuf);
            System.out.println("(" + group + ")" + key.type + "/" + key.world + "/" + key.time + "/" + key.id);
*/
            result.writeAll(in.slice(previous_key, cursor - 1));
        }
        return result;
    }

    private boolean filterChunk(int group) {
        for (int i = 0; i < this.negFilters.length; i++) {
            if (group == this.negFilters[i]) {
                return false;
            }
        }
        return true;
    }

}
