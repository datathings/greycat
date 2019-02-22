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
import greycat.chunk.ChunkType;
import greycat.chunk.TimeTreeChunk;
import greycat.chunk.TreeWalker;
import greycat.internal.CoreConstants;
import greycat.memory.primary.POffHeapLongArray;
import greycat.struct.Buffer;
import greycat.utility.Base64;

class OffHeapTimeTreeChunk implements TimeTreeChunk {

    private static final int DIRTY = 0;
    private static final int SIZE = 1;
    private static final int CAPACITY = 2;
    private static final int HEAD = 3;
    private static final int MAGIC = 4;
    private static final int EXTRA = 5;
    private static final int EXTRA2 = 6;

    private static final int OFFSET = 7;
    private static final int ELEM_SIZE = 5;

    private final OffHeapChunkSpace space;
    private final long index;

    OffHeapTimeTreeChunk(final OffHeapChunkSpace p_space, final long p_index) {
        space = p_space;
        index = p_index;
        space.lockByIndex(index);
        try {
            long temp_addr = space.addrByIndex(index);
            if (temp_addr == OffHeapConstants.NULL_PTR) {
                long initialCapacity = Constants.MAP_INITIAL_CAPACITY;
                temp_addr = POffHeapLongArray.allocate(OFFSET + (initialCapacity * ELEM_SIZE));
                space.setAddrByIndex(index, temp_addr);
                //init the initial values
                POffHeapLongArray.set(temp_addr, MAGIC, 0);
                POffHeapLongArray.set(temp_addr, CAPACITY, initialCapacity);
                POffHeapLongArray.set(temp_addr, SIZE, 0);
                POffHeapLongArray.set(temp_addr, DIRTY, 0);
                POffHeapLongArray.set(temp_addr, HEAD, -1);
                POffHeapLongArray.set(temp_addr, EXTRA, 0);
                POffHeapLongArray.set(temp_addr, EXTRA2, 0);
            }
        } finally {
            space.unlockByIndex(index);
        }
    }

    public static void free(final long addr) {
        if (addr != OffHeapConstants.NULL_PTR) {
            POffHeapLongArray.free(addr);
        }
    }

    @Override
    public final long world() {
        return space.worldByIndex(index);
    }

    @Override
    public final long time() {
        return space.timeByIndex(index);
    }

    @Override
    public final long id() {
        return space.idByIndex(index);
    }

    @Override
    public final int size() {
        long result;
        space.lockByIndex(index);
        try {
            result = POffHeapLongArray.get(space.addrByIndex(index), SIZE);
        } finally {
            space.unlockByIndex(index);
        }
        return (int) result;
    }

    @Override
    public long extra() {
        long result;
        space.lockByIndex(index);
        try {
            result = POffHeapLongArray.get(space.addrByIndex(index), EXTRA);
        } finally {
            space.unlockByIndex(index);
        }
        return result;
    }

