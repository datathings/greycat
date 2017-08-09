/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycatTest.ac;

import greycat.*;
import greycat.ac.AccessControlManager;
import greycat.ac.BaseAccessControlManager;
import greycat.ac.storage.BaseStorageAccessController;
import greycat.plugin.Storage;
import greycat.websocket.SecWSClient;
import greycat.websocket.SecWSServer;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */
public class EndToEndLoginTest {


    public static void main(String[] args) {
        final SecWSServer[] wsServer = new SecWSServer[1];
        Storage storage = new MockStorage();

        GraphBuilder builder = GraphBuilder.newBuilder().withStorage(storage);
        AccessControlManager acm = new BaseAccessControlManager(builder.build());
        //acm.getSessionsManager().setInactivityDelay(10 * 1000);
        acm.start(acmReady -> {

            //acm.printCurrentConfiguration();
            System.out.println("ACM ready !");

            builder.withStorage(new BaseStorageAccessController(storage, acm));
            wsServer[0] = new SecWSServer(builder, 7071, acm);
            wsServer[0].start();

            Thread clientThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Getting Token");
                    getAuthToken(token -> {
                        if (token.length() > 0) {
                            System.out.println("Token:" + token);
                            connectClient(token);
                        } else {
                            System.err.println("Received no token");
                        }
                    });
                }
            });
            clientThread.setUncaughtExceptionHandler((t, e) -> {
                e.printStackTrace();
            });
            clientThread.start();

        });

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (wsServer[0] != null) {
                    wsServer[0].stop();
                }
                acm.shutdown();
            }
        }));
    }


    private static void connectClient(String token) {

        Graph graph = GraphBuilder.newBuilder()
                .withStorage(new SecWSClient("ws://localhost:7071/ws", token.split("#")[0])).build();

        graph.connect(connected -> {
            System.out.println("Client connected");

            launchPermissionsCheck(graph, done -> {
                graph.disconnect(disconnected -> {
                    System.out.println("Client disconnected:" + disconnected);
                    System.exit(0);
                });
            });

        });


    }

    private static void launchPermissionsCheck(Graph graph, Callback<Boolean> done) {
        System.out.println("=====  Permission checks ======");
        DeferCounter counter = graph.newCounter(100);
        for (int i = 0; i < 50; i++) {
            int finalI = i;
            graph.lookup(-1, System.currentTimeMillis(), i, node -> {
                if (node != null) {
                    System.out.println("READ\t" + finalI + "\t" + (node != null ? "OK\t" + node : "FAIL"));
                }
                counter.count();
            });
        }
        for (int i = 0; i < 50; i++) {
            int finalI = i;
            graph.lookup(0, System.currentTimeMillis(), i, node -> {
                if (node != null) {
                    System.out.println("READ\t" + finalI + "\t" + (node != null ? "OK\t" + node : "FAIL"));
                }
                counter.count();
            });
        }
        counter.then(() -> {
            done.on(true);
        });
    }


    private static void getAuthToken(Callback<String> token) {
        try {
            URL url = new URL("http://localhost:7071/auth");

            Map<String, Object> params = new HashMap<>();
            params.put("login", "admin");
            params.put("pass", "7c9619638d47730bd9c1509e0d553640b762d90dd3227bb7e6a5fc96bb274acb");

            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, Object> param : params.entrySet()) {
                if (postData.length() != 0) postData.append('&');
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }
            String urlParameters = postData.toString();
            URLConnection conn = url.openConnection();
            //conn.setConnectTimeout(1000);
            //conn.setReadTimeout(4000);

            conn.setDoOutput(true);

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(urlParameters);
            writer.flush();

            String result = "";
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            while ((line = reader.readLine()) != null) {
                result += line;
            }
            writer.close();
            reader.close();
            token.on(result);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
