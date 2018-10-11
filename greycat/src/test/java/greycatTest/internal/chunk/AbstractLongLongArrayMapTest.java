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
package greycatTest.internal.chunk;

import greycat.*;
import greycat.chunk.ChunkType;
import greycat.plugin.MemoryFactory;
import greycat.struct.Buffer;
import org.junit.Assert;
import org.junit.Test;
import greycat.chunk.ChunkSpace;
import greycat.chunk.StateChunk;
import greycat.internal.CoreConstants;
import greycat.struct.LongLongArrayMap;

public abstract class AbstractLongLongArrayMapTest {

    private MemoryFactory factory;

    public AbstractLongLongArrayMapTest(MemoryFactory factory) {
        this.factory = factory;
    }

    @Test
    public void genericTest() {

        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        LongLongArrayMap map = (LongLongArrayMap) chunk.getOrCreateAt(0, Type.LONG_TO_LONG_ARRAY_MAP);

        map.put(10, 10);
        Assert.assertTrue(map.size() == 1);
        Assert.assertTrue(map.get(10).length == 1);
        Assert.assertTrue(map.get(10)[0] == 10);

        map.put(10, 100);
        Assert.assertTrue(map.size() == 2);
        Assert.assertTrue(map.get(10).length == 2);
        Assert.assertTrue(map.get(10)[0] == 100);
        Assert.assertTrue(map.get(10)[1] == 10);


        map.put(10, 100);
        Assert.assertTrue(map.size() == 2);
        Assert.assertTrue(map.get(10).length == 2);


        //force reHash
        for (int i = 0; i < CoreConstants.MAP_INITIAL_CAPACITY; i++) {
            map.put(CoreConstants.BEGINNING_OF_TIME, i);
        }
        Assert.assertTrue(map.size() == CoreConstants.MAP_INITIAL_CAPACITY + 2);

        long[] getRes = map.get(CoreConstants.BEGINNING_OF_TIME);
        Assert.assertTrue(getRes.length == CoreConstants.MAP_INITIAL_CAPACITY);
        for (int i = 0; i < CoreConstants.MAP_INITIAL_CAPACITY; i++) {
            Assert.assertTrue(getRes[i] == (CoreConstants.MAP_INITIAL_CAPACITY - i - 1));
        }

        //test previous to reHash
        Assert.assertTrue(map.get(10).length == 2);
        Assert.assertTrue(map.get(10)[0] == 100);
        Assert.assertTrue(map.get(10)[1] == 10);

        //make a remove call
        map.delete(10, 10);
        Assert.assertTrue(map.size() == CoreConstants.MAP_INITIAL_CAPACITY + 2 - 1);
        Assert.assertTrue(map.get(10).length == 1);

        map.delete(CoreConstants.BEGINNING_OF_TIME, 0);
        Assert.assertTrue(map.size() == CoreConstants.MAP_INITIAL_CAPACITY + 2 - 2);
        getRes = map.get(CoreConstants.BEGINNING_OF_TIME);
        Assert.assertTrue(getRes.length == CoreConstants.MAP_INITIAL_CAPACITY - 1);
        for (int i = 1; i < CoreConstants.MAP_INITIAL_CAPACITY; i++) {
            Assert.assertTrue(getRes[i - 1] == (CoreConstants.MAP_INITIAL_CAPACITY - i));
        }

        Buffer buffer = factory.newBuffer();
        chunk.save(buffer);
        Assert.assertEquals("C|W|A|Q:////////9:O:U:DI:////////9:M:////////9:C:////////9:E:////////9:G:////////9:I:////////9:K", buffer.toString());
        StateChunk loaded = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 10, 10, 10);
        loaded.load(buffer);
        Buffer buffer2 = factory.newBuffer();
        chunk.save(buffer2);
        Assert.assertEquals("C|W|A|Q:////////9:O:U:DI:////////9:M:////////9:C:////////9:E:////////9:G:////////9:I:////////9:K", buffer2.toString());

        buffer.free();
        buffer2.free();
        space.free(chunk);
        space.free(loaded);
        space.freeAll();

    }


    @Test
    public void reHashTest() {

        int[] order = new int[]{1, 10, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 11, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 12, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 13, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 14, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 15, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 16, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 17, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 18, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 19, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 2, 20, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 21, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 22, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 23, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 24, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 25, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 26, 260, 261, 262, 263, 264, 265, 266, 267, 268, 269, 27, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 28, 280, 281, 282, 283, 29, 3, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 4, 40, 41, 42, 43, 44, 45, 46, 47, 48, 5, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 63, 64, 65, 66, 67, 68, 69, 7, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 8, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99};

        Graph g = (new GraphBuilder()).build();
        g.connect(result -> {
            ChunkSpace space = factory.newSpace(100,-1, g, false);
            StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
            LongLongArrayMap map = (LongLongArrayMap) chunk.getOrCreateAt(0, Type.LONG_TO_LONG_ARRAY_MAP);


           // System.out.println(order.length);

            for (int index = 0; index < 257; index++) {
                int i = order[index];
                //for (int i : order) {
                Query query = space.graph().newQuery();
                query.add("number", "" + i);
                map.put(query.hash(), i);
            }

            for (int index = 0; index < 257; index++) {
                int i = order[index];
                //for (int i : order) {
                Query query = space.graph().newQuery();
                query.add("number", "" + i);
                Assert.assertTrue(map.get(query.hash()).length > 0);
                Assert.assertTrue(map.get(query.hash())[0] == i);
            }

            space.free(chunk);

            g.disconnect(new Callback<Boolean>() {
                @Override
                public void on(Boolean result) {
                    space.freeAll();
                }
            });

        });


    }

}
