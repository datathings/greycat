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
package greycat.base;

import greycat.*;
import greycat.internal.CoreConstants;
import greycat.internal.heap.HeapBuffer;
import greycat.internal.task.TaskHelper;
import greycat.plugin.Job;
import greycat.struct.Buffer;
import greycat.utility.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static greycat.utility.L3GMap.GROUP;

public class BaseTaskResult<A> implements TaskResult<A> {

    private Object[] _backend;
    private int _capacity = 0;
    private int _size = 0;

    Exception _exception = null;
    String _output = null;
    Buffer _notifications;

    @Override
    public Object[] asArray() {
        Object[] flat = new Object[_size];
        if (_backend != null) {
            System.arraycopy(_backend, 0, flat, 0, _size);
        }
        return flat;
    }

    @Override
    public Exception exception() {
        return _exception;
    }

    @Override
    public String output() {
        return _output;
    }

    @Override
    public final TaskResult<A> setException(Exception e) {
        _exception = e;
        return this;
    }

    @Override
    public final TaskResult<A> setOutput(String output) {
        _output = output;
        return this;
    }

    @Override
    public final TaskResult<A> setNotifications(Buffer buf) {
        _notifications = buf;
        return this;
    }

    @Override
    public TaskResult<A> fillWith(TaskResult<A> source) {
        if (source != null) {
            _backend = source.asArray();
            _capacity = _backend.length;
            _size = _backend.length;
        }
        return this;
    }

    public BaseTaskResult(Object toWrap, boolean protect) {
        if (toWrap instanceof Object[]) {
            Object[] castedToWrap = (Object[]) toWrap;
            _size = ((Object[]) toWrap).length;
            _capacity = _size;
            _backend = new Object[this._size];
            if (protect) {
                for (int i = 0; i < _size; i++) {
                    Object loopObj = castedToWrap[i];
                    if (loopObj instanceof BaseNode) {
                        Node loopNode = (Node) loopObj;
                        _backend[i] = loopNode.graph().cloneNode(loopNode);
                    } else {
                        _backend[i] = loopObj;
                    }
                }
            } else {
                System.arraycopy(castedToWrap, 0, _backend, 0, this._size);
            }
        } else if (toWrap instanceof long[]) {
            long[] castedOther = (long[]) toWrap;
            _backend = new Object[castedOther.length];
            for (int i = 0; i < castedOther.length; i++) {
                _backend[i] = castedOther[i];
            }
            _capacity = _backend.length;
            _size = _capacity;
        } else if (toWrap instanceof int[]) {
            int[] castedOther = (int[]) toWrap;
            _backend = new Object[castedOther.length];
            for (int i = 0; i < castedOther.length; i++) {
                _backend[i] = castedOther[i];
            }
            _capacity = _backend.length;
            _size = _capacity;
        } else if (toWrap instanceof double[]) {
            double[] castedOther = (double[]) toWrap;
            _backend = new Object[castedOther.length];
            for (int i = 0; i < castedOther.length; i++) {
                _backend[i] = castedOther[i];
            }
            _capacity = _backend.length;
            _size = _capacity;
        } else if (toWrap instanceof ArrayList) {
            ArrayList<Object> castedOtherList = (ArrayList<Object>) toWrap;
            _backend = new Object[castedOtherList.size()];
            for (int i = 0; i < castedOtherList.size(); i++) {
                _backend[i] = castedOtherList.get(i);
            }
            _capacity = _backend.length;
            _size = _capacity;
        } else if (toWrap instanceof BaseTaskResult) {
            BaseTaskResult other = (BaseTaskResult) toWrap;
            _size = other._size;
            _capacity = other._capacity;
            if (other._backend != null) {
                _backend = new Object[other._backend.length];
                if (protect) {
                    for (int i = 0; i < _size; i++) {
                        Object loopObj = other._backend[i];
                        if (loopObj instanceof BaseNode) {
                            Node loopNode = (Node) loopObj;
                            _backend[i] = loopNode.graph().cloneNode(loopNode);
                        } else {
                            _backend[i] = loopObj;
                        }
                    }
                } else {
                    System.arraycopy(other._backend, 0, _backend, 0, _size);
                }
            }
        } else {
            if (toWrap != null) {
                _backend = new Object[1];
                _capacity = 1;
                _size = 1;
                if (toWrap instanceof BaseNode) {
                    Node toWrapNode = (Node) toWrap;
                    if (protect) {
                        _backend[0] = toWrapNode.graph().cloneNode(toWrapNode);
                    } else {
                        _backend[0] = toWrapNode;
                    }
                } else {
                    _backend[0] = toWrap;
                }
            }
        }
    }

