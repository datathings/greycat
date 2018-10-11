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
import greycat.Graph;
import greycat.internal.CoreConstants;
import greycat.struct.Buffer;
import greycat.struct.EStructArray;
import greycat.struct.EStruct;
import greycat.utility.Base64;

class HeapEStructArray implements EStructArray {

    private final Graph _graph;
    private final HeapContainer parent;

    HeapEStruct[] _nodes = null;
    private int _nodes_capacity = 0;
    private int _nodes_index = 0;

    HeapEStructArray(final HeapContainer p_parent, final HeapEStructArray origin, final Graph p_graph) {
        parent = p_parent;
        _graph = p_graph;
        if (origin != null) {
            _nodes_index = origin._nodes_index;
            _nodes_capacity = origin._nodes_capacity;
            _nodes = new HeapEStruct[_nodes_capacity];
            //pass #1: copy nodes
            for (int i = 0; i < _nodes_index; i++) {
                _nodes[i] = new HeapEStruct(this, i, origin._nodes[i]);
            }
            //pass #2: rebase all links
            for (int i = 0; i < _nodes_index; i++) {
                _nodes[i].rebase();
            }
        }
    }

    @Override
    public final int size() {
        return _nodes_index;
    }

    @Override
    public final void free() {
        _nodes = null;
        _nodes_capacity = 0;
        _nodes_index = 0;
    }

    @Override
    public final Graph graph() {
        return _graph;
    }

    private void allocate(int newCapacity) {
        final int closePowerOfTwo = (int) Math.pow(2, Math.ceil(Math.log(newCapacity) / Math.log(2)));
        if (closePowerOfTwo > _nodes_capacity) {
            HeapEStruct[] new_back = new HeapEStruct[closePowerOfTwo];
            if (_nodes != null) {
                System.arraycopy(_nodes, 0, new_back, 0, _nodes_index);
            }
            _nodes = new_back;
            _nodes_capacity = closePowerOfTwo;
        }
    }

    final HeapEStruct nodeByIndex(final int index, final boolean createIfAbsent) {
        if (index < _nodes_capacity) {
            if (index >= _nodes_index) {
                _nodes_index = index + 1;
            }
            HeapEStruct elem = _nodes[index];
            if (elem == null && createIfAbsent) {
                elem = new HeapEStruct(this, index, null);
                _nodes[index] = elem;
            }
            return elem;
        } else {
            //return null;
            throw new RuntimeException("bad API usage");
        }
    }

    final void declareDirty() {
        if (parent != null) {
            parent.declareDirty();
        }
    }

    @Override
    public final EStruct newEStruct() {
        if (_nodes_index == _nodes_capacity) {
            int newCapacity = _nodes_capacity * 2;
            if (newCapacity == 0) {
                newCapacity = Constants.MAP_INITIAL_CAPACITY;
            }
            HeapEStruct[] newNodes = new HeapEStruct[newCapacity];
            if (_nodes != null) {
                System.arraycopy(_nodes, 0, newNodes, 0, _nodes_capacity);
            }
            _nodes_capacity = newCapacity;
            _nodes = newNodes;
        }
        HeapEStruct newNode = new HeapEStruct(this, _nodes_index, null);
        _nodes[_nodes_index] = newNode;
        _nodes_index++;
        return newNode;
    }

    @Override
    public EStruct estruct(int nodeIndex) {
        return nodeByIndex(nodeIndex, false);
    }

    @Override
    public final EStruct root() {
        if (_nodes_index > 0) {
            return _nodes[0];
        }
        return null;
    }

    @Override
    public final EStructArray setRoot(EStruct eStruct) {
        HeapEStruct casted = (HeapEStruct) eStruct;
        final int previousID = casted._id;
        if (previousID != 0) {
            HeapEStruct previousRoot = _nodes[0];
            _nodes[previousID] = previousRoot;
            previousRoot._id = previousID;
            _nodes[0] = casted;
            casted._id = 0;
        }
        return this;
    }

    @Override
    public final EStructArray drop(final EStruct eStruct) {
        int previousId = eStruct.id();
        if (previousId == _nodes_index - 1) {
            //free
            _nodes[previousId] = null;
            _nodes_index--;
        } else {
            _nodes[previousId] = _nodes[_nodes_index - 1];
            _nodes[previousId]._id = previousId;
            _nodes_index--;
        }
        return this;
    }

    @Override
    public final String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("{\"nodes\":[");
        for (int i = 0; i < _nodes_index; i++) {
            if (i != 0) {
                builder.append(",");
            }
            builder.append(_nodes[i].toString());
        }
        builder.append("]}");
        return builder.toString();
    }

    public final void save(final Buffer buffer) {
        if (_nodes != null) {
            Base64.encodeIntToBuffer(_nodes_index, buffer);
            for (int j = 0; j < _nodes_index; j++) {
                buffer.write(CoreConstants.BLOCK_OPEN);
                _nodes[j].save(buffer);
                buffer.write(CoreConstants.BLOCK_CLOSE);
            }
        } else {
            Base64.encodeIntToBuffer(0, buffer);
        }
    }

    public final long load(final Buffer buffer, final long offset, final long max) {
        long cursor = offset;
        byte current = buffer.read(cursor);
        boolean isFirst = true;
        int insertIndex = 0;
        while (cursor < max && current != Constants.CHUNK_SEP && current != Constants.BLOCK_CLOSE) {
            if (current == Constants.BLOCK_OPEN) {
                if (isFirst) {
                    allocate(Base64.decodeToIntWithBounds(buffer, offset, cursor));
                    isFirst = false;
                }
                cursor++;
                final HeapEStruct eNode = nodeByIndex(insertIndex, true);
                cursor = eNode.load(buffer, cursor, _graph);
                insertIndex++;
            }
            cursor++; //consume block end
            if (cursor < max) {
                current = buffer.read(cursor);
            }
        }
        return cursor;
    }

}
