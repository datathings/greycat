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
package greycatTest.internal.heap;

import greycat.chunk.ChunkType;
import greycat.chunk.TimeTreeDValueChunk;
import greycat.internal.heap.HeapBuffer;
import greycat.internal.heap.HeapChunkSpace;
import greycat.struct.Buffer;
import org.junit.Assert;
import org.junit.Test;

public class HeapTimeTreeDValueChunkTest {

    @Test
    public void test() {
        HeapChunkSpace space = new HeapChunkSpace(100, 10, null, false);
        TimeTreeDValueChunk ttvc = (TimeTreeDValueChunk) space.createAndMark(ChunkType.TIME_TREE_DVALUE_CHUNK, 0, 0, 1);
        for (int i = 0; i < 100; i = i + 10) {
            ttvc.insertValue(i, i * 1.5d);
        }
        for (int i = 5; i < 100; i = i + 10) {
            Assert.assertEquals(i - 5, ttvc.previous(i));
            int offset = ttvc.previousOffset(i);
            Assert.assertEquals(i-5,ttvc.getKey(offset));
            Assert.assertTrue((i-5)*1.5d == ttvc.getValue(offset));
        }

        Buffer buf = new HeapBuffer();
        ttvc.save(buf);
        TimeTreeDValueChunk ttvc2 = (TimeTreeDValueChunk) space.createAndMark(ChunkType.TIME_TREE_DVALUE_CHUNK, 0, 0, 2);
        ttvc2.load(buf);
        buf.free();
        for (int i = 5; i < 100; i = i + 10) {
            Assert.assertEquals(i - 5, ttvc2.previous(i));
            int offset = ttvc.previousOffset(i);
            Assert.assertEquals(i-5,ttvc.getKey(offset));
            Assert.assertTrue((i-5)*1.5d == ttvc.getValue(offset));
        }
        buf.free();
        space.free(ttvc);
        space.free(ttvc2);
        space.freeAll();
    }

}
