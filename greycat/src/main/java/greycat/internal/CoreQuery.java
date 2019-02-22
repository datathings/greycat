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
package greycat.internal;

import greycat.Constants;
import greycat.Graph;
import greycat.struct.Buffer;
import greycat.utility.Base64;
import greycat.utility.HashHelper;
import greycat.Query;
import greycat.plugin.Resolver;

public class CoreQuery implements Query {

    private final Resolver _resolver;
    private final Graph _graph;
    private int capacity = 1;
    private int[] _attributes = new int[capacity];
    private String[] _values = new String[capacity];
    private int size = 0;
    private Long _hash;

    private long _world = Constants.NULL_LONG;
    private long _time = Constants.NULL_LONG;

    public CoreQuery(Graph graph, Resolver p_resolver) {
        _graph = graph;
        _resolver = p_resolver;
        _hash = null;
    }

    @Override
    public final long world() {
        return _world;
    }

    @Override
    public final Query setWorld(final long p_world) {
        this._world = p_world;
        return this;
    }

    @Override
    public final long time() {
        return _time;
    }

    @Override
    public final Query setTime(final long p_time) {
        this._time = p_time;
        return this;
    }

    /*
    @Override
    public Query parse(String flatQuery) {
        int cursor = 0;
        long currentKey = Constants.NULL_LONG;
        int lastElemStart = 0;
        while (cursor < flatQuery.length()) {
            if (flatQuery.charAt(cursor) == Constants.QUERY_KV_SEP) {
                if (lastElemStart != -1) {
                    currentKey = _resolver.stringToHash(flatQuery.substring(lastElemStart, cursor).trim(), false);
                }
                lastElemStart = cursor + 1;
            } else if (flatQuery.charAt(cursor) == Constants.TASK_PARAM_SEP) {
                if (currentKey != Constants.NULL_LONG) {
                    internal_add(currentKey, flatQuery.substring(lastElemStart, cursor).trim());
                }
                currentKey = Constants.NULL_LONG;
                lastElemStart = cursor + 1;
            }
            cursor++;
        }
        //insert the last element
        if (currentKey != Constants.NULL_LONG) {
            internal_add(currentKey, flatQuery.substring(lastElemStart, cursor).trim());
        }
        return this;
    }*/

    @Override
    public Query add(String attributeName, String value) {
        internal_add(_resolver.stringToHash(attributeName.trim(), false), value);
        return this;
    }

    @Override
    public Query addRaw(int attributeNameHash, String value) {
        internal_add(attributeNameHash, value);
        return this;
    }

    public long hash() {
        if (_hash == null) {
            compute();
        }
        return this._hash;
    }

    @Override
    public int[] attributes() {
        return this._attributes;
    }

    @Override
    public Object[] values() {
        return this._values;
    }

    private void internal_add(int att, String val) {
        if (size == capacity) {
            //init
            int temp_capacity = capacity * 2;
            int[] temp_attributes = new int[temp_capacity];
            String[] temp_values = new String[temp_capacity];
            //copy
            System.arraycopy(_attributes, 0, temp_attributes, 0, capacity);
            System.arraycopy(_values, 0, temp_values, 0, capacity);
            //assign
            _attributes = temp_attributes;
            _values = temp_values;
            capacity = temp_capacity;

        }
        _attributes[size] = att;
        _values[size] = val;
        size++;
    }

    private void compute() {
        //SORT
        for (int i = (size - 1); i >= 0; i--) {
            for (int j = 1; j <= i; j++) {
                if (_attributes[j - 1] > _attributes[j]) {
                    int tempK = _attributes[j - 1];
                    String tempV = _values[j - 1];
                    _attributes[j - 1] = _attributes[j];
                    _values[j - 1] = _values[j];
                    _attributes[j] = tempK;
                    _values[j] = tempV;
                }
            }
        }
        Buffer buf = _graph.newBuffer();
        for (int i = 0; i < size; i++) {
            Base64.encodeLongToBuffer(_attributes[i], buf);
            Object loopValue = _values[i];
            if (loopValue != null) {
                Base64.encodeStringToBuffer(_values[i], buf);
            }
        }
        _hash = HashHelper.hashBytes(buf.data());
        buf.free();
    }

}
