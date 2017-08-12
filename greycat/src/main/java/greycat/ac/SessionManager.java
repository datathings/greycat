/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.ac;

import greycat.Callback;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */
public interface SessionManager {

    SessionManager setInactivityDelay(long delay);

    long getInactivityDelay();

    Session getOrCreateSession(long uid);

    Session getSession(String sessionId);

    void clearSession(String sessionId);

    void load(Callback<Boolean> done);

    void save(Callback<Boolean> done);

    void printCurrentConfiguration(StringBuilder sb);
}
