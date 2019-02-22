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

public class POffHeapStringArray {

    public static long allocate(final long capacity) {
        return POffHeapLongArray.allocate(capacity);
    }

    public static long reallocate(final long addr, final long nextCapacity, final long currentCapacity) {
        long newAddr = POffHeapLongArray.reallocate(addr, nextCapacity);
        POffHeapLongArray.reset(newAddr + (currentCapacity * 8), (nextCapacity - currentCapacity) * 8);
        return newAddr;
    }

    public static void set(final long addr, final long index, final String valueToInsert) {
        long stringPtr = POffHeapLongArray.get(addr, index);
        if (stringPtr != OffHeapConstants.NULL_PTR) {
            POffHeapString.free(stringPtr);
        }
        stringPtr = POffHeapString.fromObject(valueToInsert);
        POffHeapLongArray.set(addr, index, stringPtr);
    }

    public static String get(final long addr, final long index) {
        long stringPtr = POffHeapLongArray.get(addr, index);
        if (stringPtr == OffHeapConstants.NULL_PTR) {
            return null;
        } else {
            return POffHeapString.asObject(stringPtr);
        }
    }

    public static void free(final long addr, final long capacity) {
        for (long i = 0; i < capacity; i++) {
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
