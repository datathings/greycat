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
package greycat.memory;

import greycat.Constants;
import greycat.Graph;
import greycat.memory.primary.POffHeapLongArray;
import greycat.struct.Buffer;
import greycat.struct.EGraph;
import greycat.struct.ENode;
import greycat.utility.Base64;

public class OffHeapEGraph implements EGraph {
    private final Graph _graph;
    private final long index;
    private final OffHeapContainer parent;

    private static final int DIRTY = 0;
    private static final int NODES_CAPACITY = 1;
    private static final int NODES_INDEX = 2;
    private static final int OFFSET = 3;

    OffHeapEGraph(final OffHeapContainer p_container, final long p_index, final Graph p_graph) {
        parent = p_container;
        index = p_index;
        _graph = p_graph;
        long originAddr = p_container.addrByIndex(p_index);
        if (originAddr == OffHeapConstants.NULL_PTR) {
            long newAddr = POffHeapLongArray.allocate(OFFSET);
            POffHeapLongArray.set(newAddr, DIRTY, 0);
            POffHeapLongArray.set(newAddr, NODES_INDEX, 0);
            POffHeapLongArray.set(newAddr, NODES_CAPACITY, 0);
            parent.setAddrByIndex(index, newAddr);
            parent.declareDirty();
        }

        /*
        if (originAddr != OffHeapConstants.NULL_PTR) {
            long nodesCapacity = POffHeapLongArray.get(originAddr, NODES_CAPACITY);
            long nodesIndex = POffHeapLongArray.get(originAddr, NODES_INDEX);
            long newAddr = POffHeapLongArray.allocate(OFFSET + nodesCapacity);
            POffHeapLongArray.set(newAddr, NODES_INDEX, nodesIndex);
            POffHeapLongArray.set(newAddr, NODES_CAPACITY, nodesCapacity);
            //pass #1: copy nodes
            for (int i = 0; i < nodesIndex; i++) {
                OffHeapENode eNode = new OffHeapENode(this, _graph, i, nodeAddrAt(originAddr, i));
                setNodeAddrAt(newAddr, i, eNode.getAddr());
            }
            //pass #2: rebase all links
            for (int i = 0; i < nodesIndex; i++) {
                OffHeapENode.rebase(nodeAddrAt(newAddr, i), newAddr);
            }
            parent.setAddrByIndex(index, newAddr);
            parent.declareDirty();
        } else {
            long newAddr = POffHeapLongArray.allocate(OFFSET);
            POffHeapLongArray.set(newAddr, DIRTY, 0);
            POffHeapLongArray.set(newAddr, NODES_INDEX, 0);
            POffHeapLongArray.set(newAddr, NODES_CAPACITY, 0);
            parent.setAddrByIndex(index, newAddr);
            parent.declareDirty();
        }*/

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
                OffHeapENode eNode = nodeByIndex(insertIndex, true);
                cursor = eNode.load(buffer, cursor);
                insertIndex++;
            } else {
                cursor++;
            }
            current = buffer.read(cursor);
        }
        return cursor;
    }

    final void allocate(int newCapacity) {
        long addr = parent.addrByIndex(index);
        final int closePowerOfTwo = (int) Math.pow(2, Math.ceil(Math.log(newCapacity) / Math.log(2)));
        long nodesCapacity = POffHeapLongArray.get(addr, NODES_CAPACITY);
        if (closePowerOfTwo > nodesCapacity) {
            long newAddr = POffHeapLongArray.reallocate(addr, OFFSET + closePowerOfTwo);
            POffHeapLongArray.fillByte(newAddr, OFFSET + nodesCapacity, OFFSET + closePowerOfTwo, (byte) OffHeapConstants.NULL_PTR);
            parent.setAddrByIndex(index, newAddr);
            POffHeapLongArray.set(newAddr, NODES_CAPACITY, closePowerOfTwo);
        }
    }

    @Override
    public final ENode root() {
        long addr = parent.addrByIndex(index);
        if (POffHeapLongArray.get(addr, NODES_INDEX) > 0) {
            return new OffHeapENode(0, this, _graph);
        }
        return null;
    }

    @Override
    public final ENode newNode() {
        long addr = parent.addrByIndex(index);
        long nodesIndex = POffHeapLongArray.get(addr, NODES_INDEX);
        long nodesCapacity = POffHeapLongArray.get(addr, NODES_CAPACITY);
        if (nodesIndex == nodesCapacity) {
            long newCapacity = nodesCapacity * 2;
            if (newCapacity == 0) {
                newCapacity = Constants.MAP_INITIAL_CAPACITY;
            }
            // reallocate
            addr = POffHeapLongArray.reallocate(addr, OFFSET + newCapacity);
            POffHeapLongArray.fillByte(addr, OFFSET + nodesCapacity, OFFSET + newCapacity, (byte) OffHeapConstants.NULL_PTR);
            parent.setAddrByIndex(index, addr);
            POffHeapLongArray.set(addr, NODES_CAPACITY, newCapacity);
        }
        OffHeapENode newNode = new OffHeapENode(nodesIndex, this, _graph);
        POffHeapLongArray.set(addr, NODES_INDEX, nodesIndex + 1);
        return newNode;
    }

    @Override
    public ENode node(int nodeIndex) {
        return nodeByIndex(nodeIndex, false);
    }

    @Override
    public final EGraph setRoot(ENode eNode) {
       // long addr = parent.addrByIndex(index);
        final OffHeapENode casted = (OffHeapENode) eNode;
        final long previousId = casted.index;
        if (previousId != 0) {
            throw new RuntimeException("Not implemented yet!!!");
        }
        return this;

/*

        long addr = parent.addrByIndex(index);
        final OffHeapENode casted = (OffHeapENode) eNode;
        final long previousId = OffHeapENode.getId(casted.index);
        if (previousId != 0) {
            throw new RuntimeException("Not implemented yet!!!");

            long previousRootAddr = nodeAddrAt(addr, 0);
            setNodeAddrAt(addr, previousId, previousRootAddr);
            OffHeapENode.setId(previousRootAddr, previousId);
            setNodeAddrAt(addr, 0, casted.getAddr());
            OffHeapENode.setId(casted.getAddr(), 0);

            //TODO rebase here
        }
        return this;
        */
    }

    @Override
    public final EGraph drop(ENode eNode) {
        //TODO rebase here
/*
        long addr = parent.addrByIndex(index);
        OffHeapENode casted = (OffHeapENode) eNode;
        long previousId = OffHeapENode.getId(casted.getAddr());
        long nodesIndex = POffHeapLongArray.get(addr, NODES_INDEX);
        if (previousId == nodesIndex - 1) {
            //free
            OffHeapENode.free(casted.getAddr());
            setNodeAddrAt(addr, previousId, OffHeapConstants.NULL_PTR);
            POffHeapLongArray.set(addr, NODES_INDEX, nodesIndex - 1);
        } else {
            setNodeAddrAt(addr, previousId, nodeAddrAt(addr, nodesIndex - 1));
            OffHeapENode.setId(nodeAddrAt(addr, previousId), previousId);
            POffHeapLongArray.set(addr, NODES_INDEX, nodesIndex - 1);
        }
        return this;
        */
        throw new RuntimeException("Not implemented yet!!!");

    }

    @Override
    public final int size() {
        long addr = parent.addrByIndex(index);
        return (int) POffHeapLongArray.get(addr, NODES_INDEX);
    }

    @Override
    public final void free() {
        long addr = parent.addrByIndex(index);
        freeByAddr(addr);
        parent.setAddrByIndex(index, OffHeapConstants.NULL_PTR);
    }


    static void freeByAddr(long addr) {
        if (addr != OffHeapConstants.NULL_PTR) {
            long nodesIndex = POffHeapLongArray.get(addr, NODES_INDEX);
            for (long i = 0; i < nodesIndex; i++) {
                OffHeapENode.free(POffHeapLongArray.get(addr, OFFSET + i));
            }
            POffHeapLongArray.free(addr);
        }
    }

    @Override
    public final Graph graph() {
        return _graph;
    }

    final void declareDirty() {
        long addr = parent.addrByIndex(index);
        long dirty = POffHeapLongArray.get(addr, DIRTY);
        if (dirty == 0) {
            POffHeapLongArray.set(addr, DIRTY, 1);
            parent.declareDirty();
        }
    }

    final void declareUnDirty() {
        long addr = parent.addrByIndex(index);
        POffHeapLongArray.set(addr, DIRTY, 0);
    }

    @Override
    public final String toString() {
        long addr = parent.addrByIndex(index);
        final StringBuilder builder = new StringBuilder();
        builder.append("{\"nodes\":[");
        long nodesIndex = POffHeapLongArray.get(addr, NODES_INDEX);
        for (int i = 0; i < nodesIndex; i++) {
            if (i != 0) {
                builder.append(",");
            }
            //TODO optimize
            OffHeapENode enode = new OffHeapENode(POffHeapLongArray.get(addr, i + OFFSET), this, _graph);
            builder.append(enode.toString());
        }
        builder.append("]}");
        return builder.toString();
    }

    final OffHeapENode nodeByIndex(final long nodeIndex, final boolean createIfAbsent) {
        final long addr = parent.addrByIndex(index);
        final long nodesCapacity = POffHeapLongArray.get(addr, NODES_CAPACITY);
        final long nodesIndex = POffHeapLongArray.get(addr, NODES_INDEX);
        if (nodeIndex < nodesCapacity) {
            if (nodeIndex >= nodesIndex) {
                POffHeapLongArray.set(addr, NODES_INDEX, nodeIndex + 1);
            }
            OffHeapENode elem = null;
            long elemAddr = POffHeapLongArray.get(addr, nodeIndex + OFFSET);
            if (elemAddr != OffHeapConstants.NULL_PTR) {
                elem = new OffHeapENode(nodeIndex, this, _graph);
            }
            if (elemAddr == OffHeapConstants.NULL_PTR && createIfAbsent) {
                elem = new OffHeapENode(nodeIndex, this, _graph);
            }
            return elem;
        } else {
            throw new RuntimeException("bad API usage");
        }
    }

    static long clone(long originAddr) {
        if (originAddr == OffHeapConstants.NULL_PTR) {
            return OffHeapConstants.NULL_PTR;
        }
        long nodesCapacity = POffHeapLongArray.get(originAddr, NODES_CAPACITY);
        long nodesIndex = POffHeapLongArray.get(originAddr, NODES_INDEX);
        long newAddr = POffHeapLongArray.allocate(OFFSET + nodesCapacity);
        POffHeapLongArray.set(newAddr, NODES_INDEX, nodesIndex);
        POffHeapLongArray.set(newAddr, NODES_CAPACITY, nodesCapacity);
        //pass #1: copy nodes
        for (int i = 0; i < nodesIndex; i++) {
            long previousNodeAddr = POffHeapLongArray.get(originAddr, i + OFFSET);
            long clonedNodeAddr = OffHeapENode.cloneENode(previousNodeAddr);
            POffHeapLongArray.set(newAddr, i + OFFSET, clonedNodeAddr);
        }
        return newAddr;
    }

//    final long getAddr() {
//        return parent.addrByIndex(index);
//    }

    public long addrByIndex(long elemIndex) {
        return POffHeapLongArray.get(parent.addrByIndex(index), elemIndex + OFFSET);
    }

    public void setAddrByIndex(long elemIndex, long newAddr) {
        POffHeapLongArray.set(parent.addrByIndex(index), elemIndex + OFFSET, newAddr);
    }

}
