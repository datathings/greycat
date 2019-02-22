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
package greycat.memory;

import greycat.Constants;
import greycat.chunk.ChunkType;
import greycat.chunk.GenChunk;
import greycat.memory.primary.POffHeapLongArray;
import greycat.struct.Buffer;
import greycat.utility.Base64;

final class OffHeapGenChunk implements GenChunk {

    private final OffHeapChunkSpace space;
    private final long index;
    private final long prefix;
    private final long addr;

    private static final int DIRTY = 0;
    private static final int SEED = 1;

    private static final int CHUNK_SIZE = 2;

    OffHeapGenChunk(final OffHeapChunkSpace p_space, final long p_id, final long p_index) {
        index = p_index;
        space = p_space;
        //moves the prefix 53-size(short) times to the left;
        prefix = p_id << (Constants.LONG_SIZE - Constants.PREFIX_SIZE);
        space.lockByIndex(index);
        try {
            long temp_addr = space.addrByIndex(index);
            if (temp_addr == OffHeapConstants.NULL_PTR) {
                temp_addr = POffHeapLongArray.allocate(CHUNK_SIZE);
                space.setAddrByIndex(index, temp_addr);
                POffHeapLongArray.set(temp_addr, SEED, -1);
                POffHeapLongArray.set(temp_addr, DIRTY, 0);
            }
            addr = temp_addr;
        } finally {
            space.unlockByIndex(index);
        }
    }

    static void free(final long addr) {
        if (addr != OffHeapConstants.NULL_PTR) {
            POffHeapLongArray.free(addr);
        }
    }

    @Override
    public final void save(final Buffer buffer) {
        space.lockByIndex(index);
        try {
            Base64.encodeLongToBuffer(POffHeapLongArray.get(addr, SEED), buffer);
            POffHeapLongArray.set(addr, DIRTY, 0);
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public void saveDiff(Buffer buffer) {
        save(buffer);
    }

    @Override
    public final void load(final Buffer buffer) {
        space.lockByIndex(index);
        try {
            if (buffer == null || buffer.length() == 0) {
                return;
            }
            long loaded = Base64.decodeToLongWithBounds(buffer, 0, buffer.length());
            long previousSeed = POffHeapLongArray.get(addr, SEED);
            POffHeapLongArray.set(addr, SEED, loaded);
            if (previousSeed != -1 && previousSeed != loaded) {
                if (POffHeapLongArray.get(addr, DIRTY) != 1) {
                    POffHeapLongArray.set(addr, DIRTY, 1);
                    space.notifyUpdate(index);
                }
            }
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public void loadDiff(Buffer buffer) {
        load(buffer);
    }

    @Override
    public final long newKey() {
        space.lockByIndex(index);
        try {
            long seed = POffHeapLongArray.get(addr, SEED);
            if (seed == Constants.KEY_PREFIX_MASK) {
                throw new IndexOutOfBoundsException("Object Index could not be created because it exceeded the capacity of the current prefix. Ask for a new prefix.");
            }
            if (seed == -1) {
                seed = 0;
            }
            seed++;
            POffHeapLongArray.set(addr, SEED, seed);
            long objectKey = prefix + seed;
            space.notifyUpdate(index);
            if (objectKey >= Constants.END_OF_TIME) {
                throw new IndexOutOfBoundsException("Object Index exceeds the maximum JavaScript number capacity. (2^" + Constants.LONG_SIZE + ")");
            }
            return objectKey;
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public final long index() {
        return index;
    }

    @Override
    public final long world() {
        return space.worldByIndex(index);
    }

    @Override
    public final long time() {
        return space.timeByIndex(index);
    }

    @Override
    public final long id() {
        return space.idByIndex(index);
    }

    @Override
    public final byte chunkType() {
        return ChunkType.GEN_CHUNK;
    }

}
