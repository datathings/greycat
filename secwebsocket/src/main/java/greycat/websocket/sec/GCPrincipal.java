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
public class GCPrincipal<T extends Node> implements Principal {

    private T user;

    public GCPrincipal(T user) {
        this.user = user;
    }

    public T getUser() {
        return this.user;
    }

    @Override
    public String getName() {
        return this.user.toString();
    }

    @Override
    public boolean implies(Subject subject) {
        return false;
    }
}
