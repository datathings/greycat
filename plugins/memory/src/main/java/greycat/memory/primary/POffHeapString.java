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
import greycat.struct.Buffer;
import greycat.utility.Base64;
import greycat.utility.Unsafe;

public class POffHeapString {

    private static int COW = 0;
    private static int SIZE = 8;
    private static int SHIFT = 12;

    private static final sun.misc.Unsafe unsafe = Unsafe.getUnsafe();

    public static void save(final long addr, final Buffer buffer) {
        if (addr == OffHeapConstants.NULL_PTR) {
            return;
        }
        Base64.encodeStringToBuffer(asObject(addr), buffer);
    }

    public static long fromObject(String origin) {
        final byte[] valueAsByte = origin.getBytes();
        final long allocationSize = SHIFT + valueAsByte.length;
        final long newStringPtr = unsafe.allocateMemory(allocationSize);
        if (OffHeapConstants.DEBUG_MODE) {
            OffHeapConstants.SEGMENTS.put(newStringPtr, allocationSize);
        }
        unsafe.putLong(newStringPtr, 1);
        unsafe.putInt(newStringPtr + 8, valueAsByte.length);
        for (int i = 0; i < valueAsByte.length; i++) {
            unsafe.putByte(newStringPtr + SHIFT + i, valueAsByte[i]);
        }
        return newStringPtr;
    }

    public static String asObject(final long addr) {
        if (addr == OffHeapConstants.NULL_PTR) {
            return null;
        }
        int length = unsafe.getInt(addr + SIZE);
        byte[] bytes = new byte[length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = unsafe.getByte(addr + SHIFT + i);
        }
        return new String(bytes);
    }

    public static long clone(final long addr) {
        long cow;
        long cow_after;
        do {
            cow = unsafe.getLong(addr + COW);
            cow_after = cow + 1;
        } while (!unsafe.compareAndSwapLong(null, addr + COW, cow, cow_after));
        return addr;
    }

    public static void free(final long addr) {
        long cow;
        long cow_after;
        do {
            cow = unsafe.getLong(addr + COW);
            cow_after = cow - 1;
        } while (!unsafe.compareAndSwapLong(null, addr + COW, cow, cow_after));
        if (cow == 1 && cow_after == 0) {
            if (OffHeapConstants.DEBUG_MODE) {
                if (!OffHeapConstants.SEGMENTS.containsKey(addr)) {
                    throw new RuntimeException("Bad ADDR! "+addr);
                }
                OffHeapConstants.SEGMENTS.remove(addr);
            }
            unsafe.freeMemory(addr);
        }
    }

}
