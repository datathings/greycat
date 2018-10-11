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
import greycat.chunk.Chunk;
import greycat.chunk.ChunkType;
import greycat.internal.CoreConstants;
import greycat.struct.Buffer;
import greycat.utility.Base64;
import greycat.chunk.GenChunk;
import greycat.utility.HashHelper;

final class HeapGenChunk implements GenChunk {

    private final HeapChunkSpace _space;
    private final long _index;

    /**
     * {@native ts
     * private _prefix: Long;
     * }
     */
    private final long _prefix;
    private long _seed;

    private long _hash;
    private boolean _inSync;
    private int _group;

    /**
     * {@native ts
     * this._index = p_index;
     * this._space = p_space;
     * this._prefix = Long.fromNumber(p_id).shiftLeft((Constants.LONG_SIZE - Constants.PREFIX_SIZE));
     * this._seed = -1;
     * this._group = 0;
     * }
     */
    HeapGenChunk(final HeapChunkSpace p_space, final long p_id, final long p_index) {
        _index = p_index;
        _space = p_space;
        //moves the prefix 53-size(short) times to the left;
        _prefix = p_id << (Constants.LONG_SIZE - Constants.PREFIX_SIZE);
        _seed = -1;
        _hash = 0;
        _inSync = true;
    }

    @Override
    public synchronized final void save(final Buffer buffer) {
        final long beginIndex = buffer.writeIndex();
        if (_group != 0) {
            Base64.encodeIntToBuffer(_group, buffer);
            buffer.write(CoreConstants.CHUNK_META_SEP);
        }
        Base64.encodeLongToBuffer(_seed, buffer);
        _hash = HashHelper.hashBuffer(buffer, beginIndex, buffer.writeIndex());
    }

    @Override
    public synchronized void saveDiff(Buffer buffer) {
        final long beginIndex = buffer.writeIndex();
        Base64.encodeLongToBuffer(_seed, buffer);
        _hash = HashHelper.hashBuffer(buffer, beginIndex, buffer.writeIndex());
    }

    @Override
    public synchronized final void load(final Buffer buffer) {
        internal_load(buffer, false);
    }

    @Override
    public synchronized void loadDiff(final Buffer buffer) {
        internal_load(buffer, true);
    }

    @Override
    public final long hash() {
        return _hash;
    }

    @Override
    public synchronized final boolean inSync() {
        return _inSync;
    }

    @Override
    public synchronized final boolean sync(long remoteHash) {
        if (_inSync && remoteHash != _hash) {
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
    public final Chunk setGroup(int g) {
        _group = g;
        return this;
    }

    private void internal_load(Buffer buffer, boolean diff) {
        if (buffer == null || buffer.length() == 0) {
            return;
        }
        long loaded = Base64.decodeToLongWithBounds(buffer, 0, buffer.length());
        long previousSeed = _seed;
        _seed = loaded;
        if (previousSeed != -1 && previousSeed != _seed) {
            if (_space != null && _hash != Constants.EMPTY_HASH) {
                _hash = Constants.EMPTY_HASH;
                _space.notifyUpdate(_index);
            }
        }
    }

    /**
     * {@native ts
     * if (this._seed == Constants.KEY_PREFIX_MASK) {
     * throw new Error("Object Index could not be created because it exceeded the capacity of the current prefix. Ask for a new prefix.");
     * }
     * if(this._seed == -1){
     * this._seed = 0;
     * }
     * this._seed++;
     * var nextIndex = this._seed;
     * if(this._space){
     * this._space.notifyUpdate(this._index);
     * }
     * var objectKey = this._prefix.add(this._seed).toNumber();
     * if (objectKey >= Constants.NULL_LONG) {
     * throw new Error("Object Index exceeds the maximum JavaScript number capacity. (2^"+Constants.LONG_SIZE+")");
     * }
     * return objectKey;
     * }
     * }
     */
    @Override
    public synchronized final long newKey() {
        if (_seed == Constants.KEY_PREFIX_MASK) {
            throw new IndexOutOfBoundsException("Object Index could not be created because it exceeded the capacity of the current prefix. Ask for a new prefix.");
        }
        if (_seed == -1) {
            _seed = 0;
        }
        _seed++;
        final long nextIndex = _seed;
        long objectKey = _prefix + nextIndex;
        if (_space != null) {
            _space.notifyUpdate(_index);
        }
        if (objectKey >= Constants.END_OF_TIME) {
            throw new IndexOutOfBoundsException("Object Index exceeds the maximum JavaScript number capacity. (2^" + Constants.LONG_SIZE + ")");
        }
        return objectKey;
    }

    @Override
    public final long index() {
        return _index;
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
    public final byte chunkType() {
        return ChunkType.GEN_CHUNK;
    }

}
