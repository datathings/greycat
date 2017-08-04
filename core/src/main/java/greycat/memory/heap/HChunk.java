package greycat.memory.heap;

import greycat.memory.Chunk;
import greycat.memory.Struct;

import java.util.Map;

public class HChunk implements Chunk, HHost {

    private boolean dirty = false;
    private Struct payload;
    private Map<Integer, Chunk> children = null;

    int mark = 0;

    private final long offset;
    private final HHost host;

    HChunk(long offset, HHost host) {
        this.offset = offset;
        this.host = host;
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
    public void unregister(long offset) {
        //TODO
    }

}

