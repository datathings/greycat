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

import greycat.Type;
import greycat.chunk.ChunkSpace;
import greycat.chunk.ChunkType;
import greycat.chunk.StateChunk;
import greycat.plugin.MemoryFactory;
import greycat.struct.Buffer;
import org.junit.Assert;
import org.junit.Test;
import greycat.struct.DMatrix;

public abstract class AbstractMatrixTest {

    private MemoryFactory factory;

    public AbstractMatrixTest(MemoryFactory factory) {
        this.factory = factory;
    }

    @Test
    public void genericTest() {

        ChunkSpace space = factory.newSpace(100,-1, null, false);

        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        DMatrix matrix = (DMatrix) chunk.getOrCreateAt(0, Type.DMATRIX);

        matrix.init(3, 4); //3 rows, 4 columns

        Assert.assertEquals(3, matrix.rows());
        Assert.assertEquals(4, matrix.columns());

        for (double i = 0; i < matrix.rows(); i++) {
            for (double j = 0; j < matrix.columns(); j++) {
                matrix.set((int) i, (int) j, (i + j) / 100);
            }
        }

        for (double i = 0; i < matrix.rows(); i++) {
            for (double j = 0; j < matrix.columns(); j++) {
                Assert.assertEquals(matrix.get((int) i, (int) j) + "", "" + ((i + j) / 100));
            }
        }

        Buffer buffer = factory.newBuffer();
        chunk.save(buffer);

        //  Assert.assertEquals(buffer.toString(), "C|A,a,c:QAI:QBA:AAA:P4EeuFHrhR7:P5EeuFHrhR7:P4EeuFHrhR7:P5EeuFHrhR7:P5OuFHrhR64:P5EeuFHrhR7:P5OuFHrhR64:P6EeuFHrhR7:P5OuFHrhR64:P6EeuFHrhR7:P6JmZmZmZma");

        StateChunk chunk2 = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 1);
        chunk2.load(buffer);

        Buffer bufferAfter = factory.newBuffer();
        chunk2.save(bufferAfter);

        // Assert.assertEquals(bufferAfter.toString(), "C|A,a,c:QAI:QBA:AAA:P4EeuFHrhR7:P5EeuFHrhR7:P4EeuFHrhR7:P5EeuFHrhR7:P5OuFHrhR64:P5EeuFHrhR7:P5OuFHrhR64:P6EeuFHrhR7:P5OuFHrhR64:P6EeuFHrhR7:P6JmZmZmZma");

        Assert.assertTrue(compareBuffers(buffer, bufferAfter));


        buffer.free();
        bufferAfter.free();

        space.free(chunk);
        space.free(chunk2);
        space.freeAll();

    }

    @Test
    public void extractTest() {
        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
    DMatrix matrix = (DMatrix) chunk.getOrCreateAt(0, Type.DMATRIX);
        matrix.init(3, 2);
        for (int i = 0; i < 2; i++) {
            matrix.set(0, i, i + 1);
            matrix.set(1, i, i + 1);
            matrix.set(2, i, i + 1);
        }
        for (int i = 0; i < 2; i++) {
            double[] extracted = matrix.column(i);
            Assert.assertEquals(3, extracted.length);
            double val = i + 1;
            Assert.assertEquals(val + "", extracted[0] + "");
            Assert.assertEquals(val + "", extracted[1] + "");
            Assert.assertEquals(val + "", extracted[2] + "");
        }
        space.free(chunk);
        space.freeAll();
    }

    @Test
    public void appendTest() {
        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        DMatrix matrix = (DMatrix) chunk.getOrCreateAt(0, Type.DMATRIX);
        matrix.appendColumn(new double[]{1, 2, 3});
        matrix.appendColumn(new double[]{4, 5, 6});

        Assert.assertEquals(matrix.rows(), 3);
        Assert.assertEquals(matrix.columns(), 2);

        Assert.assertTrue(matrix.get(0, 0) == 1);
        Assert.assertTrue(matrix.get(1, 0) == 2);
        Assert.assertTrue(matrix.get(2, 0) == 3);

        Assert.assertTrue(matrix.get(0, 1) == 4);
        Assert.assertTrue(matrix.get(1, 1) == 5);
        Assert.assertTrue(matrix.get(2, 1) == 6);

        space.free(chunk);
        space.freeAll();
    }

    private boolean compareBuffers(Buffer buffer, Buffer buffer2) {
        if (buffer.length() != buffer2.length()) {
            return false;
        }
        for (int i = 0; i < buffer.length(); i++) {
            if (buffer.read(i) != buffer2.read(i)) {
                return false;
            }
        }
        return true;
    }


}
