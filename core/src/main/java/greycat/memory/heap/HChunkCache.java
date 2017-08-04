package greycat.memory.heap;

import greycat.memory.ChunkCache;
import greycat.memory.Chunk;
import greycat.memory.ChunkKey;

import java.util.HashMap;
import java.util.Map;

public class HChunkCache implements ChunkCache {

    //TODO optimize later to avoid created of string...
    private Map<String, Chunk> backend = new HashMap<String, Chunk>();

    @Override
    public Chunk get(long id, long world, long time, int seq) {
        return backend.get(ChunkKey.flat(id, world, time, seq));
    }

    @Override
    public Chunk set(long id, long world, long time, int seq, Chunk chunk) {
        return backend.put(ChunkKey.flat(id, world, time, seq), chunk);
    }

}
