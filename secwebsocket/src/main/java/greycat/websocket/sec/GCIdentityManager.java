/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket.sec;

import greycat.Callback;
import greycat.Constants;
import greycat.Graph;
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
        graph.index(0, Constants.BEGINNING_OF_TIME, usersIndex, indexNode->{
            indexNode.find(users->{

                if(users.length <= 1) {

                    GCSecAccount active = null;
                    for(GCSecAccount acc : new ArrayList<>(activeAccounts)) {
                        if(acc.isExpired()) {
                            activeAccounts.remove(acc);
                        } else if(acc.getPrincipal().getEmail().equals(login)) {
                            active = acc;
                            break;
                        }
                    }

                    if(users.length == 1) {
                        System.out.println("Password check: " + users[0].get(passAttribute) + " === " + pass);
                        boolean passCheck = users[0].get(passAttribute).equals(pass);

                        if(passCheck) {
                            if(active == null){
                                active = new GCSecAccount(new GCPrincipal((String)users[0].get("firstName"),(String)users[0].get("lastName"),(String)users[0].get("email")));
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
            }, loginAttribute, login);
        });
    }

    public void verifySession(String uuid, Callback<GCSecAccount> callback) {
        GCSecAccount active = null;
        for(GCSecAccount acc : new ArrayList<>(activeAccounts)) {
            if(acc.isExpired()) {
                activeAccounts.remove(acc);
            } else if(acc.checkSameUUID(uuid)) {
                active = acc;
                break;
            }
        }
        callback.on(active);
    }

}
