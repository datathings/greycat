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

import greycat.chunk.Stack;
import greycat.memory.primary.POffHeapLongArray;

final class OffHeapFixedStack implements Stack {

    private final long _next;
    private final long _prev;

    private final long _capacity;
    private long _first;
    private long _last;
    private long _count;

    OffHeapFixedStack(long capacity, boolean fill) {
        _capacity = capacity;
        _next = POffHeapLongArray.allocate(capacity);
        _prev = POffHeapLongArray.allocate(capacity);
        this._first = -1;
        this._last = -1;
        if (fill) {
            for (long i = 0; i < capacity; i++) {
                long l = _last;
                POffHeapLongArray.set(_prev, i, l);
                _last = i;
                if (_first == -1) {
                    _first = i;
                } else {
                    POffHeapLongArray.set(_next, l, i);
                }
            }
            _count = capacity;
        } else {
            _count = 0;
        }
    }

    @Override
    public synchronized final boolean enqueue(long index) {
        if (_count >= _capacity) {
            return false;
        }
        if (_first == index || _last == index) {
            return false;
        }
        if (POffHeapLongArray.get(_prev, index) != -1 || POffHeapLongArray.get(_next, index) != -1) { //test if was already in FIFO
            return false;
        }
        long l = _last;
        POffHeapLongArray.set(_prev, index, l);
        _last = index;
        if (_first == -1) {
            _first = index;
        } else {
            POffHeapLongArray.set(_next, l, index);
        }
        _count++;
        return true;
    }

    @Override
    public synchronized final long dequeueTail() {
        long f = _first;
        if (f == -1) {
            return -1;
        }
        long n = POffHeapLongArray.get(_next, f);
        //tag as unused
        POffHeapLongArray.set(_next, f, -1);
        POffHeapLongArray.set(_prev, f, -1);
        _first = n;
        if (n == -1) {
            _last = -1;
        } else {
            POffHeapLongArray.set(_prev, n, -1);
        }
        _count--;
        return f;
    }

    @Override
    public synchronized final boolean dequeue(long index) {
        long p = POffHeapLongArray.get(_prev, index);
        long n = POffHeapLongArray.get(_next, index);
        if (p == -1 && n == -1) {
            return false;
        }
        if (p == -1) {
            long f = _first;
            if (f == -1) {
                return false;
            }
            long n2 = POffHeapLongArray.get(_next, f);
            POffHeapLongArray.set(_next, f, -1);
            POffHeapLongArray.set(_prev, f, -1);
            _first = n2;
            if (n2 == -1) {
                _last = -1;
            } else {
                POffHeapLongArray.set(_prev, n2, -1);
            }
            --_count;
        } else if (n == -1) {
            long l = _last;
            if (l == -1) {
                return false;
            }
            long p2 = POffHeapLongArray.get(_prev, l);
            POffHeapLongArray.set(_next, l, -1);
            POffHeapLongArray.set(_prev, l, -1);
            _last = p2;
            if (p2 == -1) {
                _first = -1;
            } else {
                POffHeapLongArray.set(_next, p2, -1);
            }
            --_count;
        } else {
            POffHeapLongArray.set(_next, p, n);
            POffHeapLongArray.set(_prev, n, p);
            POffHeapLongArray.set(_next, index, -1);
            POffHeapLongArray.set(_prev, index, -1);
            _count--;
        }
        return true;
    }

    @Override
    public final void free() {
        POffHeapLongArray.free(_next);
        POffHeapLongArray.free(_prev);
    }

    @Override
    public synchronized long size() {
        return _count;
    }

}