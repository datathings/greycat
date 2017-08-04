package greycat.memory.heap;

import greycat.memory.Chunk;
import greycat.memory.ChunkHeap;

import java.util.HashMap;
import java.util.Map;

public class HChunkHeap implements ChunkHeap, HHost {

    private Map<Long, Chunk> backend = new HashMap<Long, Chunk>();

    @Override
    public Chunk getOrCreateAndMark(long id) {
        Chunk root = backend.get(id);
        if (root != null) {
            root.mark();
            return root;
        } else {
            Chunk newChunk = new HChunk(id, this);
            newChunk.mark();
            backend.put(id, newChunk);
            return newChunk;
        }
    }

    @Override
    public void unregister(long offset) {
        backend.remove(offset);//TODO put in cache here
    }
}
