/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket.sec;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;

import java.util.Deque;

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
        Deque<String> tokens = httpServerExchange.getQueryParameters().get("gc-auth-key");

        if (tokens != null && tokens.size() == 1) {
            this.identityManager.verifySession(tokens.getFirst(), account -> {
                if (account != null) {
                    try {
                        this._nextHandler.handleRequest(httpServerExchange);
                    } catch (Exception e) {
                        e.printStackTrace();
                        httpServerExchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                        httpServerExchange.endExchange();
                    }
                } else {
                    httpServerExchange.setStatusCode(StatusCodes.UNAUTHORIZED);
                    httpServerExchange.endExchange();
                }
            });
        } else {
            httpServerExchange.setStatusCode(StatusCodes.UNAUTHORIZED);
            httpServerExchange.endExchange();
        }
    }
}
