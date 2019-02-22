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
import greycat.chunk.WorldOrderChunk;
import greycat.internal.CoreConstants;
import greycat.memory.primary.POffHeapLongArray;
import greycat.struct.Buffer;
import greycat.struct.LongLongMapCallBack;
import greycat.utility.Base64;
import greycat.utility.HashHelper;

final class OffHeapWorldOrderChunk implements WorldOrderChunk {

    private static final int DIRTY = 0;
    private static final int SIZE = 1;
    private static final int CAPACITY = 2;
    private static final int LOCK = 3;
    private static final int LOCK_EXT = 4;
    private static final int MAGIC = 5;
    private static final int EXTRA = 6;
    private static final int HASH_SUB = 7;

    private static final int KV_OFFSET = 8;

    private final OffHeapChunkSpace space;
    private final long index;//direct pointer to space

    OffHeapWorldOrderChunk(final OffHeapChunkSpace p_space, final long p_index) {
        index = p_index;
        space = p_space;
        space.lockByIndex(index);
        try {
            long temp_addr = space.addrByIndex(index);
            if (temp_addr == OffHeapConstants.NULL_PTR) {
                long initialCapacity = Constants.MAP_INITIAL_CAPACITY;
                temp_addr = POffHeapLongArray.allocate(KV_OFFSET + (initialCapacity * 2));
                space.setAddrByIndex(index, temp_addr);
                //init the initial values
                POffHeapLongArray.set(temp_addr, DIRTY, 0);
                POffHeapLongArray.set(temp_addr, SIZE, 0);
                POffHeapLongArray.set(temp_addr, CAPACITY, initialCapacity);
                POffHeapLongArray.set(temp_addr, LOCK, 0);
                POffHeapLongArray.set(temp_addr, LOCK_EXT, 0);
                POffHeapLongArray.set(temp_addr, MAGIC, 0);
                POffHeapLongArray.set(temp_addr, EXTRA, Constants.NULL_LONG);
                POffHeapLongArray.set(temp_addr, HASH_SUB, OffHeapConstants.NULL_PTR);
            }
        } finally {
            space.unlockByIndex(index);
        }
    }

    private void setKV(final long addr, final long key, final long value, final long index) {
        POffHeapLongArray.set(addr, KV_OFFSET + (index * 2), key);
        POffHeapLongArray.set(addr, KV_OFFSET + (index * 2) + 1, value);
    }

    private long key(final long addr, final long index) {
        return POffHeapLongArray.get(addr, KV_OFFSET + (index * 2));
    }

    private long value(final long addr, final long index) {
        return POffHeapLongArray.get(addr, KV_OFFSET + (index * 2) + 1);
    }

    private long hash(final long sub_hash_ptr, final long capacity, final long index) {
        return POffHeapLongArray.get(sub_hash_ptr, capacity + index);
    }

    private long next(final long sub_hash_ptr, final long index) {
        return POffHeapLongArray.get(sub_hash_ptr, index);
    }

