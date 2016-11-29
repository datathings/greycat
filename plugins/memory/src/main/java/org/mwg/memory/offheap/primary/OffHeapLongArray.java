package org.mwg.memory.offheap.primary;

import org.mwg.Constants;
import org.mwg.memory.offheap.OffHeapConstants;
import org.mwg.struct.Buffer;
import org.mwg.utility.Base64;
import org.mwg.utility.Unsafe;

public class OffHeapLongArray {

    private static int COW_INDEX = 0;
    private static int SIZE_INDEX = 1;
    private static int SHIFT_INDEX = 2;

    private static final sun.misc.Unsafe unsafe = Unsafe.getUnsafe();

    public static long allocate(final long capacity) {
        //create the memory segment
        long newMemorySegment = unsafe.allocateMemory(capacity * 8);
        if (OffHeapConstants.DEBUG_MODE) {
            OffHeapConstants.SEGMENTS.put(newMemorySegment, capacity * 8);
        }
        //init the memory
        unsafe.setMemory(newMemorySegment, capacity * 8, (byte) OffHeapConstants.OFFHEAP_NULL_PTR);
        //return the newly created segment
        return newMemorySegment;
    }

    public static void reset(final long addr, final long capacity) {
        unsafe.setMemory(addr, capacity * 8, (byte) OffHeapConstants.OFFHEAP_NULL_PTR);
    }

    public static long reallocate(final long addr, final long nextCapacity) {
        long new_segment = unsafe.reallocateMemory(addr, nextCapacity * 8);
        if (OffHeapConstants.DEBUG_MODE) {
            OffHeapConstants.SEGMENTS.remove(addr);
            OffHeapConstants.SEGMENTS.put(new_segment, nextCapacity * 8);
        }
        return new_segment;
    }

    public static void set(final long addr, final long index, final long valueToInsert) {
        if (OffHeapConstants.DEBUG_MODE) {
            Long allocated = OffHeapConstants.SEGMENTS.get(addr);
            if (allocated == null || index < 0 || (index * 8) > allocated) {
                throw new RuntimeException("set: bad address " + index + "(" + index * 8 + ")" + " in " + allocated);
            }
        }
        unsafe.putLongVolatile(null, addr + (index * 8), valueToInsert);
    }

    public static void insert(final long addr, final long index, final long valueToInsert, final long size) {
        if (OffHeapConstants.DEBUG_MODE) {
            Long allocated = OffHeapConstants.SEGMENTS.get(addr);
            if (allocated == null || index < 0 || (index * 8) > allocated) {
                throw new RuntimeException("set: bad address " + index + "(" + index * 8 + ")" + " in " + allocated);
            }
        }
        unsafe.copyMemory(addr + (index * 8), addr + ((index + 1) * 8), (size - index) * 8);
        unsafe.putLongVolatile(null, addr + (index * 8), valueToInsert);
    }

    public static void delete(final long addr, final long index, final long size) {
        if (OffHeapConstants.DEBUG_MODE) {
            Long allocated = OffHeapConstants.SEGMENTS.get(addr);
            if (allocated == null || index < 0 || (index * 8) > allocated) {
                throw new RuntimeException("set: bad address " + index + "(" + index * 8 + ")" + " in " + allocated);
            }
        }
        unsafe.copyMemory(addr + ((index + 1) * 8), addr + (index * 8), (size - index - 1) * 8);
    }

    public static long get(final long addr, final long index) {
        if (OffHeapConstants.DEBUG_MODE) {
            Long allocated = OffHeapConstants.SEGMENTS.get(addr);
            if (allocated == null || index < 0 || (index * 8) > allocated) {
                throw new RuntimeException("get: bad address " + index + "*8 in " + allocated);
            }
        }
        return unsafe.getLongVolatile(null, addr + (index * 8));
    }

    public static void free(final long addr) {
        if (OffHeapConstants.DEBUG_MODE) {
            OffHeapConstants.SEGMENTS.remove(addr);
        }
        unsafe.freeMemory(addr);
    }

    public static boolean compareAndSwap(final long addr, final long index, final long expectedValue, final long updatedValue) {
        return unsafe.compareAndSwapLong(null, addr + index * 8, expectedValue, updatedValue);
    }

    public static long cloneArray(final long srcAddr, final long length) {
        if (srcAddr == OffHeapConstants.OFFHEAP_NULL_PTR) {
            return srcAddr;
        }
        long newAddr = unsafe.allocateMemory(length * 8);
        if (OffHeapConstants.DEBUG_MODE) {
            OffHeapConstants.SEGMENTS.put(newAddr, (length * 8));
        }
        unsafe.copyMemory(srcAddr, newAddr, (length * 8));
        return newAddr;
    }

    public static void save(final long addr, final Buffer buffer) {
        if (addr == OffHeapConstants.OFFHEAP_NULL_PTR) {
            return;
        }
        final long rawSize = OffHeapLongArray.get(addr, SIZE_INDEX);
        Base64.encodeLongToBuffer(rawSize, buffer);
        for (int j = 0; j < rawSize; j++) {
            buffer.write(Constants.CHUNK_SUB_SUB_SEP);
            Base64.encodeLongToBuffer(OffHeapLongArray.get(addr, j + SHIFT_INDEX), buffer);
        }
    }

    public static long[] asObject(final long addr) {
        if (addr == OffHeapConstants.OFFHEAP_NULL_PTR) {
            return null;
        }
        int longArrayLength = (int) get(addr, SIZE_INDEX); // can be safely casted
        long[] longArray = new long[longArrayLength];
        for (int i = 0; i < longArrayLength; i++) {
            longArray[i] = get(addr, i + SHIFT_INDEX);
        }
        return longArray;
    }

    public static long fromObject(long[] origin) {
        long longArrayToInsert_ptr = allocate(SHIFT_INDEX + origin.length);
        set(longArrayToInsert_ptr, SIZE_INDEX, origin.length);
        set(longArrayToInsert_ptr, COW_INDEX, 1);
        for (int i = 0; i < origin.length; i++) {
            set(longArrayToInsert_ptr, SHIFT_INDEX + i, origin[i]);
        }
        return longArrayToInsert_ptr;
    }

    public static long cloneObject(final long addr) {
        long cow;
        long cow_after;
        do {
            cow = get(addr, COW_INDEX);
            cow_after = cow + 1;
        } while (!compareAndSwap(addr, COW_INDEX, cow, cow_after));
        return addr;
    }

    public static void freeObject(final long addr) {
        long cow;
        long cow_after;
        do {
            cow = get(addr, COW_INDEX);
            cow_after = cow - 1;
        } while (!compareAndSwap(addr, COW_INDEX, cow, cow_after));
        if (cow == 1 && cow_after == 0) {
            if (OffHeapConstants.DEBUG_MODE) {
                if (!OffHeapConstants.SEGMENTS.containsKey(addr)) {
                    throw new RuntimeException("Bad ADDR!");
                }
                OffHeapConstants.SEGMENTS.remove(addr);
            }
            unsafe.freeMemory(addr);
        }
    }

}