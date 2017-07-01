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
package greycat.internal.heap;

import greycat.Constants;
import greycat.internal.CoreConstants;
import greycat.struct.Buffer;
import greycat.struct.LongArray;
import greycat.utility.Base64;

final class HeapLongArray implements LongArray {

    private long[] _backend = null;
    private final HeapContainer _parent;

    HeapLongArray(final HeapContainer parent) {
        this._parent = parent;
    }

    @Override
    public synchronized final long get(int index) {
        if (_backend != null) {
            if (index >= _backend.length) {
                throw new RuntimeException("Array Out of Bounds");
            }
            return _backend[index];
        }
        return -1;
    }

    @Override
    public synchronized final void set(int index, long value) {
        if (_backend == null || index >= _backend.length) {
            throw new RuntimeException("allocate first!");
        } else {
            _backend[index] = value;
            _parent.declareDirty();
        }
    }

    @Override
    public synchronized final int size() {
        if (_backend != null) {
            return _backend.length;
        }
        return 0;
    }

    @Override
    public final synchronized void clear() {
        _backend = null;
        _parent.declareDirty();
    }

    @Override
    public synchronized final void init(int size) {
        _backend = new long[size];
        _parent.declareDirty();
    }

    @Override
    public synchronized final void initWith(final long[] values) {
        _backend = new long[values.length];
        System.arraycopy(values, 0, _backend, 0, values.length);
        _parent.declareDirty();
    }

    @Override
    public final synchronized long[] extract() {
        if(_backend == null){
            return new long[0];
        }
        final long[] extracted = new long[_backend.length];
        System.arraycopy(_backend, 0, extracted, 0, _backend.length);
        return extracted;
    }

    @Override
    public final synchronized boolean removeElement(long value) {
        if (_backend == null) {
            return false;
        }
        int index = -1;
        for (int i = 0; i < _backend.length; i++) {
            if (_backend[i] == value) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            removeElementByIndexInternal(index);
            return true;
        } else {
            return false;
        }

    }

    @Override
    public final synchronized boolean removeElementbyIndex(int index) {
        if (_backend == null) {
            return false;
        }
        if (index < 0 || index >= _backend.length) {
            return false;
        }
        removeElementByIndexInternal(index);
        return true;
    }

    private void removeElementByIndexInternal(int index) {
        long[] newBackend = new long[_backend.length - 1];
        System.arraycopy(_backend, 0, newBackend, 0, index);
        System.arraycopy(_backend, index + 1, newBackend, index, _backend.length - index - 1);
        _backend = newBackend;
        _parent.declareDirty();

    }

    @Override
    public final synchronized void addElement(long value) {
        if (_backend == null) {
            _backend = new long[]{value};
        } else {
            long[] newBackend = new long[_backend.length + 1];
            System.arraycopy(_backend, 0, newBackend, 0, _backend.length);
            newBackend[_backend.length] = value;
            _backend = newBackend;
        }
        _parent.declareDirty();
    }

    @Override
    public final synchronized void addAll(long[] values) {
        if (_backend == null) {
            initWith(values);
        } else {
            long[] newBackend = new long[_backend.length + values.length];
            System.arraycopy(_backend, 0, newBackend, 0, _backend.length);
            System.arraycopy(values, 0, newBackend, _backend.length, values.length);
            _backend = newBackend;
            _parent.declareDirty();
        }
    }

    @Override
    public final synchronized boolean insertElementAt(int position, long value) {
        if (_backend == null) {
            return false;
        }
        if (position < 0 || position >= _backend.length) {
            return false;
        }
        long[] newBackend = new long[_backend.length + 1];
        System.arraycopy(_backend, 0, newBackend, 0, position);
        newBackend[position] = value;
        System.arraycopy(_backend, position, newBackend, position + 1, _backend.length - position);
        _backend = newBackend;
        _parent.declareDirty();
        return true;
    }

    @Override
    public final synchronized boolean replaceElementby(long element, long value) {
        if (_backend == null) {
            return false;
        }
        int index = -1;
        for (int i = 0; i < _backend.length; i++) {
            if (_backend[i] == element) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            _backend[index] = value;
            _parent.declareDirty();
            return true;
        } else {
            return false;
        }
    }

    public final long load(final Buffer buffer, final long offset, final long max) {
        long cursor = offset;
        byte current = buffer.read(cursor);
        boolean isFirst = true;
        long previous = offset;
        int elemIndex = 0;
        while (cursor < max && current != Constants.CHUNK_SEP && current != Constants.BLOCK_CLOSE) {
            if (current == Constants.CHUNK_VAL_SEP) {
                if (isFirst) {
                    _backend = new long[Base64.decodeToIntWithBounds(buffer, previous, cursor)];
                    isFirst = false;
                } else {
                    _backend[elemIndex] = Base64.decodeToLongWithBounds(buffer, previous, cursor);
                    elemIndex++;
                }
                previous = cursor + 1;
            }
            cursor++;
            if (cursor < max) {
                current = buffer.read(cursor);
            }
        }
        if (isFirst) {
            _backend = new long[Base64.decodeToIntWithBounds(buffer, previous, cursor)];
        } else {
            _backend[elemIndex] = Base64.decodeToLongWithBounds(buffer, previous, cursor);
        }
        return cursor;
    }

    final HeapLongArray cloneFor(HeapContainer target) {
        HeapLongArray cloned = new HeapLongArray(target);
        if (_backend != null) {
            cloned.initWith(_backend);
        }
        return cloned;
    }

    public final void save(final Buffer buffer) {
        if (_backend != null) {
            Base64.encodeIntToBuffer(_backend.length, buffer);
            for (int j = 0; j < _backend.length; j++) {
                buffer.write(CoreConstants.CHUNK_VAL_SEP);
                Base64.encodeLongToBuffer(_backend[j], buffer);
            }
        } else {
            Base64.encodeIntToBuffer(0, buffer);
        }
    }


}
