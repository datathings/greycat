package greycat.core;

import greycat.Graph;
import greycat.Node;
import greycat.base.GenericNode;
import greycat.memory.ChunkCache;
import greycat.memory.ChunkHeap;
import greycat.memory.ChunkStorage;
import greycat.memory.Struct;
import greycat.memory.heap.HChunkCache;
import greycat.memory.heap.HChunkHeap;
import greycat.memory.heap.HChunkStorage;

public class CoreGraph implements Graph {

    private final ChunkCache cache;
    private final ChunkHeap heap;
    private final ChunkStorage storage;
    private final MWResolver resolver;

    private int idGen = 0;

    public CoreGraph() {
        //goes to builder
        cache = new HChunkCache(1000);
        storage = new HChunkStorage();
        //automatic
        heap = new HChunkHeap(cache);
        resolver = new MWResolver(cache, heap);
    }

    @Override
    public final Node newNode(long world, long time) {
        idGen++;
        final Struct payload = resolver.newRoot(idGen, time, world);
        return new GenericNode(payload);
    }

    @Override
    public void free(Node n) {
        resolver.free(n);

    }

    @Override
    public Node alias(Node n) {
        return new GenericNode(n.chunk().payload());
    }

}
