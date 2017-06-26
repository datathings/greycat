/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket.sec;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;

import java.io.IOException;
import java.util.Deque;

import static io.undertow.util.Methods.OPTIONS;

/**
 * Created by Gregory NAIN on 23/05/2017.
 */
public class GCAuthHandler implements HttpHandler {

    private GCIdentityManager identityManager;

    public GCAuthHandler(GCIdentityManager identityManager) {
        this.identityManager = identityManager;
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

                            Deque<FormData.FormValue> loginDeque = data.get("login");
                            Deque<FormData.FormValue> passDeque = data.get("pass");

                            if (loginDeque.size() == 1 && passDeque.size() == 1) {
                                String login = loginDeque.getFirst().getValue();
                                String pass = passDeque.getFirst().getValue();

                                GCAuthHandler.this.identityManager.verifyCredentials(login, pass, account -> {
                                    if (account != null) {
                                        httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Origin"), "*");
                                        httpServerExchange.setStatusCode(StatusCodes.OK);
                                        httpServerExchange.getResponseSender().send(account.getUUID() + "#" + account.getPrincipal().getUserId());

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
