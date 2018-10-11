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
package greycat.internal.heap;

import greycat.Constants;
import greycat.NodeListener;
import greycat.chunk.Chunk;
import greycat.chunk.ChunkType;
import greycat.struct.Buffer;
import greycat.struct.LongLongMap;
import greycat.struct.LongLongMapCallBack;
import greycat.utility.Base64;
import greycat.utility.HashHelper;
import greycat.utility.Listeners;
import greycat.utility.Unsafe;
import greycat.chunk.WorldOrderChunk;
import greycat.internal.CoreConstants;

import java.util.Arrays;

final class HeapWorldOrderChunk implements WorldOrderChunk {

    /**
     * @ignore ts
     */
    private static final sun.misc.Unsafe unsafe = Unsafe.getUnsafe();

    private final HeapChunkSpace _space;
    private final long _index;

    private volatile int _lock;
    private volatile int _externalLock;

    private volatile long _magic;
    private volatile long _type;

    private volatile int _size;
    private int _capacity;
    private long[] _kv;
    private boolean[] _diff;

    private int[] _next;
    private int[] _hash;
    private int _group;

    private long _chunkHash;
    private boolean _inSync;

    private Listeners _listeners = null;

    /**
     * @ignore ts
     */
    private static final long _lockOffset;
    /**
     * @ignore ts
     */
    private static final long _externalLockOffset;

