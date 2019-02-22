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

import greycat.Callback;
import greycat.Constants;
import greycat.Graph;
import greycat.chunk.Chunk;
import greycat.chunk.ChunkSpace;
import greycat.chunk.ChunkType;
import greycat.chunk.Stack;
import greycat.memory.primary.POffHeapByteArray;
import greycat.memory.primary.POffHeapLongArray;
import greycat.struct.Buffer;
import greycat.struct.BufferIterator;
import greycat.struct.EGraph;
import greycat.utility.HashHelper;
import greycat.utility.KeyHelper;

import static greycat.Constants.BUFFER_SEP;

class OffHeapChunkSpace implements ChunkSpace {

    private static final long HASH_LOAD_FACTOR = 4;

    private final long _maxEntries;
    private final long _hashEntries;

    private final Stack _lru;
    private final Stack _dirtiesStack;
    private final Graph _graph;

    private final long locks;
    private final long hashNext;
    private final long hash;
    private final long worlds;
    private final long times;
    private final long ids;
    private final long types;
    private final long marks;
    private final long addrs;

    @Override
    public final Graph graph() {
        return this._graph;
    }

    final long worldByIndex(long index) {
        return POffHeapLongArray.get(worlds, index);
    }

    final long timeByIndex(long index) {
        return POffHeapLongArray.get(times, index);
    }

    final long idByIndex(long index) {
        return POffHeapLongArray.get(ids, index);
    }

    final long addrByIndex(long index) {
        return POffHeapLongArray.get(addrs, index);
    }

    final void setAddrByIndex(long index, long addr) {
        POffHeapLongArray.set(addrs, index, addr);
    }

    final void lockByIndex(final long index) {
        while (!POffHeapLongArray.compareAndSwap(locks, index, -1, 1)) ;
    }

    final void unlockByIndex(final long index) {
        if (!POffHeapLongArray.compareAndSwap(locks, index, 1, -1)) {
            System.out.println("CAS error !!!");
        }
    }

    OffHeapChunkSpace(final long initialCapacity, final Graph p_graph) {
        _graph = p_graph;
        _maxEntries = initialCapacity;
        _hashEntries = initialCapacity * HASH_LOAD_FACTOR;
        _lru = new OffHeapFixedStack(initialCapacity, true);
        _dirtiesStack = new OffHeapFixedStack(initialCapacity, false);
        locks = POffHeapLongArray.allocate(initialCapacity);
        hashNext = POffHeapLongArray.allocate(initialCapacity);
        hash = POffHeapLongArray.allocate(_hashEntries);
        addrs = POffHeapLongArray.allocate(initialCapacity);
        worlds = POffHeapLongArray.allocate(_maxEntries);
        times = POffHeapLongArray.allocate(_maxEntries);
        ids = POffHeapLongArray.allocate(_maxEntries);
        types = POffHeapByteArray.allocate(_maxEntries);
        marks = POffHeapLongArray.allocate(_maxEntries);
        for (long i = 0; i < _maxEntries; i++) {
            POffHeapLongArray.set(marks, i, 0);
        }
    }

    @Override
    public final void freeAll() {
        _lru.free();
        _dirtiesStack.free();
        POffHeapLongArray.free(hashNext);
        POffHeapLongArray.free(hash);
        POffHeapLongArray.free(addrs);
        POffHeapLongArray.free(worlds);
        POffHeapLongArray.free(times);
        POffHeapLongArray.free(ids);
        POffHeapByteArray.free(types);
        POffHeapLongArray.free(marks);
        POffHeapLongArray.free(locks);
    }

    @Override
    public final Chunk getAndMark(final byte type, final long world, final long time, final long id) {
        final long index = HashHelper.tripleHash(type, world, time, id, this._hashEntries);
        long m = POffHeapLongArray.get(hash, index);
        long found = -1;
        while (m != -1) {
            if (POffHeapByteArray.get(types, m) == type
                    && POffHeapLongArray.get(worlds, m) == world
                    && POffHeapLongArray.get(times, m) == time
                    && POffHeapLongArray.get(ids, m) == id) {
                if (mark(m) > 0) {
                    found = m;
                }
                break;
            } else {
                m = POffHeapLongArray.get(hashNext, m);
            }
        }
        if (found != -1) {
            return get(found);
        } else {
            return null;
        }
    }

    @Override
    public final Chunk get(final long index) {
        switch (POffHeapByteArray.get(types, index)) {
            case ChunkType.STATE_CHUNK:
                return new OffHeapStateChunk(this, index);
            case ChunkType.WORLD_ORDER_CHUNK:
                return new OffHeapWorldOrderChunk(this, index);
            case ChunkType.TIME_TREE_CHUNK:
                return new OffHeapTimeTreeChunk(this, index);
            case ChunkType.GEN_CHUNK:
                return new OffHeapGenChunk(this, POffHeapLongArray.get(ids, index), index);
        }
        return null;
    }

