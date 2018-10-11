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
package greycat.memory;

import greycat.Constants;
import greycat.memory.primary.POffHeapByteArray;
import greycat.struct.Buffer;
import greycat.struct.BufferIterator;
import greycat.utility.DefaultBufferIterator;

class OffHeapBuffer implements Buffer {

    private long bufferPtr = OffHeapConstants.NULL_PTR;
    private long writeCursor = 0;
    private long capacity = 0;

    @Override
    public byte[] slice(long initPos, long endPos) {
        int newSize = (int) (endPos - initPos + 1);
        byte[] result = new byte[newSize];
        for (int i = 0; i < newSize; i++) {
            result[i] = POffHeapByteArray.get(bufferPtr, i + initPos);
        }
        return result;
    }

    @Override
    public void write(byte b) {
        if (bufferPtr == OffHeapConstants.NULL_PTR) {
            capacity = Constants.MAP_INITIAL_CAPACITY;
            bufferPtr = POffHeapByteArray.allocate(capacity);
            POffHeapByteArray.set(bufferPtr, writeCursor, b);
            writeCursor++;
        } else if (writeCursor == capacity) {
            long newCapacity = capacity * 2;
            bufferPtr = POffHeapByteArray.reallocate(bufferPtr, newCapacity);
            capacity = newCapacity;
            POffHeapByteArray.set(bufferPtr, writeCursor, b);
            writeCursor++;
        } else {
            POffHeapByteArray.set(bufferPtr, writeCursor, b);
            writeCursor++;
        }
    }

    @Override
    public void writeAll(byte[] bytes) {
        if (bufferPtr == OffHeapConstants.NULL_PTR) {
            capacity = getNewSize(Constants.MAP_INITIAL_CAPACITY, bytes.length);
            bufferPtr = POffHeapByteArray.allocate(capacity);
            POffHeapByteArray.copyArray(bytes, bufferPtr, writeCursor, bytes.length);
            writeCursor = bytes.length;
        } else if (writeCursor + bytes.length > capacity) {
            long newCapacityWanted = getNewSize(capacity, capacity + bytes.length);
            final int newCapacity = (int) Math.pow(2, Math.ceil(Math.log(newCapacityWanted) / Math.log(2)));
            bufferPtr = POffHeapByteArray.reallocate(bufferPtr, newCapacity);
            POffHeapByteArray.copyArray(bytes, bufferPtr, writeCursor, bytes.length);
            capacity = newCapacity;
            writeCursor = writeCursor + bytes.length;
        } else {
            POffHeapByteArray.copyArray(bytes, bufferPtr, writeCursor, bytes.length);
            writeCursor = writeCursor + bytes.length;
        }
    }

    @Override
    public void writeString(String input) {
        writeAll(input.getBytes());
    }

    @Override
    public void writeChar(char input) {
        write((byte) input);
    }

    @Override
    public byte read(long position) {
        if (bufferPtr != OffHeapConstants.NULL_PTR && position < capacity) {
            return POffHeapByteArray.get(bufferPtr, position);
        }
        return -1;
    }

    @Override
    public byte[] data() {
        byte[] result = new byte[(int) writeCursor];
        for (long i = 0; i < writeCursor; i++) {
            result[(int) i] = POffHeapByteArray.get(bufferPtr, i);
        }
        return result;
    }

    @Override
    public long length() {
        return writeCursor;
    }

    @Override
    public void free() {
        if (bufferPtr != OffHeapConstants.NULL_PTR) {
            POffHeapByteArray.free(bufferPtr);
            bufferPtr = OffHeapConstants.NULL_PTR;
            capacity = 0;
            writeCursor = 0;
        }
    }

    @Override
    public BufferIterator iterator() {
        return new DefaultBufferIterator(this);
    }

    @Override
    public void removeLast() {
        writeCursor--;
    }

    private long getNewSize(long old, long target) {
        while (old < target) {
            old = old * 2;
        }
        return old;
    }

    @Override
    public String toString() {
        return new String(data());
    }

}
