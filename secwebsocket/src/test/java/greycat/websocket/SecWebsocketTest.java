/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket;

import greycat.*;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

                SecWSServer graphServer = new SecWSServer(graph, 8050, "Users", "email", "password");
                graphServer.addHandler("hello", new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
                        httpServerExchange.getResponseSender().send("hello");
                    }
                });
                graphServer.start();
                System.out.println("Server Connected!");



                Graph clientConnection = GraphBuilder.newBuilder().withStorage(new SecWSClient("ws://localhost:8050/ws", "[KEY]")).build();
                clientConnection.connect(connected->{
                    System.out.println("Client connected:" + connected);
                });


            }
        });

    }




}
