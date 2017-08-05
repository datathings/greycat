package greycat.ac;

import greycat.*;
import greycat.ac.auth.BaseAuthenticationManager;
import greycat.ac.groups.Group;
import greycat.ac.groups.GroupsManager;
import greycat.ac.permissions.Permission;
import greycat.ac.permissions.PermissionsManager;
import greycat.ac.sessions.BaseSessionsManager;

import java.util.concurrent.CountDownLatch;

/**
 * Created by Gregory NAIN on 03/08/2017.
 */
public class BaseAccessControlManager implements AccessControlManager {

    private Graph _graph;
    private String _acIndexName = "$acm";
    private GroupsManager _groupsManager;
    private PermissionsManager _permissionsManager;
    private AuthenticationManager _authManager;
    private SessionManager _sessionManager;


    public BaseAccessControlManager(Graph graph) {
        this._graph = graph;
    }

    public BaseAccessControlManager setAcIndexName(String acIndexName) {
        this._acIndexName = acIndexName;
        return this;
    }

    public void shutdown() {
        System.out.println("Shutting down ACM");
        CountDownLatch latch = new CountDownLatch(4);
        this._authManager.save(result -> {
            latch.countDown();
        });
        this._groupsManager.save(result -> {
            latch.countDown();
        });
        this._permissionsManager.save(result -> {
            latch.countDown();
        });
        this._sessionManager.save(result -> {
            latch.countDown();
        });
        try {
            latch.await();
            System.out.println("ACM shutdown complete");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void start(Callback<Boolean> callback) {
        _graph.connect(connected -> {
            initManagers();
            _graph.indexNames(-1, System.currentTimeMillis(), indexNames -> {
                try {
                    boolean indexExists = indexNames != null && indexNames.length > 0;
                    if (indexExists) {
                        indexExists = false;
                        for (String s : indexNames) {
                            if (s.equals(_acIndexName)) {
                                indexExists = true;
                                break;
                            }
                        }
                    }
                    if (!indexExists) {

                        boolean[] ready = new boolean[3];
                        CountDownLatch latch = new CountDownLatch(3);

                        _graph.declareIndex(-1, _acIndexName, newAcmIndex -> {
                            newAcmIndex.setGroup(4);
                            System.out.println("ACM:LoadingInitialData");
                            this._authManager.loadInitialData(result -> {
                                System.out.println("ACM:AuthManager:Done:" + result);
                                ready[0] = result;
                                latch.countDown();
                            });
                            this._groupsManager.loadInitialData(result -> {
                                System.out.println("ACM:GroupsManager:Done:" + result);
                                ready[1] = result;
                                latch.countDown();
                            });
                            this._permissionsManager.loadInitialData(result -> {
                                System.out.println("ACM:PermissionsManager:Done:" + result);
                                ready[2] = result;
                                latch.countDown();
                            });
                        }, "name");

                        latch.await();
                        if (ready[0] && ready[1] && ready[2]) {
                            _graph.save(callback);
                        } else {
                            callback.on(false);
                        }
                    } else {

                        boolean[] ready = new boolean[4];
                        CountDownLatch latch = new CountDownLatch(4);

                        this._authManager.load(result -> {
                            ready[0] = result;
                            latch.countDown();
                        });
                        this._groupsManager.load(result -> {
                            ready[1] = result;
                            latch.countDown();
                        });
                        this._permissionsManager.load(result -> {
                            ready[2] = result;
                            latch.countDown();
                        });
                        this._sessionManager.load(result -> {
                            ready[3] = result;
                            latch.countDown();
                        });

                        latch.await();
                        if (ready[0] && ready[1] && ready[2] && ready[3]) {
                            _graph.save(callback);
                        } else {
                            callback.on(false);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    callback.on(false);
                }
            });
        });
    }

    private void initManagers() {
        if (this._groupsManager == null) {
            this._groupsManager = new GroupsManager(_graph, _acIndexName);
        }
        if (this._permissionsManager == null) {
            this._permissionsManager = new PermissionsManager(_graph, _acIndexName);
        }
        if (this._authManager == null) {
            this._authManager = new BaseAuthenticationManager(_graph, _acIndexName);
        }
        if (this._sessionManager == null) {
            this._sessionManager = new BaseSessionsManager(_graph, _acIndexName);
        }
    }

    public GroupsManager getSecurityGroupsManager() {
        return _groupsManager;
    }

    public PermissionsManager getPermissionsManager() {
        return _permissionsManager;
    }

    @Override
    public AuthenticationManager getAuthenticationManager() {
        return _authManager;
    }

    @Override
    public SessionManager getSessionsManager() {
        return _sessionManager;
    }


    @Override
    public boolean canRead(long uid, int gid) {
        if (gid == 0) {
            return true; //Public group
        }

        Permission userPermissions = _permissionsManager.get(uid);
        Group resourceGroup = _groupsManager.get(gid);

        //Check if group is in explicit NoRead permissions
        int[] userNotReadGroups = userPermissions.notRead();
        if (match(resourceGroup, userNotReadGroups)) {
            return false;
        } else {
            //Check if group is in explicit read permissions
            int[] userReadGroups = userPermissions.read();
            return match(resourceGroup, userReadGroups);
        }
    }

    @Override
    public boolean canWrite(long uid, int gid) {
        Permission userPermissions = _permissionsManager.get(uid);
        Group resourceGroup = _groupsManager.get(gid);

        //Check if group is in explicit NoRead permissions
        int[] userNotWriteGroups = userPermissions.notWrite();
        if (match(resourceGroup, userNotWriteGroups)) {
            return false;
        } else {
            //Check if group is in explicit read permissions
            int[] userWriteGroups = userPermissions.write();
            return match(resourceGroup, userWriteGroups);
        }
    }

    private boolean match(Group resourceGroup, int[] userGroups) {
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
