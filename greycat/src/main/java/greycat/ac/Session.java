package greycat.ac;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */
public interface Session {
    long uid();

    String sessionId();

    long deadline();

    boolean setLastHit(long lastHit);

    boolean isExpired();
}
