package greycat.ac.auth;

import greycat.*;
import greycat.plugin.NodeState;
import greycat.struct.EStructArray;

import java.util.*;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */
public class BaseAuthenticationManager implements AuthenticationManager {

    private Graph _graph;
    private String _acIndexName;
    private String _usersIndexName = "Users";
    private String _loginAttribute = "login";
    private String _passwordAttribute = "pass";

    private OtpManager _otpManager;

    private Map<String, PasswordKey> _keyToPasswordKey = new HashMap<>();

    private boolean _locked = false;

    public BaseAuthenticationManager(Graph _graph, String acIndexName) {
        this._graph = _graph;
        this._acIndexName = acIndexName;
        this._graph.actionRegistry().getOrCreateDeclaration(ActionResetPassword.ACTION_RESET_PASSWORD).setFactory(params -> new ActionResetPassword(this));
    }

    public BaseAuthenticationManager setUsersIndexName(String usersIndexName) {
        if (_locked) {
            throw new RuntimeException("User index name must be done before ACM initialization.");
        } else {
            this._usersIndexName = usersIndexName;
        }
        return this;
    }

    public BaseAuthenticationManager setLoginAttribute(String loginAttribute) {
        if (_locked) {
            throw new RuntimeException("Login attribute name must be done before ACM initialization.");
        } else {
            this._loginAttribute = loginAttribute;
        }
        return this;
    }

    public BaseAuthenticationManager setPasswordAttribute(String passwordAttribute) {
        if (_locked) {
            throw new RuntimeException("Password attribute name must be done before ACM initialization.");
        } else {
            this._passwordAttribute = passwordAttribute;
        }
        return this;
    }

    public BaseAuthenticationManager activateTwoFactorsAuth(String issuer, boolean strict) {
        this._otpManager = new OtpManager(_graph, _acIndexName, issuer, strict);
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
                PasswordKey key = new PasswordKey(uid, tmpUUID.toString(), System.currentTimeMillis() + 5 * 60 * 1000);
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
        PasswordKey pk = _keyToPasswordKey.get(authKey);
        if (pk != null) {
            _graph.lookup(0, System.currentTimeMillis(), pk.uid(), user -> {
                if (user != null) {
                    _keyToPasswordKey.remove(pk.authKey());
                    if (System.currentTimeMillis() < pk.deadline()) {
                        user.set(_passwordAttribute, Type.STRING, newPass);
                        _graph.save((ok) -> {
                            callback.on(1);
                        });
                    } else {
                        callback.on(-1);
                    }
                } else {
                    throw new RuntimeException("Could not resolve user with uid:" + pk.uid());
                }
            });
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
                    attKeys.add(attributeKey);
                    PasswordKey pk = PasswordKey.load((EStructArray) elem);
                    if (System.currentTimeMillis() < pk.deadline()) {
                        _keyToPasswordKey.put(pk.authKey(), pk);
                    }
                });
                for (int attKey : attKeys) {
                    tmpPasswdKeysNode.removeAt(attKey);
                }
                this._locked = true;
                _graph.save(done);
            }, "secGrps");
        });

    }

    @Override
    public void save(Callback<Boolean> done) {
        _graph.index(-1, System.currentTimeMillis(), _acIndexName, acIndex -> {
            acIndex.findFrom(passwdKeyNodes -> {
                Node passwdKeysNode;
                if (passwdKeyNodes == null || passwdKeyNodes.length == 0) {
                    passwdKeysNode = _graph.newNode(acIndex.world(), acIndex.time());
                    passwdKeysNode.set("name", Type.STRING, "passwdKeys");
                    acIndex.update(passwdKeysNode);
                } else {
                    passwdKeysNode = passwdKeyNodes[0];
                }
                int i = 0;
                for (PasswordKey pk : _keyToPasswordKey.values()) {
                    EStructArray passwordKeyContainer = (EStructArray) passwdKeysNode.getOrCreateAt(i++, Type.ESTRUCT_ARRAY);
                    pk.save(passwordKeyContainer);
                }
                _graph.save(done);
            }, "passwdKeys");
        });
    }

    @Override
    public void loadInitialData(Callback<Boolean> done) {
        _graph.declareIndex(0, _usersIndexName, newUsersIndex -> {
            newUsersIndex.setGroup(2);

            Node admin = _graph.newNode(0, System.currentTimeMillis());
            admin.setGroup(3);
            admin.set(_loginAttribute, Type.STRING, "admin")
                    .set(_passwordAttribute, Type.STRING, "7c9619638d47730bd9c1509e0d553640b762d90dd3227bb7e6a5fc96bb274acb");
            newUsersIndex.update(admin);

            this._locked = true;
            done.on(true);
        }, _loginAttribute);
    }

}