    static void free(final long addr) {
        if (addr != OffHeapConstants.NULL_PTR) {
            final long hash_sub = POffHeapLongArray.get(addr, HASH_SUB);
            if (hash_sub != OffHeapConstants.NULL_PTR) {
                POffHeapLongArray.free(hash_sub);
            }
            POffHeapLongArray.free(addr);
        }
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
    public final long extra() {
        return POffHeapLongArray.get(space.addrByIndex(index), EXTRA);
    }

    @Override
    public final byte chunkType() {
        return ChunkType.WORLD_ORDER_CHUNK;
    }

    @Override
    public final long magic() {
        return POffHeapLongArray.get(space.addrByIndex(index), MAGIC);
    }

    @Override
    public final void setExtra(final long extraValue) {
        POffHeapLongArray.set(space.addrByIndex(index), EXTRA, extraValue);
    }

    @Override
    public final long index() {
        return index;
    }

    @Override
    public final int size() {
        //TODO CAS
        return (int) POffHeapLongArray.get(space.addrByIndex(index), SIZE);
    }

    @Override
    public final void lock() {
        space.lockByIndex(index);
        try {
            final long addr = space.addrByIndex(index);
            while (!POffHeapLongArray.compareAndSwap(addr, LOCK, 0, 1)) ;
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public final void unlock() {
        space.lockByIndex(index);
        try {
            final long addr = space.addrByIndex(index);
            if (!POffHeapLongArray.compareAndSwap(addr, LOCK, 1, 0)) {
                throw new RuntimeException("CAS Error !!!");
            }
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public void externalLock() {
        space.lockByIndex(index);
        try {
            final long addr = space.addrByIndex(index);
            while (!POffHeapLongArray.compareAndSwap(addr, LOCK_EXT, 0, 1)) ;
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public void externalUnlock() {
        space.lockByIndex(index);
        try {
            final long addr = space.addrByIndex(index);
            if (!POffHeapLongArray.compareAndSwap(addr, LOCK_EXT, 1, 0)) {
                throw new RuntimeException("CAS Error !!!");
            }
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public final void each(final LongLongMapCallBack callback) {
        space.lockByIndex(index);
        try {
            final long addr = space.addrByIndex(index);
            final long size = POffHeapLongArray.get(addr, SIZE);
            for (long i = 0; i < size; i++) {
                callback.on(key(addr, i), value(addr, i));
            }
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public final long get(final long requestKey) {
        long result = Constants.NULL_LONG;
        space.lockByIndex(index);
        try {
            final long addr = space.addrByIndex(index);
            final long size = POffHeapLongArray.get(addr, SIZE);
            if (size > 0) {
                final long hash_sub_ptr = POffHeapLongArray.get(addr, HASH_SUB);
                if (hash_sub_ptr == OffHeapConstants.NULL_PTR) {
                    /* NO HASH STRUCTURE, LET'S ITERATES */
                    for (long i = 0; i < size; i++) {
                        if (requestKey == key(addr, i)) {
                            result = value(addr, i);
                            break;
                        }
                    }
                } else {
                    /* HASH STRUCTURE IS PRESENT LET'S USE IT */
                    final long capacity = POffHeapLongArray.get(addr, CAPACITY);
                    final long hashed_requestKey = HashHelper.longHash(requestKey, capacity * 2);
                    long m = hash(hash_sub_ptr, capacity, hashed_requestKey);
                    while (m >= 0) {
                        if (requestKey == key(addr, m)) {
                            result = value(addr, m);
                            break;
                        } else {
                            m = next(hash_sub_ptr, m);
                        }
                    }
                }
            }
        } finally {
            space.unlockByIndex(index);
        }
        return result;
    }

    @Override
    public final void put(final long insertKey, final long insertValue) {
        space.lockByIndex(index);
        try {
            internal_put(space.addrByIndex(index), insertKey, insertValue, true);
        } finally {
            space.unlockByIndex(index);
        }
    }

    private long resize(final long currentAddr, final long previousSize, final long previousCapacity, final long newCapacity) {
        if (newCapacity > previousCapacity) {
            final long newAddr = POffHeapLongArray.reallocate(currentAddr, KV_OFFSET + newCapacity * 2);
            space.setAddrByIndex(index, newAddr);
            long sub_hash = POffHeapLongArray.get(newAddr, HASH_SUB);
            if (sub_hash == OffHeapConstants.NULL_PTR) {
                if (newCapacity > Constants.MAP_INITIAL_CAPACITY) {
                    sub_hash = POffHeapLongArray.allocate(newCapacity * 3);
                }
            } else {
                sub_hash = POffHeapLongArray.reallocate(sub_hash, newCapacity * 3);
                POffHeapLongArray.reset(sub_hash, newCapacity * 3);
            }
            if (sub_hash != OffHeapConstants.NULL_PTR) {
                /* reHash everything */
                final long double_newCapacity = newCapacity * 2;
                for (long i = 0; i < previousSize; i++) {
                    long hashed_loop_key = HashHelper.longHash(key(newAddr, i), double_newCapacity);
                    POffHeapLongArray.set(sub_hash, i, hash(sub_hash, newCapacity, hashed_loop_key));
                    POffHeapLongArray.set(sub_hash, newCapacity + hashed_loop_key, i);
                }
                POffHeapLongArray.set(newAddr, HASH_SUB, sub_hash);
            }
            POffHeapLongArray.set(newAddr, CAPACITY, newCapacity);
            return newAddr;
        } else {
            return currentAddr;
        }
    }

    private long internal_put(final long initialAddr, final long insertKey, final long insertValue, final boolean notifyUpdate) {

        long addr = initialAddr;
        long size = POffHeapLongArray.get(addr, SIZE);
        long capacity = POffHeapLongArray.get(addr, CAPACITY);
        //   long double_capacity = capacity * 2;

        long hashed_requestKey = HashHelper.longHash(insertKey, capacity * 2);

        long foundIndex = -1;
        long hash_sub_ptr = POffHeapLongArray.get(addr, HASH_SUB);
        if (hash_sub_ptr == OffHeapConstants.NULL_PTR) {
            //NO HASH STRUCTURE, LET'S ITERATES
            for (long i = 0; i < size; i++) {
                if (insertKey == key(addr, i)) {
                    foundIndex = i;
                    break;
                }
            }
        } else {
            //HASH STRUCTURE IS PRESENT LET'S USE IT
            long m = hash(hash_sub_ptr, capacity, hashed_requestKey);
            while (m >= 0) {
                if (insertKey == key(addr, m)) {
                    foundIndex = m;
                    break;
                } else {
                    m = next(hash_sub_ptr, m);
                }
            }
        }
        //ok not found
        if (foundIndex == -1) {
            if (capacity == size) {
                long newCapacity = capacity * 2;
                addr = resize(addr, size, capacity, newCapacity);
                capacity = newCapacity;
                hash_sub_ptr = POffHeapLongArray.get(addr, HASH_SUB);
                hashed_requestKey = HashHelper.longHash(insertKey, capacity * 2);
            }
            setKV(addr, insertKey, insertValue, size);
            //if present, update hashing structure
            if (hash_sub_ptr != OffHeapConstants.NULL_PTR) {
                POffHeapLongArray.set(hash_sub_ptr, size, hash(hash_sub_ptr, capacity, hashed_requestKey));
                POffHeapLongArray.set(hash_sub_ptr, capacity + hashed_requestKey, size);
            }
            POffHeapLongArray.set(addr, SIZE, size + 1);
            POffHeapLongArray.set(addr, MAGIC, POffHeapLongArray.get(addr, MAGIC) + 1);
            if (notifyUpdate && POffHeapLongArray.get(addr, DIRTY) != 1) {
                POffHeapLongArray.set(addr, DIRTY, 1);
                space.notifyUpdate(index);
            }
        } else {
            if (value(addr, foundIndex) != insertValue) {
                setKV(addr, insertKey, insertValue, foundIndex);
                POffHeapLongArray.set(addr, MAGIC, POffHeapLongArray.get(addr, MAGIC) + 1);
                if (notifyUpdate && POffHeapLongArray.get(addr, DIRTY) != 1) {
                    POffHeapLongArray.set(addr, DIRTY, 1);
                    space.notifyUpdate(index);
                }
            }
        }
        return addr;
    }

    @Override
    public final void load(final Buffer buffer) {
        if (buffer == null || buffer.length() == 0) {
            return;
        }
        space.lockByIndex(index);
        try {
            long addr = space.addrByIndex(index);
            final long initialSize = POffHeapLongArray.get(addr, SIZE);
            final long initialCapacity = POffHeapLongArray.get(addr, CAPACITY);
            final boolean isInitial = initialSize == 0;
            long cursor = 0;
            long bufferSize = buffer.length();
            boolean initDone = false;
            long previousStart = 0;
            long loopKey = Constants.NULL_LONG;
            while (cursor < bufferSize) {
                final byte current = buffer.read(cursor);
                switch (current) {
                    case Constants.CHUNK_SEP:
                        POffHeapLongArray.set(addr, EXTRA, Base64.decodeToLongWithBounds(buffer, previousStart, cursor));
                        previousStart = cursor + 1;
                        break;
                    case Constants.CHUNK_VAL_SEP:
                        if (!initDone) {
                            final long mapSize = Base64.decodeToLongWithBounds(buffer, previousStart, cursor);
                            final long closePowerOfTwo = (long) Math.pow(2, Math.ceil(Math.log(mapSize) / Math.log(2)));
                            addr = resize(addr, initialSize, initialCapacity, closePowerOfTwo);
                            initDone = true;
                        } else if (loopKey == CoreConstants.NULL_LONG) {
                            loopKey = Base64.decodeToLongWithBounds(buffer, previousStart, cursor);
                        } else {
                            long loopValue = Base64.decodeToLongWithBounds(buffer, previousStart, cursor);
                            internal_put(addr, loopKey, loopValue, !isInitial);
                            //reset key for next round
                            loopKey = CoreConstants.NULL_LONG;
                        }
                        previousStart = cursor + 1;
                        break;
                }
                cursor++;
            }
            if (loopKey != CoreConstants.NULL_LONG) {
                long loopValue = Base64.decodeToLongWithBounds(buffer, previousStart, cursor);
                internal_put(addr, loopKey, loopValue, !isInitial);
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
    public final void remove(long key) {
        throw new RuntimeException("Not implemented yet!!!");
    }

    @Override
    public final void save(final Buffer buffer) {
        space.lockByIndex(index);
        try {
            final long addr = space.addrByIndex(index);
            final long extra = POffHeapLongArray.get(addr, EXTRA);
            if (extra != Constants.NULL_LONG) {
                Base64.encodeLongToBuffer(extra, buffer);
                buffer.write(Constants.CHUNK_SEP);
            }
            final int size = (int) POffHeapLongArray.get(addr, SIZE);
            Base64.encodeIntToBuffer(size, buffer);
            for (long i = 0; i < size; i++) {
                //save KV
                buffer.write(Constants.CHUNK_VAL_SEP);
                Base64.encodeLongToBuffer(key(addr, i), buffer);
                buffer.write(Constants.CHUNK_VAL_SEP);
                Base64.encodeLongToBuffer(value(addr, i), buffer);
            }
            POffHeapLongArray.set(addr, DIRTY, 0);//set dirty to false
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public void saveDiff(Buffer buffer) {
        save(buffer);
    }

}



