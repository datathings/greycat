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
import greycat.Container;
import greycat.Graph;
import greycat.Type;
import greycat.chunk.ChunkType;
import greycat.chunk.StateChunk;
import greycat.internal.CoreConstants;
import greycat.internal.tree.KDTree;
import greycat.internal.tree.NDTree;
import greycat.memory.primary.POffHeapDoubleArray;
import greycat.memory.primary.POffHeapIntArray;
import greycat.memory.primary.POffHeapLongArray;
import greycat.memory.primary.POffHeapString;
import greycat.plugin.NodeStateCallback;
import greycat.struct.Buffer;
import greycat.utility.Base64;

class OffHeapStateChunk implements StateChunk, OffHeapContainer {

    private final OffHeapChunkSpace space;
    private final long index;

    private static final int DIRTY = 0;
    private static final int SIZE = 1;
    private static final int CAPACITY = 2;
    private static final int SUBHASH = 3;

    private static final int OFFSET = 4;
    private static final int ELEM_SIZE = 3;

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

    OffHeapStateChunk(final OffHeapChunkSpace p_space, final long p_index) {
        index = p_index;
        space = p_space;
    }

    Graph graph() {
        return space.graph();
    }

    @Override
    public final void lock() {
        space.lockByIndex(index);
    }

    @Override
    public final void unlock() {
        space.unlockByIndex(index);
    }

    @Override
    public final long addrByIndex(long elemIndex) {
        return POffHeapLongArray.get(space.addrByIndex(index), OFFSET + (elemIndex * ELEM_SIZE) + 2);
    }

    @Override
    public void setAddrByIndex(long elemIndex, long newAddr) {
        POffHeapLongArray.set(space.addrByIndex(index), OFFSET + (elemIndex * ELEM_SIZE) + 2, newAddr);
    }

    private static long key(final long addr, final long index) {
        return POffHeapLongArray.get(addr, OFFSET + (index * ELEM_SIZE));
    }

    private static void setKey(final long addr, final long index, final long insertKey) {
        POffHeapLongArray.set(addr, OFFSET + (index * ELEM_SIZE), insertKey);
    }

    private static byte type(final long addr, final long index) {
        return (byte) POffHeapLongArray.get(addr, OFFSET + (index * ELEM_SIZE) + 1);
    }

    private static void setType(final long addr, final long index, final byte insertType) {
        POffHeapLongArray.set(addr, OFFSET + (index * ELEM_SIZE) + 1, insertType);
    }

    private static long value(final long addr, final long index) {
        return POffHeapLongArray.get(addr, OFFSET + (index * ELEM_SIZE) + 2);
    }

    private static void setValue(final long addr, final long index, final long insertValue) {
        POffHeapLongArray.set(addr, OFFSET + (index * ELEM_SIZE) + 2, insertValue);
    }

    private static double doubleValue(final long addr, final long index) {
        return POffHeapDoubleArray.get(addr, OFFSET + (index * ELEM_SIZE) + 2);
    }

    private static void setDoubleValue(final long addr, final long index, final double insertValue) {
        POffHeapDoubleArray.set(addr, OFFSET + (index * ELEM_SIZE) + 2, insertValue);
    }

    private static long next(final long hashAddr, final long index) {
        return POffHeapLongArray.get(hashAddr, index);
    }

    private static void setNext(final long hashAddr, final long index, final long insertNext) {
        POffHeapLongArray.set(hashAddr, index, insertNext);
    }

    private static long hash(final long hashAddr, final long capacity, final long index) {
        return POffHeapLongArray.get(hashAddr, capacity + index);
    }

    private static void setHash(final long hashAddr, final long capacity, final long index, final long insertHash) {
        POffHeapLongArray.set(hashAddr, capacity + index, insertHash);
    }

    @Override
    public final byte chunkType() {
        return ChunkType.STATE_CHUNK;
    }

    @Override
    public final long index() {
        return index;
    }