    @Override
    public final void getOrLoadAndMark(final byte type, final long world, final long time, final long id, final Callback<Chunk> callback) {
        final Chunk fromMemory = getAndMark(type, world, time, id);
        if (fromMemory != null) {
            callback.on(fromMemory);
        } else {
            final Buffer keys = graph().newBuffer();
            KeyHelper.keyToBuffer(keys, type, world, time, id);
            graph().storage().get(keys, new Callback<Buffer>() {
                @Override
                public void on(final Buffer result) {
                    if (result != null && result.length() > 0) {
                        Chunk loadedChunk = createAndMark(type, world, time, id);
                        loadedChunk.load(result);
                        result.free();
                        callback.on(loadedChunk);
                    } else {
                        keys.free();
                        callback.on(null);
                    }
                }
            });
        }
    }

    @Override
    public final void getOrLoadAndMarkAll(final long[] keys, final Callback<Chunk[]> callback) {
        final int querySize = keys.length / Constants.KEY_SIZE;
        final Chunk[] finalResult = new Chunk[querySize];
        int[] reverse = null;
        int reverseIndex = 0;
        Buffer toLoadKeys = null;
        for (int i = 0; i < querySize; i++) {
            final int offset = i * Constants.KEY_SIZE;
            final byte loopType = (byte) keys[offset];
            if (loopType != -1) {
                final Chunk fromMemory = getAndMark((byte) keys[offset], keys[offset + 1], keys[offset + 2], keys[offset + 3]);
                if (fromMemory != null) {
                    finalResult[i] = fromMemory;
                } else {
                    if (reverse == null) {
                        reverse = new int[querySize];
                        toLoadKeys = graph().newBuffer();
                    }
//                    reverse[i] = reverseIndex;
                    reverse[reverseIndex] = i;
                    if (reverseIndex != 0) {
                        toLoadKeys.write(BUFFER_SEP);
                    }
                    KeyHelper.keyToBuffer(toLoadKeys, (byte) keys[offset], keys[offset + 1], keys[offset + 2], keys[offset + 3]);
                    reverseIndex++;
                }
            } else {
                finalResult[i] = null;
            }
        }
        if (reverse != null) {
            final int[] finalReverse = reverse;
            graph().storage().get(toLoadKeys, new Callback<Buffer>() {
                @Override
                public void on(final Buffer loadAllResult) {
                    BufferIterator it = loadAllResult.iterator();
                    int i = 0;
                    while (it.hasNext()) {
                        Buffer view = it.next();
                        int reversedIndex = finalReverse[i];
                        int reversedOffset = reversedIndex * Constants.KEY_SIZE;
                        if (view.length() > 0) {
                            Chunk loadedChunk = createAndMark((byte) keys[reversedOffset], keys[reversedOffset + 1], keys[reversedOffset + 2], keys[reversedOffset + 3]);
                            loadedChunk.load(view);
                            finalResult[reversedIndex] = loadedChunk;
                        } else {
                            finalResult[reversedIndex] = null;
                        }
                        i++;
                    }
                    loadAllResult.free();
                    callback.on(finalResult);
                }
            });
        } else {
            callback.on(finalResult);
        }
    }

    @Override
    public final long mark(final long index) {
        long before;
        long after;
        do {
            before = POffHeapLongArray.get(marks, index);
            if (before != -1) {
                after = before + 1;
            } else {
                after = before;
            }
        } while (!POffHeapLongArray.compareAndSwap(marks, index, before, after));
        if (before == 0 && after == 1) {
            //was at zero before, risky operation, check selectWith LRU
            this._lru.dequeue(index);
        }
        return after;
    }

    @Override
    public final void unmark(final long index) {
        long before;
        long after;
        do {
            before = POffHeapLongArray.get(marks, index);
            if (before > 0) {
                after = before - 1;
            } else {
                System.err.println("WARNING: DOUBLE UNMARK");
                after = before;
            }
        } while (!POffHeapLongArray.compareAndSwap(marks, index, before, after));
        if (before == 1 && after == 0) {
            //was at zero before, risky operation, check selectWith LRU
            this._lru.enqueue(index);
        }
    }

    @Override
    public final void free(Chunk chunk) {
        freeByIndex(chunk.index());
    }

    private void freeByIndex(long index) {
        final long rawValue = POffHeapLongArray.get(addrs, index);
        switch (POffHeapByteArray.get(types, index)) {
            case ChunkType.STATE_CHUNK:
                OffHeapStateChunk.free(rawValue, this);
                break;
            case ChunkType.WORLD_ORDER_CHUNK:
                OffHeapWorldOrderChunk.free(rawValue);
                break;
            case ChunkType.TIME_TREE_CHUNK:
                OffHeapTimeTreeChunk.free(rawValue);
                break;
            case ChunkType.GEN_CHUNK:
                OffHeapGenChunk.free(rawValue);
                break;
        }
        POffHeapLongArray.set(addrs, index, OffHeapConstants.NULL_PTR);
    }

