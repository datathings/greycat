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
package greycat.chunk;

import greycat.Constants;
import greycat.struct.Buffer;
import greycat.utility.Base64;

public class KeyHelper {

    /**
     * @param buffer
     * @param chunkType
     * @param world
     * @param time
     * @param id
     */
    public static void keyToBuffer(Buffer buffer, byte chunkType, long world, long time, long id) {
        Base64.encodeIntToBuffer((int) chunkType, buffer);
        buffer.write(Constants.KEY_SEP);
        Base64.encodeLongToBuffer(world, buffer);
        buffer.write(Constants.KEY_SEP);
        Base64.encodeLongToBuffer(time, buffer);
        buffer.write(Constants.KEY_SEP);
        Base64.encodeLongToBuffer(id, buffer);
    }

    public static void chunckKeyToBuffer(ChunkKey key, Buffer buffer) {
        Base64.encodeIntToBuffer((int) key.type, buffer);
        buffer.write(Constants.KEY_SEP);
        Base64.encodeLongToBuffer(key.world, buffer);
        buffer.write(Constants.KEY_SEP);
        Base64.encodeLongToBuffer(key.time, buffer);
        buffer.write(Constants.KEY_SEP);
        Base64.encodeLongToBuffer(key.id, buffer);
    }

    public static ChunkKey bufferToKey(Buffer buffer) {
        ChunkKey tuple = new ChunkKey();
        long cursor = 0;
        long length = buffer.length();
        long previous = 0;
        int index = 0;
        while (cursor < length) {
            byte current = buffer.read(cursor);
            if (current == Constants.KEY_SEP) {
                switch (index) {
                    case 0:
                        tuple.type = (byte) Base64.decodeToIntWithBounds(buffer, previous, cursor);
                        break;
                    case 1:
                        tuple.world = Base64.decodeToLongWithBounds(buffer, previous, cursor);
                        break;
                    case 2:
                        tuple.time = Base64.decodeToLongWithBounds(buffer, previous, cursor);
                        break;
                    case 3:
                        tuple.id = Base64.decodeToLongWithBounds(buffer, previous, cursor);
                        break;
                }
                index++;
                previous = cursor + 1;
            }
            cursor++;
        }
        //collect last
        switch (index) {
            case 0:
                tuple.type = (byte) Base64.decodeToIntWithBounds(buffer, previous, cursor);
                break;
            case 1:
                tuple.world = Base64.decodeToLongWithBounds(buffer, previous, cursor);
                break;
            case 2:
                tuple.time = Base64.decodeToLongWithBounds(buffer, previous, cursor);
                break;
            case 3:
                tuple.id = Base64.decodeToLongWithBounds(buffer, previous, cursor);
                break;
        }
        return tuple;
    }
}
