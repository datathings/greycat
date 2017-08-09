/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.ac;

import greycat.struct.EStructArray;

/**
 * Created by Gregory NAIN on 06/08/2017.
 */
public interface Group {

    int gid();

    String name();

    int[] path();

    Group createSubGroup(int gid, String name);

    void save(EStructArray container);
}
