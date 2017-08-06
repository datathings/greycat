package greycat.ac.permissions;

import greycat.Callback;
import greycat.Graph;
import greycat.Node;
import greycat.Type;
import greycat.plugin.NodeState;
import greycat.struct.EStructArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Gregory NAIN on 04/08/2017.
 */
public class BasePermissionsManager {

    public static final int READ_ALLOWED = 0;
    public static final int WRITE_ALLOWED = 1;
    public static final int READ_DENIED = 2;
    public static final int WRITE_DENIED = 3;

    private Graph _graph;
    private String _acmIndexName;

    private Map<Long, BasePermission> _permissions = new HashMap<>();

    public BasePermissionsManager(Graph rootAccessGraph, String acmIndexName) {
        this._graph = rootAccessGraph;
        this._acmIndexName = acmIndexName;
    }

    public Collection<BasePermission> all() {
        return new ArrayList<>(_permissions.values());
    }

    public BasePermission get(long uid) {
        return _permissions.get(uid);
    }

    public boolean add(long uid, int permType, int gid) {
        BasePermission perm = _permissions.get(uid);
        if (perm == null) {
            perm = new BasePermission(uid);
            _permissions.put(uid, perm);
        }
        return perm.add(permType, gid);
    }

    public boolean add(long uid, int permType, int[] gids) {
        BasePermission perm = _permissions.get(uid);
        if (perm == null) {
            perm = new BasePermission(uid);
            _permissions.put(uid, perm);
        }
        return perm.add(permType, gids);
    }

    public boolean remove(int uid, int gid, int permtype) {
        throw new RuntimeException("Not implemented");
    }

    public void load(Callback<Boolean> done) {
        _graph.index(-1, System.currentTimeMillis(), _acmIndexName, acIndex -> {
            acIndex.findFrom(permissionNodes -> {
                Node permissionsNode;
                if (permissionNodes == null || permissionNodes.length == 0) {
                    throw new RuntimeException("Should not load if never saved !");
                } else {
                    permissionsNode = permissionNodes[0];
                }
                NodeState ns = _graph.resolver().resolveState(permissionsNode);
                ArrayList<Integer> attKeys = new ArrayList<>();
                ns.each((attributeKey, elemType, elem) -> {
                    if (elemType == Type.STRING) {
                        return;
                    }
                    attKeys.add(attributeKey);
                    BasePermission p = BasePermission.load((EStructArray) elem);
                    _permissions.put(p.uid(), p);
                });
                for (int attKey : attKeys) {
                    permissionsNode.removeAt(attKey);
                }
                _graph.save(done);
            }, "perms");
        });
    }

    public void save(Callback<Boolean> done) {
        _graph.index(-1, System.currentTimeMillis(), _acmIndexName, acIndex -> {
            acIndex.findFrom(permsNodes -> {
                Node permissionsNode;
                if (permsNodes == null || permsNodes.length == 0) {
                    permissionsNode = _graph.newNode(acIndex.world(), acIndex.time());
                    permissionsNode.set("name", Type.STRING, "perms");
                    acIndex.update(permissionsNode);
                } else {
                    permissionsNode = permsNodes[0];
                }
                int i = 0;
                for (BasePermission p : _permissions.values()) {
                    EStructArray permissionContainer = (EStructArray) permissionsNode.getOrCreateAt(i++, Type.ESTRUCT_ARRAY);
                    p.save(permissionContainer);
                }

                _graph.save(done);
            }, "perms");
        });
    }

    public void loadInitialData(Callback<Boolean> done) {
        add(3, BasePermissionsManager.READ_ALLOWED, 1);
        add(3, BasePermissionsManager.WRITE_ALLOWED, new int[]{0, 1});
        _graph.save(done);
    }

}
