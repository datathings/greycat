package greycat.memory;

public interface ChunkCache {

    Chunk get(long id, long world, long time, long seq);

    void put(Chunk chunk);

}
