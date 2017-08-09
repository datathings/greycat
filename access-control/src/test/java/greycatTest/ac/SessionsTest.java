/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycatTest.ac;

import greycat.Graph;
import greycat.GraphBuilder;
import greycat.ac.Session;
import greycat.ac.sessions.BaseSessionsManager;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Created by Gregory NAIN on 09/08/2017.
 */
public class SessionsTest {

    @Test
    public void baseLoadSaveTest() {
        MockStorage storage = new MockStorage();
        Graph graph = GraphBuilder.newBuilder().withStorage(storage).build();
        graph.connect(connected -> {
            graph.declareIndex(-1, "idx", nodeIndex -> {
                BaseSessionsManager manager = new BaseSessionsManager(graph, "idx");
                Session s = manager.getOrCreateSession(1);
                manager.save(saveDone -> {
                    graph.disconnect(disconnected -> {
                        Graph graph2 = GraphBuilder.newBuilder().withStorage(storage).build();
                        graph2.connect(connected2 -> {
                            BaseSessionsManager manager2 = new BaseSessionsManager(graph, "idx");
                            manager2.load(loadDone -> {
                                Session loaded = manager2.getSession(s.sessionId());
                                if(loaded == null) {
                                    fail("Session not loaded");
                                }
                                Session loaded2 = manager2.getOrCreateSession(1);
                                if(loaded2 != loaded) {
                                    fail("Session not loaded");
                                }
                            });
                        });
                    });
                });
            }, "name");
        });
    }

    @Test
    public void notSaveExpiredTest() {
        MockStorage storage = new MockStorage();
        Graph graph = GraphBuilder.newBuilder().withStorage(storage).build();
        graph.connect(connected -> {
            graph.declareIndex(-1, "idx", nodeIndex -> {
                BaseSessionsManager manager = new BaseSessionsManager(graph, "idx");
                manager.setInactivityDelay(300);
                Session s = manager.getOrCreateSession(1);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                manager.save(saveDone -> {
                    graph.disconnect(disconnected -> {
                        Graph graph2 = GraphBuilder.newBuilder().withStorage(storage).build();
                        graph2.connect(connected2 -> {
                            BaseSessionsManager manager2 = new BaseSessionsManager(graph, "idx");
                            manager2.load(loadDone -> {
                                Session loaded = manager2.getSession(s.sessionId());
                                if(loaded != null) {
                                    fail("Session loaded");
                                }
                            });
                        });
                    });
                });
            }, "name");
        });
    }

    @Test
    public void notLoadExpiredTest() {
        MockStorage storage = new MockStorage();
        Graph graph = GraphBuilder.newBuilder().withStorage(storage).build();
        graph.connect(connected -> {
            graph.declareIndex(-1, "idx", nodeIndex -> {
                BaseSessionsManager manager = new BaseSessionsManager(graph, "idx");
                manager.setInactivityDelay(1000);
                Session s = manager.getOrCreateSession(1);
                manager.save(saveDone -> {
                    graph.disconnect(disconnected -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Graph graph2 = GraphBuilder.newBuilder().withStorage(storage).build();
                        graph2.connect(connected2 -> {
                            BaseSessionsManager manager2 = new BaseSessionsManager(graph, "idx");
                            manager2.load(loadDone -> {
                                Session loaded = manager2.getSession(s.sessionId());
                                if(loaded != null) {
                                    fail("Session loaded");
                                }
                            });
                        });
                    });
                });
            }, "name");
        });
    }

}
