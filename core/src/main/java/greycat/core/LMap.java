/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
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
package greycat.core;

import greycat.Constants;
import greycat.Helper;

import java.util.Arrays;

public class LMap {

    private int mapSize = 0;
    private int capacity = 0;
    private long[] keys = null;
    private long[] values = null;
    private int[] next_hashs = null;

    private final boolean _withValue;

    public LMap(final boolean withValue) {
        this._withValue = withValue;
    }

    private long key(int i) {
        return keys[i];
    }

    private void setKey(int i, long newValue) {
        keys[i] = newValue;
    }

    private long value(int i) {
        return values[i];
    }

    private void setValue(int i, long newValue) {
        values[i] = newValue;
    }

    private int next(int i) {
        return next_hashs[i];
    }

    private void setNext(int i, int newValue) {
        next_hashs[i] = newValue;
    }

    private int hash(int i, int capacity) {
        return next_hashs[i + capacity];
    }

    private void setHash(int i, int newValue, int capacity) {
        next_hashs[i + capacity] = newValue;
    }


    private void reallocate(int newCapacity) {
        if (newCapacity > capacity) {
            //extend keys
            long[] new_keys = new long[newCapacity];
            if (keys != null) {
                System.arraycopy(keys, 0, new_keys, 0, capacity);
            }
            keys = new_keys;
            //extend values
            if (_withValue) {
                long[] new_values = new long[newCapacity];
                if (values != null) {
                    System.arraycopy(values, 0, new_values, 0, capacity);
                }
                values = new_values;
            }
            int[] new_next_hashes = new int[newCapacity * 3];
            Arrays.fill(new_next_hashes, 0, (newCapacity * 3), -1);
            next_hashs = new_next_hashes;
            for (int i = 0; i < mapSize; i++) {
                int new_key_hash = (int) Helper.longHash(key(i), newCapacity * 2);
                setNext(i, hash(new_key_hash, newCapacity));
                setHash(new_key_hash, i, newCapacity);
            }
            capacity = newCapacity;
        }
    }

    public synchronized final boolean atomicPut(final long requestKey, final long requestValue) {
        return put(requestKey, requestValue);
    }

    public synchronized final void atomicRemove(final long requestKey) {
        remove(requestKey);
    }

    public final boolean contains(final long requestKey) {
        boolean result = false;
        if (keys != null) {
            final int hashIndex = (int) Helper.longHash(requestKey, capacity * 2);
            int m = hash(hashIndex, capacity);
            while (m >= 0) {
                if (requestKey == key(m)) {
                    result = true;
                    break;
                }
                m = next(m);
            }
        }
        return result;
    }

    public final long get(final long requestKey) {
        long result = Constants.NULL_LONG;
        if (keys != null) {
            final int hashIndex = (int) Helper.longHash(requestKey, capacity * 2);
            int m = hash(hashIndex, capacity);
            while (m >= 0) {
                if (requestKey == key(m)) {
                    result = value(m);
                    break;
                }
                m = next(m);
            }
        }
        return result;
    }

    /*
    public final void each(LongLongMapCallBack callback) {
        for (int i = 0; i < mapSize; i++) {
            callback.on(key(i), value(i));
        }
    }*/

    public final long getKey(int index) {
        return key(index);
    }

    public final long getValue(int index) {
        return value(index);
    }

    public final int size() {
        return mapSize;
    }

    public final void remove(final long requestKey) {
        if (keys != null && mapSize != 0) {
            long hashCapacity = capacity * 2;
            int hashIndex = (int) Helper.longHash(requestKey, hashCapacity);
            int m = hash(hashIndex, capacity);
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
                int toRemoveHash = (int) Helper.longHash(requestKey, hashCapacity);
                m = hash(toRemoveHash, capacity);
                if (m == found) {
                    setHash(toRemoveHash, next(m), capacity);
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
                    final long lastKey = key(lastIndex);
                    setKey(found, lastKey);
                    if (_withValue) {
                        setValue(found, value(lastIndex));
                    }
                    setNext(found, next(lastIndex));
                    int victimHash = (int) Helper.longHash(lastKey, hashCapacity);
                    m = hash(victimHash, capacity);
                    if (m == lastIndex) {
                        //the victim was the head of hashing list
                        setHash(victimHash, found, capacity);
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
            }
        }
    }

    public final boolean add(final long insertKey) {
        return put(insertKey, insertKey);
    }

    public final boolean put(final long insertKey, final long insertValue) {
        if (keys == null) {
            reallocate(Constants.MAP_INITIAL_CAPACITY);
            setKey(0, insertKey);
            if (_withValue) {
                setValue(0, insertValue);
            }
            setHash((int) Helper.longHash(insertKey, capacity * 2), 0, capacity);
            setNext(0, -1);
            mapSize++;
            return true;
        } else {
            long hashCapacity = capacity * 2;
            int insertKeyHash = (int) Helper.longHash(insertKey, hashCapacity);
            int currentHash = hash(insertKeyHash, capacity);
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
                    insertKeyHash = (int) Helper.longHash(insertKey, hashCapacity);
                    currentHash = hash(insertKeyHash, capacity);
                }
                setKey(lastIndex, insertKey);
                if (_withValue) {
                    setValue(lastIndex, insertValue);
                }
                setHash((int) Helper.longHash(insertKey, capacity * 2), lastIndex, capacity);
                setNext(lastIndex, currentHash);
                mapSize++;
                return true;
            } else {
                if (_withValue) {
                    if (value(found) != insertValue) {
                        setValue(found, insertValue);
                        return true;
                    }
                }
            }
            return false;
        }
    }

}
