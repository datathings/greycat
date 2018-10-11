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
package greycat.struct;

/**
 * Buffer defines the interface to exchange byte[] (payload), between Storage and the various KChunk
 */
public interface Buffer {

    long writeIndex();

    /**
     * Append a byte to the buffer
     *
     * @param b byte to append
     */
    void write(byte b);

    /**
     * Append an array of bytes to the buffer
     *
     * @param bytes byte array to append
     */
    void writeAll(byte[] bytes);

    void writeString(String input);

    void writeChar(char input);

    /**
     * Read the buffer at a precise position
     *
     * @param position index in the buffer
     * @return read byte
     */
    byte read(long position);

    /**
     * Extract data as byte[]
     *
     * @return content as native byte[]
     */
    byte[] data();

    /**
     * Size of the buffer
     *
     * @return length of the buffer
     */
    long length();

    /**
     * Free the buffer from memory, this method should be the last called
     */
    void free();

    /**
     * Create a new iterator for this buffer
     *
     * @return the newly created iterator
     */
    BufferIterator iterator();
    
    byte[] slice(long initPos, long endPos);

}
