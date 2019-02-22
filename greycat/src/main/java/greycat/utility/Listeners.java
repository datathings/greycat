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
package greycat.utility;

import greycat.Constants;
import greycat.NodeListener;

import java.util.Arrays;

public class Listeners {

    private int mapSize = 0;
    private int capacity = 0;
    private int[] keys = null;
    private NodeListener[] values = null;
    private int[] next_hashs = null;

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
            int[] new_keys = new int[newCapacity];
            if (keys != null) {
                System.arraycopy(keys, 0, new_keys, 0, capacity);
            }
            keys = new_keys;
            //extend values
            NodeListener[] new_values = new NodeListener[newCapacity];
            if (values != null) {
                System.arraycopy(values, 0, new_values, 0, capacity);
            }
            values = new_values;
            int[] new_next_hashes = new int[newCapacity * 3];
            Arrays.fill(new_next_hashes, 0, (newCapacity * 3), -1);
            next_hashs = new_next_hashes;
            for (int i = 0; i < mapSize; i++) {
                int new_key_hash = HashHelper.intHash(keys[i], newCapacity * 2);
                setNext(i, hash(new_key_hash, newCapacity));
                setHash(new_key_hash, i, newCapacity);
            }
            capacity = newCapacity;
        }
    }

    public final void dispatch(long[] callback) {
        for (int i = 0; i < mapSize; i++) {
            values[i].on(callback);
        }
    }

    public final void unlisten(final int requestKey) {
        if (keys != null && mapSize != 0) {
            int hashCapacity = capacity * 2;
            int hashIndex = HashHelper.intHash(requestKey, hashCapacity);
            int m = hash(hashIndex, capacity);
            int found = -1;
            while (m >= 0) {
                if (requestKey == keys[m]) {
                    found = m;
                    break;
                }
                m = next(m);
            }
            if (found != -1) {
                //first remove currentKey from hashChain
                int toRemoveHash = (int) HashHelper.longHash(requestKey, hashCapacity);
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
                    final int lastKey = keys[lastIndex];
                    keys[found] = lastKey;
                    values[found] = values[lastIndex];
                    setNext(found, next(lastIndex));
                    int victimHash = (int) HashHelper.longHash(lastKey, hashCapacity);
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

    private int generator = 0;

    public synchronized final int listen(NodeListener l) {
        int next = generator;
        generator++;
        if (keys == null) {
            reallocate(Constants.MAP_INITIAL_CAPACITY);
            keys[0] = next;
            values[0] = l;
            setHash(HashHelper.intHash(next, capacity * 2), 0, capacity);
            setNext(0, -1);
            mapSize++;
            return next;
        } else {
            int hashCapacity = capacity * 2;
            int insertKeyHash = HashHelper.intHash(next, hashCapacity);
            int currentHash = hash(insertKeyHash, capacity);
            int m = currentHash;
            int found = -1;
            while (m >= 0) {
                if (next == keys[m]) {
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
                    insertKeyHash = HashHelper.intHash(next, hashCapacity);
                    currentHash = hash(insertKeyHash, capacity);
                }
                keys[lastIndex] = next;
                values[lastIndex] = l;
                setHash(HashHelper.intHash(next, capacity * 2), lastIndex, capacity);
                setNext(lastIndex, currentHash);
                mapSize++;
                return next;
            } else {
                values[found] = l;
                return next;
            }
        }
    }

}
