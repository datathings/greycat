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

    public TotpManager(Graph graph, String usersIndex, String loginAttribute, String passAttribute, String issuer) {
        super(graph, usersIndex, loginAttribute, passAttribute);
        this._issuer = issuer;
    }


    @Override
    public void verifyCredentials(Map<String, String> credentials, Callback<GCAccount> callback) {
        super.verifyCredentials(credentials, result -> {
            if (result != null) {
                String secret = (String) result.getUser().get("totp.secret");
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
        user.set("totp.secret", Type.STRING, secret);
        user.graph().save((aBoolean)->initialized.on(secret));
    }


}
