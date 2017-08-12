/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.ac.auth;

import greycat.*;
import greycat.ac.AuthenticationManager;
import greycat.ac.Session;
import greycat.plugin.NodeState;
import greycat.struct.EStructArray;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */
public class BaseAuthenticationManager implements AuthenticationManager {

    private Graph _graph;
    private String _acIndexName;
    private String _usersIndexName = "Users";
    private String _loginAttribute = "login";
    private String _passwordAttribute = "pass";
    private String _firstAdminLogin = "admin@local.host";

    private long _passwordChangeKeyValidity = 5 * 60 * 1000;

    private OtpManager _otpManager;

    private Map<String, PasswordKey> _keyToPasswordKey = new HashMap<>();

    private boolean _locked = false;

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public BaseAuthenticationManager(Graph _graph, String acIndexName) {
        this._graph = _graph;
        this._acIndexName = acIndexName;
        executor.scheduleAtFixedRate(() -> {
            Collection<PasswordKey> pks = new ArrayList<>(_keyToPasswordKey.values());
            for (PasswordKey pk : pks) {
                if (System.currentTimeMillis() > pk.deadline()) {
                    _keyToPasswordKey.remove(pk.authKey());
                }
            }
        }, 20, 20, TimeUnit.SECONDS);
    }

    @Override
    public AuthenticationManager setUsersIndexName(String usersIndexName) {
        if (_locked) {
            throw new RuntimeException("User index name must be done before ACM initialization.");
        } else {
            this._usersIndexName = usersIndexName;
        }
        return this;
    }

    @Override
    public AuthenticationManager setLoginAttribute(String loginAttribute) {
        if (_locked) {
            throw new RuntimeException("Login attribute name must be done before ACM initialization.");
        } else {
            this._loginAttribute = loginAttribute;
        }
        return this;
    }

    @Override
    public AuthenticationManager setPasswordAttribute(String passwordAttribute) {
        if (_locked) {
            throw new RuntimeException("Password attribute name must be done before ACM initialization.");
        } else {
            this._passwordAttribute = passwordAttribute;
        }
        return this;
    }

    @Override
    public AuthenticationManager setFirstAdminLogin(String adminLogin) {
        if (_locked) {
            throw new RuntimeException("Password attribute name must be done before ACM initialization.");
        } else {
            this._firstAdminLogin = adminLogin;
        }
        return this;
    }

    @Override
    public AuthenticationManager activateTwoFactorsAuth(String issuer, boolean strict) {
        this._otpManager = new OtpManager(_graph, _acIndexName, issuer, strict);
        return this;
    }

    @Override
    public void resetTwoFactorSecret(long uid, Callback<String> newSecret) {
        if (this._otpManager == null) {
            throw new RuntimeException("Two factor authentication not activated");
        } else {
            this._otpManager.resetSecret(uid, secret -> {
                newSecret.on(secret.secret());
            });
        }
    }

    @Override
    public String getAuthenticatorUri(Node user, String secret) {
        if (_otpManager == null) {
            throw new RuntimeException("Two Factor not activated. Cannot get Authenticator URI.");
        }
        return OtpEngine.getAuthenticatorUri((String) user.get(_loginAttribute), secret, _otpManager.getIssuer());
    }

    @Override
    public void revokeTwoFactorSecret(long uid, Callback<Boolean> done) {
        if (this._otpManager == null) {
            throw new RuntimeException("Two factor authentication not activated");
        } else {
            this._otpManager.deleteSecret(uid, done);
        }
    }


    @Override
    public AuthenticationManager setPasswordChangeKeyValidity(long duration) {
        this._passwordChangeKeyValidity = duration;
        return this;
    }

    @Override
    public void verifyCredentials(Map<String, String> credentials, Callback<Long> callback) {

        String login = credentials.get(_loginAttribute);
        String pass = credentials.get(_passwordAttribute);

        if (login == null || pass == null || login.trim().equals("")) {
            callback.on(null);
            return;
        }

        _graph.index(0, System.currentTimeMillis(), _usersIndexName, indexNode -> {
            indexNode.findFrom(users -> {
                if (users.length != 1) {
                    if (users.length > 1) {
                        throw new RuntimeException("multiple users indexed with the same ID !");
                    }
                    System.err.println("User not found");
                    callback.on(null);
                } else {
                    Node user = users[0];
                    System.out.println("Password check: " + user.get(_passwordAttribute) + " === " + pass);
                    boolean passCheck = user.get(_passwordAttribute).equals(pass);
                    if (passCheck) {
                        if (_otpManager == null) {
                            callback.on(user.id());
                        } else {
                            _otpManager.verifyCredentials(user.id(), credentials, callback);
                        }
                    } else {
                        callback.on(null);
                    }
                }
            }, login);
        });
    }

    @Override
    public void createPasswordChangeAuthKey(long uid, Callback<String> callback) {
        _graph.lookup(0, System.currentTimeMillis(), uid, user -> {
            if (user != null) {
                UUID tmpUUID = UUID.randomUUID();
                PasswordKey key = new PasswordKey(uid, tmpUUID.toString(), System.currentTimeMillis() + _passwordChangeKeyValidity);
                _keyToPasswordKey.put(key.authKey(), key);
                callback.on(key.authKey());
            } else {
                System.err.println("Lookup failed on uid:" + uid);
                callback.on(null);
            }
        });
    }

