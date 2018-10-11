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
package greycat.utility;

import greycat.Constants;

public class LArray {

    private int index = 0;
    private int capacity = Constants.MAP_INITIAL_CAPACITY;
    private long[] backend = new long[capacity];

    public int size() {
        return index;
    }

    public void add(long elem) {
        if (capacity == index) {
            int newCapacity = capacity * 2;
            long[] new_back = new long[newCapacity];
            System.arraycopy(backend, 0, new_back, 0, capacity);
            capacity = newCapacity;
            backend = new_back;
        }
        backend[index] = elem;
        index++;
    }

    public long[] all() {
        if (capacity == index) {
            return backend;
        } else {
            long[] new_back = new long[index];
            System.arraycopy(backend, 0, new_back, 0, index);
            return new_back;
        }
    }

    public final long get(int index) {
        return backend[index];
    }

}
