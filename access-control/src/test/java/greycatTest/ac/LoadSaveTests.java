package greycatTest.ac;

import greycat.Graph;
import greycat.GraphBuilder;
import greycat.ac.groups.BaseGroup;
import greycat.ac.groups.BaseGroupsManager;
import greycat.ac.permissions.BasePermission;
import greycat.ac.permissions.BasePermissionsManager;
import greycat.ac.sessions.BaseSession;
import greycat.ac.sessions.BaseSessionsManager;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */
public class LoadSaveTests {

    @Test
    public void sessionsTest() {
        MockStorage storage = new MockStorage();
        Graph graph = GraphBuilder.newBuilder().withStorage(storage).build();
        graph.connect(connected -> {

            graph.declareIndex(-1, "idx", nodeIndex -> {

                BaseSessionsManager manager = new BaseSessionsManager(graph, "idx");
                BaseSession s = manager.getOrCreateSession(1);
                manager.save(saveDone -> {
                    graph.disconnect(disconnected -> {
                        Graph graph2 = GraphBuilder.newBuilder().withStorage(storage).build();
                        graph2.connect(connected2 -> {
                            BaseSessionsManager manager2 = new BaseSessionsManager(graph, "idx");
                            manager2.load(loadDone -> {
                                BaseSession loaded = manager2.getOrCreateSession(1);
                                assertEquals(s.sessionId(), loaded.sessionId());
                            });
                        });
                    });
                });

            }, "name");
        });
    }

    @Test
    public void permissionsTest() {
        MockStorage storage = new MockStorage();
        Graph graph = GraphBuilder.newBuilder().withStorage(storage).build();
        graph.connect(connected -> {
            graph.declareIndex(-1, "idx", nodeIndex -> {

                BasePermissionsManager manager = new BasePermissionsManager(graph, "idx");
                manager.add(1, BasePermissionsManager.READ_ALLOWED,1);
                BasePermission p1 = manager.get(1);
                manager.save(saveDone -> {
                    graph.disconnect(disconnected -> {
                        Graph graph2 = GraphBuilder.newBuilder().withStorage(storage).build();
                        graph2.connect(connected2 -> {
                            BasePermissionsManager manager2 = new BasePermissionsManager(graph, "idx");
                            manager2.load(loadDone -> {
                                BasePermission p2 = manager2.get(1);
                                assertArrayEquals(p1.read(), p2.read());
                            });
                        });
                    });
                });

            }, "name");
        });
    }

    @Test
    public void groupsTest() {
        MockStorage storage = new MockStorage();
        Graph graph = GraphBuilder.newBuilder().withStorage(storage).build();
        graph.connect(connected -> {
            graph.declareIndex(-1, "idx", nodeIndex -> {

                BaseGroupsManager manager = new BaseGroupsManager(graph, "idx");
                BaseGroup gp1 = manager.add(null, "test");
                manager.save(saveDone -> {
                    graph.disconnect(disconnected -> {
                        Graph graph2 = GraphBuilder.newBuilder().withStorage(storage).build();
                        graph2.connect(connected2 -> {
                            BaseGroupsManager manager2 = new BaseGroupsManager(graph, "idx");
                            manager2.load(loadDone -> {
                                BaseGroup gp2 = manager2.get(gp1.gid());
                                assertNotNull(gp2);
                                assertArrayEquals(gp1.path(), gp2.path());
                            });
                        });
                    });
                });

            }, "name");
        });
    }
}
