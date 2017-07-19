/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.auth.login;

import greycat.*;
import greycat.auth.actions.ActionResetPassword;
import greycat.auth.GCAccount;
import greycat.auth.IdentityManager;
import jdk.nashorn.internal.codegen.CompilerConstants;

import java.util.*;

/**
 * Created by Gregory NAIN on 23/05/2017.
 */
public class LoginManager implements IdentityManager{

    protected Graph graph;
    protected String usersIndex;
    protected String loginAttribute;
    protected String passAttribute;
    protected long allowedInactivityDelay;

    //private static final long VALIDITY_PERIOD = 30*60*1000; // 5min

    protected List<GCAccount> activeAccounts = new ArrayList<>();
    protected Map<String, GCAccount> activeConnections = new HashMap<>();

    public LoginManager(Graph graph, String usersIndex, String loginAttribute, String passAttribute, long allowedInactivityDelay) {
        this.graph = graph;
        this.usersIndex = usersIndex;
        this.loginAttribute = loginAttribute;
        this.passAttribute = passAttribute;
        this.allowedInactivityDelay = allowedInactivityDelay;

        this.graph.actionRegistry().getOrCreateDeclaration(ActionResetPassword.ACTION_RESET_PASSWORD).setFactory(params -> new ActionResetPassword(this));
    }

    @Override
    public void onChannelConnected(String sessionId, GCAccount account) {
        activeConnections.put(sessionId, account);
        account.hit();
    }

    @Override
    public void onChannelDisconnected(String sessionId) {
        activeConnections.remove(sessionId);
    }

    @Override
    public boolean onChannelActivity(String sessionId) {
        GCAccount account = activeConnections.get(sessionId);
        if(account != null && !account.isExpired()) {
            account.hit();
            return true;
        }
        return false;
    }

    @Override
    public void verifySession(String uuid, Callback<GCAccount> callback) {
        GCAccount active = null;
        for(GCAccount acc : new ArrayList<>(activeAccounts)) {
            if(acc.isExpired()) {
                activeAccounts.remove(acc);
            } else if(acc.checkSameSession(uuid)) {
                active = acc;
                break;
            }
        }
        callback.on(active);
    }

    @Override
    public void verifyCredentials(Map<String, String> credentials, Callback<GCAccount> callback) {

        String login = credentials.get(loginAttribute);
        String pass = credentials.get(passAttribute);

        if(login == null || pass == null || login.trim().equals("")) {
            callback.on(null);
        }

        graph.index(0, Constants.BEGINNING_OF_TIME, usersIndex, indexNode->{
            indexNode.findFrom(users->{

                if(users.length <= 1) {

                    GCAccount active = null;
                    for(GCAccount acc : new ArrayList<>(activeAccounts)) {
                        if(acc.isExpired()) {
                            activeAccounts.remove(acc);
                        } else if(acc.getLogin().equals(login)) {
                            active = acc;
                            break;
                        }
                    }

                    if(users.length == 1) {
                        System.out.println("Password check: " + users[0].get(passAttribute) + " === " + pass);
                        boolean passCheck = users[0].get(passAttribute).equals(pass);

                        if(passCheck) {
                            if(active == null){
                                active = new LoginAccount(login, users[0], allowedInactivityDelay, true);
                                activeAccounts.add(active);
                            } else {
                                active.hit();
                            }
                        } else {
                            if(active != null) {
                                activeAccounts.remove(active);
                                active = null;
                            }
                        }
                    } else if(users.length == 0){
                        if(active != null) {
                            activeAccounts.remove(active);
                            active = null;
                        }
                    }
                    callback.on(active);

                } else {
                    throw new RuntimeException("multiple users indexed with the same ID !");
                }
            }, login);
        });
    }

    public void createPasswordUUID(Node user, Callback<String> callback) {
        UUID tmpUUID = UUID.randomUUID();
        user.set("password_reset_uuid", Type.STRING, tmpUUID.toString());
        user.set("password_reset_timeout", Type.LONG, System.currentTimeMillis() + 5 * 60 * 1000);
        user.graph().save(result -> callback.on(tmpUUID.toString()));
    }

    public void resetPassword(String uuid, String pass, Callback<Integer> callback) {
        graph.index(0, Constants.BEGINNING_OF_TIME, usersIndex, indexNode -> {
            indexNode.findFrom(users -> {
                boolean acted = false;
                for (int i = 0; i < users.length; i++) {
                    Node user = users[i];
                    String userUuid = (String) user.get("password_reset_uuid");
                    if (userUuid != null) {
                        long timeout = (long) user.get("password_reset_timeout");
                        if (System.currentTimeMillis() < timeout) {
                            if(userUuid.equals(uuid)) {
                                user.set(passAttribute, Type.STRING, pass);
                                user.set("password_reset_uuid", Type.STRING, null);
                                user.set("password_reset_timeout", Type.LONG, null);
                                graph.save((ok)->{
                                    callback.on(1);
                                });
                                acted = true;
                                break;
                            }
                        } else {
                            user.set("password_reset_uuid", Type.STRING, null);
                            user.set("password_reset_timeout", Type.LONG, null);
                            if(userUuid.equals(uuid)) {
                                callback.on(-1);
                            }
                        }
                    }
                }
                if (!acted) {
                    callback.on(-2);
                }
            });
        });
    }

}
