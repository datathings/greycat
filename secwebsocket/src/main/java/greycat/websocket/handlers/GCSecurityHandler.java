/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket.handlers;

import greycat.auth.IdentityManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;

import java.util.Deque;

/**
 * Created by Gregory NAIN on 23/05/2017.
 */
public class GCSecurityHandler implements HttpHandler {

    private static final String AUTH_PARAM_KEY = "gc-auth-key";

    private HttpHandler _nextHandler;
    private IdentityManager identityManager;

    public GCSecurityHandler(HttpHandler secured, IdentityManager identityManager) {
        this._nextHandler = secured;
        this.identityManager = identityManager;
    }


    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        Deque<String> tokens = httpServerExchange.getQueryParameters().get(AUTH_PARAM_KEY);
        if (tokens != null && tokens.size() == 1) {
            String sessionId = tokens.getFirst();
            this.identityManager.verifySession(sessionId, account -> {
                if (account != null) {
                    try {
                        this._nextHandler.handleRequest(httpServerExchange);
                        this.identityManager.onChannelConnected(sessionId, account);
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