    @Override
    public final synchronized Chunk createAndMark(final byte type, final long world, final long time, final long id) {
        //first mark the object
        long entry = -1;
        long hashIndex = HashHelper.tripleHash(type, world, time, id, this._hashEntries);
        long m = POffHeapLongArray.get(hash, hashIndex);
        while (m >= 0) {
            if (type == POffHeapByteArray.get(types, m) && world == POffHeapLongArray.get(worlds, m) && time == POffHeapLongArray.get(times, m) && id == POffHeapLongArray.get(ids, m)) {
                entry = m;
                break;
            }
            m = POffHeapLongArray.get(hashNext, m);
        }
        if (entry != -1) {
            long previous;
            long after;
            do {
                previous = POffHeapLongArray.get(marks, entry);
                if (previous != -1) {
                    after = previous + 1;
                } else {
                    after = previous;
                }
            } while (!POffHeapLongArray.compareAndSwap(marks, entry, previous, after));
            if (after == (previous + 1)) {
                return get(entry);
            }
        }
        long currentVictimIndex = -1;
        while (currentVictimIndex == -1) {
            long temp_victim = this._lru.dequeueTail();
            if (temp_victim == -1) {
                break;
            } else {
                if (POffHeapLongArray.compareAndSwap(marks, temp_victim, 0, -1)) {
                    currentVictimIndex = temp_victim;
                }
            }
        }
        if (currentVictimIndex == -1) {
            throw new RuntimeException("mwDB crashed, cache is full, please avoid to much retention of nodes or augment cache capacity! available:" + available());
        }
        if (POffHeapLongArray.get(addrs, currentVictimIndex) != OffHeapConstants.NULL_PTR) {
            final long victimWorld = POffHeapLongArray.get(worlds, currentVictimIndex);
            final long victimTime = POffHeapLongArray.get(times, currentVictimIndex);
            final long victimObj = POffHeapLongArray.get(ids, currentVictimIndex);
            final byte victimType = POffHeapByteArray.get(types, currentVictimIndex);
            final long indexVictim = HashHelper.tripleHash(victimType, victimWorld, victimTime, victimObj, this._hashEntries);
            m = POffHeapLongArray.get(hash, indexVictim);
            long last = -1;
            while (m >= 0) {
                if (victimType == POffHeapByteArray.get(types, m) && victimWorld == POffHeapLongArray.get(worlds, m) && victimTime == POffHeapLongArray.get(times, m) && victimObj == POffHeapLongArray.get(ids, m)) {
                    break;
                }
                last = m;
                m = POffHeapLongArray.get(hashNext, m);
            }
            //POP THE VALUE FROM THE NEXT LIST
            if (last == -1) {
                long previousNext = POffHeapLongArray.get(hashNext, m);
                POffHeapLongArray.set(hash, indexVictim, previousNext);
            } else {
                if (m == -1) {
                    POffHeapLongArray.set(hashNext, last, -1);
                } else {
                    POffHeapLongArray.set(hashNext, last, POffHeapLongArray.get(hashNext, m));
                }
            }
            POffHeapLongArray.set(hashNext, m, -1);
            freeByIndex(currentVictimIndex);
        }
        //will be registered by the chunk itself
        POffHeapLongArray.set(marks, currentVictimIndex, 1);
        POffHeapByteArray.set(types, currentVictimIndex, type);
        POffHeapLongArray.set(worlds, currentVictimIndex, world);
        POffHeapLongArray.set(times, currentVictimIndex, time);
        POffHeapLongArray.set(ids, currentVictimIndex, id);
        //negociate the lock to write on hashIndex
        POffHeapLongArray.set(hashNext, currentVictimIndex, POffHeapLongArray.get(hash, hashIndex));
        POffHeapLongArray.set(hash, hashIndex, currentVictimIndex);
        //free the lock
        return get(currentVictimIndex);
    }

    @Override
    public void notifyUpdate(long index) {
        if (_dirtiesStack.enqueue(index)) {
            mark(index);
        }
    }

    @Override
    public synchronized void save(final Callback<Boolean> callback) {
        final Buffer stream = this._graph.newBuffer();
        boolean isFirst = true;
        while (_dirtiesStack.size() != 0) {
            long tail = _dirtiesStack.dequeueTail();
            //Save chunk Key
            if (isFirst) {
                isFirst = false;
            } else {
                stream.write(BUFFER_SEP);
            }
            KeyHelper.keyToBuffer(stream, POffHeapByteArray.get(types, tail), POffHeapLongArray.get(worlds, tail), POffHeapLongArray.get(times, tail), POffHeapLongArray.get(ids, tail));
            //Save chunk payload
            stream.write(BUFFER_SEP);
            try {
                Chunk loopChunk = get(tail);
                if (loopChunk != null) {
                    loopChunk.save(stream);
                }
                unmark(tail);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //shrink in case of i != full size
        this.graph().storage().put(stream, new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                //free all value
                stream.free();
                if (callback != null) {
                    callback.on(result);
                }
            }
        });
    }

    @Override
    public final void clear() {
        //TODO reset everything
    }

    @Override
    public final long available() {
        return _lru.size();
    }

    @Override
    public EGraph newVolatileGraph() {
        return new OffHeapEGraph(new OffHeapVolatileContainer(), OffHeapConstants.NULL_PTR, _graph);
    }

}



