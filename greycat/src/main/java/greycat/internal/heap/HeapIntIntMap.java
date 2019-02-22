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

import greycat.Constants;
import greycat.internal.CoreConstants;
import greycat.struct.Buffer;
import greycat.struct.IntIntMap;
import greycat.struct.IntIntMapCallBack;
import greycat.utility.Base64;
import greycat.utility.HashHelper;

import java.util.Arrays;

@SuppressWarnings("Duplicates")
public class HeapIntIntMap implements IntIntMap {
    private final HeapContainer parent;

    private int mapSize = 0;
    private int capacity = 0;

    private int[] keys = null;
    private int[] values = null;

    private int[] nexts = null;
    private int[] hashs = null;

    HeapIntIntMap(final HeapContainer p_listener) {
        this.parent = p_listener;
    }

    private int key(int i) {
        return keys[i];
    }

    private void setKey(int i, int newValue) {
        keys[i] = newValue;
    }

    private int value(int i) {
        return values[i];
    }

    private void setValue(int i, int newValue) {
        values[i] = newValue;
    }

    private int next(int i) {
        return nexts[i];
    }

    private void setNext(int i, int newValue) {
        nexts[i] = newValue;
    }

    private int hash(int i) {
        return hashs[i];
    }

    private void setHash(int i, int newValue) {
        hashs[i] = newValue;
    }

    void reallocate(int newCapacity) {
        if (newCapacity > capacity) {
            //extend keys
            int[] new_keys = new int[newCapacity];
            if (keys != null) {
                System.arraycopy(keys, 0, new_keys, 0, capacity);
            }
            keys = new_keys;
            //extend values
            int[] new_values = new int[newCapacity];
            if (values != null) {
                System.arraycopy(values, 0, new_values, 0, capacity);
            }
            values = new_values;

            int[] new_nexts = new int[newCapacity];
            int[] new_hashes = new int[newCapacity * 2];
            Arrays.fill(new_nexts, 0, newCapacity, -1);
            Arrays.fill(new_hashes, 0, (newCapacity * 2), -1);
            hashs = new_hashes;
            nexts = new_nexts;
            for (int i = 0; i < mapSize; i++) {
                int new_key_hash = (int) HashHelper.longHash(key(i), newCapacity * 2);
                setNext(i, hash(new_key_hash));
                setHash(new_key_hash, i);
            }
            capacity = newCapacity;
        }
    }

    HeapIntIntMap cloneFor(HeapContainer newParent) {
        HeapIntIntMap cloned = new HeapIntIntMap(newParent);
        cloned.mapSize = mapSize;
        cloned.capacity = capacity;
        if (keys != null) {
            int[] cloned_keys = new int[capacity];
            System.arraycopy(keys, 0, cloned_keys, 0, capacity);
            cloned.keys = cloned_keys;
        }
        if (values != null) {
            int[] cloned_values = new int[capacity];
            System.arraycopy(values, 0, cloned_values, 0, capacity);
            cloned.values = cloned_values;
        }
        if (nexts != null) {
            int[] cloned_nexts = new int[capacity];
            System.arraycopy(nexts, 0, cloned_nexts, 0, capacity);
            cloned.nexts = cloned_nexts;
        }
        if (hashs != null) {
            int[] cloned_hashs = new int[capacity * 2];
            System.arraycopy(hashs, 0, cloned_hashs, 0, capacity * 2);
            cloned.hashs = cloned_hashs;
        }
        return cloned;
    }

    public final int get(final int requestKey) {
        int result = Constants.NULL_INT;
        synchronized (parent) {
            if (keys != null) {
                final int hashIndex = HashHelper.intHash(requestKey, capacity * 2);
                int m = hash(hashIndex);
                while (m >= 0) {
                    if (requestKey == key(m)) {
                        result = value(m);
                        break;
                    }
                    m = next(m);
                }
            }
        }
        return result;
    }

    public final void each(IntIntMapCallBack callback) {
        synchronized (parent) {
            unsafe_each(callback);
        }
    }

    void unsafe_each(IntIntMapCallBack callback) {
        for (int i = 0; i < mapSize; i++) {
            callback.on(key(i), value(i));
        }
    }

    public int size() {
        int result;
        synchronized (parent) {
            result = mapSize;
        }
        return result;
    }

