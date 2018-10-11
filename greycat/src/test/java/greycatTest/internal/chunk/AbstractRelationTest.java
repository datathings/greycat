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

import greycat.chunk.ChunkSpace;
import greycat.chunk.ChunkType;
import greycat.chunk.StateChunk;
import greycat.plugin.MemoryFactory;
import org.junit.Assert;
import org.junit.Test;
import greycat.Type;
import greycat.struct.Relation;

public abstract class AbstractRelationTest {

    private MemoryFactory factory;

    public AbstractRelationTest(MemoryFactory factory) {
        this.factory = factory;
    }

    @Test
    public void genericTest() {

        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        Relation relation = (Relation) chunk.getOrCreateAt(0, Type.RELATION);

        Assert.assertEquals(relation.size(), 0);
        relation.add(-1);
        relation.add(10);
        relation.add(100);
        Assert.assertEquals(relation.size(), 3);

        Assert.assertEquals(relation.get(0), -1);
        Assert.assertEquals(relation.get(1), 10);
        Assert.assertEquals(relation.get(2), 100);

        for (int i = 200; i < 1000; i++) {
            relation.add(i);
        }

        Assert.assertEquals(relation.size(), 803);
        Assert.assertEquals(relation.get(802), 999);

        relation.remove(3000);
        Assert.assertEquals(relation.size(), 803);

        relation.remove(950);
        Assert.assertEquals(relation.size(), 802);

        space.free(chunk);
        space.freeAll();

    }

    @Test
    public void insertTest() {
        ChunkSpace space = factory.newSpace(100, -1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        Relation relation = (Relation) chunk.getOrCreateAt(0, Type.RELATION);
        relation.add(1);
        relation.add(3);
        relation.insert(1, 2);
        relation.insert(0, 0);
        relation.insert(4, 4);
        Assert.assertEquals(relation.size(), 5);
        for (int i = 0; i < relation.size(); i++) {
            Assert.assertEquals(i, relation.get(i));
        }

        space.free(chunk);
        space.freeAll();
    }

}
