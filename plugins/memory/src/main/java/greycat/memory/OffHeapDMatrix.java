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
import greycat.memory.primary.POffHeapDoubleArray;
import greycat.memory.primary.POffHeapLongArray;
import greycat.struct.Buffer;
import greycat.struct.DMatrix;
import greycat.utility.Base64;

class OffHeapDMatrix implements DMatrix {

    private static final int INDEX_ROWS = 0;
    private static final int INDEX_COLUMNS = 1;
    private static final int INDEX_MAX_COLUMN = 2;
    private static final int INDEX_OFFSET = 3;
    private final long index;
    private final OffHeapContainer container;

    OffHeapDMatrix(final OffHeapContainer p_container, final long p_index) {
        container = p_container;
        index = p_index;
    }

    @Override
    public final DMatrix init(int rows, int columns) {
        container.lock();
        try {
            long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                POffHeapDoubleArray.free(addr);
            }
            addr = POffHeapDoubleArray.allocate(rows * columns + INDEX_OFFSET);
            POffHeapDoubleArray.set(addr, INDEX_ROWS, rows);
            POffHeapDoubleArray.set(addr, INDEX_COLUMNS, columns);
            POffHeapDoubleArray.set(addr, INDEX_MAX_COLUMN, columns);
            container.setAddrByIndex(index, addr);
            container.declareDirty();
        } finally {
            container.unlock();
        }
        return this;
    }

    @Override
    public DMatrix appendColumn(double[] newColumn) {
        container.lock();
        try {
            int nbRows;
            int nbColumns;
            int nbMaxColumn;
            long addr = container.addrByIndex(index);
            long indexAddr = OffHeapConstants.NULL_PTR;
            if (addr != OffHeapConstants.NULL_PTR) {
                indexAddr = POffHeapLongArray.get(addr, INDEX_COLUMNS);
            }
            if (addr == OffHeapConstants.NULL_PTR || indexAddr == OffHeapConstants.NULL_PTR) {
                nbRows = newColumn.length;
                nbColumns = Constants.MAP_INITIAL_CAPACITY;
                nbMaxColumn = 0;
                addr = POffHeapDoubleArray.allocate(nbRows * nbColumns + INDEX_OFFSET);
                POffHeapDoubleArray.set(addr, INDEX_ROWS, nbRows);
                POffHeapDoubleArray.set(addr, INDEX_COLUMNS, nbColumns);
                POffHeapDoubleArray.set(addr, INDEX_MAX_COLUMN, nbMaxColumn);
                container.setAddrByIndex(index, addr);
            } else {
                nbRows = (int) POffHeapDoubleArray.get(addr, INDEX_ROWS);
                nbColumns = (int) POffHeapDoubleArray.get(addr, INDEX_COLUMNS);
                nbMaxColumn = (int) POffHeapDoubleArray.get(addr, INDEX_MAX_COLUMN);
            }
            if (nbMaxColumn == nbColumns) {
                nbColumns = nbColumns * 2;
                final int newLength = nbColumns * nbRows + INDEX_OFFSET;
                addr = POffHeapDoubleArray.reallocate(addr, newLength);
                POffHeapDoubleArray.set(addr, INDEX_COLUMNS, nbColumns);
                container.setAddrByIndex(index, addr);
            }
            long base = nbMaxColumn * nbRows + INDEX_OFFSET;
            for (int i = 0; i < newColumn.length; i++) {
                POffHeapDoubleArray.set(addr, i + base, newColumn[i]);
            }
            POffHeapDoubleArray.set(addr, INDEX_MAX_COLUMN, nbMaxColumn + 1);
        } finally {
            container.declareDirty();
            container.unlock();
        }
        return this;
    }

    void unsafe_init(int size) {
        long addr = container.addrByIndex(index);
        if (addr != OffHeapConstants.NULL_PTR) {
            POffHeapDoubleArray.free(addr);
        }
        addr = POffHeapDoubleArray.allocate(size);
        POffHeapDoubleArray.set(addr, INDEX_ROWS, 0);
        POffHeapDoubleArray.set(addr, INDEX_COLUMNS, 0);
        container.setAddrByIndex(index, addr);
    }

    void unsafe_set(long setIndex, double value) {
        final long addr = container.addrByIndex(index);
        if (addr != OffHeapConstants.NULL_PTR) {
            POffHeapDoubleArray.set(addr, setIndex, value);
        }
    }

    @Override
    public final DMatrix fill(double value) {
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                int nbRows = (int) POffHeapDoubleArray.get(addr, INDEX_ROWS);
                int nbColumns = (int) POffHeapDoubleArray.get(addr, INDEX_COLUMNS);
                POffHeapDoubleArray.fill(addr, INDEX_OFFSET, INDEX_OFFSET + (nbRows * nbColumns), value);
                container.declareDirty();
            }
        } finally {
            container.unlock();
        }
        return this;
    }

    @Override
    public DMatrix fillWith(double[] values) {
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                for (int i = 0; i < values.length; i++) {
                    POffHeapDoubleArray.set(addr, INDEX_OFFSET + i, values[i]);
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
                result = (int) POffHeapDoubleArray.get(addr, INDEX_ROWS);
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
                result = (int) POffHeapDoubleArray.get(addr, INDEX_MAX_COLUMN);
            }
        } finally {
            container.unlock();
        }
        return result;
    }

    @Override
    public int length() {
        container.lock();
        int result = 0;
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                result = ((int) POffHeapDoubleArray.get(addr, INDEX_MAX_COLUMN)) * ((int) POffHeapDoubleArray.get(addr, INDEX_ROWS));
            }
        } finally {
            container.unlock();
        }
        return result;
    }

    @Override
    public double[] column(int columnIndex) {
        double[] result = null;
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                long nbRows = (int) POffHeapDoubleArray.get(addr, INDEX_ROWS);
                result = new double[(int) nbRows];
                long base = INDEX_OFFSET + (columnIndex * nbRows);
                for (int i = 0; i < nbRows; i++) {
                    result[i] = POffHeapDoubleArray.get(addr, base + i);
                }
            }
        } finally {
            container.unlock();
        }
        return result;
    }

    @Override
    public final double get(int rowIndex, int columnIndex) {
        container.lock();
        double result = 0;
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final int nbRows = (int) POffHeapDoubleArray.get(addr, INDEX_ROWS);
                result = POffHeapDoubleArray.get(addr, INDEX_OFFSET + rowIndex + columnIndex * nbRows);
            }
        } finally {
            container.unlock();
        }
        return result;
    }

    @Override
    public final DMatrix set(int rowIndex, int columnIndex, double value) {
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final int nbRows = (int) POffHeapDoubleArray.get(addr, INDEX_ROWS);
                POffHeapDoubleArray.set(addr, INDEX_OFFSET + rowIndex + columnIndex * nbRows, value);
            }
        } finally {
            container.unlock();
        }
        return this;
    }

    @Override
    public DMatrix add(int rowIndex, int columnIndex, double value) {
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final int nbRows = (int) POffHeapDoubleArray.get(addr, INDEX_ROWS);
                final int raw_index = INDEX_OFFSET + rowIndex + columnIndex * nbRows;
                final double previous = POffHeapDoubleArray.get(addr, raw_index);
                POffHeapDoubleArray.set(addr, raw_index, value + previous);
            }
        } finally {
            container.unlock();
        }
        return this;
    }

    @Override
    public double[] data() {
        container.lock();
        double[] flat;
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                final int nbRows = (int) POffHeapDoubleArray.get(addr, INDEX_ROWS);
                final int nbColumns = (int) POffHeapDoubleArray.get(addr, INDEX_COLUMNS);
                final int flatSize = nbRows * nbColumns;
                flat = new double[flatSize];
                for (int i = 0; i < flatSize; i++) {
                    flat[i] = POffHeapDoubleArray.get(addr, i + INDEX_OFFSET);
                }
            } else {
                flat = new double[0];
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
                final int nbRows = (int) POffHeapDoubleArray.get(addr, INDEX_ROWS);
                final int nbColumns = (int) POffHeapDoubleArray.get(addr, INDEX_COLUMNS);
                result = Math.max(nbRows, nbColumns);
            }
        } finally {
            container.unlock();
        }
        return result;
    }

    @Override
    public double unsafeGet(int indexValue) {
        container.lock();
        double result = 0;
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                result = POffHeapDoubleArray.get(addr, INDEX_OFFSET + indexValue);
            }
        } finally {
            container.unlock();
        }
        return result;
    }

    @Override
    public DMatrix unsafeSet(int indexValue, double value) {
        container.lock();
        try {
            final long addr = container.addrByIndex(index);
            if (addr != OffHeapConstants.NULL_PTR) {
                POffHeapDoubleArray.set(addr, INDEX_OFFSET + indexValue, value);
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
        final int nbRows = (int) POffHeapDoubleArray.get(addr, INDEX_ROWS);
        final int nbColumns = (int) POffHeapDoubleArray.get(addr, INDEX_COLUMNS);
        final int flatSize = nbRows * nbColumns;
        final int size = flatSize + INDEX_OFFSET;
        Base64.encodeLongToBuffer(size, buffer);
        for (long i = 0; i < size; i++) {
            buffer.write(Constants.CHUNK_VAL_SEP);
            Base64.encodeDoubleToBuffer(POffHeapDoubleArray.get(addr, i), buffer);
        }
    }

    static long clone(final long addr) {
        if (addr == OffHeapConstants.NULL_PTR) {
            return OffHeapConstants.NULL_PTR;
        }
        final int nbRows = (int) POffHeapDoubleArray.get(addr, INDEX_ROWS);
        final int nbColumns = (int) POffHeapDoubleArray.get(addr, INDEX_COLUMNS);
        final int flatSize = nbRows * nbColumns;
        return POffHeapLongArray.cloneArray(addr, flatSize + INDEX_OFFSET);
    }

    static void free(final long addr) {
        POffHeapDoubleArray.free(addr);
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
                    unsafe_set(elemIndex, Base64.decodeToDoubleWithBounds(buffer, previous, cursor));
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
            unsafe_set(elemIndex, Base64.decodeToDoubleWithBounds(buffer, previous, cursor));
        }
        return cursor;
    }


}
