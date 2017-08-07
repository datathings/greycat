package greycat.memory.heap;

import greycat.memory.ChunkCache;
import greycat.memory.Chunk;
import greycat.memory.ChunkKey;

import java.util.LinkedHashMap;
import java.util.Map;

public class HChunkCache implements ChunkCache {

    //TODO optimize later to avoid created of string...
    private final int max;

    private LinkedHashMap<String, Chunk> backend = new LinkedHashMap<String, Chunk>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Chunk> eldest) {
            //TODO pop into secondary cache in cache of still out of sync
            return size() > max;
        }
    };

    public HChunkCache(int max) {
        this.max = max;
    }

    @Override
    public Chunk get(long id, long world, long time, long seq) {
        return backend.get(ChunkKey.flat(id, world, time, seq));
    }

    @Override
    public void put(Chunk chunk) {
        backend.put(ChunkKey.flatFrom(chunk), chunk);
    }

}
