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
import greycat.struct.StringIntMap;
import greycat.struct.StringLongMapCallBack;
import greycat.utility.Base64;
import greycat.utility.HashHelper;

import java.util.Arrays;

class HeapStringIntMap implements StringIntMap {

    private final HeapContainer parent;

    private int mapSize = 0;
    private int capacity = 0;

    private String[] keys = null;
    private int[] keysH = null;
    private int[] values = null;

    private int[] nexts = null;
    private int[] hashs = null;

    HeapStringIntMap(final HeapContainer p_parent) {
        this.parent = p_parent;
    }

    private String key(final int i) {
        return keys[i];
    }

    private void setKey(final int i, final String newValue) {
        keys[i] = newValue;
    }

    private int keyH(final int i) {
        return keysH[i];
    }

    private void setKeyH(final int i, int newValue) {
        keysH[i] = newValue;
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
            String[] new_keys = new String[newCapacity];
            if (keys != null) {
                System.arraycopy(keys, 0, new_keys, 0, capacity);
            }
            keys = new_keys;
            //extend keysH
            int[] new_keysH = new int[newCapacity];
            if (keysH != null) {
                System.arraycopy(keysH, 0, new_keysH, 0, capacity);
            }
            keysH = new_keysH;
            //extend values
            int[] new_values = new int[newCapacity];
            if (values != null) {
                System.arraycopy(values, 0, new_values, 0, capacity);
            }
            values = new_values;
            int[] new_nexts = new int[newCapacity];
            int[] new_hashes = new int[newCapacity * 2];
            Arrays.fill(new_nexts, 0, newCapacity, -1);
            Arrays.fill(new_hashes, 0, newCapacity * 2, -1);
            hashs = new_hashes;
            nexts = new_nexts;
            int double_capacity = newCapacity * 2;
            for (int i = 0; i < mapSize; i++) {
                int new_key_hash = keyH(i) % double_capacity;
                if (new_key_hash < 0) {
                    new_key_hash = new_key_hash * -1;
                }
                setNext(i, hash(new_key_hash));
                setHash(new_key_hash, i);
            }
            capacity = newCapacity;
        }
    }

    HeapStringIntMap cloneFor(HeapContainer newContainer) {
        HeapStringIntMap cloned = new HeapStringIntMap(newContainer);
        cloned.mapSize = mapSize;
        cloned.capacity = capacity;
        if (keys != null) {
            String[] cloned_keys = new String[capacity];
            System.arraycopy(keys, 0, cloned_keys, 0, capacity);
            cloned.keys = cloned_keys;
        }
        if (keysH != null) {
            int[] cloned_keysH = new int[capacity];
            System.arraycopy(keysH, 0, cloned_keysH, 0, capacity);
            cloned.keysH = cloned_keysH;
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

    @Override
    public final int getValue(final String requestString) {
        int result = -1;
        synchronized (parent) {
            if (keys != null) {
                final int keyHash = HashHelper.hash(requestString);
                int hashIndex = keyHash % (capacity * 2);
                if (hashIndex < 0) {
                    hashIndex = hashIndex * -1;
                }
                int m = hash(hashIndex);
                while (m >= 0) {
                    if (keyHash == keyH(m)) {
                        result = value(m);
                        break;
                    }
                    m = next(m);
                }
            }
        }
        return result;
    }


    @Override
    public String getByHash(final int keyHash) {
        String result = null;
        synchronized (parent) {
            if (keys != null) {
                int hashIndex = keyHash % (capacity * 2);
                if (hashIndex < 0) {
                    hashIndex = hashIndex * -1;
                }
                int m = hash(hashIndex);
                while (m >= 0) {
                    if (keyHash == keyH(m)) {
                        result = key(m);
                        break;
                    }
                    m = next(m);
                }
            }
        }
        return result;
    }

    @Override
    public boolean containsHash(int keyHash) {
        boolean result = false;
        synchronized (parent) {
            if (keys != null) {
                int hashIndex = keyHash % (capacity * 2);
                if (hashIndex < 0) {
                    hashIndex = hashIndex * -1;
                }
                int m = hash(hashIndex);
                while (m >= 0) {
                    if (keyHash == keyH(m)) {
                        result = true;
                        break;
                    }
                    m = next(m);
                }
            }
        }
        return result;
    }

    @Override
    public final void each(StringLongMapCallBack callback) {
        synchronized (parent) {
            unsafe_each(callback);
        }
    }

    final void unsafe_each(StringLongMapCallBack callback) {
        for (int i = 0; i < mapSize; i++) {
            callback.on(key(i), value(i));
        }
    }

    @Override
    public final int size() {
        int result;
        synchronized (parent) {
            result = mapSize;
        }
        return result;
    }

    @Override
    public final void remove(final String requestKey) {
        synchronized (parent) {
            if (keys != null && mapSize != 0) {
                final int keyHash = HashHelper.hash(requestKey);
                int hashCapacity = capacity * 2;
                int hashIndex = keyHash % hashCapacity;
                if (hashIndex < 0) {
                    hashIndex = hashIndex * -1;
                }
                int m = hash(hashIndex);
                int found = -1;
                while (m >= 0) {
                    if (keyHash == keyH(m)) {
                        found = m;
                        break;
                    }
                    m = next(m);
                }
                if (found != -1) {
                    //first remove currentKey from hashChain
                    int toRemoveHash = keyHash % hashCapacity;
                    if (toRemoveHash < 0) {
                        toRemoveHash = toRemoveHash * -1;
                    }
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
                        final String lastKey = key(lastIndex);
                        final int lastKeyH = keyH(lastIndex);
                        setKey(found, lastKey);
                        setKeyH(found, lastKeyH);
                        setValue(found, value(lastIndex));
                        setNext(found, next(lastIndex));
                        int victimHash = lastKeyH % hashCapacity;
                        if (victimHash < 0) {
                            victimHash = victimHash * -1;
                        }
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

    private StringIntMap internal_put(final String insertKey, final int insertValue, final boolean notify) {
        synchronized (parent) {
            final int keyHash = HashHelper.hash(insertKey);
            if (keys == null) {
                reallocate(Constants.MAP_INITIAL_CAPACITY);
                setKey(0, insertKey);
                setKeyH(0, keyHash);
                setValue(0, insertValue);
                int hashIndex = keyHash % (capacity * 2);
                if (hashIndex < 0) {
                    hashIndex = hashIndex * -1;
                }
                setHash(hashIndex, 0);
                setNext(0, -1);
                mapSize++;
            } else {
                int hashCapacity = capacity * 2;
                int insertKeyHash = keyHash % hashCapacity;
                if (insertKeyHash < 0) {
                    insertKeyHash = insertKeyHash * -1;
                }
                int currentHash = hash(insertKeyHash);
                int m = currentHash;
                int found = -1;
                while (m >= 0) {
                    if (keyHash == keyH(m)) {
                        if (!(insertKey.equals(key(m)))) {
                            throw new RuntimeException("Lotteries Winner !!! hashing conflict between " + key(m) + " and " + insertKey);
                        }
                        found = m;
                        break;
                    }
                    m = next(m);
                }
                if (found == -1) {
                    final int lastIndex = mapSize;
                    if (lastIndex == capacity) {
                        reallocate(capacity * 2);
                    }
                    setKey(lastIndex, insertKey);
                    setKeyH(lastIndex, keyHash);
                    setValue(lastIndex, insertValue);
                    int hashIndex = keyHash % (capacity * 2);
                    if (hashIndex < 0) {
                        hashIndex = hashIndex * -1;
                    }
                    setHash(hashIndex, lastIndex);
                    setNext(lastIndex, currentHash);
                    mapSize++;
                    if (notify) {
                        parent.declareDirty();
                    }
                } else {
                    if (value(found) != insertValue) {
                        setValue(found, insertValue);
                        if (notify) {
                            parent.declareDirty();
                        }
                    }
                }
            }
        }
        return this;
    }

    @Override
    public final StringIntMap put(final String insertKey, final int insertValue) {
        return internal_put(insertKey, insertValue, true);
    }

    public final void save(final Buffer buffer) {
        if (mapSize != 0) {
            Base64.encodeIntToBuffer(mapSize, buffer);
            for (int j = 0; j < mapSize; j++) {
                buffer.write(CoreConstants.CHUNK_VAL_SEP);
                Base64.encodeStringToBuffer(keys[j], buffer);
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
        String previousKey = null;

        final boolean merge = capacity != 0;

        while (cursor < max && current != Constants.CHUNK_SEP && current != Constants.BLOCK_CLOSE) {
            if (current == Constants.CHUNK_VAL_SEP) {
                if (isFirst) {
                    reallocate(Base64.decodeToIntWithBounds(buffer, previous, cursor));
                    isFirst = false;
                } else {
                    if (previousKey == null) {
                        previousKey = Base64.decodeToStringWithBounds(buffer, previous, cursor);
                    } else {
                        internal_put(previousKey, Base64.decodeToIntWithBounds(buffer, previous, cursor), merge);
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
            reallocate(Base64.decodeToIntWithBounds(buffer, previous, cursor));
        } else {
            if (previousKey != null) {
                internal_put(previousKey, Base64.decodeToIntWithBounds(buffer, previous, cursor), merge);
            }
        }
        return cursor;
    }

}



