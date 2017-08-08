package greycatTest.ac;

import com.sun.tools.doclets.formats.html.SourceToHTMLConverter;
import greycat.*;
import greycat.ac.AccessControlManager;
import greycat.ac.BaseAccessControlManager;
import greycat.ac.Group;
import greycat.ac.PermissionsManager;
import greycat.ac.storage.BaseStorageAccessController;
import greycat.plugin.Storage;
import greycat.websocket.SecWSClient;
import greycat.websocket.SecWSServer;
import jdk.nashorn.internal.codegen.CompilerConstants;
import junit.framework.TestCase;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static greycat.Tasks.newTask;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */

public class PasswordChangeTest {

    static CountDownLatch latch;
    AccessControlManager acm;
    final SecWSServer[] wsServer = new SecWSServer[1];

    @Before
    public void startTest() throws InterruptedException {
        latch = new CountDownLatch(1);
        CountDownLatch serverReady = new CountDownLatch(1);

        Storage storage = new JsonStorage("./target/test.json");
        GraphBuilder builder = GraphBuilder.newBuilder().withStorage(storage);
        Graph rootGraph = builder.build();
        acm = new BaseAccessControlManager(rootGraph);
        acm.getAuthenticationManager().setPasswordChangeKeyValidity(5000);
        System.out.println("ACM Start");
        acm.start(acmReady -> {
            assertTrue("ACM not ready !", acmReady);
            //acm.printCurrentConfiguration();

            builder.withStorage(new BaseStorageAccessController(storage, acm));
            wsServer[0] = new SecWSServer(builder, 7071, acm);
            wsServer[0].start();

            try {
                Thread.sleep(400);
                serverReady.countDown();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        });
        serverReady.await();
    }


    @After
    public void stopTest() {
        if (wsServer[0] != null) {
            wsServer[0].stop();
        }
        if (acm != null) {
            System.out.println("ACM shutdown");
            acm.shutdown();
        }
    }

    @AfterClass
    public static void cleanBase() {
        try {
            Files.delete(Paths.get("./target/test.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String reloadTestToken;

    @Test
    public void _0passwordChangeTest() {
        TestsUtils.authenticateAndConnect("admin", "7c9619638d47730bd9c1509e0d553640b762d90dd3227bb7e6a5fc96bb274acb", adminGraph -> {
            addTestUser(adminGraph, acm, "Test", testUserAdded -> {
                newTask().travelInTime("" + System.currentTimeMillis()).lookup("274877906945")
                        .pipe(newTask().action("resetPassword"), newTask().action("resetPassword")).flat()
                        .executeRemotely(adminGraph, result -> {
                            if (result.exception() != null) {
                                result.exception().printStackTrace();
                            }
                            String renewPasswordToken = (String) result.get(0);
                            System.out.println("Token 1:" + renewPasswordToken);
                            reloadTestToken = (String) result.get(1);
                            adminGraph.disconnect(disconnected -> {
                                changePasswordForTest(renewPasswordToken, passwordRenewResult -> {
                                    System.out.println(passwordRenewResult);
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    TestsUtils.authenticateAndConnect("Test", "7c9619638d47730bd9c1509e0d553640b762d90dd3227bb7e6a5fc96bb274acb", testGraph -> {
                                        latch.countDown();
                                        assertNotNull("Could not connect client after password renewed.", testGraph);
                                    });
                                });
                            });
                        });
            });
        });
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void _1reloadTest() {

        changePasswordForTest(reloadTestToken, passwordRenewResult -> {
            System.out.println("2nd test res: " + passwordRenewResult);
            TestsUtils.authenticateAndConnect("Test", "7c9619638d47730bd9c1509e0d553640b762d90dd3227bb7e6a5fc96bb274acb", testGraph -> {
                latch.countDown();
                assertNotNull("Could not connect client after password renewed.", testGraph);
            });
        });
        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private static void addTestUser(Graph graph, AccessControlManager acm, String username, Callback<Boolean> callback) {
        //create user
        graph.index(0, System.currentTimeMillis(), "Users", index -> {
            Node user = graph.newNode(0, System.currentTimeMillis());
            user.set("login", Type.STRING, username);
            Group secGroup = acm.getSecurityGroupsManager().add(acm.getSecurityGroupsManager().get(2), "TestUserSecurityGroup");
            user.setGroup(secGroup.gid());
            index.update(user);
            System.out.println(user.id());

            acm.getPermissionsManager().add(user.id(), PermissionsManager.READ_ALLOWED, secGroup.gid());
            acm.getPermissionsManager().add(user.id(), PermissionsManager.WRITE_ALLOWED, secGroup.gid());

            graph.save(callback);
        });
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
