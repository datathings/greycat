/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket.sec;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * Created by Gregory NAIN on 23/05/2017.
 */
public class GCSecurityHandler implements HttpHandler {

    private HttpHandler _nextHandler;
    private GCIdentityManager identityManager;

    public GCSecurityHandler(HttpHandler secured, GCIdentityManager identityManager) {
        this._nextHandler = secured;
        this.identityManager = identityManager;
    }


    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        System.out.println("TODO check UUID");
        this._nextHandler.handleRequest(httpServerExchange);
    }
}
