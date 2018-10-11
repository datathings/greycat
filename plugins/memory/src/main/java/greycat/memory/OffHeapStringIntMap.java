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
package greycat.memory;

import greycat.Constants;
import greycat.memory.primary.POffHeapLongArray;
import greycat.memory.primary.POffHeapString;
import greycat.struct.Buffer;
import greycat.struct.StringIntMap;
import greycat.struct.StringLongMapCallBack;
import greycat.utility.Base64;
import greycat.utility.HashHelper;

@SuppressWarnings("ALL")
class OffHeapStringIntMap implements StringIntMap {

    private static int SIZE = 0;
    private static int CAPACITY = 1;
    private static int SUBHASH = 2;

    private static int OFFSET = 3;
    private static int ELEM_SIZE = 3;

    private final long index;
    private final OffHeapContainer container;

    OffHeapStringIntMap(final OffHeapContainer p_container, final long p_index) {
        container = p_container;
        index = p_index;
    }

    private static long key(final long addr, final long elemIndex) {
        return POffHeapLongArray.get(addr, OFFSET + (elemIndex * ELEM_SIZE));
    }

    private void setKey(final long addr, final long elemIndex, long newValue) {
        POffHeapLongArray.set(addr, OFFSET + (elemIndex * ELEM_SIZE), newValue);
    }

    private static long keyHash(final long addr, final long elemIndex) {
        return POffHeapLongArray.get(addr, OFFSET + (elemIndex * ELEM_SIZE) + 1);
    }

    private void setKeyHash(final long addr, final long elemIndex, long newValue) {
        POffHeapLongArray.set(addr, OFFSET + (elemIndex * ELEM_SIZE) + 1, newValue);
    }

    private static long value(final long addr, final long elemIndex) {
        return POffHeapLongArray.get(addr, OFFSET + (elemIndex * ELEM_SIZE) + 2);
    }

    private void setValue(final long addr, final long elemIndex, long newValue) {
        POffHeapLongArray.set(addr, OFFSET + (elemIndex * ELEM_SIZE) + 2, newValue);
    }

    private long next(final long subHashAddr, final long elemIndex) {
        return POffHeapLongArray.get(subHashAddr, elemIndex);
    }

    private void setNext(final long subHashAddr, final long elemIndex, final long newValue) {
        POffHeapLongArray.set(subHashAddr, elemIndex, newValue);
    }

    private long hash(final long subHashAddr, final long capacity, final long elemIndex) {
        return POffHeapLongArray.get(subHashAddr, (capacity + elemIndex));
    }

    private void setHash(final long subHashAddr, final long capacity, final long elemIndex, final long newValue) {
        POffHeapLongArray.set(subHashAddr, (capacity + elemIndex), newValue);
    }

    void preAllocate(long wantedCapacity) {
        long addr = container.addrByIndex(index);
        if (addr == OffHeapConstants.NULL_PTR) {
            addr = POffHeapLongArray.allocate(OFFSET + (wantedCapacity * ELEM_SIZE));
            container.setAddrByIndex(index, addr);
            POffHeapLongArray.set(addr, SIZE, 0);
            POffHeapLongArray.set(addr, CAPACITY, wantedCapacity);
            long subHash = POffHeapLongArray.allocate(wantedCapacity * 3);
            POffHeapLongArray.set(addr, SUBHASH, subHash);
        } else {
            long currentCapacity = POffHeapLongArray.get(addr, CAPACITY);
            if (wantedCapacity > currentCapacity) {
                addr = POffHeapLongArray.reallocate(addr, OFFSET + (wantedCapacity * ELEM_SIZE));
                container.setAddrByIndex(index, addr);
                POffHeapLongArray.set(addr, CAPACITY, wantedCapacity);
                long subHash = POffHeapLongArray.get(addr, SUBHASH);
                subHash = POffHeapLongArray.reallocate(subHash, wantedCapacity * 3);
                POffHeapLongArray.set(addr, SUBHASH, subHash);
                POffHeapLongArray.reset(subHash, wantedCapacity * 3);
                long size = POffHeapLongArray.get(addr, SIZE);
                long double_wanted_capacity = wantedCapacity * 2;
                for (long i = 0; i < size; i++) {
                    long new_key_hash = keyHash(addr, i) % double_wanted_capacity;
                    if (new_key_hash < 0) {
                        new_key_hash = new_key_hash * -1;
                    }
                    setNext(subHash, i, hash(subHash, wantedCapacity, new_key_hash));
                    setHash(subHash, wantedCapacity, new_key_hash, i);
                }
            }
        }
    }

