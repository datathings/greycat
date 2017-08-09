/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycatTest.ac;

import greycat.Graph;
import greycat.GraphBuilder;
import greycat.ac.Group;
import greycat.ac.Permission;
import greycat.ac.PermissionsManager;
import greycat.ac.Session;
import greycat.ac.groups.BaseGroupsManager;
import greycat.ac.permissions.BasePermissionsManager;
import greycat.ac.sessions.BaseSession;
import greycat.ac.sessions.BaseSessionsManager;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */
public class LoadSaveTests {



    @Test
    public void permissionsTest() {
        MockStorage storage = new MockStorage();
        Graph graph = GraphBuilder.newBuilder().withStorage(storage).build();
        graph.connect(connected -> {
            graph.declareIndex(-1, "idx", nodeIndex -> {

                BasePermissionsManager manager = new BasePermissionsManager(graph, "idx");
                manager.add(1, PermissionsManager.READ_ALLOWED,1);
                Permission p1 = manager.get(1);
                manager.save(saveDone -> {
                    graph.disconnect(disconnected -> {
                        Graph graph2 = GraphBuilder.newBuilder().withStorage(storage).build();
                        graph2.connect(connected2 -> {
                            BasePermissionsManager manager2 = new BasePermissionsManager(graph, "idx");
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

                BaseGroupsManager manager = new BaseGroupsManager(graph, "idx");
                Group gp1 = manager.add(null, "test");
                manager.save(saveDone -> {
                    graph.disconnect(disconnected -> {
                        Graph graph2 = GraphBuilder.newBuilder().withStorage(storage).build();
                        graph2.connect(connected2 -> {
                            BaseGroupsManager manager2 = new BaseGroupsManager(graph, "idx");
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
