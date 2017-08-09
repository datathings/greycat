/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.ac;

import java.util.Collection;

/**
 * Created by Gregory NAIN on 06/08/2017.
 */
public interface PermissionsManager {
    int READ_ALLOWED = 0;
    int WRITE_ALLOWED = 1;
    int READ_DENIED = 2;
    int WRITE_DENIED = 3;

    Collection<Permission> all();

    Permission get(long uid);

    boolean add(long uid, int permType, int gid);

    boolean add(long uid, int permType, int[] gids);
}
