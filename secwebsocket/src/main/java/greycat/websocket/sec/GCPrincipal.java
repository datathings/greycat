/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket.sec;

import greycat.Node;

import javax.security.auth.Subject;
import java.security.Principal;

/**
 * Created by Gregory NAIN on 23/05/2017.
 */
public class GCPrincipal implements Principal {

    private Node _userNode;
    private String _login;

    public GCPrincipal(String login, Node userNode) {
        this._userNode = userNode;
        this._login = login;
    }

    public Long getUserId() {
        return _userNode.id();
    }

    @Override
    public String getName() {
        return this._login;
    }

    @Override
    public boolean implies(Subject subject) {
        return false;
    }
}
