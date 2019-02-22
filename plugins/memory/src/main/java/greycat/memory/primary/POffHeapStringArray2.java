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

public class POffHeapStringArray2 {

    private static int SIZE = 0;
    private static int SHIFT = 1;

    public static long allocate(final long capacity) {
        long addr = POffHeapLongArray.allocate(capacity + SHIFT);
        POffHeapLongArray.set(addr,SIZE,capacity);
        return addr;
    }

    public static long reallocate(final long addr, final long nextCapacity) {
        long currentCapacity = POffHeapLongArray.get(addr,SIZE);
        long newAddr = POffHeapLongArray.reallocate(addr, nextCapacity + 1);
        POffHeapLongArray.set(newAddr,SIZE,nextCapacity);
        POffHeapLongArray.reset(newAddr + ((currentCapacity + SHIFT) * 8), (nextCapacity - currentCapacity + SHIFT) * 8);
        return newAddr;
    }

    public static void set(final long addr, final long index, final String valueToInsert) {
        long stringPtr = POffHeapLongArray.get(addr, index);
        if (stringPtr != OffHeapConstants.NULL_PTR) {
            POffHeapString.free(stringPtr);
        }
        stringPtr = POffHeapString.fromObject(valueToInsert);
        POffHeapLongArray.set(addr, index + SHIFT, stringPtr);
    }

    public static String get(final long addr, final long index) {
        long stringPtr = POffHeapLongArray.get(addr, index + SHIFT);
        if (stringPtr == OffHeapConstants.NULL_PTR) {
            return null;
        } else {
            return POffHeapString.asObject(stringPtr);
        }
    }

    public static long size(final long addr) {
        if (addr == OffHeapConstants.NULL_PTR) {
            return 0;
        }
        return POffHeapLongArray.get(addr, SIZE);
    }

    public static String[] asObject(final long addr) {
        if (addr == OffHeapConstants.NULL_PTR) {
            return null;
        }
        int extractSize = (int) POffHeapLongArray.get(addr,SIZE);
        String[] extracted = new String[extractSize];
        for (int i = 0; i < extractSize; i++) {
            extracted[i] = get(addr, i);
        }
        return extracted;
    }

    public static long cloneArray(final long srcAddr) {
        throw new RuntimeException("not implemented");
        /*
        if (srcAddr == OffHeapConstants.NULL_PTR) {
            return srcAddr;
        }
        long newAddr = unsafe.allocateMemory(length * 8);
        if (OffHeapConstants.DEBUG_MODE) {
            OffHeapConstants.SEGMENTS.put(newAddr, (length * 8));
        }
        unsafe.copyMemory(srcAddr, newAddr, (length * 8));
        return newAddr;
        */ //return -1;
    }

    public static void free(final long addr) {
        long capacity = POffHeapLongArray.get(addr, SIZE);
        for (long i = SHIFT; i < capacity+SHIFT; i++) {
            long stringPtr = POffHeapLongArray.get(addr, i);
            if (stringPtr != OffHeapConstants.NULL_PTR) {
                if (OffHeapConstants.DEBUG_MODE) {
                    if (!OffHeapConstants.SEGMENTS.containsKey(stringPtr)) {
                        throw new RuntimeException("Bad ADDR!");
                    }
                    OffHeapConstants.SEGMENTS.remove(stringPtr);
                }
                POffHeapString.free(stringPtr);
            }
        }
        if (OffHeapConstants.DEBUG_MODE) {
            if (!OffHeapConstants.SEGMENTS.containsKey(addr)) {
                throw new RuntimeException("Bad ADDR!");
            }
            OffHeapConstants.SEGMENTS.remove(addr);
        }
        POffHeapLongArray.free(addr);
    }

}
