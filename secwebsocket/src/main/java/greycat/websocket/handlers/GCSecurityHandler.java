/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket.handlers;

import greycat.AccessControlManager;
import greycat.Session;
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
    private AccessControlManager _acm;

    public GCSecurityHandler(HttpHandler secured, AccessControlManager acm) {
        this._nextHandler = secured;
        this._acm = acm;
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        Deque<String> tokens = httpServerExchange.getQueryParameters().get(AUTH_PARAM_KEY);
        if (tokens != null && tokens.size() == 1) {
            String sessionId = tokens.getFirst();
            Session session = this._acm.getSessionsManager().getSession(sessionId);
            if (session != null) {
                if (!session.isExpired() && session.setLastHit(System.currentTimeMillis())) {
                    try {
                        this._nextHandler.handleRequest(httpServerExchange);
                    } catch (Exception e) {
                        e.printStackTrace();
                        httpServerExchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                        httpServerExchange.endExchange();
                    }
                } else {
                    this._acm.getSessionsManager().clearSession(sessionId);
                    httpServerExchange.setStatusCode(StatusCodes.UNAUTHORIZED);
                    httpServerExchange.endExchange();
                }
            } else {
                httpServerExchange.setStatusCode(StatusCodes.UNAUTHORIZED);
                httpServerExchange.endExchange();
            }
        } else {
            httpServerExchange.setStatusCode(StatusCodes.UNAUTHORIZED);
            httpServerExchange.endExchange();
        }
    }
}
