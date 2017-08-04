package greycat.core;

import greycat.Node;
import greycat.base.GenericNode;
import greycat.memory.ChunkCache;
import greycat.memory.ChunkHeap;
import greycat.memory.Struct;

public class MWResolver implements Resolver {

    private final ChunkCache cache;
    private final ChunkHeap heap;

    MWResolver(ChunkCache cache, ChunkHeap heap) {
        this.cache = cache;
        this.heap = heap;
    }

    @Override
    public Struct newRoot(long id, long time, long world) {
        return heap.create(id, time, world, 0).payload();
    }

    void free(Node n) {
        GenericNode casted = (GenericNode) n;
        casted.chunk().unmark();
    }

}
