package greycat.ac;

import greycat.struct.EStructArray;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */
public interface Session {
    long uid();

    String sessionId();

    long deadline();

    boolean setLastHit(long lastHit);

    boolean isExpired();

    void save(EStructArray container);
}
