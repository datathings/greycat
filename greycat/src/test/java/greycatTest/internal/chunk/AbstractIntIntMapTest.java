/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
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

import greycat.Type;
import greycat.chunk.ChunkSpace;
import greycat.chunk.ChunkType;
import greycat.chunk.StateChunk;
import greycat.internal.CoreConstants;
import greycat.plugin.MemoryFactory;
import greycat.struct.Buffer;
import greycat.struct.IntIntMap;
import greycat.struct.IntIntMapCallBack;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractIntIntMapTest {
    private MemoryFactory factory;

    public AbstractIntIntMapTest(MemoryFactory factory) {
        this.factory = factory;
    }

    @Test
    public void genericTest() {

        ChunkSpace space = factory.newSpace(100, -1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        IntIntMap map = (IntIntMap) chunk.getOrCreateAt(0, Type.INT_TO_INT_MAP);

        Assert.assertEquals(map.size(), 0);

        map.put(0, 0);
        Assert.assertEquals(map.size(), 1);
        Assert.assertTrue(0 == map.get(0));

        map.put(1, 1);
        Assert.assertEquals(map.size(), 2);
        Assert.assertTrue(0 == map.get(0));
        Assert.assertTrue(1 == map.get(1));

        //no effect
        map.put(1, 1);

        map.put(0, 1);
        map.put(1, 2);
        Assert.assertTrue(1 == map.get(0));
        Assert.assertTrue(2 == map.get(1));

        map.put(2, 2);
        Assert.assertTrue(2 == map.get(2));

        final int[] keys = new int[3];
        final int[] values = new int[3];
        final int[] resIndex = {0};
        map.each(new IntIntMapCallBack() {
            @Override
            public void on(int key, int value) {
                keys[resIndex[0]] = key;
                values[resIndex[0]] = value;
                resIndex[0]++;
            }
        });
        Assert.assertTrue(1 == values[0]);
        Assert.assertTrue(2 == values[1]);
        Assert.assertTrue(2 == values[2]);
        Assert.assertTrue(0 == keys[0]);
        Assert.assertTrue(1 == keys[1]);
        Assert.assertTrue(2 == keys[2]);

        //force the graph to do a rehash capacity
        for (int i = 0; i < CoreConstants.MAP_INITIAL_CAPACITY; i++) {
            map.put(i, i);
        }
        //test that all values are consistent
        for (int i = 0; i < CoreConstants.MAP_INITIAL_CAPACITY; i++) {
            Assert.assertTrue(map.get(i) == i);
        }

        Buffer buffer = factory.newBuffer();
        chunk.save(buffer);
        Assert.assertEquals("C|s|A|Q:A:A:C:C:E:E:G:G:I:I:K:K:M:M:O:O", buffer.toString());
        StateChunk loaded = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 10, 10, 10);
        loaded.load(buffer);
        Buffer buffer2 = factory.newBuffer();
        chunk.save(buffer2);
        Assert.assertEquals("C|s|A|Q:A:A:C:C:E:E:G:G:I:I:K:K:M:M:O:O", buffer2.toString());

        buffer.free();
        buffer2.free();
        space.free(chunk);
        space.free(loaded);
        space.freeAll();

    }
}
