package greycat.memory.heap;

import greycat.memory.Chunk;
import greycat.memory.ChunkCache;
import greycat.memory.Struct;

import java.util.Map;

public class HChunk implements Chunk, HHost {

    private boolean dirty = false;
    private Struct payload;
    private Map<Long, Chunk> children = null;

    private int mark = 0;

    private final long $id;
    private final long $time;
    private final long $world;
    private final long $seq;

    private final long offset;
    private final HHost host;
    private final ChunkCache cache;

    HChunk(long id, long time, long world, long seq, long offset, HHost host, ChunkCache cache) {
        this.$id = id;
        this.$time = time;
        this.$world = world;
        this.$seq = seq;
        //For un registration
        this.offset = offset;
        this.host = host;
        this.cache = cache;
        this.payload = new HStruct(this);
    }

    @Override
    public void setDirty() {
        //TODO propagate
        dirty = true;
    }

    @Override
    public void unmark() {
        mark--;
        if (mark == 0) {
            host.unregister(offset);
        } else {
            //host.unmark();
        }
    }

    @Override
    public void mark() {
        mark++;
    }

    @Override
    public Struct payload() {
        return payload;
    }

    @Override
    public final long id() {
        return $id;
    }

    @Override
    public final long time() {
        return $time;
    }

    @Override
    public final long world() {
        return $world;
    }

    @Override
    public final long seq() {
        return $seq;
    }

    @Override
    public final void unregister(long offset) {
        Chunk c = children.get(offset);
        children.remove(offset);
        cache.put(c);
    }

}

