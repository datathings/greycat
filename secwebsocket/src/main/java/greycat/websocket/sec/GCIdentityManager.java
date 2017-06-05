/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket.sec;

import greycat.*;
import greycat.websocket.sec.GCPrincipal;
import greycat.websocket.sec.GCSecAccount;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Created by Gregory NAIN on 23/05/2017.
 */
public class GCIdentityManager {

    protected Graph graph;
    protected String usersIndex;
    protected String loginAttribute;
    protected String passAttribute;

    protected List<GCSecAccount> activeAccounts = new ArrayList<>();

    public GCIdentityManager(Graph graph, String usersIndex, String loginAttribute, String passAttribute) {
        this.graph = graph;
        this.usersIndex = usersIndex;
        this.loginAttribute = loginAttribute;
        this.passAttribute = passAttribute;
    }


    public void verifyCredentials(String login, String pass, Callback<GCSecAccount> callback) {
        graph.index(0, Constants.BEGINNING_OF_TIME, usersIndex, indexNode -> {
            indexNode.find(users -> {

                if (users.length <= 1) {

                    GCSecAccount active = null;
                    for (GCSecAccount acc : new ArrayList<>(activeAccounts)) {
                        if (acc.isExpired()) {
                            activeAccounts.remove(acc);
                        } else if (acc.getPrincipal().getUser().get(loginAttribute).equals(login)) {
                            active = acc;
                            break;
                        }
                    }

                    if (users.length == 1) {
                        System.out.println("Password check: " + users[0].get(passAttribute) + " === " + pass);
                        boolean passCheck = users[0].get(passAttribute).equals(pass);

                        if (passCheck) {
                            if (active == null) {
                                active = new GCSecAccount(new GCPrincipal(users[0]));
                                activeAccounts.add(active);
                            } else {
                                active.hit();
                            }
                        } else {
                            if (active != null) {
                                activeAccounts.remove(active);
                                active = null;
                            }
                        }
                    } else if (users.length == 0) {
                        if (active != null) {
                            activeAccounts.remove(active);
                            active = null;
                        }
                    }
                    callback.on(active);

                } else {
                    throw new RuntimeException("multiple users indexed with the same ID !");
                }
            }, loginAttribute, login);
        });
    }

    public void verifySession(String uuid, Callback<GCSecAccount> callback) {
        GCSecAccount active = null;
        for (GCSecAccount acc : new ArrayList<>(activeAccounts)) {
            if (acc.isExpired()) {
                activeAccounts.remove(acc);
            } else if (acc.checkSameUUID(uuid)) {
                active = acc;
                break;
            }
        }
        callback.on(active);
    }

    public String createPasswordChangeUUID(Node user) {
        UUID tmpUUID = UUID.randomUUID();
        user.set("password_reset_uuid", Type.STRING, tmpUUID.toString());
        user.set("password_reset_timeout", Type.LONG, System.currentTimeMillis() + 5 * 60 * 1000);
        return tmpUUID.toString();
    }

    public void resetPassword(String uuid, String pass, Callback<Integer> callback) {
        graph.index(0, Constants.BEGINNING_OF_TIME, usersIndex, indexNode -> {
            indexNode.find(users -> {
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
