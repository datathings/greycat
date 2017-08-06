package greycatTest.ac;

import greycat.*;
import greycat.ac.AccessControlManager;
import greycat.ac.BaseAccessControlManager;
import greycat.ac.Group;
import greycat.ac.PermissionsManager;
import greycat.ac.storage.BaseStorageAccessController;
import greycat.plugin.Storage;
import greycat.websocket.SecWSClient;
import greycat.websocket.SecWSServer;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import static greycat.Tasks.newTask;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */
public class PasswordChangeTest {


    public static void main(String[] args) {
        final SecWSServer[] wsServer = new SecWSServer[1];
        //Storage storage = new JsonStorage("testStorage.json");
        Storage storage = new MockStorage();
        GraphBuilder builder = GraphBuilder.newBuilder().withStorage(storage);
        Graph rootGraph = builder.build();
        AccessControlManager acm = new BaseAccessControlManager(rootGraph);
        acm.getAuthenticationManager().setPasswordChangeKeyValidity(3000);
        acm.start(acmReady -> {


            System.out.println("ACM ready !");

            //create user
            rootGraph.index(0, System.currentTimeMillis(), "Users", index -> {
                Node user = rootGraph.newNode(0, System.currentTimeMillis());
                user.set("login", Type.STRING, "test");
                Group secGroup = acm.getSecurityGroupsManager().add(acm.getSecurityGroupsManager().get(2), "TestUserSecurityGroup");
                user.setGroup(secGroup.gid());
                index.update(user);
                System.out.println("User created: " + user);

                acm.getPermissionsManager().add(user.id(), PermissionsManager.READ_ALLOWED, secGroup.gid());
                acm.getPermissionsManager().add(user.id(), PermissionsManager.WRITE_ALLOWED, secGroup.gid());

                rootGraph.save(rootGraphSaved -> {

                    acm.printCurrentConfiguration();

                    builder.withStorage(new BaseStorageAccessController(storage, acm));
                    wsServer[0] = new SecWSServer(builder, 7071, acm);
                    wsServer[0].start();

                    Thread clientThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("Getting Token");
                            getAuthToken("admin", token -> {
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
            });
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
            System.out.println("Client connected with ADMIN");

            newTask().travelInTime("" + System.currentTimeMillis()).lookup("4")
                    .log("User:{{result}}").action("resetPassword")
                    .executeRemotely(graph, result -> {

                        if (result.exception() != null) {
                            result.exception().printStackTrace();
                        }

                        String renewPasswordToken = (String) result.get(0);
                        System.out.println("Renew Password token:" + renewPasswordToken);

                        graph.disconnect(disconnected -> {
                            System.out.println("Admin disconnected:" + disconnected);

                            changePasswordForTest(renewPasswordToken, passwordRenewResult -> {
                                System.out.println("Password renew result: " + passwordRenewResult);
                                tryTestConnection();
                            });
                        });
                    });
        });
    }

    private static void tryTestConnection() {
        getAuthToken("test", token -> {
            Graph graph = GraphBuilder.newBuilder()
                    .withStorage(new SecWSClient("ws://localhost:7071/ws", token.split("#")[0])).build();
            graph.connect(connected -> {
                System.out.println("Client connected with TEST");
                graph.disconnect(disconnected -> {
                    System.out.println("Test disconnected:" + disconnected);
                });
            });
        });
    }


    private static void getAuthToken(String user, Callback<String> token) {
        try {
            URL url = new URL("http://localhost:7071/auth");

            Map<String, Object> params = new HashMap<>();
            params.put("login", user);
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

    private static void changePasswordForTest(String key, Callback<String> done) {
        try {
            URL url = new URL("http://localhost:7071/renewpasswd");

            Map<String, Object> params = new HashMap<>();
            params.put("uuid", key);
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
            done.on(result);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
