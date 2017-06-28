/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket.sec;

import io.undertow.security.idm.Account;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static java.lang.System.currentTimeMillis;

/**
 * Created by Gregory NAIN on 23/05/2017.
 */
public class GCSecAccount implements Account {
    private GCPrincipal _principal;
    private UUID _sessionId;
    private long expiration;
    private static final long VALIDITY_PERIOD = 30*60*1000; // 5min


    public GCSecAccount(GCPrincipal principal) {
        _sessionId = UUID.randomUUID();
        _principal = principal;
        hit();
    }

    public void hit() {
        expiration = currentTimeMillis() + VALIDITY_PERIOD;
    }

    public boolean checkSameUUID(String uuidString) {
        UUID other = UUID.fromString(uuidString);
        return other.equals(this._sessionId);
    }

    public String getUUID() {
        return this._sessionId.toString();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiration;
    }

    @Override
    public GCPrincipal getPrincipal() {
        return _principal;
    }

    @Override
    public Set<String> getRoles() {
        return Collections.EMPTY_SET;
    }
}
