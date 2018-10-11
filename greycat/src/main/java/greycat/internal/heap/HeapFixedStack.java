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
package greycat.internal.heap;


import greycat.chunk.Stack;

import java.util.Arrays;

public final class HeapFixedStack implements Stack {

    private final int[] _next;
    private final int[] _prev;
    private final int _capacity;
    private int _first;
    private int _last;
    private int _count;

    public HeapFixedStack(int capacity, boolean fill) {
        this._capacity = capacity;
        this._next = new int[capacity];
        this._prev = new int[capacity];
        this._first = -1;
        this._last = -1;
        Arrays.fill(_next, 0, capacity, -1);
        Arrays.fill(_prev, 0, capacity, -1);
        if (fill) {
            for (int i = 0; i < capacity; i++) {
                int l = _last;
                _prev[i] = l;
                _last = i;
                if (_first == -1) {
                    _first = i;
                } else {
                    _next[l] = i;
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
        int castedIndex = (int) index;
        if (_first == castedIndex || _last == castedIndex) {
            return false;
        }
        if (_prev[castedIndex] != -1 || _next[castedIndex] != -1) { //test if was already in FIFO
            return false;
        }
        int l = _last;
        _prev[castedIndex] = l;
        _last = castedIndex;
        if (_first == -1) {
            _first = castedIndex;
        } else {
            _next[l] = castedIndex;
        }
        _count++;
        return true;
    }

    @Override
    public synchronized final long dequeueTail() {
        int f = _first;
        if (f == -1) {
            return -1;
        }
        int n = _next[f];
        //tag as unused
        _next[f] = -1;
        _prev[f] = -1;
        _first = n;
        if (n == -1) {
            _last = -1;
        } else {
            _prev[n] = -1;
        }
        _count--;
        return f;
    }

    @Override
    public synchronized final boolean dequeue(long index) {
        int castedIndex = (int) index;
        int p = _prev[castedIndex];
        int n = _next[castedIndex];

        if (p == -1 && n == -1) {
            if (_count == 1) {
                if (this._first == castedIndex) {
                    this._first = -1;
                    this._last = -1;
                    this._count = 0;
                }
            }
            return false;
        }
        if (p == -1) {
            int f = _first;
            if (f == -1) {
                return false;
            }
            int n2 = _next[f];
            _next[f] = -1;
            _prev[f] = -1;
            _first = n2;
            if (n2 == -1) {
                _last = -1;
            } else {
                _prev[n2] = -1;
            }
            --_count;
        } else if (n == -1) {
            int l = _last;
            if (l == -1) {
                return false;
            }
            int p2 = _prev[l];
            _prev[l] = -1;
            _next[l] = -1;
            _last = p2;
            if (p2 == -1) {
                _first = -1;
            } else {
                _next[p2] = -1;
            }
            --_count;
        } else {
            _next[p] = n;
            _prev[n] = p;
            _prev[castedIndex] = -1;
            _next[castedIndex] = -1;
            _count--;
        }
        return true;
    }

    @Override
    public final void free() {
        //noop
    }

    @Override
    public synchronized long size() {
        return _count;
    }

}