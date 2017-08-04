package greycat.memory;

import greycat.Constants;

public class ChunkKey {

    public static String flat(long id, long world, long time, int seq) {
        StringBuilder buffer = new StringBuilder();
        if (id != 0) {
            buffer.append(id);
        }
        buffer.append(Constants.KEY_SEP);
        if (world != 0) {
            buffer.append(world);
        }
        buffer.append(Constants.KEY_SEP);
        if (time != 0) {
            buffer.append(time);
        }
        buffer.append(Constants.KEY_SEP);
        if (seq != 0) {
            buffer.append(seq);
        }
        return buffer.toString();
    }

}
