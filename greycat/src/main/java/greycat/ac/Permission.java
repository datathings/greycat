package greycat.ac;

import greycat.struct.EStructArray;

/**
 * Created by Gregory NAIN on 06/08/2017.
 */
public interface Permission {

    long uid();

    int[] read();

    int[] write();

    int[] notRead();

    int[] notWrite();

    void addRoot(long rootId);

    boolean addPerm(int permType, int gid);

    boolean addPerm(int permType, int[] gids);

    void save(EStructArray container);
}
