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

import greycat.internal.CoreConstants;
import greycat.struct.Buffer;
import greycat.struct.BufferIterator;
import greycat.utility.DefaultBufferIterator;

public class HeapBuffer implements Buffer {

    private byte[] buffer;

    private int writeCursor = 0;

    @Override
    public final byte[] slice(long initPos, long endPos) {
        int newSize = (int) (endPos - initPos + 1);
        byte[] newResult = new byte[newSize];
        System.arraycopy(buffer, (int) initPos, newResult, 0, newSize);
        return newResult;
    }

    @Override
    public long writeIndex() {
        return writeCursor;
    }

    @Override
    public final void write(byte b) {
        if (buffer == null) {
            buffer = new byte[CoreConstants.MAP_INITIAL_CAPACITY];
            buffer[0] = b;
            writeCursor = 1;
        } else if (writeCursor == buffer.length) {
            byte[] temp = new byte[buffer.length * 2];
            System.arraycopy(buffer, 0, temp, 0, buffer.length);
            temp[writeCursor] = b;
            writeCursor++;
            buffer = temp;
        } else {
            buffer[writeCursor] = b;
            writeCursor++;
        }
    }

    private long getNewSize(long old, long target) {
        while (old < target) {
            old = old * 2;
        }
        return old;
    }

    @Override
    public final void writeAll(byte[] bytes) {
        if (buffer == null) {
            int initSize = (int) getNewSize(CoreConstants.MAP_INITIAL_CAPACITY, bytes.length);
            buffer = new byte[initSize];
            System.arraycopy(bytes, 0, buffer, 0, bytes.length);
            writeCursor = bytes.length;
        } else if (writeCursor + bytes.length > buffer.length) {
            int newSize = (int) getNewSize(buffer.length, buffer.length + bytes.length);
            //final int closePowerOfTwo = (int) Math.pow(2, Math.ceil(Math.log(newSize) / Math.log(2)));
            //byte[] tmp = new byte[closePowerOfTwo];
            byte[] tmp = new byte[newSize];//already power of two
            System.arraycopy(buffer, 0, tmp, 0, buffer.length);
            System.arraycopy(bytes, 0, tmp, writeCursor, bytes.length);
            buffer = tmp;
            writeCursor = writeCursor + bytes.length;
        } else {
            System.arraycopy(bytes, 0, buffer, writeCursor, bytes.length);
            writeCursor = writeCursor + bytes.length;
        }
    }

    /**
     * @native ts
     * var ua = new Int8Array(input.length);
     * Array.prototype.forEach.call(input, function (ch:string, i:number) { ua[i] = ch.charCodeAt(0);});
     * return this.writeAll(ua);
     */
    @Override
    public final void writeString(String input) {
        writeAll(input.getBytes());
    }

    /**
     * @native ts
     * this.write(input.charCodeAt(0));
     */
    @Override
    public final void writeChar(char input) {
        write((byte) input);
    }

    @Override
    public final byte read(long position) {
        return buffer[(int) position];
    }

    @Override
    public final byte[] data() {
        byte[] copy = new byte[writeCursor];
        if (buffer != null) {
            System.arraycopy(buffer, 0, copy, 0, writeCursor);
        }
        return copy;
    }

    @Override
    public final long length() {
        return writeCursor;
    }

    @Override
    public final void free() {
        buffer = null;
    }

    @Override
    public final BufferIterator iterator() {
        return new DefaultBufferIterator(this);
    }

    /**
     * @native ts
     * return String.fromCharCode.apply(null,this.data());
     */
    @Override
    public final String toString() {
        return new String(data());
    }

    /**
     * {@native ts
     * if(startPos + 3 >= this.writeCursor) {
     * throw new Error("Cannot readInt at " + startPos + ". Last position is out of buffer: " + (startPos + 3) + " >= " + this.writeCursor);
     * }
     * let res = 0;
     * if((this.buffer[startPos] & 0x80) !== 0) {
     * res = 0x80 << 56;
     * }
     * res |= (this.buffer[startPos] & 0x7F) << 24;
     * res |= (this.buffer[startPos+1] & 0xFF) << 16;
     * res |= (this.buffer[startPos+2] & 0xFF) << 8;
     * res |= (this.buffer[startPos+3] & 0xFF);
     * return res;
     * }
     */
    @Override
    public int readInt(int startPos) {
        if (startPos + 3 >= writeCursor) {
            throw new IndexOutOfBoundsException("Cannot readInt at " + startPos + ". Last position is out of buffer: " + (startPos + 3) + " >= " + writeCursor);
        }
        int res ;
        res = (buffer[startPos] & 0xFF) << 24;
        res |= (buffer[startPos + 1] & 0xFF) << 16;
        res |= (buffer[startPos + 2] & 0xFF) << 8;
        res |= (buffer[startPos + 3] & 0xFF);
        return res;
    }

    /**
     * {@native ts
     * this.write((value < 0 ? 0x80 : 0) | ((value >> 24) & 0x7F));
     * this.write((value >> 16) & 0xFF);
     * this.write((value >> 8) & 0xFF);
     * this.write(value & 0xFF);
     * }
     */
    @Override
    public void writeInt(int value) {
        write((byte) (0xFF & (value >> 24)));
        write((byte) (0xFF & (value >> 16)));
        write((byte) (0xFF & (value >> 8)));
        write((byte) (0xFF & value));
    }

    /**
     * {@native ts
     * if(startPos + 3 >= this.writeCursor) {
     * throw new Error("Cannot write at " + startPos + ". Last position is out of buffer: " + (startPos + 3) + " >= " + this.writeCursor);
     * }
     * this.buffer[startPos] = (value < 0 ? 0x80 : 0) | ((value >> 24) & 0x7F);
     * this.buffer[startPos+1] = (value >> 16) & 0xFF;
     * this.buffer[startPos+2] = (value >> 8) & 0xFF;
     * this.buffer[startPos+3] = value & 0xFF;
     * }
     */
    @Override
    public void writeIntAt(int value, int startPos) {
        if (startPos + 3 >= writeCursor) {
            throw new IndexOutOfBoundsException("Cannot write at " + startPos + ". Last position is out of buffer: " + (startPos + 3) + " >= " + writeCursor);
        }
        buffer[startPos] = (byte) (0xFF & (value >> 24));
        buffer[startPos+1] = (byte) (0xFF & (value >> 16));
        buffer[startPos+2] = (byte) (0xFF & (value >> 8));
        buffer[startPos+3] = (byte) (0xFF & value);
    }
}
