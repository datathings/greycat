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
package greycat.memory.primary;

import greycat.Constants;
import greycat.memory.OffHeapConstants;
import greycat.struct.Buffer;
import greycat.utility.Base64;
import greycat.utility.Unsafe;

public class POffHeapIntArray {

    private static int COW_INDEX = 0;
    private static int SIZE_INDEX = 1;
    private static int SHIFT_INDEX = 2;

    public static long alloc_counter = 0;

    private static final sun.misc.Unsafe unsafe = Unsafe.getUnsafe();

    public static long allocate(final long capacity) {
        //create the memory segment
        long newMemorySegment = unsafe.allocateMemory(capacity * 4);
        if (OffHeapConstants.DEBUG_MODE) {
            OffHeapConstants.SEGMENTS.put(newMemorySegment, capacity * 4);
        }
        //init the memory
        unsafe.setMemory(newMemorySegment, capacity * 4, (byte) OffHeapConstants.NULL_PTR);
        //return the newly created segment
        return newMemorySegment;
    }

    public static long reallocate(final long addr, final long nextCapacity) {
        long new_segment = unsafe.reallocateMemory(addr, nextCapacity * 4);
        if (OffHeapConstants.DEBUG_MODE) {
            OffHeapConstants.SEGMENTS.remove(addr);
            OffHeapConstants.SEGMENTS.put(new_segment, nextCapacity * 4);
        }
        return new_segment;
    }

    public static void set(final long addr, final long index, final int valueToInsert) {
        if (OffHeapConstants.DEBUG_MODE) {
            Long allocated = OffHeapConstants.SEGMENTS.get(addr);
            if (allocated == null || index < 0 || (index * 4) > allocated) {
                throw new RuntimeException("set: bad address " + index + "(" + index * 4 + ")" + " in " + allocated);
            }
        }
        unsafe.putIntVolatile(null, addr + index * 4, valueToInsert);
    }

    public static int get(final long addr, final long index) {
        if (OffHeapConstants.DEBUG_MODE) {
            Long allocated = OffHeapConstants.SEGMENTS.get(addr);
            if (allocated == null || index < 0 || (index * 4) > allocated) {
                throw new RuntimeException("get: bad address " + index + " in " + allocated);
            }
        }
        return unsafe.getIntVolatile(null, addr + index * 4);
    }

    public static void free(final long addr) {
        if (OffHeapConstants.DEBUG_MODE) {
            OffHeapConstants.SEGMENTS.remove(addr);
        }
        unsafe.freeMemory(addr);
    }

    static boolean compareAndSwap(final long addr, final long index, final int expectedValue, final int updatedValue) {
        return unsafe.compareAndSwapInt(null, addr + index * 4, expectedValue, updatedValue);
    }

    public static void save(final long addr, final Buffer buffer) {
        if (addr == OffHeapConstants.NULL_PTR) {
            return;
        }
        final long rawSize = POffHeapIntArray.get(addr, SIZE_INDEX);
        Base64.encodeLongToBuffer(rawSize, buffer);
        for (int j = 0; j < rawSize; j++) {
            buffer.write(Constants.CHUNK_VAL_SEP);
            Base64.encodeLongToBuffer(get(addr, j + SHIFT_INDEX), buffer);
        }
    }

    public static int[] asObject(final long addr) {
        if (addr == OffHeapConstants.NULL_PTR) {
            return null;
        }
        int longArrayLength = get(addr, SIZE_INDEX); // can be safely casted
        int[] longArray = new int[longArrayLength];
        for (int i = 0; i < longArrayLength; i++) {
            longArray[i] = get(addr, i + SHIFT_INDEX);
        }
        return longArray;
    }

    public static long fromObject(int[] origin) {
        long intArrayToInsert_ptr = POffHeapIntArray.allocate(SHIFT_INDEX + origin.length);
        set(intArrayToInsert_ptr, SIZE_INDEX, origin.length);
        set(intArrayToInsert_ptr, COW_INDEX, 1);
        for (int i = 0; i < origin.length; i++) {
            set(intArrayToInsert_ptr, SHIFT_INDEX + i, origin[i]);
        }
        return intArrayToInsert_ptr;
    }

    public static long cloneObject(final long addr) {
        int cow;
        int cow_after;
        do {
            cow = get(addr, COW_INDEX);
            cow_after = cow + 1;
        } while (!compareAndSwap(addr, COW_INDEX, cow, cow_after));
        return addr;
    }

    public static void freeObject(final long addr) {
        int cow;
        int cow_after;
        do {
            cow = get(addr, COW_INDEX);
            cow_after = cow - 1;
        } while (!compareAndSwap(addr, COW_INDEX, cow, cow_after));
        if (cow == 1 && cow_after == 0) {
            unsafe.freeMemory(addr);
            if (OffHeapConstants.DEBUG_MODE) {
                OffHeapConstants.SEGMENTS.remove(addr);
            }
        }
    }

    public static long cloneArray(final long srcAddr, final long length) {
        if (srcAddr == OffHeapConstants.NULL_PTR) {
            return srcAddr;
        }
        long newAddr = unsafe.allocateMemory(length * 4);
        if (OffHeapConstants.DEBUG_MODE) {
            OffHeapConstants.SEGMENTS.put(newAddr, (length * 4));
        }
        unsafe.copyMemory(srcAddr, newAddr, (length * 4));
        return newAddr;
    }

}
