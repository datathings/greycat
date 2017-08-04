package greycat.core;

import greycat.Node;

public class CoreNode implements Node {

    final StructProxy chunk;

    CoreNode(long id, long time, long world) {
        chunk = new StructProxy(id, time, world);
    }

    //TODO

}
