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

import greycat.chunk.ChunkSpace;
import greycat.chunk.ChunkType;
import greycat.chunk.TimeTreeChunk;
import greycat.struct.Buffer;
import greycatTest.internal.chunk.AbstractTimeTreeTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class OffHeapTimeTreeChunkTest extends AbstractTimeTreeTest {

    public OffHeapTimeTreeChunkTest() {
        super(new OffHeapMemoryFactory());
    }

    @After
    public void tearDown() throws Exception {
        if (OffHeapConstants.DEBUG_MODE) {
            Assert.assertEquals(OffHeapConstants.SEGMENTS.size(), 0);
        }
    }

    @Test
    public void nextTest() {
        ChunkSpace space = factory.newSpace(100, null, false);
        TimeTreeChunk tree = (TimeTreeChunk) space.createAndMark(ChunkType.TIME_TREE_CHUNK, 0, 0, 0);
        Buffer buffer = factory.newBuffer();
        buffer.writeAll("W:////////9:q76Juyg:q7/CVoA:q7/dx+Q:q8APAXQ:q8BNF/w:q8BNdbw:q8BSEWw:q8CrNbw:q8DTxIw:q8D3lng".getBytes());
        tree.load(buffer);
        buffer.free();
        space.free(tree);
        space.freeAll();
    }

}
