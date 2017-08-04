package greycat.core;

import greycat.memory.Struct;

public interface Resolver {

    Struct newRoot(long id, long time, long world);

}
