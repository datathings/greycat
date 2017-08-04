package greycat.memory;

import greycat.Constants;

public class ChunkKey {

    public static String flatFrom(Chunk chunk) {
        return flat(chunk.id(), chunk.time(), chunk.world(), chunk.seq());
    }

    public static String flat(long id, long world, long time, long seq) {
        StringBuilder buffer = new StringBuilder();
        if (id != 0) {
            buffer.append(id);
        }
        buffer.append((char)Constants.KEY_SEP);
        if (world != 0) {
            buffer.append(world);
        }
        buffer.append((char)Constants.KEY_SEP);
        if (time != 0) {
            buffer.append(time);
        }
        buffer.append((char)Constants.KEY_SEP);
        if (seq != 0) {
            buffer.append(seq);
        }
        return buffer.toString();
    }

}