    @Override
    public TaskResultIterator iterator() {
        return new BaseTaskResultIterator(_backend, _size);
    }

    @Override
    public A get(int index) {
        if (index < _size) {
            return (A) _backend[index];
        } else {
            return null;
        }
    }

    @Override
    public synchronized TaskResult<A> set(int index, A input) {
        if (index >= _capacity) {
            extendTil(index);
        }
        _backend[index] = input;
        if (index >= _size) {
            _size = index+1;
        }
        return this;
    }

    @Override
    public TaskResult<A> allocate(int index) {
        if (index >= _capacity) {
            if (_backend == null) {
                _backend = new Object[index];
                _capacity = index;
            } else {
                throw new RuntimeException("Not implemented yet!!!");
            }
        }
        return this;
    }

    @Override
    public TaskResult<A> add(A input) {
        if (_size >= _capacity) {
            extendTil(_size);
        }
        set(_size, input);
        return this;
    }

    @Override
    public TaskResult<A> clear() {
        this._backend = null;
        this._capacity = 0;
        this._size = 0;
        return this;
    }

    @Override
    public TaskResult<A> clone() {
        return new BaseTaskResult<A>(this, true);
    }

    @Override
    public void free() {
        for (int i = 0; i < _capacity; i++) {
            if (_backend[i] instanceof BaseNode) {
                ((Node) _backend[i]).free();
            } else if (_backend[i] instanceof BaseTaskResult) {
                ((BaseTaskResult) _backend[i]).free();
            }
        }
    }

    @Override
    public int size() {
        return this._size;
    }

    private synchronized void extendTil(int index) {
        if (_capacity <= index) {
            int newCapacity = _capacity * 2;
            if (newCapacity <= index) {
                newCapacity = index + 1;
            }
            Object[] extendedBackend = new Object[newCapacity];
            if (_backend != null) {
                System.arraycopy(_backend, 0, extendedBackend, 0, _size);
            }
            _backend = extendedBackend;
            _capacity = newCapacity;
        }
    }

    @Override
    public String toString() {
        return toJson(true);
    }

    /* { NOTIF | OUTPUT | EXCEPTION | SIZE | T | V | T | V ... } */
    @Override
    public synchronized final void saveToBuffer(Buffer buffer) {
        buffer.write(CoreConstants.BLOCK_OPEN);
        if (_notifications != null) {
            buffer.writeAll(_notifications.data());//this could be optimize
        }
        buffer.write(CoreConstants.CHUNK_SEP);
        if (_output != null) {
            Base64.encodeStringToBuffer(_output, buffer);
        }
        buffer.write(CoreConstants.CHUNK_SEP);
        if (_exception != null) {
            saveExceptionToBuffer(buffer);
        }
        buffer.write(CoreConstants.CHUNK_SEP);
        Base64.encodeIntToBuffer(_size, buffer);
        for (int i = 0; i < _size; i++) {
            final Object it = _backend[i];
            buffer.write(CoreConstants.CHUNK_SEP);
            if (it != null) {
                if (it instanceof BaseNode) {
                    Node castedNode = (Node) it;
                    Base64.encodeIntToBuffer((int) Type.NODE, buffer);
                    buffer.write(CoreConstants.CHUNK_SEP);
                    Base64.encodeLongToBuffer(castedNode.world(), buffer);
                    buffer.write(CoreConstants.CHUNK_VAL_SEP);
                    Base64.encodeLongToBuffer(castedNode.time(), buffer);
                    buffer.write(CoreConstants.CHUNK_VAL_SEP);
                    Base64.encodeLongToBuffer(castedNode.id(), buffer);
                } else if (it instanceof TaskResult) {
                    ((TaskResult)it).saveToBuffer(buffer);
                } else if (it instanceof String) {
                    Base64.encodeIntToBuffer((int) Type.STRING, buffer);
                    buffer.write(CoreConstants.CHUNK_SEP);
                    Base64.encodeStringToBuffer((String) it, buffer);
                } else if (it instanceof Long) {
                    Base64.encodeIntToBuffer((int) Type.LONG, buffer);
                    buffer.write(CoreConstants.CHUNK_SEP);
                    Base64.encodeLongToBuffer((Long) it, buffer);
                } else if (it instanceof Double) {
                    Base64.encodeIntToBuffer((int) Type.DOUBLE, buffer);
                    buffer.write(CoreConstants.CHUNK_SEP);
                    Base64.encodeDoubleToBuffer((Double) it, buffer);
                } else if (it instanceof Integer) {
                    Base64.encodeIntToBuffer((int) Type.INT, buffer);
                    buffer.write(CoreConstants.CHUNK_SEP);
                    Base64.encodeIntToBuffer((Integer) it, buffer);
                } else if (it instanceof double[]) {
                    final double[] castedDA = (double[]) it;
                    Base64.encodeIntToBuffer((int) Type.DOUBLE_ARRAY, buffer);
                    buffer.write(CoreConstants.CHUNK_SEP);
                    Base64.encodeIntToBuffer(castedDA.length, buffer);
                    for (int j = 0; j < castedDA.length; j++) {
                        buffer.write(CoreConstants.CHUNK_VAL_SEP);
                        Base64.encodeDoubleToBuffer(castedDA[j], buffer);
                    }
                } else {
                    throw new RuntimeException("Unsupported yet! "+it.getClass());
                }
            }
        }
        buffer.write(CoreConstants.BLOCK_CLOSE);
    }

