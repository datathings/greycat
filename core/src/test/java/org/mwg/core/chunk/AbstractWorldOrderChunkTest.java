package org.mwg.core.chunk;

import org.junit.Assert;
import org.junit.Test;
import org.mwg.chunk.ChunkSpace;
import org.mwg.chunk.ChunkType;
import org.mwg.chunk.WorldOrderChunk;
import org.mwg.core.CoreConstants;
import org.mwg.plugin.MemoryFactory;
import org.mwg.struct.Buffer;

public abstract class AbstractWorldOrderChunkTest {

    private MemoryFactory factory;

    public AbstractWorldOrderChunkTest(MemoryFactory factory) {
        this.factory = factory;
    }

    @Test
    public void orderTest() {
        ChunkSpace space = factory.newSpace(100, 100, null);
        WorldOrderChunk map = (WorldOrderChunk) space.create(ChunkType.WORLD_ORDER_CHUNK, 0, 0, 0, null, null);
        //mass insert
        for (long i = 0; i < 10000; i++) {
            map.put(i, i * 3);
        }
        //mass check
        for (long i = 0; i < 10000; i++) {
            Assert.assertTrue(map.get(i) == i * 3);
        }
        space.freeChunk(map);
        space.free();
    }

    @Test
    public void saveLoadTest() {
        ChunkSpace space = factory.newSpace(100, 100, null);
        WorldOrderChunk map = (WorldOrderChunk) space.create(ChunkType.WORLD_ORDER_CHUNK, 0, 0, 0, null, null);
        //mass insert
        for (long i = 0; i < 10000; i++) {
            map.put(i, i * 3);
        }
        Assert.assertTrue(map.extra() == CoreConstants.NULL_LONG);
        map.setExtra(1000000);
        Assert.assertTrue(map.size() == 10000);
        Assert.assertTrue(map.extra() == 1000000);

        Buffer buffer = factory.newBuffer();
        map.save(buffer);
        WorldOrderChunk map2 = (WorldOrderChunk) space.create(ChunkType.WORLD_ORDER_CHUNK, 0, 0, 1, buffer, null);
        for (long i = 0; i < 10000; i++) {
            Assert.assertTrue(map2.get(i) == i * 3);
        }
        Assert.assertTrue(map2.extra() == 1000000);

        Buffer buffer2 = factory.newBuffer();
        map2.save(buffer2);
        Assert.assertTrue(compareBuffers(buffer, buffer2));
        buffer.free();
        buffer2.free();

        space.freeChunk(map);
        space.freeChunk(map2);
        space.free();
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