    /** @ignore ts */
    static {
        try {
            _lockOffset = unsafe.objectFieldOffset(HeapWorldOrderChunk.class.getDeclaredField("_lock"));
            _externalLockOffset = unsafe.objectFieldOffset(HeapWorldOrderChunk.class.getDeclaredField("_externalLock"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    @Override
    public synchronized final boolean inSync() {
        return _inSync;
    }

    @Override
    public synchronized final boolean sync(long remoteHash) {
        if (_inSync && remoteHash != _chunkHash) {
            _inSync = false;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public final int group() {
        return _group;
    }

    @Override
    public final Chunk setGroup(final int g) {
        _group = g;
        return this;
    }

    HeapWorldOrderChunk(final HeapChunkSpace p_space, final long p_index) {
        _index = p_index;
        _space = p_space;
        _lock = 0;
        _magic = 0;
        _type = CoreConstants.NULL_LONG;
        _size = 0;
        _capacity = 0;
        _kv = null;
        _next = null;
        _diff = null;
        _hash = null;
        _chunkHash = 0;
        _inSync = true;
        _group = 0;
    }

    @Override
    public final long world() {
        return _space.worldByIndex(_index);
    }

    @Override
    public final long time() {
        return _space.timeByIndex(_index);
    }

    @Override
    public final long id() {
        return _space.idByIndex(_index);
    }

    @Override
    public final long type() {
        return this._type;
    }

    @Override
    public final void setType(final long v) {
        this._type = v;
    }

    @Override
    public synchronized int listen(NodeListener listener) {
        if (_listeners == null) {
            _listeners = new Listeners();
        }
        return _listeners.listen(listener);
    }

    @Override
    public void unlisten(int registrationID) {
        if (_listeners != null) {
            _listeners.unlisten(registrationID);
        }
    }

    @Override
    public final Listeners listeners() {
        return _listeners;
    }

    /**
     * @native ts
     */
    @Override
    public final void lock() {
        while (!unsafe.compareAndSwapInt(this, _lockOffset, 0, 1)) {
        }
    }

    /**
     * @native ts
     */
    @Override
    public final void unlock() {
        if (!unsafe.compareAndSwapInt(this, _lockOffset, 1, 0)) {
            throw new RuntimeException("CAS Error !!!");
        }
    }

    /**
     * @native ts
     */
    @Override
    public final void externalLock() {
        while (!unsafe.compareAndSwapInt(this, _externalLockOffset, 0, 1)) {

        }
    }

    /**
     * @native ts
     */
    @Override
    public final void externalUnlock() {
        if (!unsafe.compareAndSwapInt(this, _externalLockOffset, 1, 0)) {
            throw new RuntimeException("CAS Error !!!");
        }
    }


    @Override
    public final long magic() {
        return this._magic;
    }

    @Override
    public synchronized final void each(final LongLongMapCallBack callback) {
        for (int i = 0; i < _size; i++) {
            callback.on(_kv[i * 2], _kv[i * 2 + 1]);
        }
    }

    @Override
    public synchronized final long get(final long key) {
        if (_size > 0) {
            final int index = (int) HashHelper.longHash(key, _capacity * 2);
            int m = _hash[index];
            while (m >= 0) {
                if (key == _kv[m * 2]) {
                    return _kv[(m * 2) + 1];
                } else {
                    m = _next[m];
                }
            }
        }
        return CoreConstants.NULL_LONG;
    }

    @Override
    public synchronized final LongLongMap put(final long key, final long value) {
        internal_put(key, value, true);
        return this;
    }

    private void internal_put(final long key, final long value, final boolean notifyUpdate) {
        if (_capacity > 0) {
            //we need to init
            int hashIndex = (int) HashHelper.longHash(key, _capacity * 2);
            int m = _hash[hashIndex];
            int found = -1;
            while (m >= 0) {
                if (key == _kv[m * 2]) {
                    found = m;
                    break;
                }
                m = _next[m];
            }
            if (found == -1) {
                if (_capacity == _size) {
                    resize(_capacity * 2);
                    hashIndex = (int) HashHelper.longHash(key, _capacity * 2);
                }
                _kv[_size * 2] = key;
                _kv[_size * 2 + 1] = value;
                if (notifyUpdate) {
                    _diff[_size] = true;
                }
                _next[_size] = _hash[hashIndex];
                _hash[hashIndex] = _size;
                _size++;
                _magic = _magic + 1;
                if (notifyUpdate && _chunkHash != Constants.EMPTY_HASH) {
                    _chunkHash = Constants.EMPTY_HASH;
                    if (_space != null) {
                        _space.notifyUpdate(_index);
                    }
                }
            } else {
                if (_kv[found * 2 + 1] != value) {
                    _kv[found * 2 + 1] = value;
                    if (notifyUpdate) {
                        _diff[found] = true;
                    }
                    _magic = _magic + 1;
                    if (notifyUpdate && _chunkHash != Constants.EMPTY_HASH) {
                        _chunkHash = Constants.EMPTY_HASH;
                        if (_space != null) {
                            _space.notifyUpdate(_index);
                        }
                    }
                }
            }
        } else {
            _capacity = Constants.MAP_INITIAL_CAPACITY;
            _next = new int[_capacity];
            Arrays.fill(_next, 0, _capacity, -1);
            _diff = new boolean[_capacity];
            CoreConstants.fillBooleanArray(_diff, false);
            _hash = new int[_capacity * 2];
            Arrays.fill(_hash, 0, _capacity * 2, -1);
            _kv = new long[_capacity * 2];
            _size = 1;
            _kv[0] = key;
            _kv[1] = value;
            if (notifyUpdate) {
                _diff[0] = true;
            }
            _hash[(int) HashHelper.longHash(key, _capacity * 2)] = 0;
            if (notifyUpdate && _chunkHash != Constants.EMPTY_HASH) {
                _chunkHash = Constants.EMPTY_HASH;
                if (_space != null) {
                    _space.notifyUpdate(_index);
                }
            }
        }
    }

    private boolean resize(final int newCapacity) {
        if (newCapacity > _capacity) {
            if (_kv == null) {
                _kv = new long[newCapacity * 2];
                _hash = new int[newCapacity * 2];
                _next = new int[newCapacity];
                _diff = new boolean[newCapacity];
                _capacity = newCapacity;
                Arrays.fill(_next, 0, newCapacity, -1);
                CoreConstants.fillBooleanArray(_diff, false);
                Arrays.fill(_hash, 0, newCapacity * 2, -1);
                return true;
            } else {
                final long[] temp_kv = new long[newCapacity * 2];
                System.arraycopy(_kv, 0, temp_kv, 0, _size * 2);
                final boolean[] temp_diff = new boolean[newCapacity];
                CoreConstants.fillBooleanArray(temp_diff, false);
                System.arraycopy(_diff, 0, temp_diff, 0, _size);
                final int[] temp_next = new int[newCapacity];
                final int[] temp_hash = new int[newCapacity * 2];
                Arrays.fill(temp_next, 0, newCapacity, -1);
                Arrays.fill(temp_hash, 0, newCapacity * 2, -1);
                for (int i = 0; i < _size; i++) {
                    int loopIndex = (int) HashHelper.longHash(temp_kv[i * 2], newCapacity * 2);
                    temp_next[i] = temp_hash[loopIndex];
                    temp_hash[loopIndex] = i;
                }
                _capacity = newCapacity;
                _hash = temp_hash;
                _next = temp_next;
                _kv = temp_kv;
                _diff = temp_diff;
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public synchronized final void load(final Buffer buffer) {
        internal_load(true, buffer);
    }

    @Override
    public synchronized void loadDiff(Buffer buffer) {
        internal_load(false, buffer);
        _chunkHash = Constants.EMPTY_HASH;
        if (_space != null) {
            _space.notifyUpdate(_index);
        }
    }

    @Override
    public final long hash() {
        return _chunkHash;
    }

    private void internal_load(final boolean initial, final Buffer buffer) {
        if (buffer != null && buffer.length() > 0) {
            long cursor = 0;
            long bufferSize = buffer.length();
            long previousStart = 0;
            long loopKey = CoreConstants.NULL_LONG;
            int extraCursor = 0;
            while (cursor < bufferSize) {
                final byte current = buffer.read(cursor);
                switch (current) {
                    case Constants.CHUNK_META_SEP:
                        if (previousStart != cursor) {
                            _group = Base64.decodeToIntWithBounds(buffer, previousStart, cursor);
                        }
                        previousStart = cursor + 1;
                        break;
                    case Constants.CHUNK_SEP:
                        switch (extraCursor) {
                            case 0:
                                final int mapSize = Base64.decodeToIntWithBounds(buffer, previousStart, cursor);
                                final int closePowerOfTwo = (int) Math.pow(2, Math.ceil(Math.log(mapSize) / Math.log(2)));
                                resize(closePowerOfTwo);
                                break;
                            case 1:
                                if (previousStart != cursor) {
                                    _type = Base64.decodeToLongWithBounds(buffer, previousStart, cursor);
                                }
                                break;
                        }
                        extraCursor++;
                        previousStart = cursor + 1;
                        break;
                    case Constants.CHUNK_VAL_SEP:
                        if (loopKey == CoreConstants.NULL_LONG) {
                            loopKey = Base64.decodeToLongWithBounds(buffer, previousStart, cursor);
                        } else {
                            long loopValue = Base64.decodeToLongWithBounds(buffer, previousStart, cursor);
                            internal_put(loopKey, loopValue, !initial);
                            //reset key for next round
                            loopKey = CoreConstants.NULL_LONG;
                        }
                        previousStart = cursor + 1;
                        break;
                }
                cursor++;
            }
        }
    }

    @Override
    public final long index() {
        return _index;
    }

    @Override
    public final synchronized void remove(long key) {
        throw new RuntimeException("Not implemented yet!!!");
    }

    @Override
    public final int size() {
        return _size;
    }

    @Override
    public final byte chunkType() {
        return ChunkType.WORLD_ORDER_CHUNK;
    }

    @Override
    public final synchronized void save(final Buffer buffer) {
        final long beginIndex = buffer.writeIndex();
        if (_group != 0) {
            Base64.encodeIntToBuffer(_group, buffer);
            buffer.write(CoreConstants.CHUNK_META_SEP);
        }
        Base64.encodeIntToBuffer(_size, buffer);
        buffer.write(CoreConstants.CHUNK_SEP);
        if (_type != Constants.NULL_LONG) {
            Base64.encodeLongToBuffer(_type, buffer);
        }
        buffer.write(CoreConstants.CHUNK_SEP);
        for (int i = 0; i < _size; i++) {
            Base64.encodeLongToBuffer(_kv[i * 2], buffer);
            buffer.write(CoreConstants.CHUNK_VAL_SEP);
            Base64.encodeLongToBuffer(_kv[i * 2 + 1], buffer);
            buffer.write(CoreConstants.CHUNK_VAL_SEP);
        }
        _chunkHash = HashHelper.hashBuffer(buffer, beginIndex, buffer.writeIndex());
    }

    @Override
    public final synchronized void saveDiff(Buffer buffer) {
        throw new RuntimeException("Not implemented yet!");
        /*
        if (_chunkHash == Constants.EMPTY_HASH) {
            final long beginIndex = buffer.writeIndex();
            Base64.encodeIntToBuffer(_size, buffer);
            buffer.write(CoreConstants.CHUNK_SEP);
            Base64.encodeLongToBuffer(_offset, buffer);
            buffer.write(CoreConstants.CHUNK_SEP);
            Base64.encodeLongToBuffer(_type, buffer);
            buffer.write(CoreConstants.CHUNK_SEP);
            Base64.encodeIntToBuffer(_size, buffer);
            for (int i = 0; i < _size; i++) {
                if (_diff[i]) {
                    buffer.write(CoreConstants.CHUNK_VAL_SEP);
                    Base64.encodeLongToBuffer(_kv[i * 2], buffer);
                    buffer.write(CoreConstants.CHUNK_VAL_SEP);
                    Base64.encodeLongToBuffer(_kv[i * 2 + 1], buffer);
                }
            }
            _chunkHash = HashHelper.hashBuffer(buffer, beginIndex, buffer.writeIndex());
        }
        */
    }

}



