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
import greycat.Node;
import greycat.struct.Buffer;
import greycat.utility.Base64;
import greycat.struct.Relation;

class HeapRelation implements Relation {

    private long[] _back;
    private volatile int _size;
    private final HeapContainer parent;
    private boolean aligned = true;

    HeapRelation(final HeapContainer p_parent, final HeapRelation origin) {
        parent = p_parent;
        if (origin != null) {
            aligned = false;
            _back = origin._back;
            _size = origin._size;
        } else {
            _back = null;
            _size = 0;
        }
    }

    final void allocate(int _capacity) {
        if (_capacity > 0) {
            long[] new_back = new long[_capacity];
            if (_back != null) {
                System.arraycopy(_back, 0, new_back, 0, _back.length);
            }
            _back = new_back;
            aligned = true;
        }
    }

    @Override
    public long[] all() {
        long[] ids;
        synchronized (parent) {
            if (_back == null) {
                ids = new long[0];
            } else {
                final int relSize = _size;
                ids = new long[relSize];
                for (int i = 0; i < relSize; i++) {
                    ids[i] = _back[i];
                }
            }
        }
        return ids;
    }

    @Override
    public final int size() {
        return _size;
    }

    @Override
    public final long get(int index) {
        long result;
        synchronized (parent) {
            result = _back[index];
        }
        return result;
    }

    @Override
    public final void set(int index, long value) {
        synchronized (parent) {
            _back[index] = value;
        }
    }

    final long unsafe_get(int index) {
        return _back[index];
    }

    @Override
    public final Relation addNode(Node node) {
        return add(node.id());
    }

    @Override
    public final Relation add(long newValue) {
        synchronized (parent) {
            internal_add(newValue);
            parent.declareDirty();
        }
        return this;
    }

    private void internal_add(long newValue) {
        if (_back == null) {
            aligned = true;
            _back = new long[Constants.MAP_INITIAL_CAPACITY];
            _back[0] = newValue;
            _size = 1;
        } else if (_size == _back.length) {
            long[] ex_back = new long[_back.length * 2];
            System.arraycopy(_back, 0, ex_back, 0, _size);
            _back = ex_back;
            _back[_size] = newValue;
            aligned = true;
            _size++;
        } else {
            if (!aligned) {
                long[] temp_back = new long[_back.length];
                System.arraycopy(_back, 0, temp_back, 0, _back.length);
                _back = temp_back;
                aligned = true;
            }
            _back[_size] = newValue;
            _size++;
        }
    }

    @Override
    public Relation addAll(final long[] newValues) {
        synchronized (parent) {
            int nextSize = newValues.length + _size;
            final int closePowerOfTwo = (int) Math.pow(2, Math.ceil(Math.log(nextSize) / Math.log(2)));
            allocate(closePowerOfTwo);
            System.arraycopy(newValues, 0, _back, _size, newValues.length);
            parent.declareDirty();
        }
        return this;
    }

    @Override
    public final Relation insert(final int targetIndex, final long newValue) {
        synchronized (parent) {
            if (_back == null) {
                if (targetIndex != 0) {
                    throw new RuntimeException("Bad API usage ! index out of bounds: " + targetIndex);
                }
                _back = new long[Constants.MAP_INITIAL_CAPACITY];
                _back[0] = newValue;
                _size = 1;
                aligned = true;
            } else if (_size == _back.length) {
                if (targetIndex > _size) {
                    throw new RuntimeException("Bad API usage ! index out of bounds: " + targetIndex);
                }
                final long[] ex_back = new long[_back.length * 2];
                if (_size == targetIndex) {
                    System.arraycopy(_back, 0, ex_back, 0, _size);
                    _back = ex_back;
                    _back[_size] = newValue;
                    _size++;
                } else {
                    System.arraycopy(_back, 0, ex_back, 0, targetIndex);
                    ex_back[targetIndex] = newValue;
                    System.arraycopy(_back, targetIndex, ex_back, targetIndex + 1, (_size - targetIndex));
                    _back = ex_back;
                    _size++;
                }
                aligned = true;
            } else {
                if (targetIndex > _size) {
                    throw new RuntimeException("Bad API usage ! index out of bounds: " + targetIndex);
                }
                if (!aligned) {
                    long[] temp_back = new long[_back.length];
                    System.arraycopy(_back, 0, temp_back, 0, _back.length);
                    _back = temp_back;
                    aligned = true;
                }
                final int afterIndexSize = _size - targetIndex;
                long[] temp = new long[afterIndexSize];
                System.arraycopy(_back, targetIndex, temp, 0, afterIndexSize);
                _back[targetIndex] = newValue;
                System.arraycopy(temp, 0, _back, targetIndex + 1, afterIndexSize);
                _size++;
            }
            parent.declareDirty();
        }
        return this;
    }

    @Override
    public final Relation remove(long oldValue) {
        synchronized (parent) {
            int indexToRemove = -1;
            for (int i = 0; i < _size; i++) {
                if (_back[i] == oldValue) {
                    indexToRemove = i;
                    break;
                }
            }
            if (indexToRemove != -1) {
                if (!aligned) {
                    long[] temp_back = new long[_back.length];
                    System.arraycopy(_back, 0, temp_back, 0, _back.length);
                    _back = temp_back;
                    aligned = true;
                }
                System.arraycopy(_back, indexToRemove + 1, _back, indexToRemove, _size - indexToRemove - 1);
                _size--;
            }
        }
        return this;
    }

    @Override
    public final Relation delete(int toRemoveIndex) {
        synchronized (parent) {
            if (toRemoveIndex != -1) {
                if (!aligned) {
                    long[] temp_back = new long[_back.length];
                    System.arraycopy(_back, 0, temp_back, 0, _back.length);
                    _back = temp_back;
                    aligned = true;
                }
                System.arraycopy(_back, toRemoveIndex + 1, _back, toRemoveIndex, _size - toRemoveIndex - 1);
                _size--;
            }
        }
        return this;
    }

    @Override
    public final Relation clear() {
        synchronized (parent) {
            _back = null;
            _size = 0;
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
            buffer.append(_back[i]);
        }
        buffer.append("]");
        return buffer.toString();
    }

    public final long load(final Buffer buffer, final long offset, final long max) {
        long cursor = offset;
        byte current = buffer.read(cursor);
        boolean isFirst = true;
        long previous = offset;
        while (cursor < max && current != Constants.CHUNK_SEP && current != Constants.CHUNK_ENODE_SEP && current != Constants.CHUNK_ESEP) {
            if (current == Constants.CHUNK_VAL_SEP) {
                if (isFirst) {
                    allocate(Base64.decodeToIntWithBounds(buffer, previous, cursor));
                    isFirst = false;
                } else {
                    internal_add(Base64.decodeToLongWithBounds(buffer, previous, cursor));
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
            internal_add(Base64.decodeToLongWithBounds(buffer, previous, cursor));
        }
        return cursor;
    }

}
