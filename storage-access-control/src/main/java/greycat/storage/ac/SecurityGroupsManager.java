package greycat.storage.ac;

import greycat.Callback;
import greycat.Graph;
import greycat.Node;
import greycat.Type;
import greycat.plugin.NodeState;
import greycat.struct.EStruct;
import greycat.struct.EStructArray;

import java.util.*;

/**
 * Created by Gregory NAIN on 04/08/2017.
 */
public class SecurityGroupsManager {

    private Graph _graph;
    private String _acmIndexName;
    private int _rootGroups = 0;

    private Map<Integer, SecurityGroup> _groups = new HashMap<>();

    SecurityGroupsManager(Graph rootAccessGraph, String acmIndexName) {
        this._graph = rootAccessGraph;
        this._acmIndexName = acmIndexName;
    }

    public Collection<SecurityGroup> all() {
        return new ArrayList<>(_groups.values());
    }

    public SecurityGroup get(int gid) {
        return _groups.get(gid);
    }

    public SecurityGroup add(SecurityGroup parent, String name) {
        SecurityGroup newGroup;
        if(parent== null) {
            newGroup = new SecurityGroup(_groups.size(), name, new int[]{_rootGroups});
            _rootGroups++;
        } else {
            newGroup = parent.createSubGroup(_groups.size(), name);
        }
        _groups.put(newGroup.gid(), newGroup);
        return newGroup;
    }

    public boolean remove(int gid) {
        _groups.remove(gid);
        return true;
    }

    void load(Callback<Boolean> done) {
        _graph.index(-1, System.currentTimeMillis(), _acmIndexName, acIndex -> {
            acIndex.findFrom(secGrpNodes -> {
                Node securityGroupsNode;
                if(secGrpNodes == null || secGrpNodes.length == 0) {
                    throw new RuntimeException("Should not load if never saved !");
                } else {
                    securityGroupsNode = secGrpNodes[0];
                }
                NodeState ns = _graph.resolver().resolveState(securityGroupsNode);
                ns.each((attributeKey, elemType, elem) -> {
                   _groups.put(attributeKey, SecurityGroup.load((EStructArray) elem));
                });
            }, "secGrps");
        });
        done.on(true);
    }

    void save(Callback<Boolean> done) {
        _graph.index(-1, System.currentTimeMillis(), _acmIndexName, acIndex -> {
            acIndex.findFrom(secGrpNodes -> {
                Node securityGroupsNode;
                if(secGrpNodes == null || secGrpNodes.length == 0) {
                    securityGroupsNode = _graph.newNode(acIndex.world(), acIndex.time());
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

    void loadInitialGroups(Callback<Boolean> done) {
        add(null, "Public");
        SecurityGroup rootAdmin = add(null, "Admin Root");
        SecurityGroup usersAdmin = add(rootAdmin, "Users Admin");
        add(usersAdmin, "Admin User Admin");
        SecurityGroup acm = add(rootAdmin, "Access Control Admin");
        add(acm, "Security Groups Admin");
        add(acm, "Permissions Admin");
        add(rootAdmin, "Business Security Root");
        done.on(true);
    }
}
