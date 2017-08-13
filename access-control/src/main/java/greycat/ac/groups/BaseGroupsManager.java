/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.ac.groups;

import greycat.Callback;
import greycat.Graph;
import greycat.Node;
import greycat.Type;
import greycat.ac.Group;
import greycat.ac.GroupsManager;
import greycat.plugin.NodeState;
import greycat.struct.EStructArray;

import java.util.*;

/**
 * Created by Gregory NAIN on 04/08/2017.
 */
public class BaseGroupsManager implements GroupsManager {

    private Graph _graph;
    private String _acmIndexName;
    private int _rootGroups = 0;

    private Map<Integer, Group> _groups = new HashMap<>();

    public BaseGroupsManager(Graph rootAccessGraph, String acmIndexName) {
        this._graph = rootAccessGraph;
        this._acmIndexName = acmIndexName;
    }

    @Override
    public Collection<Group> all() {
        return new ArrayList<>(_groups.values());
    }

    @Override
    public Group get(int gid) {
        return _groups.get(gid);
    }

    @Override
    public Group add(Group parent, String name) {
        Group newGroup;
        if (parent == null) {
            newGroup = new BaseGroup(_groups.size(), -1, name, new int[]{_rootGroups});
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

    @Override
    public void save(Callback<Boolean> done) {
        _graph.index(-1, System.currentTimeMillis(), _acmIndexName, acIndex -> {
            acIndex.findFrom(secGrpNodes -> {
                Node securityGroupsNode;
                if (secGrpNodes == null || secGrpNodes.length == 0) {
                    securityGroupsNode = _graph.newNode(acIndex.world(), acIndex.time());
                    securityGroupsNode.setGroup(5);
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

    @Override
    public void loadInitialData(Callback<Boolean> done) {
        add(null, "Public");
        Group rootAdmin = add(null, "Admin Root");
        Group usersAdmin = add(rootAdmin, "Users Admin");
        add(usersAdmin, "Admin User Admin");
        Group acm = add(rootAdmin, "Access Control Admin");
        add(acm, "Security Groups Admin");
        add(acm, "Permissions Admin");
        add(rootAdmin, "Business Security Root");
        _graph.save(done);
    }

    public void printCurrentConfiguration(StringBuilder sb) {
        sb.append("#########   Security Groups Manager - CurrentConfiguration   #########\n\n");
        _groups.values().forEach(baseGroup -> {
            sb.append(baseGroup.toString() + "\n");
        });
    }
}
