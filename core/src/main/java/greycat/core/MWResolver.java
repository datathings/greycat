package greycat.core;

import greycat.Node;
import greycat.memory.ChunkCache;
import greycat.memory.ChunkHeap;

public class MWResolver implements Resolver {

    private final ChunkCache cache;
    private final ChunkHeap heap;

    MWResolver(ChunkCache cache, ChunkHeap heap) {
        this.cache = cache;
        this.heap = heap;
    }

    @Override
    public void initNode(Node n) {
        CoreNode casted = (CoreNode) n;
        StructProxy proxy = casted.chunk;
        proxy.target = heap.getOrCreateAndMark(proxy.id);
    }

    void free(Node n) {
        CoreNode casted = (CoreNode) n;
        casted.chunk.target.unmark();
    }

}
