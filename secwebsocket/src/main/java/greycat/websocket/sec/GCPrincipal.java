/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket.sec;

import javax.security.auth.Subject;
import java.security.Principal;

/**
 * Created by Gregory NAIN on 23/05/2017.
 */
public class GCPrincipal implements Principal {

    private String firstName, lastName, email;

    public GCPrincipal(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    @Override
    public String getName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean implies(Subject subject) {
        return false;
    }
}
