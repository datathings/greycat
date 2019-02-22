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
import greycat.struct.ENode;
import greycat.struct.ERelation;

public class OffHeapERelation implements ERelation {

    private static final int SIZE = 0;
    private static final int CAPACITY = 1;
    private static final int OFFSET = 2;

    private final long index;
    private final OffHeapContainer container;
    private final OffHeapEGraph eGraph;
    private final Graph graph;

    OffHeapERelation(final OffHeapContainer p_container, final long p_index, OffHeapEGraph eGraph, Graph graph) {
        container = p_container;
        index = p_index;
        this.eGraph = eGraph;
        this.graph = graph;
    }

    final long allocate(long newCapacity) {
        long addr = container.addrByIndex(index);
        if (addr != OffHeapConstants.NULL_PTR) {
            final long capacity = POffHeapLongArray.get(addr, CAPACITY);
            if (capacity < newCapacity) {
                final long closePowerOfTwo = (long) Math.pow(2, Math.ceil(Math.log(newCapacity) / Math.log(2)));
                addr = POffHeapLongArray.reallocate(addr, OFFSET + closePowerOfTwo);
                container.setAddrByIndex(index, addr);
            }
        } else {
            final long closePowerOfTwo = (long) Math.pow(2, Math.ceil(Math.log(newCapacity) / Math.log(2)));
            addr = POffHeapLongArray.allocate(OFFSET + closePowerOfTwo);
            POffHeapLongArray.set(addr, SIZE, 0);
            POffHeapLongArray.set(addr, CAPACITY, closePowerOfTwo);
            container.setAddrByIndex(index, addr);
        }
        return addr;
    }

    static long clone(final long addr) {
        if (addr == OffHeapConstants.NULL_PTR) {
            return OffHeapConstants.NULL_PTR;
        }
        final long capacity = POffHeapLongArray.get(addr, CAPACITY);
        return POffHeapLongArray.cloneArray(addr, capacity + OFFSET);
    }

    @Override
    public ENode[] nodes() {
        long addr = container.addrByIndex(index);
        int size = (int) POffHeapLongArray.get(addr, SIZE);
        ENode[] nodes = new ENode[size];
        for (int i = 0; i < size; i++) {
            nodes[i] = new OffHeapENode(nodeIndexAt(addr, i), eGraph, graph);
        }
        return nodes;
    }

    @Override
    public ENode node(int nodeIndex) {
        return new OffHeapENode(nodeIndex, eGraph, graph);
    }

    @Override
    public int size() {
        long addr = container.addrByIndex(index);
        return (int) POffHeapLongArray.get(addr, SIZE);
    }

    @Override
    public ERelation add(ENode eNode) {
        long addr = container.addrByIndex(index);
        long size = 0;
        if (addr == OffHeapConstants.NULL_PTR) {
            addr = allocate(Constants.MAP_INITIAL_CAPACITY);
        } else {
            long capacity = POffHeapLongArray.get(addr, CAPACITY);
            size = POffHeapLongArray.get(addr, SIZE);
            if (capacity == size) {
                addr = allocate(capacity * 2);
            }
        }
        setNodeIndexAt(addr, size, ((OffHeapENode) eNode).index);
        POffHeapLongArray.set(addr, SIZE, size + 1);
        eGraph.declareDirty();
        return this;
    }

    @Override
    public ERelation addAll(ENode[] eNodes) {
        long addr = container.addrByIndex(index);
        long size = 0;
        if (addr == OffHeapConstants.NULL_PTR) {
            addr = allocate(Constants.MAP_INITIAL_CAPACITY);
        } else {
            size = POffHeapLongArray.get(addr, SIZE);
            long capacity = POffHeapLongArray.get(addr, CAPACITY);
            long neededCapacity = eNodes.length + size;
            if (neededCapacity > capacity) {
                final long closePowerOfTwo = (long) Math.pow(2, Math.ceil(Math.log(neededCapacity) / Math.log(2)));
                addr = allocate(OFFSET + closePowerOfTwo);
            }
        }
        for (int i = 0; i < eNodes.length; i++) {
            setNodeIndexAt(addr, size + i, ((OffHeapENode) eNodes[i]).index);
        }
        eGraph.declareDirty();
        return this;
    }

    @Override
    public ERelation clear() {
        long addr = container.addrByIndex(index);
        POffHeapLongArray.set(addr, SIZE, 0);
        eGraph.declareDirty();
        return this;
    }

    /*
    static void rebase(long addr, long newEGraphAddr) {
        long size = POffHeapLongArray.get(addr, SIZE);

        for (int i = 0; i < size; i++) {
            long previousNodeAddr = nodeAddrAt(addr, i);
            long previousNodeId = OffHeapENode.getId(previousNodeAddr);

            long newNodeAddr = OffHeapEGraph.nodeAddrAt(newEGraphAddr, previousNodeId);
            setNodeAddrAt(addr, i, newNodeAddr);
        }
    }*/

    static void free(long addr) {
        POffHeapLongArray.free(addr);
    }

    static long nodeIndexAt(long addr, long index) {
        return POffHeapLongArray.get(addr, OFFSET + index);
    }

    static void setNodeIndexAt(long addr, long index, long value) {
        POffHeapLongArray.set(addr, OFFSET + index, value);
    }

    @Override
    public final String toString() {
        long addr = container.addrByIndex(index);
        StringBuilder buffer = new StringBuilder();
        buffer.append("[");
        long size = POffHeapLongArray.get(addr, SIZE);
        for (long i = 0; i < size; i++) {
            if (i != 0) {
                buffer.append(",");
            }
            buffer.append(nodeIndexAt(addr, i));
        }
        buffer.append("]");
        return buffer.toString();
    }

    public long index() {
        return this.index;
    }


}