    public final void remove(final int requestKey) {
        synchronized (parent) {
            if (keys != null && mapSize != 0) {
                int hashCapacity = capacity * 2;
                int hashIndex = HashHelper.intHash(requestKey, hashCapacity);
                int m = hash(hashIndex);
                int found = -1;
                while (m >= 0) {
                    if (requestKey == key(m)) {
                        found = m;
                        break;
                    }
                    m = next(m);
                }
                if (found != -1) {
                    //first remove currentKey from hashChain
                    int toRemoveHash = HashHelper.intHash(requestKey, hashCapacity);
                    m = hash(toRemoveHash);
                    if (m == found) {
                        setHash(toRemoveHash, next(m));
                    } else {
                        while (m != -1) {
                            int next_of_m = next(m);
                            if (next_of_m == found) {
                                setNext(m, next(next_of_m));
                                break;
                            }
                            m = next_of_m;
                        }
                    }
                    final int lastIndex = mapSize - 1;
                    if (lastIndex == found) {
                        //easy, was the last element
                        mapSize--;
                    } else {
                        //less cool, we have to unchain the last value of the map
                        final int lastKey = key(lastIndex);
                        setKey(found, lastKey);
                        setValue(found, value(lastIndex));
                        setNext(found, next(lastIndex));
                        int victimHash = HashHelper.intHash(lastKey, hashCapacity);
                        m = hash(victimHash);
                        if (m == lastIndex) {
                            //the victim was the head of hashing list
                            setHash(victimHash, found);
                        } else {
                            //the victim is in the next, reChain it
                            while (m != -1) {
                                int next_of_m = next(m);
                                if (next_of_m == lastIndex) {
                                    setNext(m, found);
                                    break;
                                }
                                m = next_of_m;
                            }
                        }
                        mapSize--;
                    }
                    parent.declareDirty();
                }
            }
        }
    }

    public final IntIntMap put(final int insertKey, final int insertValue) {
        synchronized (parent) {
            if (keys == null) {
                reallocate(Constants.MAP_INITIAL_CAPACITY);
                setKey(0, insertKey);
                setValue(0, insertValue);
                setHash(HashHelper.intHash(insertKey, capacity * 2), 0);
                setNext(0, -1);
                mapSize++;
            } else {
                int hashCapacity = capacity * 2;
                int insertKeyHash = HashHelper.intHash(insertKey, hashCapacity);
                int currentHash = hash(insertKeyHash);
                int m = currentHash;
                int found = -1;
                while (m >= 0) {
                    if (insertKey == key(m)) {
                        found = m;
                        break;
                    }
                    m = next(m);
                }
                if (found == -1) {
                    final int lastIndex = mapSize;
                    if (lastIndex == capacity) {
                        reallocate(capacity * 2);
                        hashCapacity = capacity * 2;
                        insertKeyHash = HashHelper.intHash(insertKey, hashCapacity);
                        currentHash = hash(insertKeyHash);
                    }
                    setKey(lastIndex, insertKey);
                    setValue(lastIndex, insertValue);
                    setHash(HashHelper.intHash(insertKey, capacity * 2), lastIndex);
                    setNext(lastIndex, currentHash);
                    mapSize++;
                    parent.declareDirty();
                } else {
                    if (value(found) != insertValue) {
                        setValue(found, insertValue);
                        parent.declareDirty();
                    }
                }
            }
        }
        return this;
    }

    public final void save(final Buffer buffer) {
        if (mapSize != 0) {
            Base64.encodeIntToBuffer(mapSize, buffer);
            for (int j = 0; j < mapSize; j++) {
                buffer.write(CoreConstants.CHUNK_VAL_SEP);
                Base64.encodeIntToBuffer(keys[j], buffer);
                buffer.write(CoreConstants.CHUNK_VAL_SEP);
                Base64.encodeIntToBuffer(values[j], buffer);
            }
        } else {
            Base64.encodeIntToBuffer(0, buffer);
        }
    }

    public final long load(final Buffer buffer, final long offset, final long max) {
        long cursor = offset;
        byte current = buffer.read(cursor);
        boolean isFirst = true;
        long previous = offset;
        int previousKey = -1;
        boolean waitingVal = false;
        while (cursor < max && current != Constants.CHUNK_SEP && current != Constants.BLOCK_CLOSE) {
            if (current == Constants.CHUNK_VAL_SEP) {
                if (isFirst) {
                    reallocate(Base64.decodeToIntWithBounds(buffer, previous, cursor));
                    isFirst = false;
                } else {
                    if (!waitingVal) {
                        previousKey = Base64.decodeToIntWithBounds(buffer, previous, cursor);
                        waitingVal = true;
                    } else {
                        waitingVal = false;
                        put(previousKey, Base64.decodeToIntWithBounds(buffer, previous, cursor));
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
            reallocate(Base64.decodeToIntWithBounds(buffer, previous, cursor));
        } else {
            if (waitingVal) {
                put(previousKey, Base64.decodeToIntWithBounds(buffer, previous, cursor));
            }
        }
        return cursor;
    }
}
