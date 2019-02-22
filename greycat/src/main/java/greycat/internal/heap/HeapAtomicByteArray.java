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
package greycat.internal.heap;

import greycat.utility.Unsafe;

public class HeapAtomicByteArray {

    /**
     * @ignore ts
     */
    private static final sun.misc.Unsafe unsafe = Unsafe.getUnsafe();
    /**
     * @ignore ts
     */
    private static final int base = unsafe.arrayBaseOffset(byte[].class);
    /**
     * @ignore ts
     */
    private static final int scale = unsafe.arrayIndexScale(byte[].class);

    private final byte[] _back;

    HeapAtomicByteArray(int initialSize) {
        _back = new byte[initialSize];
    }


    /**
     * Retrieves the byte at a specific index.
     * @param index The index requested
     * @return the byte value contained at this index in the array.
     */
    /**
     * {@native ts
     * return this._back[index];
     * }
     */
    public byte get(int index) {
        return unsafe.getByteVolatile(_back, base + index * scale);
    }


    /**
     * Sets the value of the array, for a specific index.
     * @param index the index to set
     * @param value the value to put
     */
    /**
     * {@native ts
     * this._back[index] = value;
     * }
     */
    public void set(int index, byte value) {
        unsafe.putByteVolatile(_back, base + index * scale, value);
    }

}
