package greycat.memory;

public interface ChunkCache {

    Chunk get(long id, long world, long time, int seq);

    Chunk set(long id, long world, long time, int seq, Chunk chunk);

}
