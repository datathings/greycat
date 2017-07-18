/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.auth;

import greycat.Node;

/**
 * Created by Gregory NAIN on 18/07/2017.
 */
public interface GCAccount {

    String getSessionId();

    Node getUser();

    String getLogin();

    boolean isExpired();

    boolean checkSameSession(String sessionId);

    void hit();

}
