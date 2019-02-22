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
import greycat.memory.primary.POffHeapLongArray;
import greycat.struct.Buffer;
import greycat.struct.LongArray;
import greycat.utility.Base64;

public final class OffHeapLongArray implements LongArray {

    private static int SIZE = 0;
    private static int SHIFT = 1;

    private final long index;
    private final OffHeapContainer container;

    OffHeapLongArray(final OffHeapContainer p_container, final long p_index) {
        container = p_container;
        index = p_index;
    }

    @Override
    public long get(int elemIndex) {
        container.lock();
        long result = 0;
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                result = POffHeapLongArray.get(addr, SHIFT + elemIndex);
            }
        } finally {
            container.unlock();
        }
        return (int) result;
    }

    @Override
    public void set(int elemIndex, long value) {
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                POffHeapLongArray.set(addr, SHIFT + elemIndex, value);
            }
        } finally {
            container.unlock();
        }
        container.declareDirty();
    }

    @Override
    public int size() {
        container.lock();
        long size = 0;
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                size = POffHeapLongArray.get(addr, SIZE);
            }
        } finally {
            container.unlock();
        }
        return (int) size;
    }

    @Override
    public final void init(int newSize) {
        container.lock();
        try {
            unsafe_allocate(newSize);
        } finally {
            container.unlock();
        }
        container.declareDirty();
    }

    private long unsafe_allocate(int newSize) {
        final long addr = container.addrByIndex(index);
        if (addr == OffHeapConstants.NULL_PTR) {
            //initial allocation
            final long newly = POffHeapLongArray.allocate(newSize + SHIFT);
            POffHeapLongArray.set(newly, SIZE, newSize);
            container.setAddrByIndex(index, newly);
            return newly;
        } else {
            long exAddr = POffHeapLongArray.reallocate(addr, newSize + SHIFT);
            POffHeapLongArray.set(exAddr, SIZE, newSize);
            container.setAddrByIndex(index, exAddr);
            return exAddr;
        }
    }

    @Override
    public void initWith(long[] values) {
        container.lock();
        try {
            long addr = unsafe_allocate(values.length);
            for(int i=0;i<values.length;i++){
                POffHeapLongArray.set(addr,i,values[i]);
            }
        } finally {
            container.unlock();
        }
        container.declareDirty();
    }

    @Override
    public final long[] extract() {
        long[] result = null;
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if(addr != OffHeapConstants.NULL_PTR){
                result = POffHeapLongArray.asObject(addr);
            }
        } finally {
            container.unlock();
        }
        return result;
    }

    static void save(final long addr, final Buffer buffer) {
        if (addr == OffHeapConstants.NULL_PTR) {
            return;
        }
        final long size = POffHeapLongArray.get(addr, SIZE);
        Base64.encodeLongToBuffer(size, buffer);
        for (long i = 0; i < size; i++) {
            buffer.write(Constants.CHUNK_VAL_SEP);
            Base64.encodeLongToBuffer(POffHeapLongArray.get(addr, i + SHIFT), buffer);
        }
    }

    static long clone(final long addr) {
        if (addr == OffHeapConstants.NULL_PTR) {
            return OffHeapConstants.NULL_PTR;
        }
        final long size = POffHeapLongArray.get(addr, SIZE);
        return POffHeapLongArray.cloneArray(addr, size + SHIFT);
    }

    static void free(final long addr) {
        POffHeapLongArray.free(addr);
    }

    final long load(final Buffer buffer, final long offset, final long max) {
        long cursor = offset;
        byte current = buffer.read(cursor);
        boolean isFirst = true;
        long previous = offset;
        int elemIndex = SHIFT;
        long addr = -1;
        while (cursor < max && current != Constants.CHUNK_SEP && current != Constants.CHUNK_ENODE_SEP && current != Constants.CHUNK_ESEP) {
            if (current == Constants.CHUNK_VAL_SEP) {
                if (isFirst) {
                    addr = unsafe_allocate(Base64.decodeToIntWithBounds(buffer, previous, cursor));
                    isFirst = false;
                } else {
                    POffHeapLongArray.set(addr, elemIndex, Base64.decodeToLongWithBounds(buffer, previous, cursor));
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
            unsafe_allocate(Base64.decodeToIntWithBounds(buffer, previous, cursor));
        } else {
            POffHeapLongArray.set(addr, elemIndex, Base64.decodeToLongWithBounds(buffer, previous, cursor));
        }
        return cursor;
    }

}
