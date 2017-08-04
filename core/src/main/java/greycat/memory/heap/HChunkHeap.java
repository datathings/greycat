package greycat.memory.heap;

import greycat.memory.Chunk;
import greycat.memory.ChunkCache;
import greycat.memory.ChunkHeap;

import java.util.HashMap;
import java.util.Map;

public class HChunkHeap implements ChunkHeap, HHost {

    private Map<Long, Chunk> backend = new HashMap<Long, Chunk>();
    private final ChunkCache cache;

    public HChunkHeap(ChunkCache cache) {
        this.cache = cache;
    }

    @Override
    public final Chunk create(long id, long time, long world, long seq) {
        Chunk newChunk = new HChunk(id, time, world, seq, id, this);
        newChunk.mark();
        backend.put(id, newChunk);
        return newChunk;
    }

    @Override
    public final void unregister(long offset) {
        Chunk c = backend.get(offset);
        backend.remove(offset);
        cache.put(c);
    }

}
