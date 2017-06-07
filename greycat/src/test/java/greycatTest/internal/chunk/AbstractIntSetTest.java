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
import greycat.plugin.MemoryFactory;
import greycat.struct.Buffer;
import greycat.struct.IntSet;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractIntSetTest {
    private MemoryFactory factory;

    public AbstractIntSetTest(MemoryFactory factory) {
        this.factory = factory;
    }

    @Test
    public void genericTest() {

        ChunkSpace space = factory.newSpace(100, -1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        IntSet set = (IntSet) chunk.getOrCreateAt(0, Type.INT_SET);

        Assert.assertEquals(set.size(), 0);

        set.put(0);
        Assert.assertEquals(set.size(), 1);
        Assert.assertTrue(set.contains(0));

        set.put(1);
        Assert.assertEquals(set.size(), 2);
        Assert.assertTrue(set.contains(0));
        Assert.assertTrue(set.contains(1));

        //no effect
        Assert.assertFalse(set.put(1));

        set.put(2);

        int[] extract = set.extract();

        Assert.assertTrue(0 == extract[0]);
        Assert.assertTrue(1 == extract[1]);
        Assert.assertTrue(2 == extract[2]);


        Buffer buffer = factory.newBuffer();
        chunk.save(buffer);
        String a = buffer.toString();
        StateChunk loaded = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 10, 10, 10);
        loaded.load(buffer);
        Buffer buffer2 = factory.newBuffer();
        chunk.save(buffer2);
        String b = buffer.toString();
        Assert.assertEquals(a,b);

        buffer.free();
        buffer2.free();
        space.free(chunk);
        space.free(loaded);
        space.freeAll();

    }
}
