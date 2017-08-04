package greycat.storage.ac;

import greycat.Callback;
import greycat.Graph;
import greycat.Node;
import greycat.Type;

import java.util.concurrent.CountDownLatch;

/**
 * Created by Gregory NAIN on 03/08/2017.
 */
public class AccessControlManager {

    private Graph _graph;
    private String _acIndexName;
    private SecurityGroupsManager _groupsManager;
    private PermissionsManager _permissionsManager;

    public AccessControlManager(Graph graph, String acIndexName) {
        this._graph = graph;
        this._acIndexName = acIndexName;
        this._groupsManager = new SecurityGroupsManager(graph, acIndexName);
        this._permissionsManager = new PermissionsManager(graph, acIndexName);
    }

    public void init(Callback<Boolean> callback) {
        try {
            boolean[] ready = new boolean[2];
            CountDownLatch latch = new CountDownLatch(2);

            _graph.index(-1, System.currentTimeMillis(), _acIndexName, nodeIndex -> {
                if (nodeIndex != null) {
                    this._groupsManager.load(aBoolean -> {
                        ready[0] = aBoolean;
                        latch.countDown();
                    });
                    this._permissionsManager.load(aBoolean -> {
                        ready[1] = aBoolean;
                        latch.countDown();
                    });
                } else {
                    _graph.declareIndex(-1, _acIndexName, newAcmIndex -> {
                        newAcmIndex.setGroup(4);
                        _graph.declareIndex(0, "Users", newUsersIndex -> {
                            newUsersIndex.setGroup(2);
                            Node admin = _graph.newNode(0, System.currentTimeMillis());
                            admin.setGroup(3);
                            admin.set("login", Type.STRING, "admin")
                                    .set("password", Type.STRING, "7c9619638d47730bd9c1509e0d553640b762d90dd3227bb7e6a5fc96bb274acb");
                            newUsersIndex.update(admin);
                            this._groupsManager.loadInitialGroups(aBoolean -> {
                                ready[0] = aBoolean;
                                latch.countDown();
                            });
                            this._permissionsManager.loadInitialPermissions(admin.id(), aBoolean -> {
                                ready[1] = aBoolean;
                                latch.countDown();
                            });
                        }, "login");
                    }, "name");
                }
            });
            latch.await();
            if (ready[0] && ready[1]) {
                _graph.save(callback);
            } else {
                callback.on(false);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            callback.on(false);
        }
    }

    public SecurityGroupsManager getSecurityGroupsManager() {
        return _groupsManager;
    }

    public PermissionsManager getPermissionsManager() {
        return _permissionsManager;
    }

    public void canRead(long uid, int gid, Callback<Boolean> cb) {
        if (gid == 0) {
            cb.on(true); //Public group
            return;
        }

        Permission userPermissions = _permissionsManager.get(uid);
        SecurityGroup resourceGroup = _groupsManager.get(gid);

        //Check if group is in explicit NoRead permissions
        int[] userNotReadGroups = userPermissions.notRead();
        if (match(resourceGroup, userNotReadGroups)) {
            cb.on(false);
        } else {
            //Check if group is in explicit read permissions
            int[] userReadGroups = userPermissions.read();
            cb.on(match(resourceGroup, userReadGroups));
        }
    }

    public void canWrite(long uid, int gid, Callback<Boolean> cb) {
        Permission userPermissions = _permissionsManager.get(uid);
        SecurityGroup resourceGroup = _groupsManager.get(gid);

        //Check if group is in explicit NoRead permissions
        int[] userNotWriteGroups = userPermissions.notWrite();
        if (match(resourceGroup, userNotWriteGroups)) {
            cb.on(false);
        } else {
            //Check if group is in explicit read permissions
            int[] userWriteGroups = userPermissions.write();
            cb.on(match(resourceGroup, userWriteGroups));
        }
    }

    private boolean match(SecurityGroup resourceGroup, int[] userGroups) {
        int resourceGpId = resourceGroup.gid();
        int[] resourcePath = resourceGroup.path();
        for (int i = 0; i < userGroups.length; i++) {
            //If groupIds match
            if (resourceGpId == userGroups[i]) {
                return true;
            }
            // Collect group path
            int[] userReadGpPath = _groupsManager.get(userGroups[i]).path();
            int g = 0;
            // Loops while resourcePath and groupPath elements match, up to the end of one path
            for (; g < resourcePath.length && g < userReadGpPath.length; g++) {
                if (resourcePath[g] != userReadGpPath[g]) {
                    break;
                }
            }
            //Checks if end pf a path has been reached => matched
            boolean matched = g == resourcePath.length || g == userReadGpPath.length;
            if (matched) {
                // Check in which way it matched
                return resourcePath.length >= userReadGpPath.length;
            }
        }
        return false;
    }

}
