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
package greycatTest.internal.chunk;

import greycat.chunk.ChunkType;
import greycat.struct.StringIntMap;
import greycat.utility.HashHelper;
import org.junit.Assert;
import org.junit.Test;
import greycat.Type;
import greycat.chunk.ChunkSpace;
import greycat.chunk.StateChunk;
import greycat.plugin.MemoryFactory;

public abstract class AbstractStringIntMapTest {

    private MemoryFactory factory;

    public AbstractStringIntMapTest(MemoryFactory factory) {
        this.factory = factory;
    }

    @Test
    public void genericTest() {

        ChunkSpace space = factory.newSpace(100, -1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        StringIntMap map = (StringIntMap) chunk.getOrCreateAt(0, Type.STRING_TO_INT_MAP);
        map.put("Hello", 0);
        Assert.assertTrue(0 == map.getValue("Hello"));

        map.put("Hello1", 1);
        Assert.assertTrue(0 == map.getValue("Hello"));
        Assert.assertTrue(1 == map.getValue("Hello1"));

        //no effect
        map.put("Hello1", 1);
        map.put("Hello", 1);
        map.put("Hello1", 2);
        Assert.assertTrue(1 == map.getValue("Hello"));
        Assert.assertTrue(2 == map.getValue("Hello1"));

        map.put("DictionaryUsage", 10);
        Assert.assertTrue(10 == map.getValue("DictionaryUsage"));

        Assert.assertEquals(1, map.getValue("Hello"));
        Assert.assertTrue(HashHelper.equals("Hello", map.getByHash(HashHelper.hash("Hello"))));

        /*


        final String[] keys = new String[3];
        final long[] values = new long[3];
        final int[] resIndex = {0};
        map.each(new StringLongMapCallBack() {
            @Override
            public void on(final String key, final long value) {
                keys[resIndex[0]] = key;
                values[resIndex[0]] = value;
                resIndex[0]++;
            }
        });
        Assert.assertTrue(1 == values[0]);
        Assert.assertTrue(2 == values[1]);
        Assert.assertTrue(Constants.NULL_LONG == values[2]);
        Assert.assertTrue(HashHelper.equals("Hello", keys[0]));
        Assert.assertTrue(HashHelper.equals("Hello1", keys[1]));
        Assert.assertTrue(HashHelper.equals("DictionaryUsage", keys[2]));

        //force the graph to do a rehash capacity
        for (int i = 0; i < CoreConstants.MAP_INITIAL_CAPACITY; i++) {
            map.put("i_" + i, i);
        }
        //test that all values are consistent
        for (int i = 0; i < CoreConstants.MAP_INITIAL_CAPACITY; i++) {
            Assert.assertTrue(map.getValue("i_" + i) == i);
        }
        */

        space.free(chunk);
        space.freeAll();

    }

}
