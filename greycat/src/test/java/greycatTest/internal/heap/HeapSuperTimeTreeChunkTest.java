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
package greycatTest.internal.heap;

import greycat.internal.heap.HeapBuffer;
import greycat.internal.heap.HeapChunkSpace;
import greycat.internal.heap.HeapSuperTimeTreeChunk;
import greycat.struct.Buffer;
import org.junit.Assert;
import org.junit.Test;

public class HeapSuperTimeTreeChunkTest {

    @Test
    public void test() {
        HeapChunkSpace space = new HeapChunkSpace(100, 10, null, false);
        HeapSuperTimeTreeChunk tree = new HeapSuperTimeTreeChunk(space, -1);
        for (int i = 0; i < 100; i = i + 10) {
            tree.insert(i, i);
        }
        for (int i = 5; i < 100; i = i + 10) {
            Assert.assertEquals(tree.previous(i), i - 5);
        }
    }

    @Test
    public void loadSaveTest() {
        HeapChunkSpace space = new HeapChunkSpace(100, 10, null, false);
        HeapSuperTimeTreeChunk tree = new HeapSuperTimeTreeChunk(space, -1);
        for (int i = 0; i < 100; i = i + 10) {
            tree.insert(i, i);
        }
        tree.setTimeSensitivity(-1);
        tree.setTimeSensitivityOffset(50);
        Buffer buf = new HeapBuffer();
        tree.save(buf);
        HeapSuperTimeTreeChunk tree2 = new HeapSuperTimeTreeChunk(space, -1);
        tree2.load(buf);
        buf.free();
        for (int i = 5; i < 100; i = i + 10) {
            Assert.assertEquals(i - 5, tree2.previous(i));
        }
        Assert.assertTrue(tree2.timeSensitivity() == -1);
        Assert.assertTrue(tree2.timeSensitivityOffset() == 50);
        buf.free();
        space.free(tree);
        space.free(tree2);
        space.freeAll();
    }

    @Test
    public void stressTest() {
        HeapChunkSpace space = new HeapChunkSpace(100, 10, null, false);
        HeapSuperTimeTreeChunk tree = new HeapSuperTimeTreeChunk(space, -1);
        for (long i = 1000000; i > 0; i = i - 2) {
            tree.insert(i,i);
        }
        Assert.assertEquals(95000, tree.previous(95001));
        Assert.assertEquals(120002, tree.next(120001));
        Buffer buffer = new HeapBuffer();
        tree.save(buffer);
        HeapSuperTimeTreeChunk tree2 = new HeapSuperTimeTreeChunk(space, -1);
        tree2.load(buffer);
        Assert.assertEquals(tree.size(), tree2.size());

    }

}
