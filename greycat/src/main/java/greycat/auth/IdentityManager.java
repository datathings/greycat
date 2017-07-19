/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.auth;

import greycat.Callback;
import greycat.Node;

import java.util.Map;

/**
 * Created by Gregory NAIN on 18/07/2017.
 */
public interface IdentityManager {

    void    onChannelConnected(String sessionId, GCAccount account);
    void    onChannelDisconnected(String sessionId);
    boolean onChannelActivity(String sessionId);

    void    verifyCredentials(Map<String, String> credentials, Callback<GCAccount> callback);
    void    verifySession(String uuid, Callback<GCAccount> callback);

    void    resetPassword(String uuid, String pass, Callback<Integer> callback);
}
