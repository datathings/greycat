/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket.handlers;

import greycat.ac.Session;
import greycat.ac.AccessControlManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.undertow.util.Methods.OPTIONS;

/**
 * Created by Gregory NAIN on 23/05/2017.
 */
public class GCAuthHandler implements HttpHandler {

    private AccessControlManager _acm;

    public GCAuthHandler(AccessControlManager acm) {
        this._acm = acm;
    }


    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        if (httpServerExchange.getRequestMethod() == OPTIONS) {
            httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Headers"), "Access-Control-Allow-Origin");
            httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Origin"), "*");
            httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Methods"), "POST");
            httpServerExchange.setStatusCode(StatusCodes.OK);
            httpServerExchange.endExchange();
        } else {

            final FormDataParser parser = FormParserFactory.builder().build().createParser(httpServerExchange);
            if (parser != null) {
                httpServerExchange.dispatch(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            httpServerExchange.startBlocking();
                            FormData data = parser.parseBlocking();

                            Map<String, String> credentials = new HashMap<>();

                            data.forEach(s -> {
                                credentials.put(s, data.getFirst(s).getValue());
                            });

                            if (credentials.size() >= 2) {

                                GCAuthHandler.this._acm.getAuthenticationManager().verifyCredentials(credentials, uid -> {
                                    if (uid != null) {
                                        Session session = GCAuthHandler.this._acm.getSessionsManager().getOrCreateSession(uid);
                                        httpServerExchange.setStatusCode(StatusCodes.OK);
                                        httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Origin"), "*");
                                        httpServerExchange.getResponseSender().send(session.sessionId() + "#" + session.uid());
                                    } else {
                                        httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Origin"), "*");
                                        httpServerExchange.setStatusCode(StatusCodes.UNAUTHORIZED);
                                        httpServerExchange.endExchange();
                                    }
                                });
                            } else {
                                httpServerExchange.setStatusCode(StatusCodes.BAD_REQUEST);
                                httpServerExchange.getResponseSender().send("Authentication cannot be proceeded. Not enough parameters");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            httpServerExchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                            httpServerExchange.getResponseSender().send(e.toString());
                        }
                    }
                });
            } else {
                httpServerExchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                httpServerExchange.getResponseSender().send("Form parser returned null !\n" + httpServerExchange.getQueryString());
            }
        }
    }
}