    @Override
    public final Object getAt(final int p_key) {
        Object result = null;
        lock();
        try {
            final long addr = space.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final long foundIndex = internal_find(addr, p_key);
                result = internal_get(addr, foundIndex);
            }
        } finally {
            unlock();
        }
        return result;
    }

    @Override
    public Object getRawAt(int p_key) {
        return getAt(p_key);
    }

    @Override
    public Object getTypedRawAt(int p_key, byte type) {
        Object result = null;
        lock();
        try {
            final long addr = space.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final long foundIndex = internal_find(addr, p_key);
                if (foundIndex != OffHeapConstants.NULL_PTR && type(addr, foundIndex) == type) {
                    result = internal_get(addr, foundIndex);
                }
            }
        } finally {
            unlock();
        }
        return result;
    }

    @Override
    public final Object get(final String key) {
        return getAt(space.graph().resolver().stringToHash(key, false));
    }

    @Override
    public final void each(final NodeStateCallback callBack) {
        // lock();
        //  try {
        final long addr = space.addrByIndex(index);
        if (addr != OffHeapConstants.NULL_PTR) {
            final long size = POffHeapLongArray.get(addr, SIZE);
            for (int i = 0; i < size; i++) {
                Object resolved = internal_get(addr, i);
                if (resolved != null) {
                    callBack.on((int) key(addr, i), type(addr, i), resolved);
                }
            }
        }
        // } finally {
        //unlock();
        //  }
    }

    private Object internal_get(final long addr, final long index) {
        if (index >= 0) {
            final byte elemType = type(addr, index);
            final long rawValue = value(addr, index);
            switch (type(addr, index)) {
                case Type.BOOL:
                    return rawValue == 1;
                case Type.DOUBLE:
                    return doubleValue(addr, index);
                case Type.LONG:
                    return rawValue;
                case Type.INT:
                    return (int) rawValue;
                case Type.STRING:
                    return POffHeapString.asObject(rawValue);
                case Type.LONG_ARRAY:
                    return new OffHeapLongArray(this, index);
                case Type.DOUBLE_ARRAY:
                    return new OffHeapDoubleArray(this, index);
                case Type.INT_ARRAY:
                    return new OffHeapIntArray(this, index);
                case Type.STRING_ARRAY:
                    return new OffHeapStringArray(this, index);
                case Type.RELATION:
                    return new OffHeapRelation(this, index);
                case Type.DMATRIX:
                    return new OffHeapDMatrix(this, index);
                case Type.LMATRIX:
                    return new OffHeapLMatrix(this, index);
                case Type.STRING_TO_INT_MAP:
                    return new OffHeapStringIntMap(this, index);
                case Type.LONG_TO_LONG_MAP:
                    return new OffHeapLongLongMap(this, index);
                case Type.LONG_TO_LONG_ARRAY_MAP:
                    return new OffHeapLongLongArrayMap(this, index);
                case Type.RELATION_INDEXED:
                    return new OffHeapRelationIndexed(this, index, space.graph());
                case Type.NDTREE:
                    return new NDTree(new OffHeapEGraph(this, index, space.graph()));
                case Type.KDTREE:
                    return new KDTree(new OffHeapEGraph(this, index, space.graph()));
                case Type.EGRAPH:
                    return new OffHeapEGraph(this, index, space.graph());
                case OffHeapConstants.NULL_PTR:
                    return null;
                default:
                    throw new RuntimeException("Should never happen " + elemType);
            }
        }
        return null;
    }

    private long internal_find(final long addr, final long requestKey) {
        final long size = POffHeapLongArray.get(addr, SIZE);
        final long subhash_ptr = POffHeapLongArray.get(addr, SUBHASH);
        if (size == 0) {
            return -1;
        } else if (subhash_ptr == OffHeapConstants.NULL_PTR) {
            for (int i = 0; i < size; i++) {
                if (key(addr, i) == requestKey) {
                    return i;
                }
            }
            return -1;
        } else {
            final long capacity = POffHeapLongArray.get(addr, CAPACITY);
            long hashIndex = requestKey % (capacity * 2);
            if (hashIndex < 0) {
                hashIndex = hashIndex * -1;
            }
            long m = hash(subhash_ptr, capacity, hashIndex);
            while (m >= 0) {
                if (requestKey == key(addr, m)) {
                    return m;
                } else {
                    m = next(subhash_ptr, m);
                }
            }
            return -1;
        }
    }

    @Override
    public final Object getOrCreateAt(final int requestKey, final byte requestType) {
        Object result;
        lock();
        try {
            long addr = space.addrByIndex(index);
            long foundIndex = OffHeapConstants.NULL_PTR;
            if (addr != OffHeapConstants.NULL_PTR) {
                foundIndex = internal_find(addr, requestKey);
            }
            if (foundIndex == OffHeapConstants.NULL_PTR || type(addr, foundIndex) != requestType) {
                foundIndex = internal_set(requestKey, requestType, OffHeapConstants.NULL_PTR, true, false);
                addr = space.addrByIndex(index);
            }
            result = internal_get(addr, foundIndex);
        } finally {
            unlock();
        }
        return result;
    }

    @Override
    public final Object getOrCreate(final String key, final byte elemType) {
        return getOrCreateAt(space.graph().resolver().stringToHash(key, true), elemType);
    }

    @Override
    public final Container setAt(final int p_elementIndex, final byte p_elemType, final Object p_unsafe_elem) {
        if (p_elemType == Type.LONG_TO_LONG_MAP
                || p_elemType == Type.LONG_TO_LONG_ARRAY_MAP
                || p_elemType == Type.STRING_TO_INT_MAP
                || p_elemType == Type.RELATION
                || p_elemType == Type.RELATION_INDEXED
                || p_elemType == Type.DMATRIX
                || p_elemType == Type.LMATRIX
                || p_elemType == Type.LONG_ARRAY
                || p_elemType == Type.DOUBLE_ARRAY
                || p_elemType == Type.INT_ARRAY
                || p_elemType == Type.STRING_ARRAY) {
            throw new RuntimeException("Bad API usage ! Set are forbidden for Maps and Relationship , please use getOrCreate instead");
        }
        lock();
        try {
            internal_set(p_elementIndex, p_elemType, p_unsafe_elem, true, false);
        } finally {
            unlock();
        }
        return this;
    }

    @Override
    public Container remove(String name) {
        return removeAt(space.graph().resolver().stringToHash(name, false));
    }

    @Override
    public Container removeAt(int index) {
        return setAt(index, Type.INT, null);
    }

    @Override
    public final Container set(final String key, final byte p_elemType, final Object p_unsafe_elem) {
        setAt(space.graph().resolver().stringToHash(key, true), p_elemType, p_unsafe_elem);
        return this;
    }

    @Override
    public final <A> A getWithDefault(final String key, final A defaultValue) {
        final Object result = get(key);
        if (result == null) {
            return defaultValue;
        } else {
            return (A) result;
        }
    }

    @Override
    public <A> A getAtWithDefault(int key, A defaultValue) {
        final Object result = getAt(key);
        if (result == null) {
            return defaultValue;
        } else {
            return (A) result;
        }
    }

    @Override
    public final Container rephase() {
        return this;
    }

    @Override
    public final byte typeAt(final int p_key) {
        byte result = (byte) -1;
        lock();
        try {
            final long addr = space.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final long index = internal_find(addr, p_key);
                if (index != OffHeapConstants.NULL_PTR) {
                    result = type(addr, index);
                }
            }
        } finally {
            unlock();
        }
        return result;
    }

    @Override
    public byte type(final String key) {
        return typeAt(space.graph().resolver().stringToHash(key, false));
    }

    @Override
    public final void declareDirty() {
        final long addr = space.addrByIndex(index);
        if (POffHeapLongArray.get(addr, DIRTY) != 1) {
            POffHeapLongArray.set(addr, DIRTY, 1);
            space.notifyUpdate(index);
        }
    }

    @Override
    public final void save(final Buffer buffer) {
        lock();
        try {
            final long addr = space.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                long size = POffHeapLongArray.get(addr, SIZE);
                Base64.encodeLongToBuffer(size, buffer);
                for (int i = 0; i < size; i++) {
                    buffer.write(Constants.CHUNK_SEP);
                    final byte type = type(addr, i);
                    Base64.encodeIntToBuffer((int) type, buffer);
                    buffer.write(Constants.CHUNK_SEP);
                    Base64.encodeLongToBuffer(key(addr, i), buffer);
                    buffer.write(Constants.CHUNK_SEP);
                    final long rawValue = value(addr, i);
                    switch (type) {
                        case Type.STRING:
                            POffHeapString.save(rawValue, buffer);
                            break;
                        case Type.BOOL:
                            if (rawValue == 1) {
                                Base64.encodeIntToBuffer(CoreConstants.BOOL_TRUE, buffer);
                            } else {
                                Base64.encodeIntToBuffer(CoreConstants.BOOL_FALSE, buffer);
                            }
                            break;
                        case Type.LONG:
                            Base64.encodeLongToBuffer(rawValue, buffer);
                            break;
                        case Type.DOUBLE:
                            Base64.encodeDoubleToBuffer(doubleValue(addr, i), buffer);
                            break;
                        case Type.INT:
                            Base64.encodeIntToBuffer((int) rawValue, buffer);
                            break;
                        case Type.DOUBLE_ARRAY:
                            OffHeapDoubleArray.save(rawValue, buffer);
                            break;
                        case Type.LONG_ARRAY:
                            OffHeapLongArray.save(rawValue, buffer);
                            break;
                        case Type.INT_ARRAY:
                            OffHeapIntArray.save(rawValue, buffer);
                            break;
                        case Type.STRING_ARRAY:
                            OffHeapStringArray.save(rawValue, buffer);
                            break;
                        case Type.RELATION:
                            OffHeapRelation.save(rawValue, buffer);
                            break;
                        case Type.DMATRIX:
                            OffHeapDMatrix.save(rawValue, buffer);
                            break;
                        case Type.LMATRIX:
                            OffHeapLMatrix.save(rawValue, buffer);
                            break;
                        case Type.STRING_TO_INT_MAP:
                            OffHeapStringIntMap.save(rawValue, buffer);
                            break;
                        case Type.LONG_TO_LONG_MAP:
                            OffHeapLongLongMap.save(rawValue, buffer);
                            break;
                        case Type.RELATION_INDEXED:
                        case Type.LONG_TO_LONG_ARRAY_MAP:
                            OffHeapLongLongArrayMap.save(rawValue, buffer);
                            break;
                        case Type.NDTREE:
                        case Type.KDTREE:
                        case Type.EGRAPH:
                            OffHeapEGraph castedEGraph = new OffHeapEGraph(this, i, space.graph());
                            int eGSize = castedEGraph.size();
                            Base64.encodeIntToBuffer(eGSize, buffer);
                            for (int j = 0; j < eGSize; j++) {
                                OffHeapENode eNode = new OffHeapENode(j, castedEGraph, space.graph());
                                buffer.write(CoreConstants.CHUNK_ENODE_SEP);
                                eNode.save(buffer);
                            }
                            castedEGraph.declareUnDirty();
                            break;
                        default:
                            break;
                    }
                }
            } else {
                //we save a empty chunk
                Base64.encodeLongToBuffer(0, buffer);
            }
        } finally {
            unlock();
        }
    }

    @Override
    public void saveDiff(Buffer buffer) {
        save(buffer);
    }

    @Override
    public void loadFrom(final StateChunk origin) {
        if (origin == null) {
            return;
        }
        lock();
        try {
            OffHeapStateChunk casted = (OffHeapStateChunk) origin;
            casted.lock();
            try {
                //clean previous if exist
                long addr = space.addrByIndex(index);
                if (addr != OffHeapConstants.NULL_PTR) {
                    free(addr, space);
                }
                //retrieve to clone address
                final long castedAddr = space.addrByIndex(casted.index);

                //nothing set yet, don't clone
                if (castedAddr == OffHeapConstants.NULL_PTR) {
                    space.setAddrByIndex(index, OffHeapConstants.NULL_PTR);
                } else {
                    final long castedCapacity = POffHeapLongArray.get(castedAddr, CAPACITY);
                    final long castedSize = POffHeapLongArray.get(castedAddr, SIZE);
                    final long castedSubHash = POffHeapLongArray.get(castedAddr, SUBHASH);
                    addr = POffHeapLongArray.cloneArray(castedAddr, OFFSET + (castedCapacity * ELEM_SIZE));
                    //clone sub hash if needed
                    if (castedSubHash != OffHeapConstants.NULL_PTR) {
                        POffHeapLongArray.set(addr, SUBHASH, POffHeapLongArray.cloneArray(castedSubHash, castedCapacity * 3));
                    }
                    //clone complex structures
                    //TODO optimze with a flag to avoid this iteration
                    for (int i = 0; i < castedSize; i++) {
                        switch (type(castedAddr, i)) {
                            case Type.LONG_ARRAY:
                                OffHeapLongArray.clone(value(castedAddr, i));
                                break;
                            case Type.DOUBLE_ARRAY:
                                OffHeapDoubleArray.clone(value(castedAddr, i));
                                break;
                            case Type.INT_ARRAY:
                                OffHeapIntArray.clone(value(castedAddr, i));
                                break;
                            case Type.STRING_ARRAY:
                                OffHeapStringArray.clone(value(castedAddr, i));
                                break;
                            case Type.STRING:
                                POffHeapString.clone(value(castedAddr, i));
                                break;
                            case Type.RELATION:
                                setValue(addr, i, OffHeapRelation.clone(value(castedAddr, i)));
                                break;
                            case Type.DMATRIX:
                                setValue(addr, i, OffHeapDMatrix.clone(value(castedAddr, i)));
                                break;
                            case Type.LMATRIX:
                                setValue(addr, i, OffHeapLMatrix.clone(value(castedAddr, i)));
                                break;
                            case Type.LONG_TO_LONG_MAP:
                                setValue(addr, i, OffHeapLongLongMap.clone(value(castedAddr, i)));
                                break;
                            case Type.RELATION_INDEXED:
                            case Type.LONG_TO_LONG_ARRAY_MAP:
                                setValue(addr, i, OffHeapLongLongArrayMap.clone(value(castedAddr, i)));
                                break;
                            case Type.STRING_TO_INT_MAP:
                                setValue(addr, i, OffHeapStringIntMap.clone(value(castedAddr, i)));
                                break;
                            case Type.KDTREE:
                            case Type.NDTREE:
                            case Type.EGRAPH:
                                setValue(addr, i, OffHeapEGraph.clone(value(castedAddr, i)));
                                break;
                        }
                    }
                    space.setAddrByIndex(index, addr);
                }
            } finally {
                casted.unlock();
            }
        } finally {
            unlock();
        }
    }

    private long toAddr(final byte p_type, final Object p_unsafe_elem) {
        long param_elem = -1;
        if (p_unsafe_elem != null) {
            try {
                switch (p_type) {
                    case Type.BOOL:
                        param_elem = ((boolean) p_unsafe_elem) ? 1 : 0;
                        break;
                    case Type.LONG:
                        if (p_unsafe_elem instanceof Long) {
                            param_elem = (long) p_unsafe_elem;
                        } else if (p_unsafe_elem instanceof Double) {
                            param_elem = (long) (double) p_unsafe_elem;
                        } else if (p_unsafe_elem instanceof Float) {
                            param_elem = (long) (float) p_unsafe_elem;
                        } else if (p_unsafe_elem instanceof Integer) {
                            param_elem = (long) (int) p_unsafe_elem;
                        } else if (p_unsafe_elem instanceof Byte) {
                            param_elem = (long) (byte) p_unsafe_elem;
                        } else {
                            param_elem = (long) p_unsafe_elem;
                        }
                        break;
                    case Type.INT:
                        if (p_unsafe_elem instanceof Integer) {
                            param_elem = (int) p_unsafe_elem;
                        } else if (p_unsafe_elem instanceof Double) {
                            param_elem = (int) (double) p_unsafe_elem;
                        } else if (p_unsafe_elem instanceof Float) {
                            param_elem = (int) (float) p_unsafe_elem;
                        } else if (p_unsafe_elem instanceof Long) {
                            param_elem = (int) (long) p_unsafe_elem;
                        } else if (p_unsafe_elem instanceof Byte) {
                            param_elem = (int) (byte) p_unsafe_elem;
                        } else {
                            param_elem = (int) p_unsafe_elem;
                        }
                        break;
                    case Type.STRING:
                        param_elem = POffHeapString.fromObject((String) p_unsafe_elem);
                        break;
                    case Type.LONG_ARRAY:
                    case Type.DOUBLE_ARRAY:
                    case Type.INT_ARRAY:
                    case Type.STRING_ARRAY:
                    case Type.RELATION:
                    case Type.DMATRIX:
                    case Type.LMATRIX:
                    case Type.STRING_TO_INT_MAP:
                    case Type.LONG_TO_LONG_MAP:
                    case Type.RELATION_INDEXED:
                    case Type.LONG_TO_LONG_ARRAY_MAP:
                    case Type.NDTREE:
                    case Type.KDTREE:
                    case Type.EGRAPH:
                        param_elem = OffHeapConstants.NULL_PTR; //empty initial ptr
                        break;
                    default:
                        throw new RuntimeException("Internal Exception, unknown type");
                }
            } catch (Exception e) {
                throw new RuntimeException("mwDB usage error, set method called with type " + Type.typeName(p_type) + " while param object is " + p_unsafe_elem);
            }
        }
        return param_elem;
    }

    private double toDoubleValue(final Object p_unsafe_elem) {
        if (p_unsafe_elem instanceof Double) {
            return (double) p_unsafe_elem;
        } else if (p_unsafe_elem instanceof Integer) {
            return (double) (int) p_unsafe_elem;
        } else if (p_unsafe_elem instanceof Float) {
            return (double) (float) p_unsafe_elem;
        } else if (p_unsafe_elem instanceof Long) {
            return (double) (long) p_unsafe_elem;
        } else if (p_unsafe_elem instanceof Byte) {
            return (double) (byte) p_unsafe_elem;
        }
        return (double) p_unsafe_elem;
    }

    private long internal_set(final long p_key, final byte p_type, final Object p_unsafe_elem, boolean replaceIfPresent, boolean initial) {
        long addr = space.addrByIndex(index);
        if (addr == OffHeapConstants.NULL_PTR) {
            addr = allocate(addr, Constants.MAP_INITIAL_CAPACITY);
        }

        long entry = -1;
        long prev_entry = -1;
        long hashIndex = -1;
        long size = POffHeapLongArray.get(addr, SIZE);
        long capacity = POffHeapLongArray.get(addr, CAPACITY);
        long subhash_ptr = POffHeapLongArray.get(addr, SUBHASH);
        if (subhash_ptr == OffHeapConstants.NULL_PTR) {
            for (int i = 0; i < size; i++) {
                if (key(addr, i) == p_key) {
                    entry = i;
                    break;
                }
            }
        } else {
            hashIndex = p_key % (capacity * 2);
            if (hashIndex < 0) {
                hashIndex = hashIndex * -1;
            }
            long m = hash(subhash_ptr, capacity, hashIndex);
            while (m != -1) {
                if (key(addr, m) == p_key) {
                    entry = m;
                    break;
                }
                prev_entry = m;
                m = next(subhash_ptr, m);
            }
        }
        //case already present
        if (entry != -1) {
            final byte found_type = type(addr, entry);
            if (replaceIfPresent || (p_type != found_type)) {
                if (p_unsafe_elem == null) {
                    /* Case: supression of a value */
                    //freeThePreviousValue
                    freeElement(value(addr, entry), found_type, space);
                    //then clean the acces chain
                    if (subhash_ptr != OffHeapConstants.NULL_PTR) {
                        //unHash previous
                        if (prev_entry != -1) {
                            setNext(subhash_ptr, prev_entry, next(subhash_ptr, entry));
                        } else {
                            setHash(subhash_ptr, capacity, hashIndex, -1);
                        }
                    }
                    long indexVictim = size - 1;
                    //just pop the last value
                    if (entry == indexVictim) {
                        setKey(addr, entry, OffHeapConstants.NULL_PTR);
                        setValue(addr, entry, OffHeapConstants.NULL_PTR);
                        setType(addr, entry, (byte) OffHeapConstants.NULL_PTR);
                    } else {
                        //we need to reHash the new last element at our place
                        setKey(addr, entry, key(addr, indexVictim));
                        final byte typeOfVictim = type(addr, indexVictim);
                        if (typeOfVictim == Type.DOUBLE) {
                            final double victimDoubleValue = doubleValue(addr, indexVictim);
                            setDoubleValue(addr, entry, victimDoubleValue);
                        } else {
                            setValue(addr, entry, value(addr, indexVictim));
                        }
                        setType(addr, entry, typeOfVictim);
                        if (subhash_ptr != OffHeapConstants.NULL_PTR) {
                            setNext(addr, entry, next(subhash_ptr, indexVictim));
                            long victimHash = key(addr, entry) % (capacity * 2);
                            if (victimHash < 0) {
                                victimHash = victimHash * -1;
                            }
                            long m = hash(subhash_ptr, capacity, victimHash);
                            if (m == indexVictim) {
                                //the victim was the head of hashing list
                                setHash(subhash_ptr, capacity, victimHash, entry);
                            } else {
                                //the victim is in the next, rechain it
                                while (m != -1) {
                                    if (next(addr, m) == indexVictim) {
                                        setNext(subhash_ptr, m, entry);
                                        break;
                                    }
                                    m = next(subhash_ptr, m);
                                }
                            }
                        }
                        setKey(addr, indexVictim, OffHeapConstants.NULL_PTR);
                        freeElement(value(addr, indexVictim), type(addr, indexVictim), space);
                        setValue(addr, indexVictim, OffHeapConstants.NULL_PTR);
                        setType(addr, indexVictim, (byte) OffHeapConstants.NULL_PTR);
                    }
                    POffHeapLongArray.set(addr, SIZE, size - 1);
                } else {
                    final long previous_value = value(addr, entry);
                    //freeThePreviousValue
                    if (p_type == Type.DOUBLE) {
                        setDoubleValue(addr, entry, toDoubleValue(p_unsafe_elem));
                    } else {
                        setValue(addr, entry, toAddr(p_type, p_unsafe_elem));
                    }
                    freeElement(previous_value, found_type, space);
                    if (found_type != p_type) {
                        setType(addr, entry, p_type);
                    }
                }
            }
            if (!initial) {
                declareDirty();
            }
            return entry;
        }
        if (size >= capacity) {
            long newCapacity = capacity * 2;
            addr = allocate(addr, newCapacity);
            subhash_ptr = POffHeapLongArray.get(addr, SUBHASH);
            capacity = newCapacity;
            hashIndex = p_key % (capacity * 2);
            if (hashIndex < 0) {
                hashIndex = hashIndex * -1;
            }
        }
        final long insert_index = size;
        setKey(addr, insert_index, p_key);
        if (p_type == Type.DOUBLE) {
            setDoubleValue(addr, insert_index, toDoubleValue(p_unsafe_elem));
        } else {
            setValue(addr, insert_index, toAddr(p_type, p_unsafe_elem));
        }
        setType(addr, insert_index, p_type);
        if (subhash_ptr != OffHeapConstants.NULL_PTR) {
            setNext(subhash_ptr, insert_index, hash(subhash_ptr, capacity, hashIndex));
            setHash(subhash_ptr, capacity, hashIndex, insert_index);
        }
        size++;
        POffHeapLongArray.set(addr, SIZE, size);
        if (!initial) {
            declareDirty();
        }
        return insert_index;
    }

    private long allocate(final long addr, final long newCapacity) {
        if (addr == OffHeapConstants.NULL_PTR) {
            //nothing before, initial allocation...
            final long new_addr = POffHeapLongArray.allocate(OFFSET + (newCapacity * ELEM_SIZE));
            space.setAddrByIndex(index, new_addr);
            POffHeapLongArray.set(new_addr, CAPACITY, newCapacity);
            POffHeapLongArray.set(new_addr, DIRTY, 0);
            POffHeapLongArray.set(new_addr, SIZE, 0);
            if (newCapacity > Constants.MAP_INITIAL_CAPACITY) {
                POffHeapLongArray.set(new_addr, SUBHASH, POffHeapLongArray.allocate(newCapacity * 3));
            } else {
                POffHeapLongArray.set(new_addr, SUBHASH, OffHeapConstants.NULL_PTR);
            }
            return new_addr;
        } else {
            //reallocation or overallocation
            final long new_addr = POffHeapLongArray.reallocate(addr, OFFSET + (newCapacity * ELEM_SIZE));
            space.setAddrByIndex(index, new_addr);
            POffHeapLongArray.set(new_addr, CAPACITY, newCapacity);
            long subHash_ptr = POffHeapLongArray.get(new_addr, SUBHASH);
            if (subHash_ptr == OffHeapConstants.NULL_PTR) {
                subHash_ptr = POffHeapLongArray.allocate(newCapacity * 3);
            } else {
                subHash_ptr = POffHeapLongArray.reallocate(subHash_ptr, newCapacity * 3);
                POffHeapLongArray.reset(subHash_ptr, newCapacity * 3);
            }
            POffHeapLongArray.set(new_addr, SUBHASH, subHash_ptr);
            //reHash
            final long size = POffHeapLongArray.get(new_addr, SIZE);
            final long hash_capacity = newCapacity * 2;
            for (long i = 0; i < size; i++) {
                long keyHash = key(new_addr, i) % hash_capacity;
                if (keyHash < 0) {
                    keyHash = keyHash * -1;
                }
                setNext(subHash_ptr, i, hash(subHash_ptr, newCapacity, keyHash));
                setHash(subHash_ptr, newCapacity, keyHash, i);
            }
            return new_addr;
        }
    }

    private static final byte LOAD_WAITING_ALLOC = 0;
    private static final byte LOAD_WAITING_TYPE = 1;
    private static final byte LOAD_WAITING_KEY = 2;
    private static final byte LOAD_WAITING_VALUE = 3;

    @Override
    public final void load(final Buffer buffer) {
        if (buffer != null && buffer.length() > 0) {
            lock();
            try {
                long addr = space.addrByIndex(index);
                final boolean initial = (addr == OffHeapConstants.NULL_PTR);
                long capacity = 0;
                if (addr != OffHeapConstants.NULL_PTR) {
                    capacity = POffHeapLongArray.get(addr, CAPACITY);
                }
                final long payloadSize = buffer.length();
                long previous = 0;
                long cursor = 0;
                byte state = LOAD_WAITING_ALLOC;
                byte read_type = -1;
                long read_key = -1;
                while (cursor < payloadSize) {
                    byte current = buffer.read(cursor);
                    if (current == Constants.CHUNK_SEP) {
                        switch (state) {
                            case LOAD_WAITING_ALLOC:
                                final int stateChunkSize = Base64.decodeToIntWithBounds(buffer, 0, cursor);
                                final int closePowerOfTwo = (int) Math.pow(2, Math.ceil(Math.log(stateChunkSize) / Math.log(2)));
                                if (capacity < closePowerOfTwo) {
                                    addr = allocate(addr, closePowerOfTwo);
                                    capacity = closePowerOfTwo;
                                }
                                state = LOAD_WAITING_TYPE;
                                cursor++;
                                previous = cursor;
                                break;
                            case LOAD_WAITING_TYPE:
                                read_type = (byte) Base64.decodeToIntWithBounds(buffer, previous, cursor);
                                state = LOAD_WAITING_KEY;
                                cursor++;
                                previous = cursor;
                                break;
                            case LOAD_WAITING_KEY:
                                read_key = Base64.decodeToLongWithBounds(buffer, previous, cursor);
                                //primitive default loader
                                switch (read_type) {
                                    //primitive types
                                    case Type.BOOL:
                                    case Type.INT:
                                    case Type.DOUBLE:
                                    case Type.LONG:
                                    case Type.STRING:
                                        state = LOAD_WAITING_VALUE;
                                        cursor++;
                                        previous = cursor;
                                        break;
                                    case Type.LONG_ARRAY:
                                        OffHeapLongArray longArray = new OffHeapLongArray(this, internal_set(read_key, read_type, null, true, initial));
                                        cursor++;
                                        cursor = longArray.load(buffer, cursor, payloadSize);
                                        if (cursor < payloadSize) {
                                            current = buffer.read(cursor);
                                            if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                                state = LOAD_WAITING_TYPE;
                                                cursor++;
                                                previous = cursor;
                                            }
                                        }
                                        break;
                                    case Type.DOUBLE_ARRAY:
                                        OffHeapDoubleArray doubleArray = new OffHeapDoubleArray(this, internal_set(read_key, read_type, null, true, initial));
                                        cursor++;
                                        cursor = doubleArray.load(buffer, cursor, payloadSize);
                                        if (cursor < payloadSize) {
                                            current = buffer.read(cursor);
                                            if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                                state = LOAD_WAITING_TYPE;
                                                cursor++;
                                                previous = cursor;
                                            }
                                        }
                                        break;
                                    case Type.INT_ARRAY:
                                        OffHeapIntArray intArray = new OffHeapIntArray(this, internal_set(read_key, read_type, null, true, initial));
                                        cursor++;
                                        cursor = intArray.load(buffer, cursor, payloadSize);
                                        if (cursor < payloadSize) {
                                            current = buffer.read(cursor);
                                            if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                                state = LOAD_WAITING_TYPE;
                                                cursor++;
                                                previous = cursor;
                                            }
                                        }
                                        break;
                                    case Type.STRING_ARRAY:
                                        OffHeapStringArray stringArray = new OffHeapStringArray(this, internal_set(read_key, read_type, null, true, initial));
                                        cursor++;
                                        cursor = stringArray.load(buffer, cursor, payloadSize);
                                        if (cursor < payloadSize) {
                                            current = buffer.read(cursor);
                                            if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                                state = LOAD_WAITING_TYPE;
                                                cursor++;
                                                previous = cursor;
                                            }
                                        }
                                        break;
                                    case Type.RELATION:
                                        OffHeapRelation relation = new OffHeapRelation(this, internal_set(read_key, read_type, null, true, initial));
                                        cursor++;
                                        cursor = relation.load(buffer, cursor, payloadSize);
                                        if (cursor < payloadSize) {
                                            current = buffer.read(cursor);
                                            if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                                state = LOAD_WAITING_TYPE;
                                                cursor++;
                                                previous = cursor;
                                            }
                                        }
                                        break;
                                    case Type.DMATRIX:
                                        OffHeapDMatrix dmatrix = new OffHeapDMatrix(this, internal_set(read_key, read_type, null, true, initial));
                                        cursor++;
                                        cursor = dmatrix.load(buffer, cursor, payloadSize);
                                        if (cursor < payloadSize) {
                                            current = buffer.read(cursor);
                                            if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                                state = LOAD_WAITING_TYPE;
                                                cursor++;
                                                previous = cursor;
                                            }
                                        }
                                        break;
                                    case Type.LMATRIX:
                                        OffHeapLMatrix lmatrix = new OffHeapLMatrix(this, internal_set(read_key, read_type, null, true, initial));
                                        cursor++;
                                        cursor = lmatrix.load(buffer, cursor, payloadSize);
                                        if (cursor < payloadSize) {
                                            current = buffer.read(cursor);
                                            if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                                state = LOAD_WAITING_TYPE;
                                                cursor++;
                                                previous = cursor;
                                            }
                                        }
                                        break;
                                    case Type.LONG_TO_LONG_MAP:
                                        OffHeapLongLongMap l2lmap = new OffHeapLongLongMap(this, internal_set(read_key, read_type, null, true, initial));
                                        cursor++;
                                        cursor = l2lmap.load(buffer, cursor, payloadSize);
                                        if (cursor < payloadSize) {
                                            current = buffer.read(cursor);
                                            if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                                state = LOAD_WAITING_TYPE;
                                                cursor++;
                                                previous = cursor;
                                            }
                                        }
                                        break;
                                    case Type.LONG_TO_LONG_ARRAY_MAP:
                                        OffHeapLongLongArrayMap l2lrmap = new OffHeapLongLongArrayMap(this, internal_set(read_key, read_type, null, true, initial));
                                        cursor++;
                                        cursor = l2lrmap.load(buffer, cursor, payloadSize);
                                        if (cursor < payloadSize) {
                                            current = buffer.read(cursor);
                                            if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                                state = LOAD_WAITING_TYPE;
                                                cursor++;
                                                previous = cursor;
                                            }
                                        }
                                        break;
                                    case Type.RELATION_INDEXED:
                                        OffHeapRelationIndexed relationIndexed = new OffHeapRelationIndexed(this, internal_set(read_key, read_type, null, true, initial), space.graph());
                                        cursor++;
                                        cursor = relationIndexed.load(buffer, cursor, payloadSize);
                                        if (cursor < payloadSize) {
                                            current = buffer.read(cursor);
                                            if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                                state = LOAD_WAITING_TYPE;
                                                cursor++;
                                                previous = cursor;
                                            }
                                        }
                                        break;
                                    case Type.STRING_TO_INT_MAP:
                                        OffHeapStringIntMap s2lmap = new OffHeapStringIntMap(this, internal_set(read_key, read_type, null, true, initial));
                                        cursor++;
                                        cursor = s2lmap.load(buffer, cursor, payloadSize);
                                        if (cursor < payloadSize) {
                                            current = buffer.read(cursor);
                                            if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                                state = LOAD_WAITING_TYPE;
                                                cursor++;
                                                previous = cursor;
                                            }
                                        }
                                        break;
                                    case Type.NDTREE:
                                    case Type.KDTREE:
                                    case Type.EGRAPH:
                                        OffHeapEGraph eGraph = new OffHeapEGraph(this, internal_set(read_key, read_type, null, true, initial), this.graph());
                                        cursor++;
                                        cursor = eGraph.load(buffer, cursor, payloadSize);
                                        if (cursor < payloadSize) {
                                            current = buffer.read(cursor);
                                            if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                                state = LOAD_WAITING_TYPE;
                                                cursor++;
                                                previous = cursor;
                                            }
                                        }
                                        break;
                                    default:
                                        throw new RuntimeException("Not implemented yet!!!");
                                }
                                break;
                            case LOAD_WAITING_VALUE:
                                load_primitive(read_key, read_type, buffer, previous, cursor, initial);
                                state = LOAD_WAITING_TYPE;
                                cursor++;
                                previous = cursor;
                                break;
                        }
                    } else {
                        cursor++;
                    }
                }
                if (state == LOAD_WAITING_VALUE) {
                    load_primitive(read_key, read_type, buffer, previous, cursor, initial);
                }
            } finally {
                unlock();
            }
        }
    }

    private void load_primitive(final long read_key, final byte read_type, final Buffer buffer, final long previous, final long cursor, final boolean initial) {
        switch (read_type) {
            case Type.BOOL:
                internal_set(read_key, read_type, (((byte) Base64.decodeToIntWithBounds(buffer, previous, cursor)) == CoreConstants.BOOL_TRUE), true, initial);
                break;
            case Type.INT:
                internal_set(read_key, read_type, Base64.decodeToIntWithBounds(buffer, previous, cursor), true, initial);
                break;
            case Type.DOUBLE:
                internal_set(read_key, read_type, Base64.decodeToDoubleWithBounds(buffer, previous, cursor), true, initial);
                break;
            case Type.LONG:
                internal_set(read_key, read_type, Base64.decodeToLongWithBounds(buffer, previous, cursor), true, initial);
                break;
            case Type.STRING:
                internal_set(read_key, read_type, Base64.decodeToStringWithBounds(buffer, previous, cursor), true, initial);
                break;
        }
    }

    @Override
    public void loadDiff(Buffer buffer) {
        load(buffer);
    }

    static void free(final long addr, final OffHeapChunkSpace space) {
        if (addr != OffHeapConstants.NULL_PTR) {
            final long subhash_ptr = POffHeapLongArray.get(addr, SUBHASH);
            final long size = POffHeapLongArray.get(addr, SIZE);
            for (long i = 0; i < size; i++) {
                freeElement(value(addr, i), type(addr, i), space);
            }
            if (subhash_ptr != OffHeapConstants.NULL_PTR) {
                POffHeapLongArray.free(subhash_ptr);
            }
            POffHeapLongArray.free(addr);
        }
    }

    private static void freeElement(final long addr, final byte elemType, final OffHeapChunkSpace space) {
        switch (elemType) {
            case Type.STRING:
                POffHeapString.free(addr);
                break;
            case Type.RELATION:
                OffHeapRelation.free(addr);
                break;
            case Type.DMATRIX:
                OffHeapDMatrix.free(addr);
                break;
            case Type.LMATRIX:
                OffHeapLMatrix.free(addr);
                break;
            case Type.LONG_ARRAY:
                OffHeapLongArray.free(addr);
                break;
            case Type.INT_ARRAY:
                OffHeapIntArray.free(addr);
                break;
            case Type.DOUBLE_ARRAY:
                OffHeapDoubleArray.free(addr);
                break;
            case Type.STRING_ARRAY:
                OffHeapStringArray.free(addr);
                break;
            case Type.STRING_TO_INT_MAP:
                OffHeapStringIntMap.free(addr);
                break;
            case Type.LONG_TO_LONG_MAP:
                OffHeapLongLongMap.free(addr);
                break;
            case Type.RELATION_INDEXED:
            case Type.LONG_TO_LONG_ARRAY_MAP:
                OffHeapLongLongArrayMap.free(addr);
                break;
            case Type.NDTREE:
            case Type.KDTREE:
            case Type.EGRAPH:
                OffHeapEGraph.freeByAddr(addr);

        }
    }

}