    @Override
    public void resetPassword(String authKey, String newPass, Callback<Integer> callback) {
        PasswordKey pk = _keyToPasswordKey.remove(authKey);
        if (pk != null) {
            if (System.currentTimeMillis() < pk.deadline()) {
                _graph.lookup(0, System.currentTimeMillis(), pk.uid(), user -> {
                    if (user != null) {
                        user.set(_passwordAttribute, Type.STRING, newPass);
                        _graph.save((ok) -> {
                            callback.on(1);
                        });
                    } else {
                        throw new RuntimeException("Could not resolve user with uid:" + pk.uid());
                    }
                });
            } else {
                callback.on(-1);
            }
        } else {
            callback.on(-2);
        }
    }

    @Override
    public void load(Callback<Boolean> done) {
        _graph.index(-1, System.currentTimeMillis(), _acIndexName, acIndex -> {
            acIndex.findFrom(tmpPasswdKeys -> {
                Node tmpPasswdKeysNode;
                if (tmpPasswdKeys == null || tmpPasswdKeys.length == 0) {
                    throw new RuntimeException("Should not load if never saved !");
                } else {
                    tmpPasswdKeysNode = tmpPasswdKeys[0];
                }
                NodeState ns = _graph.resolver().resolveState(tmpPasswdKeysNode);
                ArrayList<Integer> attKeys = new ArrayList<>();
                ns.each((attributeKey, elemType, elem) -> {
                    if (elemType != Type.STRING) {
                        attKeys.add(attributeKey);
                        PasswordKey pk = PasswordKey.load((EStructArray) elem);
                        if (System.currentTimeMillis() < pk.deadline()) {
                            _keyToPasswordKey.put(pk.authKey(), pk);
                        }
                    }
                });
                for (int attKey : attKeys) {
                    tmpPasswdKeysNode.removeAt(attKey);
                }
                this._locked = true;
                _graph.save(done);
            }, "passwdKeys");
        });

    }

    @Override
    public void save(Callback<Boolean> done) {
        _graph.index(-1, System.currentTimeMillis(), _acIndexName, acIndex -> {
            acIndex.findFrom(passwdKeyNodes -> {
                Node passwdKeysNode;
                if (passwdKeyNodes == null || passwdKeyNodes.length == 0) {
                    passwdKeysNode = _graph.newNode(acIndex.world(), acIndex.time());
                    passwdKeysNode.setGroup(2);
                    passwdKeysNode.set("name", Type.STRING, "passwdKeys");
                    acIndex.update(passwdKeysNode);
                } else {
                    passwdKeysNode = passwdKeyNodes[0];
                }
                int i = 0;
                for (PasswordKey pk : _keyToPasswordKey.values()) {
                    if (System.currentTimeMillis() < pk.deadline()) {
                        EStructArray passwordKeyContainer = (EStructArray) passwdKeysNode.getOrCreateAt(i++, Type.ESTRUCT_ARRAY);
                        pk.save(passwordKeyContainer);
                    }
                }
                _graph.save(done);
            }, "passwdKeys");
        });
    }

    @Override
    public void loadInitialData(boolean createAdminAtBoot, Callback<Boolean> done) {
        _graph.declareIndex(0, _usersIndexName, newUsersIndex -> {
            newUsersIndex.setGroup(2);

            if (createAdminAtBoot) {
                Node admin = _graph.newNode(0, System.currentTimeMillis());
                admin.setGroup(3);
                admin.set(_loginAttribute, Type.STRING, this._firstAdminLogin)
                        .set(_passwordAttribute, Type.STRING, "7c9619638d47730bd9c1509e0d553640b762d90dd3227bb7e6a5fc96bb274acb");
                if (this._otpManager != null) {
                    _otpManager.resetSecret(admin.id(), secret -> {
                        System.out.println("Admin authenticator account: " + OtpEngine.getQRURL(this._firstAdminLogin, secret.secret(), _otpManager.getIssuer()));
                    });
                }
                newUsersIndex.update(admin);
            }
            this._locked = true;
            done.on(true);
        }, _loginAttribute);
    }

    @Override
    public void printCurrentConfiguration(StringBuilder sb) {
        sb.append("#########   Authentication Manager - CurrentConfiguration   #########\n\n");
        sb.append("Users index: " + _usersIndexName + "\n");
        sb.append("Login attribute: " + _loginAttribute + "\n");
        sb.append("Password attribute: " + _passwordAttribute + "\n");
        sb.append("Two-Factor Authentication: " + (_otpManager != null ? "ENABLED" : "DISABLED") + "\n");
        if (_otpManager != null) {
            sb.append("Two-Factor Config {isStrict: " + Boolean.toString(_otpManager.isStrict()) + ", issuer: " + _otpManager.getIssuer() + "}\n");
        }
        sb.append("********        Authentication Manager - Users list         *********\n\n");
        _graph.index(0, System.currentTimeMillis(), _usersIndexName, usersIndex -> {
            usersIndex.findFrom(users -> {
                for (Node user : users) {
                    sb.append(user.toString() + "\n");
                }
            });
        });

    }

}
