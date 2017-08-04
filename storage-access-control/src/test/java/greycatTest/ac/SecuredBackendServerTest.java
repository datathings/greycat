package greycatTest.ac;

import greycat.*;
import greycat.storage.ac.*;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Gregory NAIN on 03/08/2017.
 */
public class SecuredBackendServerTest {


    public static void main(String[] args) {

        try {

            MockStorage storage = new MockStorage();
            Graph rootAccessGraph = GraphBuilder.newBuilder().withStorage(storage).build();
            CountDownLatch latch = new CountDownLatch(1);
            rootAccessGraph.connect(connected -> {
                AccessControlManager accessControlManager = new AccessControlManager(rootAccessGraph, "$acm");
                accessControlManager.init(aBoolean -> {
                    loadRootSecurityGroups(accessControlManager);
                    loadRootUsers(rootAccessGraph);
                    loadFakeBusinessElements(rootAccessGraph);
                    loadRootPermissions(accessControlManager);
                    printConfig(rootAccessGraph, accessControlManager);
                    completeAccessCheck(rootAccessGraph, accessControlManager);
                    latch.countDown();
                });
            });
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static SecurityGroup chiefUserAdmin, userUserAdmin, businessSecurityRoot, complexASecurityGroup, complexBSecurityGroup;

    private static void loadRootSecurityGroups(AccessControlManager acm) {
        System.out.println("Adding security Groups");
        SecurityGroupsManager sgm = acm.getSecurityGroupsManager();

        SecurityGroup usersAdmin = sgm.get(2);
        businessSecurityRoot = sgm.get(7);

        chiefUserAdmin = sgm.add(usersAdmin, "Chief User Admin");
        userUserAdmin = sgm.add(usersAdmin, "User User Admin");

        SecurityGroup complexesSecurityGroup = sgm.add(businessSecurityRoot, "Complexes Security Group");
        complexASecurityGroup = sgm.add(complexesSecurityGroup, "Complex A Security Group");
        complexBSecurityGroup = sgm.add(complexesSecurityGroup, "Complex B Security Group");

    }

    private static Node adminUser, businessUserChief, businessUser;

    private static void loadRootUsers(Graph rootAccessGraph) {
        System.out.println("Adding users");
        rootAccessGraph.index(0, System.currentTimeMillis(), "Users", nodeIndex -> {

            nodeIndex.findFrom(users -> {
                adminUser = users[0];

                businessUserChief = rootAccessGraph.newNode(nodeIndex.world(), nodeIndex.time());
                businessUserChief.setGroup(chiefUserAdmin.gid());
                businessUserChief.set("name", Type.STRING, "USER CHIEF");
                businessUserChief.set("login", Type.STRING, "chief");
                nodeIndex.update(businessUserChief);
                System.out.println("ChiefUser:" + businessUserChief.id());

                businessUser = rootAccessGraph.newNode(nodeIndex.world(), nodeIndex.time());
                businessUser.setGroup(userUserAdmin.gid());
                businessUser.set("name", Type.STRING, "USER");
                businessUser.set("login", Type.STRING, "user");
                nodeIndex.update(businessUser);
                System.out.println("User:" + businessUser.id());

            }, "admin");
        });
    }

    private static void loadRootPermissions(AccessControlManager acm) {
        System.out.println("Adding permissions");

        PermissionsManager pm = acm.getPermissionsManager();


        pm.add(businessUserChief.id(), PermissionsManager.READ_ALLOWED, new int[]{businessSecurityRoot.gid(), chiefUserAdmin.gid()});
        pm.add(businessUserChief.id(), PermissionsManager.READ_DENIED, new int[]{complexBSecurityGroup.gid()});
        pm.add(businessUserChief.id(), PermissionsManager.WRITE_ALLOWED, new int[]{businessSecurityRoot.gid(), chiefUserAdmin.gid()});
        pm.add(businessUserChief.id(), PermissionsManager.WRITE_DENIED, new int[]{complexBSecurityGroup.gid()});

        pm.add(businessUser.id(), PermissionsManager.READ_ALLOWED, new int[]{complexBSecurityGroup.gid(), userUserAdmin.gid()});
        pm.add(businessUser.id(), PermissionsManager.WRITE_ALLOWED, new int[]{complexBSecurityGroup.gid(), userUserAdmin.gid()});
    }

    private static void loadFakeBusinessElements(Graph rootAccessGraph) {
        System.out.println("Adding business elements");
        rootAccessGraph.declareIndex(0, "Complexes", nodeIndex -> {
            System.out.println("Complexes Index:" + nodeIndex.id());
            nodeIndex.setGroup(7);

            Node complexA = rootAccessGraph.newNode(nodeIndex.world(), nodeIndex.time());
            complexA.setGroup(complexASecurityGroup.gid());
            complexA.set("name", Type.STRING, "ComplexA");
            nodeIndex.update(complexA);
            System.out.println("ComplexA:" + complexA.id());

            Node complexB = rootAccessGraph.newNode(nodeIndex.world(), nodeIndex.time());
            complexB.setGroup(complexBSecurityGroup.gid());
            complexB.set("name", Type.STRING, "ComplexB");
            nodeIndex.update(complexB);
            System.out.println("ComplexB:" + complexB.id());

            Node publicResource = rootAccessGraph.newNode(nodeIndex.world(), nodeIndex.time());
            publicResource.set("name", Type.STRING, "Public resource");

        }, "name");
    }

    private static void printConfig(Graph rootAccessGraph, AccessControlManager acm) {
        System.out.println("Printing");
        System.out.println("=======   Security Groups =======");
        acm.getSecurityGroupsManager().all().forEach(System.out::println);
        System.out.println("=======   Permissions  =======");
        acm.getPermissionsManager().all().forEach(System.out::println);

        rootAccessGraph.indexNames(0, System.currentTimeMillis(), indexNames -> {
            for (String index : indexNames) {
                rootAccessGraph.index(0, System.currentTimeMillis(), index, nodeIndex -> {
                    System.out.println("=======   " + index + "(id: " + nodeIndex.id() + ", gid: " + nodeIndex.group() + ")  =======");
                    nodeIndex.findFrom(nodes -> {
                        for (Node n : nodes) {
                            System.out.println(n.toString() + " group:" + n.group());
                        }
                    });
                });
            }
        });

        rootAccessGraph.indexNames(0, System.currentTimeMillis(), result -> {
            System.out.println("Indexes Names:" + Arrays.toString(result));
        });
    }

    private static void completeAccessCheck(Graph rootAccessGraph, AccessControlManager acm) {
        System.out.println("Performing access checks");
        //AccessControlManager manager = new AccessControlManager(rootAccessGraph, "SecurityGroups", "Permissions");

        System.out.println("");
        userPermissionsCheck(acm, rootAccessGraph, "Admin", adminUser.id());
        userPermissionsCheck(acm, rootAccessGraph, "Chief", businessUserChief.id());
        userPermissionsCheck(acm, rootAccessGraph, "User", businessUser.id());

    }

    private static void userPermissionsCheck(AccessControlManager manager, Graph rootAccessGraph, String userName, long uid) {
        System.out.println("=====  Permission checks for " + userName + "  ======");
        for (int i = 0; i < 50; i++) {
            rootAccessGraph.lookup(-1, System.currentTimeMillis(), i, node -> {
                if (node != null) {
                    accessCheck(manager, userName, uid, node);
                }
            });
        }
        for (int i = 0; i < 50; i++) {
            rootAccessGraph.lookup(0, System.currentTimeMillis(), i, node -> {
                if (node != null) {
                    accessCheck(manager, userName, uid, node);
                }
            });
        }
    }


    private static void accessCheck(AccessControlManager manager, String userName, long uid, Node n) {
        manager.canRead(uid, n.group(), result -> {
            System.out.println(userName + "\tREAD\t" + (result ? "OK" : "FAIL") + "\t\t" + n);
        });
        manager.canWrite(uid, n.group(), result -> {
            System.out.println(userName + "\tWRITE\t" + (result ? "OK" : "FAIL") + "\t\t" + n);
        });
    }


}
