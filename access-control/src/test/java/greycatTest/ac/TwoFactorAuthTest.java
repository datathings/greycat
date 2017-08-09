/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycatTest.ac;

import greycat.*;
import greycat.ac.AccessControlManager;
import greycat.ac.BaseAccessControlManager;
import greycat.ac.Group;
import greycat.ac.PermissionsManager;
import greycat.ac.auth.OtpEngine;
import greycat.ac.storage.BaseStorageAccessController;
import greycat.plugin.Storage;
import greycat.websocket.SecWSServer;
import greycatTest.ac.otp.OtpGenerator;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static greycat.Tasks.newTask;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TwoFactorAuthTest {

    static CountDownLatch latch;
    AccessControlManager acm;
    final SecWSServer[] wsServer = new SecWSServer[1];

    private String adminSecret;

    @Before
    public void startTest() throws InterruptedException {
        latch = new CountDownLatch(1);
        CountDownLatch serverReady = new CountDownLatch(1);

        Storage storage = new JsonStorage("./target/test.json");
        GraphBuilder builder = GraphBuilder.newBuilder().withStorage(storage);
        Graph rootGraph = builder.build();
        acm = new BaseAccessControlManager(rootGraph);
        acm.getAuthenticationManager().setPasswordChangeKeyValidity(5000);
        acm.getAuthenticationManager().activateTwoFactorsAuth("MeIssuer", true);
        System.out.println("ACM Start");
        acm.start(acmReady -> {

            acm.getAuthenticationManager().resetTwoFactorSecret(3, newSecret -> {
                adminSecret = newSecret;

                assertTrue("ACM not ready !", acmReady);
                //acm.printCurrentConfiguration();
                builder.withStorage(new BaseStorageAccessController(storage, acm));
                wsServer[0] = new SecWSServer(builder, 7071, acm);
                wsServer[0].start();
                try {
                    Thread.sleep(200);
                    serverReady.countDown();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

            });
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

    @Test
    public void _0otpConnectionTest() {

        String pin = OtpGenerator.computePin(adminSecret, System.currentTimeMillis());
        TestsUtils.authenticateAndConnect("admin", "7c9619638d47730bd9c1509e0d553640b762d90dd3227bb7e6a5fc96bb274acb", pin, graph -> {
            latch.countDown();
            if (graph == null) {
                fail("could not connect");
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void _1noOtpConnectionTest() {

        String pin = OtpGenerator.computePin(adminSecret, System.currentTimeMillis());
        TestsUtils.authenticateAndConnect("admin", "7c9619638d47730bd9c1509e0d553640b762d90dd3227bb7e6a5fc96bb274acb", graph -> {
            latch.countDown();
            if (graph != null) {
                fail("Should not connect");
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void _2otpChangeTest() {
        acm.getAuthenticationManager().resetTwoFactorSecret(3, newSecret -> {
            adminSecret = newSecret;
            latch.countDown();
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        latch = new CountDownLatch(1);

        String pin = OtpGenerator.computePin(adminSecret, System.currentTimeMillis());
        TestsUtils.authenticateAndConnect("admin", "7c9619638d47730bd9c1509e0d553640b762d90dd3227bb7e6a5fc96bb274acb", pin, graph -> {
            latch.countDown();
            if (graph == null) {
                fail("could not connect");
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void _3otpRevokeTest() {
        acm.getAuthenticationManager().revokeTwoFactorSecret(3, newSecret -> {
            latch.countDown();
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        latch = new CountDownLatch(1);

        String pin = OtpGenerator.computePin(adminSecret, System.currentTimeMillis());
        TestsUtils.authenticateAndConnect("admin", "7c9619638d47730bd9c1509e0d553640b762d90dd3227bb7e6a5fc96bb274acb", pin, graph -> {
            latch.countDown();
            if (graph != null) {
                fail("Should not connect");
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
