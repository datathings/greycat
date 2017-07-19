/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.auth.totp;

import greycat.Callback;
import greycat.Graph;
import greycat.Node;
import greycat.Type;
import greycat.auth.GCAccount;
import greycat.auth.login.LoginManager;

import java.util.Map;

/**
 * Created by Gregory NAIN on 18/07/2017.
 */
public class TotpManager extends LoginManager {

    private String _issuer;
    private TotpEngine _engine = new TotpEngine();

    private static final String TOTP_SECRET_ATTRIBUTE_KEY = "totp.secret";

    public TotpManager(Graph graph, String usersIndex, String loginAttribute, String passAttribute, long allowedInactivityDelay, String issuer) {
        super(graph, usersIndex, loginAttribute, passAttribute, allowedInactivityDelay);
        this._issuer = issuer;
    }


    @Override
    public void verifyCredentials(Map<String, String> credentials, Callback<GCAccount> callback) {
        super.verifyCredentials(credentials, result -> {
            if (result != null) {
                String secret = (String) result.getUser().get(TOTP_SECRET_ATTRIBUTE_KEY);
                String code = credentials.get("gc.otp");
                if (secret != null && code != null && !secret.trim().equals("") && code.length() == 6) {
                    if (_engine.check_code(secret, Long.parseLong(code), System.currentTimeMillis())) {
                        callback.on(result);
                        return;
                    }
                }
            }
            callback.on(null);
        });
    }

    public void createTotpSecret(Node user, Callback<String> initialized) {
        String secret = TotpEngine.generateSecretKey();
        user.set(TOTP_SECRET_ATTRIBUTE_KEY, Type.STRING, secret);
        user.graph().save((aBoolean)->initialized.on(secret));
    }

    public String getAuthenticatorLink(Node user) {
        return TotpEngine.getAuthenticatorLink((String)user.get(loginAttribute), (String) user.get(TOTP_SECRET_ATTRIBUTE_KEY), _issuer);
    }



}
