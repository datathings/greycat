package greycatTest.ac;

import com.sun.tools.doclets.formats.html.SourceToHTMLConverter;
import greycat.*;
import greycat.ac.AccessControlManager;
import greycat.ac.BaseAccessControlManager;
import greycat.ac.storage.BaseStorageAccessController;
import greycat.plugin.Storage;
import greycat.websocket.SecWSClient;
import greycat.websocket.SecWSServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */
public class SessionExpiryTest {

    static CountDownLatch latch;
    AccessControlManager acm;
    final SecWSServer[] wsServer = new SecWSServer[1];

    @Before
    public void startTest() {
        latch = new CountDownLatch(1);

        Storage storage = new MockStorage();
        GraphBuilder builder = GraphBuilder.newBuilder().withStorage(storage);
        Graph rootGraph = builder.build();
        acm = new BaseAccessControlManager(rootGraph);
        acm.getSessionsManager().setInactivityDelay(200);
        acm.start(acmReady -> {
            assertTrue("ACM not ready !", acmReady);

            builder.withStorage(new BaseStorageAccessController(storage, acm));
            wsServer[0] = new SecWSServer(builder, 7071, acm);
            wsServer[0].start();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        });
    }


    @After
    public void stopTest() {
        if (wsServer[0] != null) {
            wsServer[0].stop();
        }
        if (acm != null) {
            acm.shutdown();
        }
    }


    @Test
    public void sessionExpiryTest() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        TestsUtils.authenticateAndConnect("admin", "7c9619638d47730bd9c1509e0d553640b762d90dd3227bb7e6a5fc96bb274acb", graph -> {
            graph.connect(connected -> {
                graph.lookup(-1, System.currentTimeMillis(), 1, node -> {
                    assertNotNull(node);
                    executor.schedule(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                graph.lookup(0, System.currentTimeMillis(), 2, node2 -> {
                                    latch.countDown();
                                    fail();
                                });
                            } catch (Exception e) {
                                latch.countDown();
                                return;
                            }
                            fail();
                        }
                    }, 2, TimeUnit.SECONDS);
                });
            });
        });
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
