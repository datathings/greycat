package greycat;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */
public interface SessionManager {

    SessionManager setInactivityDelay(long delay);

    Session getOrCreateSession(long uid);

    Session getSession(String sessionId);

    void clearSession(String sessionId);

    void load(Callback<Boolean> done);

    void save(Callback<Boolean> done);
}
