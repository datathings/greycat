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
import greycat.struct.BoolArray;
import greycat.struct.Buffer;
import greycat.utility.Base64;

final class HeapBoolArray implements BoolArray {
    private boolean[] _backend = null;
    private final HeapContainer _parent;

    public HeapBoolArray(HeapContainer parent) {
        this._parent = parent;
    }

    @Override
    public synchronized final boolean get(int index) {
        if(_backend != null) {
            if(index <0 || index>= _backend.length) {
                throw new RuntimeException("Array Out of Bounds");
            }
            return _backend[index];
        }
        return false;
    }

    @Override
    public synchronized final void set(int index, boolean value) {
        if(_backend == null) {
            throw new RuntimeException("allocate first!");
        }

        if(index <0 || index>= _backend.length) {
            throw new RuntimeException("Array Out of Bounds. Index: " + index + ". Range: [0," + _backend.length + "[");
        }

        _backend[index] = value;
        _parent.declareDirty();
    }

    /**
     *   @native ts
     *   this._backend = new Array<boolean>(values.length);
     *   java.lang.System.arraycopy(values, 0, this._backend, 0, values.length);
     *   this._parent.declareDirty();
     */
    @Override
    public synchronized final void initWith(final boolean[] values) {
        _backend = new boolean[values.length];
        System.arraycopy(values, 0, _backend, 0, values.length);
        _parent.declareDirty();
    }

    /**
     *  @native ts
     *  if (this._backend == null) {
     *  return new Array<boolean>(0);
     *  }
     *  let extracted: boolean[] = new Array<boolean>(this._backend.length);
     *  java.lang.System.arraycopy(this._backend, 0, extracted, 0, this._backend.length);
     *  return extracted;
     */
    @Override
    public synchronized final boolean[] extract() {
        if(_backend == null){
            return new boolean[0];
        }
        final boolean[] extracted = new boolean[_backend.length];
        System.arraycopy(_backend, 0, extracted, 0, _backend.length);
        return extracted;
    }

    @Override
    public synchronized final boolean removeElement(boolean value) {
        if (_backend == null) {
            return false;
        }

        for (int i = 0; i < _backend.length; i++) {
            if (_backend[i] == value) {
                removeElementByIndexInternal(i);
                return true;
            }
        }

        return false;

    }

    /**
     * @native ts
     * let newBackend: boolean[] = new Array<boolean>(this._backend.length - 1);
     * java.lang.System.arraycopy(this._backend, 0, newBackend, 0, index);
     * java.lang.System.arraycopy(this._backend, index + 1, newBackend, index, this._backend.length - index - 1);
     * this._backend = newBackend;
     * this._parent.declareDirty();
     */
    private void removeElementByIndexInternal(int index) {
        boolean[] newBackend = new boolean[_backend.length - 1];
        System.arraycopy(_backend, 0, newBackend, 0, index);
        System.arraycopy(_backend, index + 1, newBackend, index, _backend.length - index - 1);
        _backend = newBackend;
        _parent.declareDirty();
    }

    @Override
    public synchronized final boolean removeElementbyIndex(int index) {
        if (_backend == null) {
            return false;
        }
        if (index < 0 || index >= _backend.length) {
            return false;
        }
        removeElementByIndexInternal(index);
        return true;
    }

    /**
     * @native ts
     *  if (this._backend == null) {
     *  this._backend = [value];
     *  } else {
     *  let newBackend: boolean[] = new Array<boolean>(this._backend.length + 1);
     *  java.lang.System.arraycopy(this._backend, 0, newBackend, 0, this._backend.length);
     *  newBackend[this._backend.length] = value;
     *  this._backend = newBackend;
     *  }
     *  this._parent.declareDirty();
     */
    @Override
    public synchronized final void addElement(boolean value) {
        if (_backend == null) {
            _backend = new boolean[]{value};
        } else {
            boolean[] newBackend = new boolean[_backend.length + 1];
            System.arraycopy(_backend, 0, newBackend, 0, _backend.length);
            newBackend[_backend.length] = value;
            _backend = newBackend;
        }
        _parent.declareDirty();
    }

    /**
     * @native ts
     *  if (this._backend == null) {
     *  return false;
     *  }
     *  if (position < 0 || position >= this._backend.length) {
     *  return false;
     *  }
     *  let newBackend: boolean[] = new Array<boolean>(this._backend.length + 1);
     *  java.lang.System.arraycopy(this._backend, 0, newBackend, 0, position);
     *  newBackend[position] = value;
     *  java.lang.System.arraycopy(this._backend, position, newBackend, position + 1, this._backend.length - position);
     *  this._backend = newBackend;
     *  this._parent.declareDirty();
     *  return true;
     */
    @Override
    public synchronized final boolean insertElementAt(int position, boolean value) {
        if (_backend == null) {
            return false;
        }
        if (position < 0 || position >= _backend.length) {
            return false;
        }
        boolean[] newBackend = new boolean[_backend.length + 1];
        System.arraycopy(_backend, 0, newBackend, 0, position);
        newBackend[position] = value;
        System.arraycopy(_backend, position, newBackend, position + 1, _backend.length - position);
        _backend = newBackend;
        _parent.declareDirty();
        return true;
    }

    @Override
    public synchronized final boolean replaceElementby(boolean element, boolean value) {
        if (_backend == null) {
            return false;
        }

        for (int i = 0; i < _backend.length; i++) {
            if (_backend[i] == element) {
                _backend[i] = value;
                _parent.declareDirty();
                return true;
            }
        }
        return false;
    }

    /**
     * @native ts
     * if (this._backend == null) {
     * this.initWith(values);
     * } else {
     * let newBackend: boolean[] = new Array<boolean>(this._backend.length + values.length);
     * java.lang.System.arraycopy(this._backend, 0, newBackend, 0, this._backend.length);
     * java.lang.System.arraycopy(values, 0, newBackend, this._backend.length, values.length);
     * this._backend = newBackend;
     * this._parent.declareDirty();
     * }
     */
    @Override
    public synchronized final void addAll(boolean[] values) {
        if (_backend == null) {
            initWith(values);
        } else {
            boolean[] newBackend = new boolean[_backend.length + values.length];
            System.arraycopy(_backend, 0, newBackend, 0, _backend.length);
            System.arraycopy(values, 0, newBackend, _backend.length, values.length);
            _backend = newBackend;
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
    public synchronized final void clear() {
        _backend = null;
        _parent.declareDirty();
    }

    /**
     *  @native ts
     *  this._backend = new Array<boolean>(size);
     *  this._parent.declareDirty();
     */
    @Override
    public synchronized final void init(int size) {
        _backend = new boolean[size];
        _parent.declareDirty();
    }

    public final long load(final Buffer buffer, final long offset, final long max) {
        long cursor = offset;
        byte current = buffer.read(cursor);
        while (cursor < max && current != Constants.CHUNK_SEP && current != Constants.CHUNK_ENODE_SEP && current != Constants.CHUNK_ESEP) {
            cursor++;
            if(cursor < max) {
                current = buffer.read(cursor);
            }
        }

        _backend = Base64.decodeToBoolArrayWithBounds(buffer,offset,cursor);
        return cursor;
    }

    final HeapBoolArray cloneFor(HeapContainer target) {
        HeapBoolArray cloned = new HeapBoolArray(target);
        if (_backend != null) {
            cloned.initWith(_backend);
        }
        return cloned;
    }


}
