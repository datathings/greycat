package greycat.memory;

public interface Chunk {

    void setDirty();

    void unmark();

    void mark();

    Struct payload();

}
