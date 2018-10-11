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
import greycat.struct.StringArray;
import greycat.utility.Base64;

final class HeapStringArray implements StringArray {

    private String[] _backend = null;
    private final HeapContainer _parent;

    HeapStringArray(final HeapContainer parent) {
        this._parent = parent;
    }

    @Override
    public final synchronized String get(int index) {
        if (_backend != null) {
            if (index >= _backend.length) {
                throw new RuntimeException("Array Out of Bounds");
            }
            return _backend[index];
        }
        return null;
    }

    @Override
    public final synchronized void set(int index, String value) {
        if (_backend == null || index >= _backend.length) {
            throw new RuntimeException("allocate first!");
        } else {
            _backend[index] = value;
            _parent.declareDirty();
        }
    }

    @Override
    public final synchronized int size() {
        if (_backend != null) {
            return _backend.length;
        }
        return 0;
    }

    @Override
    public final synchronized void init(int size) {
        _backend = new String[size];
        _parent.declareDirty();
    }

    @Override
    public final synchronized void clear() {
        _backend = null;
        _parent.declareDirty();
    }

    @Override
    public synchronized final void initWith(final String[] values) {
        _backend = new String[values.length];
        System.arraycopy(values, 0, _backend, 0, values.length);
        _parent.declareDirty();
    }

    @Override
    public final synchronized String[] extract() {
        if (_backend == null) {
            return new String[0];
        }
        final String[] extracted = new String[_backend.length];
        System.arraycopy(_backend, 0, extracted, 0, _backend.length);
        return extracted;
    }

    @Override
    public final synchronized boolean removeElement(String value) {
        if (_backend == null) {
            return false;
        }
        int index = -1;
        for (int i = 0; i < _backend.length; i++) {
            if (_backend[i].equals(value)) {
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
        String[] newBackend = new String[_backend.length - 1];
        System.arraycopy(_backend, 0, newBackend, 0, index);
        System.arraycopy(_backend, index + 1, newBackend, index, _backend.length - index - 1);
        _backend = newBackend;
        _parent.declareDirty();
    }

    @Override
    public final synchronized StringArray addElement(String value) {
        if (_backend == null) {
            _backend = new String[]{value};
        } else {
            String[] newBackend = new String[_backend.length + 1];
            System.arraycopy(_backend, 0, newBackend, 0, _backend.length);
            newBackend[_backend.length] = value;
            _backend = newBackend;
        }
        _parent.declareDirty();
        return this;
    }

    @Override
    public final synchronized boolean insertElementAt(int position, String value) {
        if (_backend == null) {
            return false;
        }
        if (position < 0 || position >= _backend.length) {
            return false;
        }
        String[] newBackend = new String[_backend.length + 1];
        System.arraycopy(_backend, 0, newBackend, 0, position);
        newBackend[position] = value;
        System.arraycopy(_backend, position, newBackend, position + 1, _backend.length - position);
        _backend = newBackend;
        _parent.declareDirty();
        return true;
    }

    @Override
    public final synchronized boolean replaceElementby(String element, String value) {
        if (_backend == null) {
            return false;
        }
        int index = -1;
        for (int i = 0; i < _backend.length; i++) {
            if (_backend[i].equals(element)) {
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

    @Override
    public final synchronized void addAll(String[] values) {
        if (_backend == null) {
            initWith(values);
        } else {
            String[] newBackend = new String[_backend.length + values.length];
            System.arraycopy(_backend, 0, newBackend, 0, _backend.length);
            System.arraycopy(values, 0, newBackend, _backend.length, values.length);
            _backend = newBackend;
            _parent.declareDirty();
        }
    }

    public final void save(final Buffer buffer) {
        if (_backend != null) {
            Base64.encodeIntToBuffer(_backend.length, buffer);
            for (int j = 0; j < _backend.length; j++) {
                buffer.write(CoreConstants.CHUNK_VAL_SEP);
                Base64.encodeStringToBuffer(_backend[j], buffer);
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
        int elemIndex = 0;
        while (cursor < max && current != Constants.CHUNK_SEP && current != Constants.BLOCK_CLOSE) {
            if (current == Constants.CHUNK_VAL_SEP) {
                if (isFirst) {
                    _backend = new String[Base64.decodeToIntWithBounds(buffer, previous, cursor)];
                    isFirst = false;
                } else {
                    _backend[elemIndex] = Base64.decodeToStringWithBounds(buffer, previous, cursor);
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
            _backend = new String[Base64.decodeToIntWithBounds(buffer, previous, cursor)];
        } else {
            _backend[elemIndex] = Base64.decodeToStringWithBounds(buffer, previous, cursor);
        }
        return cursor;
    }

    final HeapStringArray cloneFor(HeapContainer target) {
        HeapStringArray cloned = new HeapStringArray(target);
        if (_backend != null) {
            cloned.initWith(_backend);
        }
        return cloned;
    }

}