    @Override
    public final int getValue(final String requestStringKey) {
        long result = Constants.NULL_LONG;
        container.lock();
        try {
            final long keyHash = HashHelper.hash(requestStringKey);
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final long capacity = POffHeapLongArray.get(addr, CAPACITY);
                final long subHash = POffHeapLongArray.get(addr, SUBHASH);
                long hashIndex = keyHash % (capacity * 2);
                if (hashIndex < 0) {
                    hashIndex = hashIndex * -1;
                }
                long m = hash(subHash, capacity, hashIndex);
                while (m >= 0) {
                    if (keyHash == keyHash(addr, m)) {
                        result = value(addr, m);
                        break;
                    }
                    m = next(subHash, m);
                }
            }
        } finally {
            container.unlock();
        }
        return (int) result;
    }

    @Override
    public final String getByHash(final int keyHash) {
        long result = Constants.NULL_LONG;
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final long capacity = POffHeapLongArray.get(addr, CAPACITY);
                final long subHash = POffHeapLongArray.get(addr, SUBHASH);
                long hashIndex = keyHash % (capacity * 2);
                if (hashIndex < 0) {
                    hashIndex = hashIndex * -1;
                }
                long m = hash(subHash, capacity, hashIndex);
                while (m >= 0) {
                    if (keyHash == keyHash(addr, m)) {
                        result = key(addr, m);
                        break;
                    }
                    m = next(subHash, m);
                }
            }
        } finally {
            container.unlock();
        }
        return POffHeapString.asObject(result);
    }

    @Override
    public final boolean containsHash(final int keyHash) {
        boolean result = false;
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final long capacity = POffHeapLongArray.get(addr, CAPACITY);
                final long subHash = POffHeapLongArray.get(addr, SUBHASH);
                long hashIndex = keyHash % (capacity * 2);
                if (hashIndex < 0) {
                    hashIndex = hashIndex * -1;
                }
                long m = hash(subHash, capacity, hashIndex);
                while (m >= 0) {
                    if (keyHash == keyHash(addr, m)) {
                        result = true;
                        break;
                    }
                    m = next(subHash, m);
                }
            }
        } finally {
            container.unlock();
        }
        return result;
    }

    @Override
    public final void each(StringLongMapCallBack callback) {
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final long mapSize = POffHeapLongArray.get(addr, SIZE);
                for (long i = 0; i < mapSize; i++) {
                    callback.on(POffHeapString.asObject(key(addr, i)), value(addr, i));
                }
            }
        } finally {
            container.unlock();
        }
    }

    @Override
    public int size() {
        long result = 0;
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                result = POffHeapLongArray.get(addr, SIZE);
            }
        } finally {
            container.unlock();
        }
        return (int) result;
    }

    @Override
    public final void remove(final String requestStringKey) {
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final long keyHash = HashHelper.hash(requestStringKey);
                long mapSize = POffHeapLongArray.get(addr, SIZE);
                if (mapSize != 0) {
                    long capacity = POffHeapLongArray.get(addr, CAPACITY);
                    long subHash = POffHeapLongArray.get(addr, SUBHASH);
                    long hashCapacity = capacity * 2;
                    long hashIndex = keyHash % hashCapacity;
                    if (hashIndex < 0) {
                        hashIndex = hashIndex * -1;
                    }
                    long m = hash(subHash, capacity, hashIndex);
                    long found = -1;
                    while (m >= 0) {
                        if (keyHash == keyHash(addr, m)) {
                            found = m;
                            break;
                        }
                        m = next(subHash, m);
                    }
                    if (found != -1) {
                        //first remove currentKey from hashChain
                        long toRemoveHash = keyHash % hashCapacity;
                        if (toRemoveHash < 0) {
                            toRemoveHash = toRemoveHash * -1;
                        }
                        m = hash(subHash, capacity, toRemoveHash);
                        if (m == found) {
                            setHash(subHash, capacity, toRemoveHash, next(subHash, m));
                        } else {
                            while (m != -1) {
                                long next_of_m = next(subHash, m);
                                if (next_of_m == found) {
                                    setNext(subHash, m, next(subHash, next_of_m));
                                    break;
                                }
                                m = next_of_m;
                            }
                        }
                        final long lastIndex = mapSize - 1;
                        if (lastIndex == found) {
                            //easy, was the last element
                            POffHeapLongArray.set(addr, SIZE, mapSize - 1);
                        } else {
                            //less cool, we have to unchain the last value of the map
                            final long lastKey = key(addr, lastIndex);
                            final long lastKeyHash = keyHash(addr, lastIndex);
                            setKey(addr, found, lastKey);
                            setKeyHash(addr, found, lastKeyHash);
                            setValue(addr, found, value(addr, lastIndex));
                            setNext(subHash, found, next(subHash, lastIndex));
                            long victimHash = lastKey % hashCapacity;
                            if (victimHash < 0) {
                                victimHash = victimHash * -1;
                            }
                            m = hash(subHash, capacity, victimHash);
                            if (m == lastIndex) {
                                //the victim was the head of hashing list
                                setHash(subHash, capacity, victimHash, found);
                            } else {
                                //the victim is in the next, reChain it
                                while (m != -1) {
                                    long next_of_m = next(subHash, m);
                                    if (next_of_m == lastIndex) {
                                        setNext(subHash, m, found);
                                        break;
                                    }
                                    m = next_of_m;
                                }
                            }
                            POffHeapLongArray.set(addr, SIZE, mapSize - 1);
                        }
                        container.declareDirty();
                    }
                }
            }
        } finally {
            container.unlock();
        }
    }

    @Override
    public final void put(final String insertKey, final int insertValue) {
        container.lock();
        try {
            internal_put(insertKey, insertValue);
        } finally {
            container.unlock();
        }
    }

    void internal_put(final String insertStringKey, final long insertValue) {
        final long keyHash = HashHelper.hash(insertStringKey);
        long addr = container.addrByIndex(index);
        if (addr == OffHeapConstants.NULL_PTR) {
            //initial allocation
            final long capacity = Constants.MAP_INITIAL_CAPACITY;
            addr = POffHeapLongArray.allocate(OFFSET + (capacity * ELEM_SIZE));
            container.setAddrByIndex(index, addr);
            final long subHash = POffHeapLongArray.allocate(capacity * 3);
            POffHeapLongArray.set(addr, SIZE, 1);
            POffHeapLongArray.set(addr, CAPACITY, capacity);
            POffHeapLongArray.set(addr, SUBHASH, subHash);
            final long keyAddr = POffHeapString.fromObject(insertStringKey);
            setKey(addr, 0, keyAddr);
            setKeyHash(addr, 0, keyHash);
            setValue(addr, 0, insertValue);
            long hashIndex = keyHash % (capacity * 2);
            if (hashIndex < 0) {
                hashIndex = hashIndex * -1;
            }
            setHash(subHash, capacity, hashIndex, 0);
            setNext(subHash, 0, -1);
        } else {
            long mapSize = POffHeapLongArray.get(addr, SIZE);
            long capacity = POffHeapLongArray.get(addr, CAPACITY);
            long subHash = POffHeapLongArray.get(addr, SUBHASH);
            long hashIndex = keyHash % (capacity * 2);
            if (hashIndex < 0) {
                hashIndex = hashIndex * -1;
            }
            long m = hash(subHash, capacity, hashIndex);
            long found = -1;
            while (m >= 0) {
                if (keyHash == keyHash(addr, m)) {
                    String obj = POffHeapString.asObject(key(addr, m));
                    if (!(insertStringKey.equals(obj))) {
                        throw new RuntimeException("Lotteries Winner !!! hashing conflict between " + obj + " and " + insertStringKey);
                    }
                    found = m;
                    break;
                }
                m = next(subHash, m);
            }
            if (found == -1) {
                final long lastIndex = mapSize;
                if (lastIndex == capacity) {
                    //extend capacity
                    capacity = capacity * 2;
                    addr = POffHeapLongArray.reallocate(addr, OFFSET + (capacity * ELEM_SIZE));
                    container.setAddrByIndex(index, addr);
                    POffHeapLongArray.set(addr, CAPACITY, capacity);
                    subHash = POffHeapLongArray.reallocate(subHash, capacity * 3);
                    POffHeapLongArray.reset(subHash, capacity * 3);
                    POffHeapLongArray.set(addr, SUBHASH, subHash);
                    //reHash previous stored content
                    long size = POffHeapLongArray.get(addr, SIZE);
                    for (long i = 0; i < size; i++) {
                        long new_key_hash = keyHash(addr, i) % (capacity * 2);
                        if (new_key_hash < 0) {
                            new_key_hash = new_key_hash * -1;
                        }
                        setNext(subHash, i, hash(subHash, capacity, new_key_hash));
                        setHash(subHash, capacity, new_key_hash, i);
                    }
                }
                setKey(addr, lastIndex, POffHeapString.fromObject(insertStringKey));
                setKeyHash(addr, lastIndex, keyHash);
                setValue(addr, lastIndex, insertValue);
                long hashedKey = keyHash % (capacity * 2);
                if (hashedKey < 0) {
                    hashedKey = hashedKey * -1;
                }
                setNext(subHash, lastIndex, hash(subHash, capacity, hashedKey));
                setHash(subHash, capacity, hashedKey, lastIndex);
                POffHeapLongArray.set(addr, SIZE, mapSize + 1);
                container.declareDirty();
            } else {
                if (value(addr, found) != insertValue) {
                    setValue(addr, found, insertValue);
                    container.declareDirty();
                }
            }
        }
    }

    static void save(final long addr, final Buffer buffer) {
        if (addr != OffHeapConstants.NULL_PTR) {
            final long size = POffHeapLongArray.get(addr, SIZE);
            Base64.encodeLongToBuffer(size, buffer);
            for (long i = 0; i < size; i++) {
                buffer.write(Constants.CHUNK_VAL_SEP);
                Base64.encodeStringToBuffer(POffHeapString.asObject(key(addr, i)), buffer);
                buffer.write(Constants.CHUNK_VAL_SEP);
                Base64.encodeLongToBuffer(value(addr, i), buffer);
            }
        }
    }

    static void free(final long addr) {
        if (addr != OffHeapConstants.NULL_PTR) {
            final long size = POffHeapLongArray.get(addr, SIZE);
            for (long i = 0; i < size; i++) {
                final long keyAddr = key(addr, i);
                if (keyAddr != OffHeapConstants.NULL_PTR) {
                    POffHeapString.free(keyAddr);
                }
            }
            final long previousHash = POffHeapLongArray.get(addr, SUBHASH);
            if (previousHash != OffHeapConstants.NULL_PTR) {
                if (OffHeapConstants.DEBUG_MODE) {
                    if (!OffHeapConstants.SEGMENTS.containsKey(previousHash)) {
                        throw new RuntimeException("Bad ADDR!");
                    }
                    OffHeapConstants.SEGMENTS.remove(previousHash);
                }
                POffHeapLongArray.free(previousHash);
            }
            if (OffHeapConstants.DEBUG_MODE) {
                if (!OffHeapConstants.SEGMENTS.containsKey(addr)) {
                    throw new RuntimeException("Bad ADDR!");
                }
                OffHeapConstants.SEGMENTS.remove(addr);
            }
            POffHeapLongArray.free(addr);
        }
    }

    static long clone(final long addr) {
        if (addr == OffHeapConstants.NULL_PTR) {
            return OffHeapConstants.NULL_PTR;
        } else {
            final long capacity = POffHeapLongArray.get(addr, CAPACITY);
            //copy main array
            final long new_addr = POffHeapLongArray.cloneArray(addr, OFFSET + (capacity * ELEM_SIZE));
            final long previousHash = POffHeapLongArray.get(addr, SUBHASH);
            if (previousHash != OffHeapConstants.NULL_PTR) {
                final long newHash = POffHeapLongArray.cloneArray(previousHash, (capacity * 3));
                POffHeapLongArray.set(new_addr, SUBHASH, newHash);
            }
            //increase cow counters of OffHeapStrings
            long size = POffHeapLongArray.get(addr, SIZE);
            for (long i = 0; i < size; i++) {
                // not necessary to set the cloned value
                // POffHeapString.clone always returns the same address, it just increments the cow counter
                /*long newKey = */
                POffHeapString.clone(key(addr, i));
                // POffHeapLongArray.set(addr, OFFSET + (i * ELEM_SIZE), newKey);
            }
            return new_addr;
        }
    }

    final long load(final Buffer buffer, final long offset, final long max) {
        long cursor = offset;
        byte current = buffer.read(cursor);
        boolean isFirst = true;
        long previous = offset;
        String previousKey = null;
        while (cursor < max && current != Constants.CHUNK_SEP && current != Constants.CHUNK_ENODE_SEP && current != Constants.CHUNK_ESEP) {
            if (current == Constants.CHUNK_VAL_SEP) {
                if (isFirst) {
                    preAllocate(Base64.decodeToLongWithBounds(buffer, previous, cursor));
                    isFirst = false;
                } else {
                    if (previousKey == null) {
                        previousKey = Base64.decodeToStringWithBounds(buffer, previous, cursor);
                    } else {
                        internal_put(previousKey, Base64.decodeToLongWithBounds(buffer, previous, cursor));
                        previousKey = null;
                    }
                }
                previous = cursor + 1;
            }
            cursor++;
            if (cursor < max) {
                current = buffer.read(cursor);
            }
        }
        if (isFirst) {
            preAllocate(Base64.decodeToLongWithBounds(buffer, previous, cursor));
        } else {
            if (previousKey != null) {
                internal_put(previousKey, Base64.decodeToLongWithBounds(buffer, previous, cursor));
            }
        }
        return cursor;
    }


}



