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
import greycat.plugin.MemoryFactory;
import greycat.struct.Buffer;
import greycat.utility.Base64;
import org.junit.Assert;
import org.junit.Test;
import greycat.chunk.ChunkSpace;
import greycat.chunk.GenChunk;

public abstract class AbstractGenChunkTest {

    private MemoryFactory factory;

    public AbstractGenChunkTest(MemoryFactory factory) {
        this.factory = factory;
    }

    @Test
    public void genTest() {

        ChunkSpace space = factory.newSpace(100, -1, null, false);
        GenChunk genChunk = (GenChunk) space.createAndMark(ChunkType.GEN_CHUNK, 0, 0, 0);

        Assert.assertEquals(genChunk.newKey(), 1);
        Assert.assertEquals(genChunk.newKey(), 2);
        Assert.assertEquals(genChunk.newKey(), 3);

        space.free(genChunk);

        Buffer buf = factory.newBuffer();
        Base64.encodeLongToBuffer(100, buf);
        GenChunk genChunk2 = (GenChunk) space.createAndMark(ChunkType.GEN_CHUNK, 0, 0, 0);
        genChunk2.load(buf);
        buf.free();

        Assert.assertEquals(genChunk2.newKey(), 101);
        Assert.assertEquals(genChunk2.newKey(), 102);
        Assert.assertEquals(genChunk2.newKey(), 103);

        Buffer bufSave = factory.newBuffer();
        genChunk2.save(bufSave);
        byte[] flatSaved = bufSave.data();
        Assert.assertEquals(flatSaved.length, 2);
        Assert.assertEquals(flatSaved[0], 68);
        Assert.assertEquals(flatSaved[1], 79);
        Assert.assertEquals(103, Base64.decodeToLongWithBounds(bufSave, 0, 2));

        bufSave.free();
        space.free(genChunk2);

        GenChunk genChunk_100 = (GenChunk) space.createAndMark(ChunkType.GEN_CHUNK, 0, 0, 100);
        Assert.assertEquals(genChunk_100.newKey(), 13743895347201L);
        Assert.assertEquals(genChunk_100.newKey(), 13743895347202L);

        space.free(genChunk_100);
        space.freeAll();

    }


}
