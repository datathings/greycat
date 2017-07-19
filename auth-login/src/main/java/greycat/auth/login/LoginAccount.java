/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.auth.login;

import greycat.Node;
import greycat.auth.GCAccount;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static java.lang.System.currentTimeMillis;

/**
 * Created by Gregory NAIN on 23/05/2017.
 */
public class LoginAccount implements GCAccount {

    private Node _userNode;
    private String _login;

    private UUID _sessionId;
    private long _expiration;
    private long _validityPeriod;
    private boolean _refreshOnActivity;


    public LoginAccount(String login, Node userNode, long initialValidity, boolean refreshOnActivity) {
        _sessionId = UUID.randomUUID();

        this._login = login;
        this._userNode = userNode;
        this._validityPeriod = initialValidity;
        this._refreshOnActivity = refreshOnActivity;

        _expiration = currentTimeMillis() + _validityPeriod;
    }

    public void hit() {
        if(this._refreshOnActivity){
            _expiration = currentTimeMillis() + _validityPeriod;
        }
    }

    public boolean checkSameSession(String uuidString) {
        UUID other = UUID.fromString(uuidString);
        return other.equals(this._sessionId);
    }

    public String getSessionId() {
        return this._sessionId.toString();
    }

    @Override
    public Node getUser() {
        return _userNode;
    }

    @Override
    public String getLogin() {
        return _login;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > _expiration;
    }

}
