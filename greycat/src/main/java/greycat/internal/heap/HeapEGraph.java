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
import greycat.Container;
import greycat.Graph;
import greycat.struct.Buffer;
import greycat.struct.EGraph;
import greycat.struct.ENode;
import greycat.utility.Base64;

class HeapEGraph implements EGraph {

    private final Graph _graph;
    private final HeapContainer parent;

    boolean _dirty;
    HeapENode[] _nodes = null;
    private int _nodes_capacity = 0;
    private int _nodes_index = 0;

    HeapEGraph(final HeapContainer p_parent, final HeapEGraph origin, final Graph p_graph) {
        parent = p_parent;
        _graph = p_graph;
        if (origin != null) {
            _nodes_index = origin._nodes_index;
            _nodes_capacity = origin._nodes_capacity;
            _nodes = new HeapENode[_nodes_capacity];
            //pass #1: copy nodes
            for (int i = 0; i < _nodes_index; i++) {
                _nodes[i] = new HeapENode(this, i, origin._nodes[i]);
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
            HeapENode[] new_back = new HeapENode[closePowerOfTwo];
            if (_nodes != null) {
                System.arraycopy(_nodes, 0, new_back, 0, _nodes_index);
            }
            _nodes = new_back;
            _nodes_capacity = closePowerOfTwo;
        }
    }

    final HeapENode nodeByIndex(final int index, final boolean createIfAbsent) {
        if (index < _nodes_capacity) {
            if (index >= _nodes_index) {
                _nodes_index = index + 1;
            }
            HeapENode elem = _nodes[index];
            if (elem == null && createIfAbsent) {
                elem = new HeapENode(this, index, null);
                _nodes[index] = elem;
            }
            return elem;
        } else {
            //return null;
            throw new RuntimeException("bad API usage");
        }
    }

    final void declareDirty() {
        if (!_dirty) {
            _dirty = true;
            if (parent != null) {
                parent.declareDirty();
            }
        }
    }

    @Override
    public final ENode newNode() {
        if (_nodes_index == _nodes_capacity) {
            int newCapacity = _nodes_capacity * 2;
            if (newCapacity == 0) {
                newCapacity = Constants.MAP_INITIAL_CAPACITY;
            }
            HeapENode[] newNodes = new HeapENode[newCapacity];
            if (_nodes != null) {
                System.arraycopy(_nodes, 0, newNodes, 0, _nodes_capacity);
            }
            _nodes_capacity = newCapacity;
            _nodes = newNodes;
        }
        HeapENode newNode = new HeapENode(this, _nodes_index, null);
        _nodes[_nodes_index] = newNode;
        _nodes_index++;
        return newNode;
    }

    @Override
    public ENode node(int nodeIndex) {
        return nodeByIndex(nodeIndex, false);
    }

    @Override
    public final ENode root() {
        if (_nodes_index > 0) {
            return _nodes[0];
        }
        return null;
    }

    @Override
    public final EGraph setRoot(ENode eNode) {
        HeapENode casted = (HeapENode) eNode;
        final int previousID = casted._id;
        if (previousID != 0) {
            HeapENode previousRoot = _nodes[0];
            _nodes[previousID] = previousRoot;
            previousRoot._id = previousID;
            _nodes[0] = casted;
            casted._id = 0;
        }
        return this;
    }

    @Override
    public final EGraph drop(ENode eNode) {
        HeapENode casted = (HeapENode) eNode;
        int previousId = casted._id;
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

    public final long load(final Buffer buffer, final long offset, final long max) {
        long cursor = offset;
        byte current = buffer.read(cursor);
        boolean isFirst = true;
        int insertIndex = 0;
        while (cursor < max && current != Constants.CHUNK_SEP) {
            if (current == Constants.CHUNK_ENODE_SEP) {
                if (isFirst) {
                    allocate(Base64.decodeToIntWithBounds(buffer, offset, cursor));
                    isFirst = false;
                }
                cursor++;
                HeapENode eNode = nodeByIndex(insertIndex, true);
                cursor = eNode.load(buffer, cursor, _graph);
                insertIndex++;
            } else {
                cursor++;
            }
            if (cursor < max) {
                current = buffer.read(cursor);
            }
        }
        return cursor;
    }

}
