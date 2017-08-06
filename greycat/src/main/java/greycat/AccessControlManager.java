/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat;


/**
 * Created by Gregory NAIN on 18/07/2017.
 */
public interface AccessControlManager {

    AccessControlManager setAcIndexName(String acIndexName);

    void shutdown();

    void start(Callback<Boolean> callback);

    AuthenticationManager getAuthenticationManager();

    SessionManager getSessionsManager();

    boolean canRead(long uid, int gid);

    boolean canWrite(long uid, int gid);
}
