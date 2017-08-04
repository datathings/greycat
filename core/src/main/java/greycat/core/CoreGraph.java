package greycat.core;

import greycat.Graph;
import greycat.Node;
import greycat.memory.ChunkCache;
import greycat.memory.ChunkHeap;
import greycat.memory.ChunkStorage;
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
        cache = new HChunkCache();
        heap = new HChunkHeap();
        storage = new HChunkStorage();
        resolver = new MWResolver(cache, heap);
    }

    @Override
    public Node newNode(long time) {
        idGen++;
        CoreNode newNode = new CoreNode(idGen, time, 0);
        resolver.initNode(newNode);
        return newNode;
    }

    @Override
    public void freeNode(Node n) {
        resolver.free(n);
    }

}
