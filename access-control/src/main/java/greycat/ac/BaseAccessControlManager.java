/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.ac;

import greycat.*;
import greycat.ac.auth.BaseAuthenticationManager;
import greycat.ac.groups.BaseGroupsManager;
import greycat.ac.permissions.BasePermissionsManager;
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

    private boolean _createAdminAtBoot = true;


    public BaseAccessControlManager(Graph graph) {
        this._graph = graph;

        this._groupsManager = new BaseGroupsManager(_graph, _acIndexName);
        this._permissionsManager = new BasePermissionsManager(_graph, _acIndexName);
        this._authManager = new BaseAuthenticationManager(_graph, _acIndexName);
        this._sessionManager = new BaseSessionsManager(_graph, _acIndexName);

    }

    @Override
    public AccessControlManager doNotCreateAdminAtBoot() {
        this._createAdminAtBoot = false;
        return this;
    }

    @Override
    public AccessControlManager setAcIndexName(String acIndexName) {
        this._acIndexName = acIndexName;
        return this;
    }

    @Override
    public void shutdown() {
        CountDownLatch latch = new CountDownLatch(5);
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
        this._graph.disconnect(disconnected -> {
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void start(Callback<Boolean> callback) {
        _graph.storage().listen(result -> {
            _graph.remoteNotify(result);
        });
        _graph.connect(connected -> {
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

                        _graph.declareIndex(-1, _acIndexName, newAcmIndex -> {
                            newAcmIndex.setGroup(4);
                            CountDownLatch latch = new CountDownLatch(3);
                            this._authManager.loadInitialData(_createAdminAtBoot, result -> {
                                ready[0] = result;
                                latch.countDown();
                            });
                            this._groupsManager.loadInitialData(result -> {
                                ready[1] = result;
                                latch.countDown();
                            });
                            this._permissionsManager.loadInitialData(_createAdminAtBoot, result -> {
                                ready[2] = result;
                                latch.countDown();
                            });
                            try {
                                latch.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            newAcmIndex.free();
                            if (ready[0] && ready[1] && ready[2]) {
                                _graph.save(callback);
                            } else {
                                callback.on(false);
                            }
                        }, "name");


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

    @Override
    public GroupsManager getSecurityGroupsManager() {
        return _groupsManager;
    }

    @Override
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
        if(userPermissions == null) {
            System.err.println("User " + uid + " has no permissions !");
            return false;
        }
        Group resourceGroup = _groupsManager.get(gid);

        //Check if group is in explicit NoRead permissions
        int[] userNotReadGroups = userPermissions.notRead();
        if (match(resourceGroup, userNotReadGroups)) {
            return false;
        } else {
            //Check if group is in explicit read permissions
            int[] userReadGroups = userPermissions.read();
            boolean matched = match(resourceGroup, userReadGroups);
            if(!matched) {
                System.out.println("Read rejected:"+ uid + ":"+gid);
            }
            return matched;
        }
    }

    @Override
    public boolean canWrite(long uid, int gid) {
        Permission userPermissions = _permissionsManager.get(uid);
        if(userPermissions == null) {
            System.err.println("User " + uid + " has no permissions !");
            return false;
        }
        Group resourceGroup = _groupsManager.get(gid);


        //Check if group is in explicit NoRead permissions
        int[] userNotWriteGroups = userPermissions.notWrite();
        if (match(resourceGroup, userNotWriteGroups)) {
            return false;
        } else {
            //Check if group is in explicit read permissions
            int[] userWriteGroups = userPermissions.write();
            boolean matched = match(resourceGroup, userWriteGroups);
            if(!matched) {
                System.out.println("Write rejected:"+ uid + ":"+gid);
            }
            return matched;
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

    @Override
    public void printCurrentConfiguration() {
        StringBuilder sb = new StringBuilder();
        sb.append("#########   AccessControlManager - CurrentConfiguration   #########\n\n");
        _authManager.printCurrentConfiguration(sb);
        sb.append("\n");
        _groupsManager.printCurrentConfiguration(sb);
        sb.append("\n");
        _permissionsManager.printCurrentConfiguration(sb);
        System.out.println(sb.toString());
    }
}
