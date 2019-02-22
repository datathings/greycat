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
package greycat.memory.primary;

import greycat.memory.OffHeapConstants;
import greycat.utility.Unsafe;

public class POffHeapByteArray {

    private static final sun.misc.Unsafe unsafe = Unsafe.getUnsafe();

    public static long allocate(final long capacity) {
        //create the memory segment
        long newMemorySegment = unsafe.allocateMemory(capacity);
        if (OffHeapConstants.DEBUG_MODE) {
            OffHeapConstants.SEGMENTS.put(newMemorySegment, capacity);
        }
        //init the memory
        unsafe.setMemory(newMemorySegment, capacity, (byte) OffHeapConstants.NULL_PTR);
        //return the newly created segment
        return newMemorySegment;
    }

    public static long reallocate(final long addr, final long nextCapacity) {
        long new_segment = unsafe.reallocateMemory(addr, nextCapacity);
        if (OffHeapConstants.DEBUG_MODE) {
            if(!OffHeapConstants.SEGMENTS.containsKey(addr)){
                throw new RuntimeException("Bad ADDR!");
            }
            OffHeapConstants.SEGMENTS.remove(addr);
            OffHeapConstants.SEGMENTS.put(new_segment, nextCapacity);
        }
        return new_segment;
    }

    /**
     * Transfer an primitive array from heap to off-heap memory
     *
     * @param src        Object to copy, should be an array
     * @param destAddr   start of address to store the source object
     * @param nbElements offset of destination address
     */
    public static void copyArray(final Object src, final long destAddr, final long index, final long nbElements) {
        int baseOffset = unsafe.arrayBaseOffset(src.getClass());
        int scaleOffset = unsafe.arrayIndexScale(src.getClass());
        if (OffHeapConstants.DEBUG_MODE) {
            Long allocated = OffHeapConstants.SEGMENTS.get(destAddr);
            if (allocated == null || destAddr < 0 || (nbElements * scaleOffset) > allocated) {
                throw new RuntimeException("set: bad address in " + allocated);
            }
        }
        unsafe.copyMemory(src, baseOffset, null, destAddr + index, nbElements * scaleOffset);
    }

    public static void set(final long addr, final long index, final byte valueToInsert) {
        if (OffHeapConstants.DEBUG_MODE) {
            Long allocated = OffHeapConstants.SEGMENTS.get(addr);
            if (allocated == null || index < 0 || (index) > allocated) {
                throw new RuntimeException("set: bad address " + index + "(" + index + ")" + " in " + allocated);
            }
        }
        unsafe.putByteVolatile(null, addr + index, valueToInsert);
    }

    public static byte get(final long addr, final long index) {
        if (OffHeapConstants.DEBUG_MODE) {
            Long allocated = OffHeapConstants.SEGMENTS.get(addr);
            if (allocated == null || index < 0 || (index) > allocated) {
                throw new RuntimeException("get: bad address " + index + " in " + allocated);
            }
        }
        return unsafe.getByteVolatile(null, addr + index);
    }

    public static void free(final long addr) {
        if (OffHeapConstants.DEBUG_MODE) {
            if(!OffHeapConstants.SEGMENTS.containsKey(addr)){
                throw new RuntimeException("Bad ADDR!");
            }
            OffHeapConstants.SEGMENTS.remove(addr);
        }
        unsafe.freeMemory(addr);
    }

}
