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
package greycat.memory;

import greycat.Constants;
import greycat.memory.primary.POffHeapLongArray;
import greycat.struct.Buffer;
import greycat.struct.LMatrix;
import greycat.utility.Base64;

import java.util.Random;

class OffHeapLMatrix implements LMatrix {

    private static final int INDEX_ROWS = 0;
    private static final int INDEX_COLUMNS = 1;
    private static final int INDEX_MAX_COLUMN = 2;
    private static final int INDEX_OFFSET = 3;
    private final long index;
    private final OffHeapContainer container;

    OffHeapLMatrix(final OffHeapContainer p_container, final long p_index) {
        container = p_container;
        index = p_index;
    }

    @Override
    public final LMatrix init(int rows, int columns) {
        container.lock();
        try {
            long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                POffHeapLongArray.free(addr);
            }
            addr = POffHeapLongArray.allocate(rows * columns + INDEX_OFFSET);
            POffHeapLongArray.set(addr, INDEX_ROWS, rows);
            POffHeapLongArray.set(addr, INDEX_COLUMNS, columns);
            POffHeapLongArray.set(addr, INDEX_MAX_COLUMN, columns);
            container.setAddrByIndex(index, addr);
            container.declareDirty();
        } finally {
            container.unlock();
        }
        return this;
    }

    @Override
    public LMatrix appendColumn(long[] newColumn) {
        container.lock();
        try {
            long nbRows;
            long nbColumns;
            long nbMaxColumn;
            long addr = container.addrByIndex(index);
            long indexAddr = OffHeapConstants.NULL_PTR;
            if (addr != OffHeapConstants.NULL_PTR) {
                indexAddr = POffHeapLongArray.get(addr, INDEX_COLUMNS);
            }
            if (addr == OffHeapConstants.NULL_PTR || indexAddr == OffHeapConstants.NULL_PTR) {
                nbRows = newColumn.length;
                nbColumns = Constants.MAP_INITIAL_CAPACITY;
                nbMaxColumn = 0;
                addr = POffHeapLongArray.allocate(nbRows * nbColumns + INDEX_OFFSET);
                POffHeapLongArray.set(addr, INDEX_ROWS, nbRows);
                POffHeapLongArray.set(addr, INDEX_COLUMNS, nbColumns);
                POffHeapLongArray.set(addr, INDEX_MAX_COLUMN, nbMaxColumn);
                container.setAddrByIndex(index, addr);
            } else {
                nbRows = (int) POffHeapLongArray.get(addr, INDEX_ROWS);
                nbColumns = (int) POffHeapLongArray.get(addr, INDEX_COLUMNS);
                nbMaxColumn = (int) POffHeapLongArray.get(addr, INDEX_MAX_COLUMN);
            }
            if (nbMaxColumn == nbColumns) {
                nbColumns = nbColumns * 2;
                final long newLength = nbColumns * nbRows + INDEX_OFFSET;
                addr = POffHeapLongArray.reallocate(addr, newLength);
                POffHeapLongArray.set(addr, INDEX_COLUMNS, nbColumns);
                container.setAddrByIndex(index, addr);
            }

            long base = nbMaxColumn * nbRows + INDEX_OFFSET;
            for (int i = 0; i < newColumn.length; i++) {
                POffHeapLongArray.set(addr, i + base, newColumn[i]);
            }
            POffHeapLongArray.set(addr, INDEX_MAX_COLUMN, nbMaxColumn + 1);
        } finally {
            container.declareDirty();
            container.unlock();
        }
        return this;
    }

    void unsafe_init(int size) {
        long addr = container.addrByIndex(index);
        if (addr != OffHeapConstants.NULL_PTR) {
            POffHeapLongArray.free(addr);
        }
        addr = POffHeapLongArray.allocate(size);
        POffHeapLongArray.set(addr, INDEX_ROWS, 0);
        POffHeapLongArray.set(addr, INDEX_COLUMNS, 0);
        container.setAddrByIndex(index, addr);
    }

    void unsafe_set(long setIndex, long value) {
        final long addr = container.addrByIndex(index);
        if (addr != OffHeapConstants.NULL_PTR) {
            POffHeapLongArray.set(addr, setIndex, value);
        }
    }

    @Override
    public final LMatrix fill(long value) {
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                long nbRows = POffHeapLongArray.get(addr, INDEX_ROWS);
                long nbColumns = POffHeapLongArray.get(addr, INDEX_COLUMNS);
                POffHeapLongArray.fillLong(addr, INDEX_OFFSET, (nbRows * nbColumns), value);
                container.declareDirty();
            }
        } finally {
            container.unlock();
        }
        return this;
    }

    @Override
    public LMatrix fillWith(long[] values) {
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                for (int i = 0; i < values.length; i++) {
                    POffHeapLongArray.set(addr, INDEX_OFFSET + i, values[i]);
                }
                container.declareDirty();
            }
        } finally {
            container.unlock();
        }
        return this;
    }

    @Override
    public LMatrix fillWithRandom(long min, long max, long seed) {
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final long nbRows = POffHeapLongArray.get(addr, INDEX_ROWS);
                final long nbColumns = POffHeapLongArray.get(addr, INDEX_COLUMNS);
                final Random rand = new Random();
                rand.setSeed(seed);
                for (int i = 0; i < nbColumns * nbRows; i++) {
                    POffHeapLongArray.set(addr, INDEX_OFFSET + i, rand.nextLong() * (max - min) + min);
                }
                container.declareDirty();
            }
        } finally {
            container.unlock();
        }
        return this;
    }

    @Override
    public final int rows() {
        container.lock();
        int result = 0;
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                result = (int) POffHeapLongArray.get(addr, INDEX_ROWS);
            }
        } finally {
            container.unlock();
        }
        return result;
    }

    @Override
    public final int columns() {
        container.lock();
        int result = 0;
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                result = (int) POffHeapLongArray.get(addr, INDEX_MAX_COLUMN);
            }
        } finally {
            container.unlock();
        }
        return result;
    }

    @Override
    public long[] column(int columnIndex) {
        long[] result = null;
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                long nbRows = POffHeapLongArray.get(addr, INDEX_ROWS);
                result = new long[(int) nbRows];
                long base = INDEX_OFFSET + (columnIndex * nbRows);
                for (int i = 0; i < nbRows; i++) {
                    result[i] = POffHeapLongArray.get(addr, base + i);
                }
            }
        } finally {
            container.unlock();
        }
        return result;
    }

    @Override
    public final long get(int rowIndex, int columnIndex) {
        container.lock();
        long result = 0;
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final long nbRows = (int) POffHeapLongArray.get(addr, INDEX_ROWS);
                result = POffHeapLongArray.get(addr, INDEX_OFFSET + rowIndex + columnIndex * nbRows);
            }
        } finally {
            container.unlock();
        }
        return result;
    }

    @Override
    public final LMatrix set(int rowIndex, int columnIndex, long value) {
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final long nbRows = POffHeapLongArray.get(addr, INDEX_ROWS);
                POffHeapLongArray.set(addr, INDEX_OFFSET + rowIndex + columnIndex * nbRows, value);
            }
        } finally {
            container.unlock();
        }
        return this;
    }

    @Override
    public LMatrix add(int rowIndex, int columnIndex, long value) {
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final long nbRows = POffHeapLongArray.get(addr, INDEX_ROWS);
                final long raw_index = INDEX_OFFSET + rowIndex + columnIndex * nbRows;
                final long previous = POffHeapLongArray.get(addr, raw_index);
                POffHeapLongArray.set(addr, raw_index, value + previous);
            }
        } finally {
            container.unlock();
        }
        return this;
    }

    @Override
    public long[] data() {
        container.lock();
        long[] flat;
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final int nbRows = (int) POffHeapLongArray.get(addr, INDEX_ROWS);
                final int nbColumns = (int) POffHeapLongArray.get(addr, INDEX_COLUMNS);
                final int flatSize = nbRows * nbColumns;
                flat = new long[flatSize];
                for (int i = 0; i < flatSize; i++) {
                    flat[i] = POffHeapLongArray.get(addr, i + INDEX_OFFSET);
                }
            } else {
                flat = new long[0];
            }
        } finally {
            container.unlock();
        }
        return flat;
    }

    @Override
    public int leadingDimension() {
        container.lock();
        int result = 0;
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final int nbRows = (int) POffHeapLongArray.get(addr, INDEX_ROWS);
                final int nbColumns = (int) POffHeapLongArray.get(addr, INDEX_COLUMNS);
                result = Math.max(nbRows, nbColumns);
            }
        } finally {
            container.unlock();
        }
        return result;
    }

    @Override
    public long unsafeGet(int indexValue) {
        container.lock();
        long result = 0;
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                result = POffHeapLongArray.get(addr, INDEX_OFFSET + indexValue);
            }
        } finally {
            container.unlock();
        }
        return result;
    }

    @Override
    public LMatrix unsafeSet(int indexValue, long value) {
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                POffHeapLongArray.set(addr, INDEX_OFFSET + indexValue, value);
            }
        } finally {
            container.unlock();
        }
        return this;
    }

    /*
    @Override
    public final String toString() {
        StringBuilder buffer = new StringBuilder();
        //chunk.lock();
        // try {
        final long addr = chunk.addrByIndex(index);
        if (addr != OffHeapConstants.NULL_PTR) {
            buffer.append("[");
            final long size = POffHeapLongArray.get(addr, SIZE);
            for (int i = 0; i < size; i++) {
                if (i != 0) {
                    buffer.append(",");
                }
                buffer.append(POffHeapLongArray.get(addr, i + SHIFT));
            }
            buffer.append("]");
        }
        //} finally {
        // chunk.unlock();
        //}
        return buffer.toString();
    }*/

    static void save(final long addr, final Buffer buffer) {
        if (addr == OffHeapConstants.NULL_PTR) {
            return;
        }
        final long nbRows = POffHeapLongArray.get(addr, INDEX_ROWS);
        final long nbColumns = POffHeapLongArray.get(addr, INDEX_COLUMNS);
        final long flatSize = nbRows * nbColumns;
        final long size = flatSize + INDEX_OFFSET;
        Base64.encodeLongToBuffer(size, buffer);
        for (long i = 0; i < size; i++) {
            buffer.write(Constants.CHUNK_VAL_SEP);
            Base64.encodeLongToBuffer(POffHeapLongArray.get(addr, i), buffer);
        }
    }

    static long clone(final long addr) {
        if (addr == OffHeapConstants.NULL_PTR) {
            return OffHeapConstants.NULL_PTR;
        }
        final long nbRows = (int) POffHeapLongArray.get(addr, INDEX_ROWS);
        final long nbColumns = (int) POffHeapLongArray.get(addr, INDEX_COLUMNS);
        final long flatSize = nbRows * nbColumns;
        return POffHeapLongArray.cloneArray(addr, flatSize + INDEX_OFFSET);
    }

    static void free(final long addr) {
        POffHeapLongArray.free(addr);
    }

    final long load(final Buffer buffer, final long offset, final long max) {
        long cursor = offset;
        byte current = buffer.read(cursor);
        boolean isFirst = true;
        long previous = offset;
        long elemIndex = 0;
        while (cursor < max && current != Constants.CHUNK_SEP && current != Constants.CHUNK_ENODE_SEP && current != Constants.CHUNK_ESEP) {
            if (current == Constants.CHUNK_VAL_SEP) {
                if (isFirst) {
                    unsafe_init(Base64.decodeToIntWithBounds(buffer, previous, cursor));
                    isFirst = false;
                } else {
                    unsafe_set(elemIndex, Base64.decodeToLongWithBounds(buffer, previous, cursor));
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
            unsafe_init((int) Base64.decodeToLongWithBounds(buffer, previous, cursor));
        } else {
            unsafe_set(elemIndex, Base64.decodeToLongWithBounds(buffer, previous, cursor));
        }
        return cursor;
    }

}
