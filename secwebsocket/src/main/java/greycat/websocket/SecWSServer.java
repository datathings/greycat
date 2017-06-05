/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket;

import greycat.Graph;
import greycat.websocket.sec.GCAuthHandler;
import greycat.websocket.sec.GCIdentityManager;
import greycat.websocket.sec.GCSecurityHandler;

/**
 * Created by Gregory NAIN on 23/05/2017.
 */
public class SecWSServer extends WSServer {


    private GCIdentityManager identityManager;

    public SecWSServer(Graph p_graph, int p_port, String usersIndex, String loginAttribute, String passAttribute) {
        super(p_graph, p_port);
        this.identityManager = new GCIdentityManager(p_graph, usersIndex, loginAttribute, passAttribute);
    }


    @Override
    public void start() {
        handlers.replaceAll((s, httpHandler) -> new GCSecurityHandler(httpHandler, this.identityManager));
        handlers.put("/auth", new GCAuthHandler(this.identityManager));
        super.start();
    }


}