    @Override
    public void setExtra(long extraValue) {
        space.lockByIndex(index);
        try {
            POffHeapLongArray.set(space.addrByIndex(index), EXTRA, extraValue);
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public long extra2() {
        long result;
        space.lockByIndex(index);
        try {
            result = POffHeapLongArray.get(space.addrByIndex(index), EXTRA2);
        } finally {
            space.unlockByIndex(index);
        }
        return result;
    }

    @Override
    public void setExtra2(long extraValue) {
        space.lockByIndex(index);
        try {
            POffHeapLongArray.set(space.addrByIndex(index), EXTRA2, extraValue);
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public final long index() {
        return index;
    }

    @Override
    public final long magic() {
        long result;
        space.lockByIndex(index);
        try {
            result = POffHeapLongArray.get(space.addrByIndex(index), MAGIC);
        } finally {
            space.unlockByIndex(index);
        }
        return result;
    }

    @Override
    public final void range(final long startKey, final long endKey, final long maxElements, final TreeWalker walker) {
        space.lockByIndex(index);
        try {
            final long addr = space.addrByIndex(index);
            long nbElements = 0;
            long indexEnd = internal_previousOrEqual_index(addr, endKey);
            while (indexEnd != -1 && key(addr, indexEnd) >= startKey && nbElements < maxElements) {
                walker.elem(key(addr, indexEnd));
                nbElements++;
                indexEnd = previous(addr, indexEnd);
            }
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public final void save(final Buffer buffer) {
        space.lockByIndex(index);
        try {
            final long addr = space.addrByIndex(index);
            final int size = (int) POffHeapLongArray.get(addr, SIZE);
            final long extra = POffHeapLongArray.get(addr, EXTRA);
            final long extra2 = POffHeapLongArray.get(addr, EXTRA2);
            if (extra != CoreConstants.NULL_LONG && extra != 0) {
                Base64.encodeLongToBuffer(extra, buffer);
                buffer.write(CoreConstants.CHUNK_SEP);
            }
            if (extra2 != CoreConstants.NULL_LONG && extra2 != 0) {
                Base64.encodeLongToBuffer(extra2, buffer);
                buffer.write(CoreConstants.CHUNK_SEP);
            }
            Base64.encodeIntToBuffer(size, buffer);
            for (long i = 0; i < size; i++) {
                buffer.write(Constants.CHUNK_VAL_SEP);
                Base64.encodeLongToBuffer(key(addr, i), buffer);
            }
            POffHeapLongArray.set(addr, DIRTY, 0);
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public final void saveDiff(Buffer buffer) {
        space.lockByIndex(index);
        try {
            final long addr = space.addrByIndex(index);
            final boolean dirty = POffHeapLongArray.get(addr, DIRTY) == 1;
            if (dirty) {
                final int size = (int) POffHeapLongArray.get(addr, SIZE);
                final long extra = POffHeapLongArray.get(addr, EXTRA);
                final long extra2 = POffHeapLongArray.get(addr, EXTRA2);
                if (extra != CoreConstants.NULL_LONG && extra != 0) {
                    Base64.encodeLongToBuffer(extra, buffer);
                    buffer.write(CoreConstants.CHUNK_SEP);
                }
                if (extra2 != CoreConstants.NULL_LONG && extra2 != 0) {
                    Base64.encodeLongToBuffer(extra2, buffer);
                    buffer.write(CoreConstants.CHUNK_SEP);
                }
                Base64.encodeIntToBuffer(size, buffer);
                for (long i = 0; i < size; i++) {
                    buffer.write(Constants.CHUNK_VAL_SEP);
                    Base64.encodeLongToBuffer(key(addr, i), buffer);
                }
                POffHeapLongArray.set(addr, DIRTY, 0);
            }
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public final void load(Buffer buffer) {
        if (buffer == null || buffer.length() == 0) {
            return;
        }
        space.lockByIndex(index);
        try {
            long addr = space.addrByIndex(index);
            internal_load(addr, buffer);
        } finally {
            space.unlockByIndex(index);
        }
    }

    private boolean internal_load(long addr, final Buffer buffer) {
        boolean isDirty = false;
        long cursor = 0;
        long previous = 0;
        long payloadSize = buffer.length();
        boolean isFirst = true;
        boolean isFirstExtra = true;
        while (cursor < payloadSize) {
            final byte current = buffer.read(cursor);
            switch (current) {
                case Constants.CHUNK_SEP:
                    if (isFirstExtra) {
                        POffHeapLongArray.set(addr, EXTRA, Base64.decodeToLongWithBounds(buffer, previous, cursor));
                        previous = cursor + 1;
                        isFirstExtra = false;
                    } else {
                        POffHeapLongArray.set(addr, EXTRA2, Base64.decodeToLongWithBounds(buffer, previous, cursor));
                        previous = cursor + 1;
                    }
                    break;
                case Constants.CHUNK_VAL_SEP:
                    if (isFirst) {
                        final int treeSize = Base64.decodeToIntWithBounds(buffer, previous, cursor);
                        final int closePowerOfTwo = (int) Math.pow(2, Math.ceil(Math.log(treeSize) / Math.log(2)));
                        addr = reallocate(addr, (int) POffHeapLongArray.get(addr, CAPACITY), closePowerOfTwo);
                        previous = cursor + 1;
                        isFirst = false;
                    } else {
                        boolean insertResult = internal_insert(addr, Base64.decodeToLongWithBounds(buffer, previous, cursor), true);
                        isDirty = isDirty || insertResult;
                        previous = cursor + 1;
                    }
                    break;
            }
            cursor++;
        }
        boolean insertResult = internal_insert(addr, Base64.decodeToLongWithBounds(buffer, previous, cursor), true);
        isDirty = isDirty || insertResult;
        return isDirty;
    }


    @Override
    public final void loadDiff(Buffer buffer) {
        if (buffer == null || buffer.length() == 0) {
            return;
        }
        space.lockByIndex(index);
        try {
            long addr = space.addrByIndex(index);
            boolean isDirty = internal_load(addr, buffer);
            if (isDirty && POffHeapLongArray.get(addr, DIRTY) != 1) {
                POffHeapLongArray.set(addr, DIRTY, 1);
                space.notifyUpdate(index);
            }
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public final long previousOrEqual(final long key) {
        space.lockByIndex(index);
        try {
            final long addr = space.addrByIndex(index);
            long resultKey;
            final long result = internal_previousOrEqual_index(addr, key);
            if (result != -1) {
                resultKey = key(addr, result);
            } else {
                resultKey = Constants.NULL_LONG;
            }
            return resultKey;
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public final long previous(long key) {
        space.lockByIndex(index);
        try {
            final long addr = space.addrByIndex(index);
            long resultKey;
            final long result = internal_previous_index(addr, key);
            if (result != -1) {
                resultKey = key(addr, result);
            } else {
                resultKey = Constants.NULL_LONG;
            }
            return resultKey;
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public long next(long key) {
        long resultKey;
        space.lockByIndex(index);
        try {
            final long addr = space.addrByIndex(index);
            long result = internal_previousOrEqual_index(addr, key);
            if (result != -1) {
                result = internal_next_index(addr, result);
            }
            if (result != -1) {
                resultKey = key(addr, result);
            } else {
                resultKey = Constants.NULL_LONG;
            }
        } finally {
            space.unlockByIndex(index);
        }
        return resultKey;
    }

    @Override
    public final void insert(final long insertKey) {
        space.lockByIndex(index);
        try {
            if (internal_insert(space.addrByIndex(index), insertKey, false)) {
                internal_set_dirty(space.addrByIndex(index));
            }
        } finally {
            space.unlockByIndex(index);
        }
    }

    @Override
    public final void unsafe_insert(final long p_key) {
        internal_insert(space.addrByIndex(index), p_key, false);
    }

    @Override
    public final byte chunkType() {
        return ChunkType.TIME_TREE_CHUNK;
    }

    @Override
    public final void clearAt(long max) {
        space.lockByIndex(index);
        try {
            final long previous_addr = space.addrByIndex(index);
            final long previousCapacity = POffHeapLongArray.get(previous_addr, CAPACITY);
            final long previousSize = POffHeapLongArray.get(previous_addr, SIZE);

            //alocate the new segment
            long new_addr = POffHeapLongArray.allocate(OFFSET + (previousCapacity * ELEM_SIZE));
            POffHeapLongArray.set(new_addr, MAGIC, POffHeapLongArray.get(previous_addr, MAGIC) + 1);
            POffHeapLongArray.set(new_addr, CAPACITY, previousCapacity);
            POffHeapLongArray.set(new_addr, SIZE, 0);
            POffHeapLongArray.set(new_addr, DIRTY, 0);
            POffHeapLongArray.set(new_addr, HEAD, -1);

            for (long i = 0; i < previousSize; i++) {
                long currentVal = key(previous_addr, i);
                if (currentVal < max) {
                    internal_insert(new_addr, currentVal, false);
                }
            }
            space.setAddrByIndex(index, new_addr);
            POffHeapLongArray.free(previous_addr);
            internal_set_dirty(new_addr);
        } finally {
            space.unlockByIndex(index);
        }
    }

    private long reallocate(final long addr, final int previousCapacity, final int newCapacity) {
        if (previousCapacity < newCapacity) {
            final long new_addr = POffHeapLongArray.reallocate(addr, OFFSET + (newCapacity * ELEM_SIZE));
            POffHeapLongArray.set(new_addr, CAPACITY, newCapacity);
            space.setAddrByIndex(index, new_addr);
            return new_addr;
        } else {
            return addr;
        }
    }

    private static long key(final long addr, final long index) {
        if (index == -1) {
            return -1;
        }
        return POffHeapLongArray.get(addr, OFFSET + (index * 5));
    }

    private static void setKey(final long addr, final long index, final long insertKey, final boolean initial) {
        POffHeapLongArray.set(addr, OFFSET + (index * 5), insertKey);
        if (initial) {
            setDiff(addr, index, false);
        } else {
            setDiff(addr, index, true);
        }
    }

    private static final long TRUE_CLEAN = 0;
    private static final long FALSE_CLEAN = 1;
    private static final long TRUE_DIFF = 2;
    private static final long FALSE_DIFF = 3;

    private static boolean diff(final long addr, final long index) {
        if (index == -1) {
            return true;
        }
        long previous = POffHeapLongArray.get(addr, OFFSET + (index * 5) + 1);
        return previous == TRUE_DIFF || previous == FALSE_DIFF;
    }

    private static void setDiff(final long addr, final long index, boolean insertDiff) {
        final long previous = POffHeapLongArray.get(addr, OFFSET + (index * 5) + 1);
        final long next;
        if (previous == TRUE_CLEAN || previous == TRUE_DIFF) {
            if (insertDiff) {
                next = TRUE_DIFF;
            } else {
                next = TRUE_CLEAN;
            }
        } else {
            if (insertDiff) {
                next = FALSE_DIFF;
            } else {
                next = FALSE_CLEAN;
            }
        }
        POffHeapLongArray.set(addr, OFFSET + (index * 5) + 1, next);
    }

    private static boolean color(final long addr, final long index) {
        if (index == -1) {
            return true;
        }
        final long previous = POffHeapLongArray.get(addr, OFFSET + (index * 5) + 1);
        return previous == TRUE_CLEAN || previous == TRUE_DIFF;
    }

    private static void setColor(final long addr, final long index, boolean insertColor) {
        final long previous = POffHeapLongArray.get(addr, OFFSET + (index * 5) + 1);
        final long next;
        if (previous == TRUE_DIFF || previous == FALSE_DIFF) {
            if (insertColor) {
                next = TRUE_DIFF;
            } else {
                next = FALSE_DIFF;
            }
        } else {
            if (insertColor) {
                next = TRUE_CLEAN;
            } else {
                next = FALSE_CLEAN;
            }
        }
        POffHeapLongArray.set(addr, OFFSET + (index * 5) + 1, next);
    }

    private static long left(final long addr, final long index) {
        if (index == -1) {
            return -1;
        }
        return POffHeapLongArray.get(addr, OFFSET + (index * 5) + 2);
    }

    private static void setLeft(final long addr, final long index, final long insertLeft) {
        POffHeapLongArray.set(addr, OFFSET + (index * 5) + 2, insertLeft);
    }

    private static long right(final long addr, final long index) {
        if (index == -1) {
            return -1;
        }
        return POffHeapLongArray.get(addr, OFFSET + (index * 5) + 3);
    }

    private static void setRight(final long addr, final long index, final long insertRight) {
        POffHeapLongArray.set(addr, OFFSET + +(index * 5) + 3, insertRight);
    }

    private static long parent(final long addr, final long index) {
        if (index == -1) {
            return -1;
        }
        return POffHeapLongArray.get(addr, OFFSET + (index * 5) + 4);
    }

    private static void setParent(final long addr, final long index, final long insertParent) {
        POffHeapLongArray.set(addr, OFFSET + (index * 5) + 4, insertParent);
    }

    private static long grandParent(final long addr, final long index) {
        if (index == -1) {
            return -1;
        }
        final long selfParent = parent(addr, index);
        if (selfParent != -1) {
            return parent(addr, selfParent);
        } else {
            return -1;
        }
    }

    private static long sibling(final long addr, final long index) {
        final long selfParent = parent(addr, index);
        if (selfParent == -1) {
            return -1;
        } else {
            if (index == left(addr, selfParent)) {
                return right(addr, selfParent);
            } else {
                return left(addr, selfParent);
            }
        }
    }

    private static long uncle(final long addr, final long index) {
        final long selfParent = parent(addr, index);
        if (selfParent != -1) {
            return sibling(addr, selfParent);
        } else {
            return -1;
        }
    }

    private static long previous(final long addr, final long index) {
        long p = index;
        if (left(addr, p) != -1) {
            p = left(addr, p);
            while (right(addr, p) != -1) {
                p = right(addr, p);
            }
            return p;
        } else {
            if (parent(addr, p) != -1) {
                if (p == right(addr, parent(addr, p))) {
                    return parent(addr, p);
                } else {
                    while (parent(addr, p) != -1 && p == left(addr, parent(addr, p))) {
                        p = parent(addr, p);
                    }
                    return parent(addr, p);
                }
            } else {
                return -1;
            }
        }
    }

    private static void rotateLeft(final long addr, final long n) {
        long r = right(addr, n);
        replaceNode(addr, n, r);
        setRight(addr, n, left(addr, r));
        if (left(addr, r) != -1) {
            setParent(addr, left(addr, r), n);
        }
        setLeft(addr, r, n);
        setParent(addr, n, r);
    }

    private static void rotateRight(final long addr, final long n) {
        long l = left(addr, n);
        replaceNode(addr, n, l);
        setLeft(addr, n, right(addr, l));
        if (right(addr, l) != -1) {
            setParent(addr, right(addr, l), n);
        }
        setRight(addr, l, n);
        setParent(addr, n, l);
    }

    private static void replaceNode(final long addr, final long oldn, final long newn) {
        final long parentOldN = parent(addr, oldn);
        if (parentOldN == -1) {
            POffHeapLongArray.set(addr, HEAD, newn);
        } else {
            if (oldn == left(addr, parentOldN)) {
                setLeft(addr, parentOldN, newn);
            } else {
                setRight(addr, parentOldN, newn);
            }
        }
        if (newn != -1) {
            setParent(addr, newn, parentOldN);
        }
    }

    private static void insertCase1(final long addr, final long n) {
        if (parent(addr, n) == -1) {
            setColor(addr, n, true);
        } else {
            insertCase2(addr, n);
        }
    }

    private static void insertCase2(final long addr, final long n) {
        if (!color(addr, parent(addr, n))) {
            insertCase3(addr, n);
        }
    }

    private static void insertCase3(final long addr, final long n) {
        final long uncleN = uncle(addr, n);
        if (!color(addr, uncleN)) {
            setColor(addr, parent(addr, n), true);
            setColor(addr, uncleN, true);
            final long grandParentN = grandParent(addr, n);
            setColor(addr, grandParentN, false);
            insertCase1(addr, grandParentN);
        } else {
            insertCase4(addr, n);
        }
    }

    private static void insertCase4(final long addr, final long n_n) {
        long n = n_n;
        if (n == right(addr, parent(addr, n)) && parent(addr, n) == left(addr, grandParent(addr, n))) {
            rotateLeft(addr, parent(addr, n));
            n = left(addr, n);
        } else {
            if (n == left(addr, parent(addr, n)) && parent(addr, n) == right(addr, grandParent(addr, n))) {
                rotateRight(addr, parent(addr, n));
                n = right(addr, n);
            }
        }
        insertCase5(addr, n);
    }

    private static void insertCase5(final long addr, final long n) {
        setColor(addr, parent(addr, n), true);
        setColor(addr, grandParent(addr, n), false);
        if (n == left(addr, parent(addr, n)) && parent(addr, n) == left(addr, grandParent(addr, n))) {
            rotateRight(addr, grandParent(addr, n));
        } else {
            rotateLeft(addr, grandParent(addr, n));
        }
    }

    private static long internal_previous_index(final long addr, final long p_key) {
        long p = POffHeapLongArray.get(addr, HEAD);
        if (p == -1) {
            return p;
        }
        while (p != -1) {
            if (p_key > key(addr, p)) {
                if (right(addr, p) != -1) {
                    p = right(addr, p);
                } else {
                    return p;
                }
            } else {
                if (left(addr, p) != -1) {
                    p = left(addr, p);
                } else {
                    long parent = parent(addr, p);
                    long ch = p;
                    while (parent != -1 && ch == left(addr, parent)) {
                        ch = parent;
                        parent = parent(addr, parent);
                    }
                    return parent;
                }
            }
        }
        return -1;
    }

    private static long internal_previousOrEqual_index(final long addr, final long p_key) {
        long p = POffHeapLongArray.get(addr, HEAD);
        if (p == -1) {
            return p;
        }
        while (p != -1) {
            if (p_key == key(addr, p)) {
                return p;
            }
            if (p_key > key(addr, p)) {
                if (right(addr, p) != -1) {
                    p = right(addr, p);
                } else {
                    return p;
                }
            } else {
                if (left(addr, p) != -1) {
                    p = left(addr, p);
                } else {
                    long parent = parent(addr, p);
                    long ch = p;
                    while (parent != -1 && ch == left(addr, parent)) {
                        ch = parent;
                        parent = parent(addr, parent);
                    }
                    return parent;
                }
            }
        }
        return -1;
    }

    private static long internal_next_index(final long addr, final long p_key) {
        long p = p_key;
        if (right(addr, p) != -1) {
            p = right(addr, p);
            while (left(addr, p) != -1) {
                p = left(addr, p);
            }
            return p;
        } else {
            if (parent(addr, p) != -1) {
                if (p == left(addr, parent(addr, p))) {
                    return parent(addr, p);
                } else {
                    while (parent(addr, p) != -1 && p == right(addr, parent(addr, p))) {
                        p = parent(addr, p);
                    }
                    return parent(addr, p);
                }
            } else {
                return -1;
            }
        }
    }

    private boolean internal_insert(long addr, final long insertLey, final boolean initial) {
        int size = (int) POffHeapLongArray.get(addr, SIZE);
        int capacity = (int) POffHeapLongArray.get(addr, CAPACITY);
        if (capacity == size) {
            int nextCapacity = size;
            if (nextCapacity == 0) {
                nextCapacity = Constants.MAP_INITIAL_CAPACITY;
            } else {
                nextCapacity = nextCapacity * 2;
            }
            addr = reallocate(addr, capacity, nextCapacity);
        }
        if (size == 0) {
            setKey(addr, size, insertLey, initial);
            setColor(addr, size, false);
            setLeft(addr, size, -1);
            setRight(addr, size, -1);
            setParent(addr, size, -1);
            POffHeapLongArray.set(addr, HEAD, size);
            POffHeapLongArray.set(addr, SIZE, 1);
        } else {
            long n = POffHeapLongArray.get(addr, HEAD);
            while (true) {
                if (insertLey == key(addr, n)) {
                    return false;
                } else if (insertLey < key(addr, n)) {
                    if (left(addr, n) == -1) {
                        setKey(addr, size, insertLey, initial);
                        setColor(addr, size, false);
                        setLeft(addr, size, -1);
                        setRight(addr, size, -1);
                        setParent(addr, size, -1);
                        setLeft(addr, n, size);
                        POffHeapLongArray.set(addr, SIZE, size + 1);
                        break;
                    } else {
                        n = left(addr, n);
                    }
                } else {
                    if (right(addr, n) == -1) {
                        setKey(addr, size, insertLey, initial);
                        setColor(addr, size, false);
                        setLeft(addr, size, -1);
                        setRight(addr, size, -1);
                        setParent(addr, size, -1);
                        setRight(addr, n, size);
                        POffHeapLongArray.set(addr, SIZE, size + 1);
                        break;
                    } else {
                        n = right(addr, n);
                    }
                }
            }
            setParent(addr, size, n);
        }
        insertCase1(addr, size);
        return true;
    }

    private void internal_set_dirty(final long addr) {
        POffHeapLongArray.set(addr, MAGIC, POffHeapLongArray.get(addr, MAGIC) + 1);
        if (space != null && POffHeapLongArray.get(addr, DIRTY) != 1) {
            POffHeapLongArray.set(addr, DIRTY, 1);
            space.notifyUpdate(index);
        }
    }

}
