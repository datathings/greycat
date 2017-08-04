package greycat.memory;

public interface ChunkHeap {


    Chunk create(long id, long time, long world, long seq);
}
