package greycat.memory;

public interface ChunkHeap {

    Chunk getOrCreateAndMark(long id);

}
