package org.mwg.memory.offheap;

import org.mwg.Constants;
import org.mwg.struct.Buffer;
import org.mwg.utility.Base64;
import org.mwg.utility.Unsafe;

class OffHeapIntArray {

    private static int COW_INDEX = 0;
    private static int SIZE_INDEX = 1;
    private static int SHIFT_INDEX = 2;

    public static long alloc_counter = 0;

    private static final sun.misc.Unsafe unsafe = Unsafe.getUnsafe();

    public static long allocate(final long capacity) {
        if (Unsafe.DEBUG_MODE) {
            alloc_counter++;
        }
        //create the memory segment
        long newMemorySegment = unsafe.allocateMemory(capacity * 4);
        //init the memory
        unsafe.setMemory(newMemorySegment, capacity * 4, (byte) OffHeapConstants.OFFHEAP_NULL_PTR);
        //return the newly created segment
        return newMemorySegment;
    }

    public static void reset(final long addr, final long capacity) {
        unsafe.setMemory(addr, capacity * 4, (byte) OffHeapConstants.OFFHEAP_NULL_PTR);
    }

    public static long reallocate(final long addr, final long nextCapacity) {
        return unsafe.reallocateMemory(addr, nextCapacity * 4);
    }

    public static void set(final long addr, final long index, final int valueToInsert) {
        unsafe.putIntVolatile(null, addr + index * 4, valueToInsert);
    }

    public static int get(final long addr, final long index) {
        return unsafe.getIntVolatile(null, addr + index * 4);
    }

    public static void free(final long addr) {
        if (Unsafe.DEBUG_MODE) {
            alloc_counter--;
        }
        unsafe.freeMemory(addr);
    }

    static boolean compareAndSwap(final long addr, final long index, final int expectedValue, final int updatedValue) {
        return unsafe.compareAndSwapInt(null, addr + index * 4, expectedValue, updatedValue);
    }

    static void save(final long addr, final Buffer buffer) {
        if (addr == OffHeapConstants.OFFHEAP_NULL_PTR) {
            return;
        }
        final long rawSize = OffHeapIntArray.get(addr, SIZE_INDEX);
        Base64.encodeLongToBuffer(rawSize, buffer);
        for (int j = 0; j < rawSize; j++) {
            buffer.write(Constants.CHUNK_SUB_SUB_SEP);
            Base64.encodeLongToBuffer(get(addr, j + SHIFT_INDEX), buffer);
        }
    }

    static int[] asObject(final long addr) {
        if (addr == OffHeapConstants.OFFHEAP_NULL_PTR) {
            return null;
        }
        int longArrayLength = get(addr, SIZE_INDEX); // can be safely casted
        int[] longArray = new int[longArrayLength];
        for (int i = 0; i < longArrayLength; i++) {
            longArray[i] = get(addr, i + SHIFT_INDEX);
        }
        return longArray;
    }

    static long fromObject(int[] origin) {
        long intArrayToInsert_ptr = OffHeapIntArray.allocate(SHIFT_INDEX + origin.length);
        set(intArrayToInsert_ptr, SIZE_INDEX, origin.length);
        set(intArrayToInsert_ptr, COW_INDEX, 1);
        for (int i = 0; i < origin.length; i++) {
            set(intArrayToInsert_ptr, SHIFT_INDEX + i, origin[i]);
        }
        return intArrayToInsert_ptr;
    }

    static long cloneObject(final long addr) {
        int cow;
        int cow_after;
        do {
            cow = get(addr, COW_INDEX);
            cow_after = cow + 1;
        } while (!compareAndSwap(addr, COW_INDEX, cow, cow_after));
        return addr;
    }

    static void freeObject(final long addr) {
        int cow;
        int cow_after;
        do {
            cow = get(addr, COW_INDEX);
            cow_after = cow - 1;
        } while (!compareAndSwap(addr, COW_INDEX, cow, cow_after));
        if (cow == 1 && cow_after == 0) {
            unsafe.freeMemory(addr);
            if (Unsafe.DEBUG_MODE) {
                alloc_counter--;
            }
        }
    }

}
