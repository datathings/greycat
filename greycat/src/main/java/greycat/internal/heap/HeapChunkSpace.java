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
package greycat.internal.heap;

import greycat.Callback;
import greycat.Constants;
import greycat.DeferCounter;
import greycat.Graph;
import greycat.chunk.*;
import greycat.plugin.Job;
import greycat.plugin.NodeDeclaration;
import greycat.struct.Buffer;
import greycat.struct.BufferIterator;
import greycat.struct.EStructArray;
import greycat.utility.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class HeapChunkSpace implements ChunkSpace {

    private static final int HASH_LOAD_FACTOR = 4;

    private int _maxEntries;
    private final int _batchSize;
    private int _hashEntries;

    private Stack _lru;
    private Stack _dirtiesStack;

    private AtomicIntegerArray _hashNext;
    private AtomicIntegerArray _hash;

    private AtomicLongArray _chunkWorlds;
    private AtomicLongArray _chunkTimes;
    private AtomicLongArray _chunkIds;
    private HeapAtomicByteArray _chunkTypes;

    private AtomicReferenceArray<Chunk> _chunkValues;

    private AtomicLongArray _chunkMarks;

    private final Graph _graph;

    private final boolean _deep_priority;

    private Interceptor[] _interceptors;

    private long cache_size = 0;

    @Override
    public final Graph graph() {
        return this._graph;
    }

    final long worldByIndex(long index) {
        return this._chunkWorlds.get((int) index);
    }

    final long timeByIndex(long index) {
        return this._chunkTimes.get((int) index);
    }

    final long idByIndex(long index) {
        return this._chunkIds.get((int) index);
    }

    public HeapChunkSpace(final int initialCapacity, final int batchSize, final Graph p_graph, final boolean deepWorldPriority) {
        _interceptors = null;
        _batchSize = batchSize;
        _deep_priority = deepWorldPriority;
        _graph = p_graph;
        _maxEntries = initialCapacity;
        _hashEntries = initialCapacity * HASH_LOAD_FACTOR;
        _lru = new HeapFixedStack(initialCapacity, true);
        _dirtiesStack = new HeapFixedStack(initialCapacity, false);
        _hashNext = new AtomicIntegerArray(initialCapacity);
        _hash = new AtomicIntegerArray(_hashEntries);
        for (int i = 0; i < initialCapacity; i++) {
            _hashNext.set(i, -1);
        }
        for (int i = 0; i < _hashEntries; i++) {
            _hash.set(i, -1);
        }
        _chunkValues = new AtomicReferenceArray<Chunk>(initialCapacity);
        _chunkWorlds = new AtomicLongArray(_maxEntries);
        _chunkTimes = new AtomicLongArray(_maxEntries);
        _chunkIds = new AtomicLongArray(_maxEntries);
        _chunkTypes = new HeapAtomicByteArray(_maxEntries);
        _chunkMarks = new AtomicLongArray(_maxEntries);
        for (int i = 0; i < _maxEntries; i++) {
            _chunkMarks.set(i, 0);
        }
    }

    private void capacity_extends() {
        int new_maxEntries = this._maxEntries * 2;
        this._graph.log().warn("extends cache capacity from " + this._maxEntries + " to " + new_maxEntries);
        int new_hashEntries = new_maxEntries * HASH_LOAD_FACTOR;
        Stack new_lru = new HeapFixedStack(new_maxEntries, true);
        Stack new_dirties = new HeapFixedStack(new_maxEntries, false);
        AtomicIntegerArray new_hashNext = new AtomicIntegerArray(new_maxEntries);
        AtomicIntegerArray new_hash = new AtomicIntegerArray(new_hashEntries);
        for (int i = 0; i < new_maxEntries; i++) {
            new_hashNext.set(i, -1);
        }
        for (int i = 0; i < new_hashEntries; i++) {
            new_hash.set(i, -1);
        }
        AtomicReferenceArray<Chunk> new_chunkValues = new AtomicReferenceArray<Chunk>(new_maxEntries);
        AtomicLongArray new_chunkWorlds = new AtomicLongArray(new_maxEntries);
        AtomicLongArray new_chunkTimes = new AtomicLongArray(new_maxEntries);
        AtomicLongArray new_chunkIds = new AtomicLongArray(new_maxEntries);
        HeapAtomicByteArray new_chunkTypes = new HeapAtomicByteArray(new_maxEntries);
        AtomicLongArray new_chunkMarks = new AtomicLongArray(new_maxEntries);
        for (int i = 0; i < new_maxEntries; i++) {
            new_chunkMarks.set(i, 0);
        }

        byte type;
        long world;
        long time;
        long id;
        Chunk chunk;
        long marks;
        int index;

        int offset = (int) _dirtiesStack.dequeueTail();
        while (offset != -1) {
            new_dirties.enqueue(offset);
            offset = (int) _dirtiesStack.dequeueTail();
        }

        for (int i = 0; i < _maxEntries; i++) {
            new_lru.dequeue(i);

            type = _chunkTypes.get(i);
            world = _chunkWorlds.get(i);
            time = _chunkTimes.get(i);
            id = _chunkIds.get(i);
            chunk = _chunkValues.get(i);
            marks = _chunkMarks.get(i);
            new_chunkTypes.set(i, type);
            new_chunkWorlds.set(i, world);
            new_chunkTimes.set(i, time);
            new_chunkIds.set(i, id);
            new_chunkValues.set(i, chunk);
            new_chunkMarks.set(i, marks);

            if (_deep_priority) {
                index = (int) HashHelper.tripleHash(type, world, time, id, new_hashEntries);
            } else {
                index = (int) HashHelper.simpleTripleHash(type, world, time, id, new_hashEntries);
            }

            int previous_hash = new_hash.get(index);
            new_hash.set(index, i);
            new_hashNext.set(i, previous_hash);

            if (marks == 0) {
                new_lru.enqueue(i);
            }
            if (_dirtiesStack.dequeue(i)) {
                new_dirties.enqueue(i);
            }
        }

        this._maxEntries = new_maxEntries;
        this._hashEntries = new_hashEntries;
        this._lru = new_lru;
        this._dirtiesStack = new_dirties;
        this._hashNext = new_hashNext;
        this._hash = new_hash;
        this._chunkValues = new_chunkValues;
        this._chunkWorlds = new_chunkWorlds;
        this._chunkTimes = new_chunkTimes;
        this._chunkIds = new_chunkIds;
        this._chunkTypes = new_chunkTypes;
        this._chunkMarks = new_chunkMarks;
    }

    @Override
    public final Chunk getAndMark(final byte type, final long world, final long time, final long id) {
        boolean valid = true;
        if (_interceptors != null) {
            for (int i = 0; i < _interceptors.length && valid; i++) {
                valid = _interceptors[i].preChunkRead(type, world, time, id);
            }
        }
        if (!valid) {
            return null;
        }

        final int index;
        if (_deep_priority) {
            index = (int) HashHelper.tripleHash(type, world, time, id, this._hashEntries);
        } else {
            index = (int) HashHelper.simpleTripleHash(type, world, time, id, this._hashEntries);
        }
        int m = this._hash.get(index);
        int found = -1;
        while (m != -1) {
            if (_chunkTypes.get(m) == type
                    && _chunkWorlds.get(m) == world
                    && _chunkTimes.get(m) == time
                    && _chunkIds.get(m) == id) {
                if (mark(m) > 0) {
                    found = m;
                }
                break;
            } else {
                m = this._hashNext.get(m);
            }
        }
        Chunk result = null;
        if (found != -1) {
            result = this._chunkValues.get(found);
            if (result == null) {
                unmark(found);
            } else if (!result.inSync()) {
                //cache is out of sync, force refresh
                unmark(result.index());
                result = null;
            }
        }
        return result;
    }

    @Override
    public final Chunk get(final long index) {
        final int casted = (int) index;
        if (casted == -1) {
            return null;
        }
        boolean valid = true;
        if (_interceptors != null) {
            for (int i = 0; i < _interceptors.length && valid; i++) {
                valid = _interceptors[i].preChunkRead(_chunkTypes.get(casted), _chunkWorlds.get(casted), _chunkTimes.get(casted), _chunkIds.get(casted));
            }
        }
        if (!valid) {
            return null;
        }
        return this._chunkValues.get(casted);
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
                    // reverse[i] = reverseIndex;
                    reverse[reverseIndex] = i;
                    if (reverseIndex != 0) {
                        toLoadKeys.write(Constants.BUFFER_SEP);
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
            // TODO: enhance performance
            graph().storage().get(toLoadKeys, new Callback<Buffer>() {
                @Override
                public void on(final Buffer loadAllResult) {
                    BufferIterator it = loadAllResult.iterator();
                    int i = 0;
                    while (it.hasNext()) {
                        final Buffer view = it.next();
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
        int castedIndex = (int) index;
        long before;
        long after;
        do {
            before = _chunkMarks.get(castedIndex);
            if (before != -1) {
                after = before + 1;
            } else {
                after = before;
            }
        } while (!_chunkMarks.compareAndSet(castedIndex, before, after));
        if (before == 0 && after == 1) {
            //was at zero before, risky operation, check selectWith LRU
            this._lru.dequeue(index);
        }
        return after;
    }

    @Override
    public final void unmark(final long index) {
        int castedIndex = (int) index;
        long before;
        long after;
        do {
            before = _chunkMarks.get(castedIndex);
            if (before > 0) {
                after = before - 1;
            } else {
                System.err.println("WARNING: DOUBLE UNMARK " + _chunkTypes.get(castedIndex));
                after = before;
            }
        } while (!_chunkMarks.compareAndSet(castedIndex, before, after));
        if (before == 1 && after == 0) {
            //was at zero before, risky operation, check selectWith LRU
            this._lru.enqueue(index);
        }
    }

    @Override
    public synchronized final void delete(final byte type, final long world, final long time, final long id) {
        final int index;
        if (_deep_priority) {
            index = (int) HashHelper.tripleHash(type, world, time, id, this._hashEntries);
        } else {
            index = (int) HashHelper.simpleTripleHash(type, world, time, id, this._hashEntries);
        }
        int m = this._hash.get(index);
        int found = -1;
        while (m != -1) {
            if (_chunkTypes.get(m) == type
                    && _chunkWorlds.get(m) == world
                    && _chunkTimes.get(m) == time
                    && _chunkIds.get(m) == id) {
                found = m;
                break;
            } else {
                m = this._hashNext.get(m);
            }
        }
        if (found != -1) {
            cache_size--;
            _dirtiesStack.dequeue(found);
            _chunkValues.set(found, null);
                /*
            long markBefore = _chunkMarks.get(found);
            if (markBefore != 0) {
                //System.err.println("Still mark deleted chunk " + markBefore);
                do {
                    markBefore = _chunkMarks.get(found);
                } while (!_chunkMarks.compareAndSet(found, markBefore, 0));
                if (markBefore != 0) {
                    this._lru.enqueue(found);
                }
            }
                */
        }
    }

    @Override
    public final void free(Chunk chunk) {
        //NOOP
    }

    @Override
    public final Chunk createAndMark(final byte type, final long world, final long time, final long id) {
        boolean valid = true;
        if (_interceptors != null) {
            for (int i = 0; i < _interceptors.length && valid; i++) {
                valid = _interceptors[i].preChunkCreate(type, world, time, id);
            }
        }
        if (!valid) {
            return null;
        }
        return internal_createAndMark(type, world, time, id);
    }

    private synchronized Chunk internal_createAndMark(final byte type, final long world, final long time, final long id) {
        //first mark the object
        int entry = -1;
        final int hashIndex;
        if (_deep_priority) {
            hashIndex = (int) HashHelper.tripleHash(type, world, time, id, this._hashEntries);
        } else {
            hashIndex = (int) HashHelper.simpleTripleHash(type, world, time, id, this._hashEntries);
        }
        int m = this._hash.get(hashIndex);
        while (m >= 0) {
            if (type == _chunkTypes.get(m) && world == _chunkWorlds.get(m) && time == _chunkTimes.get(m) && id == _chunkIds.get(m)) {
                entry = m;
                break;
            }
            m = this._hashNext.get(m);
        }
        if (entry != -1) {
            long previous;
            long after;
            do {
                previous = _chunkMarks.get(entry);
                if (previous != -1) {
                    after = previous + 1;
                } else {
                    after = previous;
                }
            } while (!_chunkMarks.compareAndSet(entry, previous, after));
            if (after == (previous + 1)) {
                return _chunkValues.get(entry);
            }
        }
        int currentVictimIndex = -1;
        while (currentVictimIndex == -1) {
            int temp_victim = (int) this._lru.dequeueTail();
            if (temp_victim == -1) {
                break;
            } else {
                if (_chunkMarks.compareAndSet(temp_victim, 0, -1)) {
                    currentVictimIndex = temp_victim;
                }
            }
        }
        if (currentVictimIndex == -1) {
            capacity_extends();
            return internal_createAndMark(type, world, time, id);
            //RESIZE
            //CALL again
            // printMarked();
            //throw new RuntimeException("GreyCat crashed, cache is full, please avoid to much retention of nodes or augment cache capacity! available:" + available());
        }
        Chunk toInsert = null;
        switch (type) {
            case ChunkType.STATE_CHUNK:
                toInsert = new HeapStateChunk(this, currentVictimIndex);
                break;
            case ChunkType.WORLD_ORDER_CHUNK:
                toInsert = new HeapWorldOrderChunk(this, currentVictimIndex);
                break;
            case ChunkType.TIME_TREE_CHUNK:
                toInsert = new HeapTimeTreeChunk(this, currentVictimIndex);
                break;
            case ChunkType.SUPER_TIME_TREE_CHUNK:
                toInsert = new HeapSuperTimeTreeChunk(this, currentVictimIndex);
                break;
            case ChunkType.TIME_TREE_DVALUE_CHUNK:
                toInsert = new HeapTimeTreeDValueChunk(this, currentVictimIndex);
                break;
            case ChunkType.GEN_CHUNK:
                toInsert = new HeapGenChunk(this, id, currentVictimIndex);
                break;
        }
        if (this._chunkValues.get(currentVictimIndex) != null) {
            // Chunk victim = this._chunkValues[currentVictimIndex];
            final long victimWorld = _chunkWorlds.get(currentVictimIndex);
            final long victimTime = _chunkTimes.get(currentVictimIndex);
            final long victimObj = _chunkIds.get(currentVictimIndex);
            final byte victimType = _chunkTypes.get(currentVictimIndex);
            final int indexVictim;
            if (_deep_priority) {
                indexVictim = (int) HashHelper.tripleHash(victimType, victimWorld, victimTime, victimObj, this._hashEntries);
            } else {
                indexVictim = (int) HashHelper.simpleTripleHash(victimType, victimWorld, victimTime, victimObj, this._hashEntries);
            }
            m = _hash.get(indexVictim);
            int last = -1;
            while (m >= 0) {
                if (victimType == _chunkTypes.get(m) && victimWorld == _chunkWorlds.get(m) && victimTime == _chunkTimes.get(m) && victimObj == _chunkIds.get(m)) {
                    break;
                }
                last = m;
                m = _hashNext.get(m);
            }
            //POP THE VALUE FROM THE NEXT LIST
            if (last == -1) {
                int previousNext = _hashNext.get(m);
                _hash.set(indexVictim, previousNext);
            } else {
                if (m == -1) {
                    _hashNext.set(last, -1);
                } else {
                    _hashNext.set(last, _hashNext.get(m));
                }
            }
            if (m != -1) {
                _hashNext.set(m, -1);
            }
        } else {
            cache_size++;
        }
        _chunkValues.set(currentVictimIndex, toInsert);
        _chunkMarks.set(currentVictimIndex, 1);
        _chunkTypes.set(currentVictimIndex, type);
        _chunkWorlds.set(currentVictimIndex, world);
        _chunkTimes.set(currentVictimIndex, time);
        _chunkIds.set(currentVictimIndex, id);
        //negociate the lock to write on hashIndex
        _hashNext.set(currentVictimIndex, _hash.get(hashIndex));
        _hash.set(hashIndex, currentVictimIndex);
        //free the lock
        return toInsert;
    }

    @Override
    public final void notifyUpdate(final long index) {
        if (_dirtiesStack.enqueue(index)) {
            mark(index);
        }
    }

    @Override
    public final synchronized void save(final boolean silent, final boolean partial, final LMap filter, final Callback<Buffer> callback) {

        java.util.Map<Long, Tuple<Listeners, LArray>> events = null;
        Buffer stream = this._graph.newBuffer();
        List<Buffer> stacked = null;
        boolean isFirst = true;
        int counter = 0;
        while (_dirtiesStack.size() != 0 && (!partial || _batchSize == -1 || counter <= _batchSize)) {
            int tail = (int) _dirtiesStack.dequeueTail();
            counter++;
            boolean filtered = false;
            if (filter != null) {
                if (!filter.contains(_chunkIds.get(tail))) {
                    filtered = true;
                }
            }
            if (!filtered) {
                if (stream.length() > (100 * 1024 * 1024)) {
                    if (stacked == null) {
                        stacked = new ArrayList<Buffer>();
                    }
                    stacked.add(stream);
                    stream = this._graph.newBuffer();
                    isFirst = true;
                }
                //Save chunk Key
                if (isFirst) {
                    isFirst = false;
                } else {
                    stream.write(Constants.BUFFER_SEP);
                }
                long chunkId = _chunkIds.get(tail);
                byte chunkType = _chunkTypes.get(tail);
                long chunkTime = _chunkTimes.get(tail);
                KeyHelper.keyToBuffer(stream, chunkType, _chunkWorlds.get(tail), chunkTime, chunkId);
                //we prepare the notifier
                if (chunkType == ChunkType.STATE_CHUNK) {
                    if (events != null && events.get(chunkId) != null) {
                        events.get(chunkId).right().add(chunkTime);
                    } else {
                        final WorldOrderChunk wo = (WorldOrderChunk) getAndMark(ChunkType.WORLD_ORDER_CHUNK, 0, 0, chunkId);
                        if (wo != null) {
                            final Listeners l = wo.listeners();
                            unmark(wo.index());
                            if (l != null) {
                                if (events == null) {
                                    events = new java.util.HashMap<Long, Tuple<Listeners, LArray>>();
                                }
                                LArray collector = new LArray();
                                collector.add(chunkTime);
                                events.put(chunkId, new Tuple<Listeners, LArray>(l, collector));
                            }
                        }
                    }
                }
                //Save chunk payload
                stream.write(Constants.BUFFER_SEP);
                try {
                    final Chunk loopChunk = _chunkValues.get(tail);
                    loopChunk.save(stream);
                    unmark(tail);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                _dirtiesStack.enqueue(tail);
            }
        }
        //call nocal notifier
        if (events != null) {
            final Tuple[] tuples = events.values().toArray(new Tuple[events.size()]);
            for (int i = 0; i < tuples.length; i++) {
                Tuple<Listeners, LArray> tt = tuples[i];
                tt.left().dispatch(tt.right().all());
            }
        }
        if (silent) {
            if (stacked == null) {
                Buffer finalStream = stream;
                this.graph().storage().putSilent(stream, new Callback<Buffer>() {
                    @Override
                    public void on(final Buffer result) {
                        //free all value
                        finalStream.free();
                        if (callback != null) {
                            callback.on(result);
                        }
                    }
                });
            } else {
                DeferCounter saveCounter = _graph.newCounter(stacked.size() + 1);
                for (int i = 0; i < stacked.size(); i++) {
                    Buffer finalStream = stacked.get(i);
                    this.graph().storage().putSilent(finalStream, new Callback<Buffer>() {
                        @Override
                        public void on(final Buffer result) {
                            //free all value
                            finalStream.free();
                            saveCounter.count();
                        }
                    });
                }
                Buffer finalStream = stream;
                this.graph().storage().putSilent(finalStream, new Callback<Buffer>() {
                    @Override
                    public void on(final Buffer result) {
                        //free all value
                        finalStream.free();
                        saveCounter.count();
                    }
                });
                saveCounter.then(new Job() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.on(null);
                        }
                    }
                });
            }
        } else {
            if (stacked == null) {
                Buffer finalStream = stream;
                this.graph().storage().put(finalStream, new Callback<Boolean>() {
                    @Override
                    public void on(final Boolean result) {
                        //free all value
                        finalStream.free();
                        if (callback != null) {
                            callback.on(null);
                        }
                    }
                });
            } else {
                DeferCounter saveCounter = _graph.newCounter(stacked.size() + 1);
                for (int i = 0; i < stacked.size(); i++) {
                    Buffer finalStream = stacked.get(i);
                    this.graph().storage().put(finalStream, new Callback<Boolean>() {
                        @Override
                        public void on(final Boolean result) {
                            //free all value
                            finalStream.free();
                            saveCounter.count();
                        }
                    });
                }
                Buffer finalStream = stream;
                this.graph().storage().put(finalStream, new Callback<Boolean>() {
                    @Override
                    public void on(final Boolean result) {
                        //free all value
                        finalStream.free();
                        saveCounter.count();
                    }
                });
                saveCounter.then(new Job() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.on(null);
                        }
                    }
                });
            }
        }
    }

/*
    @Override
    public final synchronized void save(final Callback<Boolean> callback) {
        final Buffer stream = this._graph.newBuffer();
        boolean isFirst = true;
        while (_dirtiesStack.size() != 0) {
            int tail = (int) _dirtiesStack.dequeueTail();
            Chunk loopChunk = _chunkValues.get(tail);
            //Save chunk Key
            if (isFirst) {
                isFirst = false;
            } else {
                stream.write(Constants.BUFFER_SEP);
            }
            KeyHelper.keyToBuffer(stream, _chunkTypes.get(tail), _chunkWorlds.get(tail), _chunkTimes.get(tail), _chunkIds.get(tail));
            //Save chunk payload
            stream.write(Constants.BUFFER_SEP);
            try {
                loopChunk.save(stream);
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
    public final synchronized void saveSilent(Callback<Buffer> callback) {
        final Buffer stream = this._graph.newBuffer();
        boolean isFirst = true;
        while (_dirtiesStack.size() != 0) {
            int tail = (int) _dirtiesStack.dequeueTail();
            Chunk loopChunk = _chunkValues.get(tail);
            //Save chunk Key
            if (isFirst) {
                isFirst = false;
            } else {
                stream.write(Constants.BUFFER_SEP);
            }
            KeyHelper.keyToBuffer(stream, _chunkTypes.get(tail), _chunkWorlds.get(tail), _chunkTimes.get(tail), _chunkIds.get(tail));
            //Save chunk payload
            stream.write(Constants.BUFFER_SEP);
            try {
                loopChunk.save(stream);
                unmark(tail);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //shrink in case of i != full size
        this.graph().storage().putSilent(stream, new Callback<Buffer>() {
            @Override
            public void on(Buffer result) {
                //free all value
                stream.free();
                if (callback != null) {
                    callback.on(result);
                }
            }
        });
    }
*/

    @Override
    public final void clear() {
        //TODO reset everything
    }

    @Override
    public final void freeAll() {
        //TODO reset everything
    }

    @Override
    public final long available() {
        return _lru.size();
    }

    @Override
    public final long capacity() {
        return _maxEntries;
    }

    @Override
    public final long dirties() {
        return _dirtiesStack.size();
    }

    @Override
    public EStructArray newVolatileGraph() {
        return new HeapEStructArray(null, null, _graph);
    }

    @Override
    public Interceptor[] interceptors() {
        return _interceptors;
    }

    @Override
    public void addInterceptorFirst(Interceptor it) {
        if (_interceptors == null) {
            _interceptors = new Interceptor[1];
            _interceptors[0] = it;
        } else {
            Interceptor[] interceptors2 = new Interceptor[_interceptors.length + 1];
            System.arraycopy(_interceptors, 0, _interceptors, 1, _interceptors.length);
            interceptors2[0] = it;
            _interceptors = interceptors2;
        }
    }

    @Override
    public void addInterceptorLast(Interceptor it) {
        if (_interceptors == null) {
            _interceptors = new Interceptor[1];
            _interceptors[0] = it;
        } else {
            Interceptor[] interceptors2 = new Interceptor[_interceptors.length + 1];
            System.arraycopy(_interceptors, 0, _interceptors, 0, _interceptors.length);
            interceptors2[_interceptors.length] = it;
            _interceptors = interceptors2;
        }
    }

    public final void printMarked() {
        for (int i = 0; i < _chunkValues.length(); i++) {
            if (_chunkValues.get(i) != null) {
                if (_chunkMarks.get(i) != 0) {
                    switch (_chunkTypes.get(i)) {
                        case ChunkType.STATE_CHUNK:
                            System.out.println("STATE\t\t\t(" + _chunkWorlds.get(i) + "," + _chunkTimes.get(i) + "," + _chunkIds.get(i) + ")\t->marks->" + _chunkMarks.get(i));
                            break;
                        case ChunkType.SUPER_TIME_TREE_CHUNK:
                            System.out.println("SUPER_TIME_TREE\t(" + _chunkWorlds.get(i) + "," + _chunkTimes.get(i) + "," + _chunkIds.get(i) + ")\t->marks->" + _chunkMarks.get(i));
                            break;
                        case ChunkType.TIME_TREE_CHUNK:
                            System.out.println("TIME_TREE\t\t(" + _chunkWorlds.get(i) + "," + _chunkTimes.get(i) + "," + _chunkIds.get(i) + ")\t->marks->" + _chunkMarks.get(i));
                            break;
                        case ChunkType.WORLD_ORDER_CHUNK: {
                            String type = "";
                            long nodeTypeValue = ((WorldOrderChunk) _chunkValues.get(i)).type();
                            if (nodeTypeValue != Constants.NULL_LONG) {
                                if (nodeTypeValue == -1) {
                                    type = " nodeType: BaseNode";
                                } else {
                                    NodeDeclaration nd = graph().nodeRegistry().declarationByHash((int) nodeTypeValue);
                                    if (nd != null) {
                                        type += " nodeType: ";
                                        type += nd.name();
                                    } else {
                                        type += " nodeType: " + nodeTypeValue;
                                    }
                                }
                            } else {
                                type = " nodeType: BaseNode";
                            }
                            System.out.println("WORLD_ORDER\t\t(" + _chunkWorlds.get(i) + "," + _chunkTimes.get(i) + "," + _chunkIds.get(i) + ")\t->marks->" + _chunkMarks.get(i) + type);
                        }
                        break;
                        case ChunkType.GEN_CHUNK:
                            System.out.println("GENERATOR\t\t(" + _chunkWorlds.get(i) + "," + _chunkTimes.get(i) + "," + _chunkIds.get(i) + ")\t->marks->" + _chunkMarks.get(i));
                            break;
                    }
                }
            }
        }
    }

    @Override
    public long clean(int percent) {
        final long nb_to_clean = this.cache_size * percent / 100;
        long i = 0;
        long cleaned = 0;
        int offset;
        int m;
        while (i < this._lru.size() && cleaned < nb_to_clean) {
            offset = (int) this._lru.dequeueTail();
            if (offset == -1) {
                break;
            }
            this._lru.enqueue(offset);
            if (_chunkValues.get(offset) != null) {

                final long victimWorld = _chunkWorlds.get(offset);
                final long victimTime = _chunkTimes.get(offset);
                final long victimObj = _chunkIds.get(offset);
                final byte victimType = _chunkTypes.get(offset);
                final int indexVictim;
                if (_deep_priority) {
                    indexVictim = (int) HashHelper.tripleHash(victimType, victimWorld, victimTime, victimObj, this._hashEntries);
                } else {
                    indexVictim = (int) HashHelper.simpleTripleHash(victimType, victimWorld, victimTime, victimObj, this._hashEntries);
                }
                m = _hash.get(indexVictim);
                int last = -1;
                while (m >= 0) {
                    if (victimType == _chunkTypes.get(m) && victimWorld == _chunkWorlds.get(m) && victimTime == _chunkTimes.get(m) && victimObj == _chunkIds.get(m)) {
                        break;
                    }
                    last = m;
                    m = _hashNext.get(m);
                }
                //POP THE VALUE FROM THE NEXT LIST
                if (last == -1) {
                    int previousNext = _hashNext.get(m);
                    _hash.set(indexVictim, previousNext);
                } else {
                    if (m == -1) {
                        _hashNext.set(last, -1);
                    } else {
                        _hashNext.set(last, _hashNext.get(m));
                    }
                }
                if (m != -1) {
                    _hashNext.set(m, -1);
                }

                _chunkValues.set(offset, null);
                cache_size--;
                cleaned++;
            }
            i++;
        }
        return cleaned;
    }

    @Override
    public long cacheSize() {
        return cache_size;
    }

}



