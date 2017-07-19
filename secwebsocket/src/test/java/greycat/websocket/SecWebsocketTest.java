/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket;

import greycat.*;
import greycat.auth.login.LoginManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;


/**
 * Created by Gregory NAIN on 23/05/2017.
 */
public class SecWebsocketTest {

    public static void main(String[] args) {

        final Graph graph = new GraphBuilder()
                .withMemorySize(10000)
                .build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean connectResult) {

                LoginManager loginManager = new LoginManager(graph, "Users", "email", "password", 5*60*1000);

                SecWSServer graphServer = new SecWSServer(graph, 8050, loginManager);
                graphServer.addHandler("hello", new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
                        httpServerExchange.getResponseSender().send("hello");
                    }
                });
                graphServer.start();
                System.out.println("Server Connected!");



                Graph clientConnection = GraphBuilder.newBuilder().withStorage(new SecWSClient("ws://localhost:8050", "[KEY]")).build();
                clientConnection.connect(connected->{
                    System.out.println("Client connected:" + connected);
                });


            }
        });

    }




}
