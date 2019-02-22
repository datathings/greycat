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

import greycat.Constants;
import greycat.chunk.ChunkSpace;
import greycat.chunk.ChunkType;
import greycat.chunk.TimeTreeChunk;
import greycat.plugin.MemoryFactory;
import greycat.struct.Buffer;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTimeTreeTest {

    protected MemoryFactory factory;

    public AbstractTimeTreeTest(MemoryFactory factory) {
        this.factory = factory;
    }

    /*
    @Test
    public void incrementalSave() {
        ChunkSpace space = factory.newSpace(100, -1, null, false);
        TimeTreeChunk tree = (TimeTreeChunk) space.createAndMark(ChunkType.TIME_TREE_CHUNK, 0, 0, 0);
        Buffer buffer = factory.newBuffer();
        tree.saveDiff(buffer);
        Assert.assertTrue(compareWithString(buffer, ""));
        buffer.free();

        tree.insert(5);
        tree.insert(7);
        tree.insert(9);

        buffer = factory.newBuffer();
        tree.saveDiff(buffer);
        Assert.assertTrue(compareWithString(buffer, "G:K:O:S"));
        buffer.free();

        buffer = factory.newBuffer();
        tree.saveDiff(buffer);
        Assert.assertTrue(compareWithString(buffer, ""));
        buffer.free();

        space.free(tree);
        space.freeAll();

    }*/

    @Test
    public void nextTest() {
        ChunkSpace space = factory.newSpace(100, -1, null, false);
        TimeTreeChunk tree = (TimeTreeChunk) space.createAndMark(ChunkType.TIME_TREE_CHUNK, 0, 0, 0);
        for (long i = 0; i <= 6; i++) {
            tree.insert(i);
        }
        tree.insert(8L);
        tree.insert(10L);
        tree.insert(11L);
        tree.insert(13L);

        Assert.assertEquals(tree.previous(-1), Constants.NULL_LONG);
        Assert.assertEquals(tree.previous(0), Constants.NULL_LONG);
        Assert.assertEquals(tree.previous(1), 0L);
        Assert.assertEquals(tree.previous(7), 6L);
        Assert.assertEquals(tree.previous(8), 6L);
        Assert.assertEquals(tree.previous(9), 8L);
        Assert.assertEquals(tree.previous(10), 8L);
        Assert.assertEquals(tree.previous(13), 11L);
        Assert.assertEquals(tree.previous(14), 13L);

        space.free(tree);
        space.freeAll();
    }

    @Test
    public void previousOrEqualsTest() {
        ChunkSpace space = factory.newSpace(100, -1, null, false);
        TimeTreeChunk tree = (TimeTreeChunk) space.createAndMark(ChunkType.TIME_TREE_CHUNK, 0, 0, 0);
        for (long i = 0; i <= 6; i++) {
            tree.insert(i);
        }

        tree.insert(8L);
        tree.insert(10L);
        tree.insert(11L);
        tree.insert(13L);

        Assert.assertEquals(tree.previousOrEqual(-1), Constants.NULL_LONG);
        Assert.assertEquals(tree.previousOrEqual(0), 0L);
        Assert.assertEquals(tree.previousOrEqual(1), 1L);
        Assert.assertEquals(tree.previousOrEqual(7), 6L);
        Assert.assertEquals(tree.previousOrEqual(8), 8L);
        Assert.assertEquals(tree.previousOrEqual(9), 8L);
        Assert.assertEquals(tree.previousOrEqual(10), 10L);
        Assert.assertEquals(tree.previousOrEqual(13), 13L);
        Assert.assertEquals(tree.previousOrEqual(14), 13L);

        space.free(tree);
        space.freeAll();
    }

    @Test
    public void saveLoad() {
        ChunkSpace space = factory.newSpace(100, -1, null, false);
        TimeTreeChunk tree = (TimeTreeChunk) space.createAndMark(ChunkType.TIME_TREE_CHUNK, 0, 0, 0);
        for (long i = 0; i <= 2; i++) {
            tree.insert(i);
        }
        Buffer buffer = factory.newBuffer();
        tree.save(buffer);
        Assert.assertTrue(compareWithString(buffer, "G|A|A:C:E"));
        Assert.assertTrue(tree.size() == 3);
        TimeTreeChunk tree2 = (TimeTreeChunk) space.createAndMark(ChunkType.TIME_TREE_CHUNK, 0, 0, 0);
        tree2.load(buffer);
        Assert.assertTrue(tree2.size() == 3);
        Buffer buffer2 = factory.newBuffer();
        tree2.save(buffer2);
        Assert.assertTrue(compareBuffers(buffer, buffer2));
        tree2.insert(10000);
        space.free(tree);
        space.free(tree2);
        buffer2.free();
        buffer.free();
        space.freeAll();
    }

    /*
    @Test
    public void saveLoadExtras() {
        ChunkSpace space = factory.newSpace(100, -1, null, false);
        TimeTreeChunk tree = (TimeTreeChunk) space.createAndMark(ChunkType.TIME_TREE_CHUNK, 0, 0, 0);
        for (long i = 0; i <= 2; i++) {
            tree.insert(i);
        }
        tree.setExtra(10);
        tree.setExtra2(12);
        Buffer buffer = factory.newBuffer();
        tree.save(buffer);
        Assert.assertTrue(compareWithString(buffer, "U|Y|G:A:C:E"));
        Assert.assertTrue(tree.size() == 3);
        TimeTreeChunk tree2 = (TimeTreeChunk) space.createAndMark(ChunkType.TIME_TREE_CHUNK, 0, 0, 0);
        Assert.assertEquals(tree2.extra(), 10);
        Assert.assertEquals(tree2.extra2(), 12);
        tree2.load(buffer);
        Assert.assertTrue(tree2.size() == 3);
        Buffer buffer2 = factory.newBuffer();
        tree2.save(buffer2);
        Assert.assertTrue(compareBuffers(buffer, buffer2));
        tree2.insert(10000);
        space.free(tree);
        space.free(tree2);
        buffer2.free();
        buffer.free();
        space.freeAll();
    }*/

    @Test
    public void massiveTest() {
        ChunkSpace space = factory.newSpace(100, -1, null, false);
        TimeTreeChunk tree = (TimeTreeChunk) space.createAndMark(ChunkType.TIME_TREE_CHUNK, 0, 0, 0);
        //  long before = System.currentTimeMillis();
        long max = 100;
        for (long i = 0; i <= max; i = i + 2) {
            tree.insert(i);
        }

        for (long i = 1; i <= max; i = i + 2) {
            Assert.assertTrue(tree.previousOrEqual(i) == i - 1);
        }

        space.free(tree);
        space.freeAll();

        // long after = System.currentTimeMillis();
        //  System.out.println((after-before) / 1000);
    }

    /*
    @Test
    public void emptyHalfTest() {
        ChunkSpace space = factory.newSpace(100, -1, null, false);
        TimeTreeChunk tree = (TimeTreeChunk) space.createAndMark(ChunkType.TIME_TREE_CHUNK, 0, 0, 0);
        int nbElements = 10;
        for (int i = 0; i < nbElements; i++) {
            tree.insert(i);
        }
        final long[] nbCall = {0};
        tree.range(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, tree.size() / 2, new TreeWalker() {
            @Override
            public void elem(long t) {
                nbCall[0]++;
            }
        });
        Assert.assertTrue((nbElements / 2) == nbCall[0]);
        final long[] median = new long[1];
        nbCall[0] = 0;
        tree.range(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, tree.size() / 2, new TreeWalker() {
            @Override
            public void elem(long t) {
                median[0] = 5;
                nbCall[0]++;
            }
        });
        Assert.assertTrue(median[0] == 5);
        Assert.assertTrue(nbCall[0] == 5);
        tree.clearAt(median[0]);
        nbCall[0] = 0;
        tree.range(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, Constants.END_OF_TIME, new TreeWalker() {
            @Override
            public void elem(long t) {
                nbCall[0]++;
            }
        });
        Assert.assertTrue(nbCall[0] == 5);
        space.free(tree);
        space.freeAll();
    }*/

    private boolean compareWithString(Buffer buffer, String content) {
        for (int i = 0; i < content.length(); i++) {
            if (buffer.read(i) != content.codePointAt(i)) {
                return false;
            }
        }
        return true;
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

    @Test
    public void stressTest() {
        ChunkSpace space = factory.newSpace(100, -1, null, false);
        TimeTreeChunk tree = (TimeTreeChunk) space.createAndMark(ChunkType.TIME_TREE_CHUNK, 0, 0, 0);
        for (long i = 1000000; i > 0; i = i - 2) {
            tree.insert(i);
        }
        Assert.assertEquals(95000, tree.previous(95001));
        Assert.assertEquals(120002, tree.next(120001));
        Buffer buffer = factory.newBuffer();
        tree.save(buffer);
        TimeTreeChunk tree2 = (TimeTreeChunk) space.createAndMark(ChunkType.TIME_TREE_CHUNK, 0, 0, 1);
        tree2.load(buffer);
        Assert.assertEquals(tree.size(), tree2.size());

    }

/*
    @Test
    public void loadTest() {
        ChunkSpace space = factory.newSpace(1000000, -1, null, false);
        TimeTreeChunk chunk = (TimeTreeChunk) space.createAndMark(ChunkType.TIME_TREE_CHUNK, 0, 0, 0);
        Buffer buffer = factory.newBuffer();
        buffer.writeAll("Eu|fQ|qe75kLE:qe7+O1E:qe7+AOi:qe7+YTk:qe8DbUM:qe8+Dxg:qe8+M/y:qe8+LEK:qe8+T2k:qe8+SgS:qe8+HyW:qe8+Z7A:qe8+JSK:qe8+Cyy:qe89/fM:qe894TG:qe8+QAg:qe89osI:qe8+CVS:qe892m2:qe9lQqu:qe9lG3C:qe9k7Go:qe9k24k:qe9lM86:qe+BBso:qe+A+Cg:qe+A5bk:qe+BKXk:qe+A/1m:qe+A8yK:qe+AzLe:qe+A1OW:qe+OZTa:qe+Nwz8:qe+OJTA:qe+OGxC:qe+tg6g:qe+te3G:qe+tUKu:qe+tI48:qe+thf8:qe+s92k:qe+tVm4:qe+tWeA:qffIOY6:qffMhWm:qffQB8w:qffVBUc:qffVees:qfgTlQK:qfhrxBK:qfh5kRS:qfh/gB4:qfiFC/a:qfiMVwU:qfiRVC4:qfiVMJm:qfiWiJa:qfifhe+:qfiiDPk:qfikGBe:qfipkk4:qfirxdw:qfiw77+:qfi1kQI:qfi6Slm:qfjBY4C:qfjDvmw:qfjF9/0:qfjK6eY:qfjNg6e:qfjTukE:qfjZf9Y:qfjeHeu:qfjj3Xc:qfjpExI:qfjtip0:qfjxb/y:qfjzLly:qfj3qvo:qfj54AU:qfj82Ii:qfkBdIc:qfkEGtk:qfkFuUi:qfkIBTS:qfkIDKY:qfkJNHQ:qfkK9z+:qfkO2Tc:qfkTEug:qfkWKQu:qfkYdBS:qfkbH6y:qfkb8uy:qfkhYeU:qfkjlZK:qfkjkgm:qfkqX68:qfktGzI:qfkvTmS:qfk0ppe:qfk2OsM:qfk5r+g:qfk+clY:qflA9xo:qflFSgE:qflIqNC:qflNori:qflRaDY:qflV0iI:qflYTFg:qflcNY4:qflfOKs:qflhtwc:qflhOUQ:qflhcda:qflhyRw:qflhcYK:qflhR8y:qflhBne:qflrzGy:qfltBfG:qfl19Qi:qfl1oZI:qfl6h9s:qfl6xzG:qfl8vAO:qfmA9Yy:qfmFAZ6:qfmFMsq:qfmFGEy:qfmHDpC:qfmJMbQ:qfmLaLG:qfmOapA:qfmS8kI:qfmVDiI:qfmXpxS:qfmeS+u:qfmelAS:qfmhoto:qfmmnTs:qfmovwg:qfmpG2w:qfmp3oa:qfmqLea:qfmp6a+:qfmp6yg:qfmpoEu:".getBytes());
        chunk.load(buffer);

        chunk.insert(1460076282850L);

        space.free(chunk);
        space.freeAll();
    }*/

}
