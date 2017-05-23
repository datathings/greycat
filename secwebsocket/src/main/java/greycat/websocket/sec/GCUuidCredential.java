/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket.sec;

import io.undertow.security.idm.Credential;

import java.util.UUID;

/**
 * Created by Gregory NAIN on 23/05/2017.
 */
public class GCUuidCredential implements Credential {
    private UUID _cred;

    public GCUuidCredential(String _cred) {
        this._cred = UUID.fromString(_cred);
    }

    public UUID _cred() {
        return _cred;
    }
}
