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

import greycat.Constants;
import greycat.internal.CoreConstants;
import greycat.struct.Buffer;
import greycat.struct.EStruct;
import greycat.struct.ERelation;
import greycat.utility.Base64;

class HeapERelation implements ERelation {

    private EStruct[] _backend;
    private int _size;
    private int _capacity;
    private final HeapEStruct parent;

    HeapERelation(final HeapEStruct p_parent, final HeapERelation origin) {
        parent = p_parent;
        if (origin != null) {
            _backend = new EStruct[origin._capacity];
            System.arraycopy(origin._backend, 0, _backend, 0, origin._capacity);
            _size = origin._size;
            _capacity = origin._capacity;
        } else {
            _backend = null;
            _size = 0;
            _capacity = 0;
        }
    }

    final void rebase(HeapEStructArray newGraph) {
        for (int i = 0; i < _size; i++) {
            final HeapEStruct previous = (HeapEStruct) _backend[i];
            _backend[i] = newGraph._nodes[previous._id];
        }
    }

    @Override
    public final int size() {
        return _size;
    }

    @Override
    public final EStruct[] nodes() {
        EStruct[] copy = new EStruct[_size];
        System.arraycopy(_backend, 0, copy, 0, _size);
        return copy;
    }

    @Override
    public final EStruct node(int index) {
        return _backend[index];
    }

    @Override
    public final ERelation add(EStruct eStruct) {
        if (_capacity == _size) {
            if (_capacity == 0) {
                allocate(Constants.MAP_INITIAL_CAPACITY);
            } else {
                allocate(_capacity * 2);
            }
        }
        _backend[_size] = eStruct;
        _size++;
        if (parent != null) {
            parent.declareDirty();
        }
        return this;
    }

    @Override
    public final ERelation addAll(final EStruct[] eStructs) {
        allocate(eStructs.length + _size);
        System.arraycopy(eStructs, 0, _backend, _size, eStructs.length);
        if (parent != null) {
            parent.declareDirty();
        }
        return this;
    }

    @Override
    public final ERelation clear() {
        _size = 0;
        _backend = null;
        if (parent != null) {
            parent.declareDirty();
        }
        return this;
    }

    @Override
    public final String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("[");
        for (int i = 0; i < _size; i++) {
            if (i != 0) {
                buffer.append(",");
            }
            buffer.append(((HeapEStruct) _backend[i])._id);
        }
        buffer.append("]");
        return buffer.toString();
    }

    final void allocate(int newCapacity) {
        final int closePowerOfTwo = (int) Math.pow(2, Math.ceil(Math.log(newCapacity) / Math.log(2)));
        if (closePowerOfTwo > _capacity) {
            EStruct[] new_back = new EStruct[closePowerOfTwo];
            if (_backend != null) {
                System.arraycopy(_backend, 0, new_back, 0, _size);
            }
            _backend = new_back;
            _capacity = closePowerOfTwo;
        }
    }

    public final void save(final Buffer buffer) {
        Base64.encodeIntToBuffer(_size, buffer);
        for (int j = 0; j < _size; j++) {
            buffer.write(CoreConstants.CHUNK_VAL_SEP);
            if (_backend[j] != null) {
                Base64.encodeIntToBuffer(_backend[j].id(), buffer);
            }
        }
    }

    public final long load(final Buffer buffer, final long offset, final long max) {

        HeapEStructArray container = (HeapEStructArray) parent.egraph();
        long cursor = offset;
        byte current = buffer.read(cursor);
        boolean isFirst = true;
        long previous = offset;
        while (cursor < max && current != Constants.CHUNK_SEP && current != Constants.BLOCK_CLOSE) {
            if (current == Constants.CHUNK_VAL_SEP) {
                if (isFirst) {
                    allocate(Base64.decodeToIntWithBounds(buffer, previous, cursor));
                    isFirst = false;
                } else {
                    add(container.nodeByIndex((int) Base64.decodeToLongWithBounds(buffer, previous, cursor), true));
                }
                previous = cursor + 1;
            }
            cursor++;
            if (cursor < max) {
                current = buffer.read(cursor);
            }
        }
        if (isFirst) {
            allocate(Base64.decodeToIntWithBounds(buffer, previous, cursor));
        } else {
            add(container.nodeByIndex(Base64.decodeToIntWithBounds(buffer, previous, cursor), true));
        }
        return cursor;
    }

}
