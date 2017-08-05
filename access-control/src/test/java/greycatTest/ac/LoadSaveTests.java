package greycatTest.ac;

import greycat.Graph;
import greycat.GraphBuilder;
import greycat.ac.groups.Group;
import greycat.ac.groups.GroupsManager;
import greycat.ac.permissions.Permission;
import greycat.ac.permissions.PermissionsManager;
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

                PermissionsManager manager = new PermissionsManager(graph, "idx");
                manager.add(1,PermissionsManager.READ_ALLOWED,1);
                Permission p1 = manager.get(1);
                manager.save(saveDone -> {
                    graph.disconnect(disconnected -> {
                        Graph graph2 = GraphBuilder.newBuilder().withStorage(storage).build();
                        graph2.connect(connected2 -> {
                            PermissionsManager manager2 = new PermissionsManager(graph, "idx");
                            manager2.load(loadDone -> {
                                Permission p2 = manager2.get(1);
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

                GroupsManager manager = new GroupsManager(graph, "idx");
                Group gp1 = manager.add(null, "test");
                manager.save(saveDone -> {
                    graph.disconnect(disconnected -> {
                        Graph graph2 = GraphBuilder.newBuilder().withStorage(storage).build();
                        graph2.connect(connected2 -> {
                            GroupsManager manager2 = new GroupsManager(graph, "idx");
                            manager2.load(loadDone -> {
                                Group gp2 = manager2.get(gp1.gid());
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
