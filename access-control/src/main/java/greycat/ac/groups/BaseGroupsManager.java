package greycat.ac.groups;

import greycat.Callback;
import greycat.Graph;
import greycat.Node;
import greycat.Type;
import greycat.ac.SecurityManager;
import greycat.plugin.NodeState;
import greycat.struct.EStructArray;

import java.util.*;

/**
 * Created by Gregory NAIN on 04/08/2017.
 */
public class BaseGroupsManager implements SecurityManager {

    private Graph _graph;
    private String _acmIndexName;
    private int _rootGroups = 0;

    private Map<Integer, BaseGroup> _groups = new HashMap<>();

    public BaseGroupsManager(Graph rootAccessGraph, String acmIndexName) {
        this._graph = rootAccessGraph;
        this._acmIndexName = acmIndexName;
    }

    public Collection<BaseGroup> all() {
        return new ArrayList<>(_groups.values());
    }

    public BaseGroup get(int gid) {
        return _groups.get(gid);
    }

    public BaseGroup add(BaseGroup parent, String name) {
        BaseGroup newGroup;
        if (parent == null) {
            newGroup = new BaseGroup(_groups.size(), name, new int[]{_rootGroups});
            _rootGroups++;
        } else {
            newGroup = parent.createSubGroup(_groups.size(), name);
        }
        _groups.put(newGroup.gid(), newGroup);
        return newGroup;
    }

    public void delete(int gid, Callback<Boolean> done) {
        _groups.remove(gid);
        _graph.index(-1, System.currentTimeMillis(), _acmIndexName, acIndex -> {
            acIndex.findFrom(secGrpNodes -> {
                Node securityGroupsNode;
                if (secGrpNodes != null && secGrpNodes.length != 0) {
                    securityGroupsNode = secGrpNodes[0];
                    securityGroupsNode.removeAt(gid);
                    _graph.save(done);
                } else {
                    done.on(true);
                }

            }, "secGrps");
        });
    }

    public void load(Callback<Boolean> done) {
        _graph.index(-1, System.currentTimeMillis(), _acmIndexName, acIndex -> {
            acIndex.findFrom(secGrpNodes -> {
                Node securityGroupsNode;
                if (secGrpNodes == null || secGrpNodes.length == 0) {
                    throw new RuntimeException("Should not load if never saved !");
                } else {
                    securityGroupsNode = secGrpNodes[0];
                }
                NodeState ns = _graph.resolver().resolveState(securityGroupsNode);
                ns.each((attributeKey, elemType, elem) -> {
                    if (elemType != Type.STRING) {
                        _groups.put(attributeKey, BaseGroup.load((EStructArray) elem));
                    }
                });
                done.on(true);
            }, "secGrps");
        });
    }

    public void save(Callback<Boolean> done) {
        _graph.index(-1, System.currentTimeMillis(), _acmIndexName, acIndex -> {
            acIndex.findFrom(secGrpNodes -> {
                Node securityGroupsNode;
                if (secGrpNodes == null || secGrpNodes.length == 0) {
                    securityGroupsNode = _graph.newNode(acIndex.world(), acIndex.time());
                    securityGroupsNode.set("name", Type.STRING, "secGrps");
                    acIndex.update(securityGroupsNode);
                } else {
                    securityGroupsNode = secGrpNodes[0];
                }
                _groups.forEach((gid, securityGroup) -> {
                    EStructArray securityGroupContainer = (EStructArray) securityGroupsNode.getOrCreateAt(gid, Type.ESTRUCT_ARRAY);
                    securityGroup.save(securityGroupContainer);
                });
                _graph.save(done);
            }, "secGrps");
        });
    }

    public void loadInitialData(Callback<Boolean> done) {
        add(null, "Public");
        BaseGroup rootAdmin = add(null, "Admin Root");
        BaseGroup usersAdmin = add(rootAdmin, "Users Admin");
        add(usersAdmin, "Admin User Admin");
        BaseGroup acm = add(rootAdmin, "Access Control Admin");
        add(acm, "Security Groups Admin");
        add(acm, "Permissions Admin");
        add(rootAdmin, "Business Security Root");
        _graph.save(done);
    }
}
