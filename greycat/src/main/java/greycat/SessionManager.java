package greycat;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */
public interface SessionManager {

    Session getOrCreateSession(long uid);

    Session sessionCheck(String sessionId);

    void load(Callback<Boolean> done);

    void save(Callback<Boolean> done);
}