    /**
     * {@native ts
     * greycat.utility.Base64.encodeStringToBuffer(this._exception.stack,buffer);
     * }
     */
    private void saveExceptionToBuffer(Buffer buffer) {
        StringBuilder sb = new StringBuilder();
        _exception.printStackTrace(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                sb.append((char) b);
            }
        }));
        Base64.encodeStringToBuffer(sb.toString(), buffer);
    }

    /**
     * {@native ts
     * let e:Error = new Error();
     * e.stack = exception;
     * this._exception = e;
     * }
     */
    private void loadExceptionFromString(String exception) {
        this._exception = new Exception(exception);
        this._exception.setStackTrace(new StackTraceElement[0]);
    }

    @Override
    public final Buffer notifications() {
        return _notifications;
    }

    private void internal_load_element(final Buffer buffer, final int previous, final int cursor, final byte type, final int index, final L3GMap<List<Tuple<Object[], Integer>>> collector) {
        Object loaded = null;
        switch (type) {
            case Type.NODE:
                final long[] keys = new long[3];
                int keys_index = 0;
                int iCursor = previous;
                int iPrevious = previous;
                while (iCursor < cursor) {
                    byte current = buffer.read(iCursor);
                    if (current == Constants.CHUNK_VAL_SEP) {
                        keys[keys_index] = Base64.decodeToLongWithBounds(buffer, iPrevious, iCursor);
                        keys_index++;
                        iPrevious = iCursor + 1;
                    }
                    iCursor++;
                }
                keys[keys_index] = Base64.decodeToLongWithBounds(buffer, iPrevious, iCursor);
                List<Tuple<Object[], Integer>> subCollector = collector.get(keys[0], keys[1], keys[2]);
                if (subCollector == null) {
                    subCollector = new ArrayList<Tuple<Object[], Integer>>();
                    collector.put(keys[0], keys[1], keys[2], subCollector);
                }
                subCollector.add(new Tuple<Object[], Integer>(_backend, index));
                break;
            case Type.STRING:
                loaded = Base64.decodeToStringWithBounds(buffer, previous, cursor);
                break;
            case Type.INT:
                loaded = Base64.decodeToIntWithBounds(buffer, previous, cursor);
                break;
            case Type.DOUBLE:
                loaded = Base64.decodeToDoubleWithBounds(buffer, previous, cursor);
                break;
            case Type.LONG:
                loaded = Base64.decodeToLongWithBounds(buffer, previous, cursor);
                break;
            case Type.DOUBLE_ARRAY: {
                int dArrayCursor = previous;
                int dArrayPrevious = previous;
                int arrayIndex = -1;
                double[] tmp = null;
                while (dArrayCursor < cursor) {
                    byte current = buffer.read(dArrayCursor);
                    if (current == Constants.CHUNK_VAL_SEP) {
                        if(arrayIndex == -1) {
                            tmp = new double[Base64.decodeToIntWithBounds(buffer, dArrayPrevious, dArrayCursor)];
                        } else {
                            tmp[arrayIndex] = Base64.decodeToDoubleWithBounds(buffer, dArrayPrevious, dArrayCursor);
                        }
                        arrayIndex++;
                        dArrayPrevious = dArrayCursor + 1;
                    }
                    dArrayCursor++;
                }
                if(dArrayCursor != dArrayPrevious) {
                    tmp[arrayIndex++] = Base64.decodeToDoubleWithBounds(buffer, dArrayPrevious, dArrayCursor);
                }
                loaded = tmp;
            }break;
        }
        if (loaded != null) {
            _backend[index] = loaded;
        }
    }

    public void loadRefs(final Graph graph, final L3GMap<List<Tuple<Object[], Integer>>> collector, final Callback<Boolean> loaded) {
        int collectorSize = collector.size();
        if (collectorSize == 0) {
            if (loaded != null) {
                loaded.on(true);
            }
        } else {
            long[] worlds = new long[collectorSize];
            long[] times = new long[collectorSize];
            long[] ids = new long[collectorSize];
            for (int i = 0; i < collectorSize; i++) {
                worlds[i] = collector.keys[i * GROUP];
                times[i] = collector.keys[i * GROUP + 1];
                ids[i] = collector.keys[i * GROUP + 2];
            }
            graph.lookupBatch(worlds, times, ids, new Callback<Node[]>() {
                @Override
                public void on(Node[] result) {
                    for (int i = 0; i < collectorSize; i++) {
                        List<Tuple<Object[], Integer>> subCollector = collector.get(worlds[i], times[i], ids[i]);
                        if (subCollector != null) {
                            for (int j = 0; j < subCollector.size(); j++) {
                                Tuple<Object[], Integer> tuple = subCollector.get(j);
                                tuple.left()[tuple.right()] = result[i];
                            }
                        }
                    }
                    if (loaded != null) {
                        loaded.on(true);
                    }
                }
            });
        }
    }

    public int load(final Buffer buffer, final int begin, final Graph graph, final L3GMap<List<Tuple<Object[], Integer>>> collector) {
        int cursor = begin;
        int previous = 0;
        int index = 0;
        byte type = -1;
        try {
            while (cursor < buffer.length()) {
                byte current = buffer.read(cursor);
                if (current == Constants.CHUNK_SEP) {
                    switch (index) {
                        case 0:
                            if (previous != cursor) {
                                _notifications = graph.newBuffer();
                                _notifications.writeAll(buffer.slice(previous, cursor - 1));
                            }
                            index++;
                            break;
                        case 1:
                            if (previous != cursor) {
                                _output = Base64.decodeToStringWithBounds(buffer, previous, cursor);
                            }
                            index++;
                            break;
                        case 2:
                            if (previous != cursor) {
                                loadExceptionFromString(Base64.decodeToStringWithBounds(buffer, previous, cursor));
                            }
                            index++;
                            break;
                        case 3:
                            int newSize = Base64.decodeToIntWithBounds(buffer, previous, cursor);
                            allocate(newSize);
                            _size = newSize;
                            index++;
                            break;
                        default:
                            if (previous == cursor) {
                                _backend[index - 4] = null;
                                index++;
                            } else {
                                if (type == -1) {
                                    type = (byte) Base64.decodeToIntWithBounds(buffer, previous, cursor);
                                } else {
                                    internal_load_element(buffer, previous, cursor, type, index - 4, collector);
                                    index++;
                                    type = -1;
                                }
                            }
                    }
                    previous = cursor + 1;
                } else if (current == Constants.BLOCK_CLOSE) {
                    try {
                        if (previous == cursor) {
                            _backend[index - 4] = null;
                        } else {
                            if (type != -1) {
                                internal_load_element(buffer, previous, cursor, type, index - 4, collector);
                            }
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    } finally {
                        return cursor;
                    }
                } else if (current == Constants.BLOCK_OPEN && cursor != begin) {
                    final BaseTaskResult subResult = new BaseTaskResult(null, false);
                    cursor = subResult.load(buffer, cursor, graph, collector);
                    cursor++;
                    previous = cursor+1;
                    _backend[index - 4] = subResult;
                    index++;
                }
                cursor++;
            }
            if (type != -1) {
                internal_load_element(buffer, previous, cursor, type, index - 4, collector);
            }
            //return cursor;
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            return cursor;
        }
    }

    private String toJson(boolean withContent) {
        final Buffer builder = new HeapBuffer();
        boolean isFirst = true;
        builder.writeString("{");
        if (_exception != null) {
            isFirst = false;
            builder.writeString("\"error\":");
            TaskHelper.serializeString(_exception.toString(), builder, false);
        }
        if (_output != null) {
            if (!isFirst) {
                builder.writeString(",");
            } else {
                isFirst = false;
            }
            builder.writeString("\"output\":");
            TaskHelper.serializeString(_output, builder, false);
        }
        if (_size > 0) {
            if (!isFirst) {
                builder.writeString(",");
            }
            builder.writeString("\"result\":[");
            for (int i = 0; i < _size; i++) {
                if (i != 0) {
                    builder.writeString(",");
                }
                Object loop = _backend[i];
                if (loop != null) {
                    final String saved = loop.toString();
                    if (saved.length() > 0) {
                        if (saved.charAt(0) == '{' || saved.charAt(0) == '[') { //Array or Nodes
                            builder.writeString(saved);
                        } else {
                            //escape string
                            TaskHelper.serializeString(saved, builder, false);
                        }
                    }
                }
            }
            builder.writeString("]");
        }
        builder.writeString("}");
        return builder.toString();
    }

}
