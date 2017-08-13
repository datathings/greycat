/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.ac.groups;


import greycat.Type;
import greycat.ac.Group;
import greycat.struct.EStruct;
import greycat.struct.EStructArray;
import greycat.struct.IntArray;

import java.util.Arrays;

/**
 * Created by Gregory NAIN on 03/08/2017.
 */
public class BaseGroup implements Group {

    private int _gid;
    private int _parentGid;
    private String _name;
    private int[] _path;
    private int _genIndex = 1;

    private BaseGroup(){};

    BaseGroup(int gid, int parentGid, String name, int[] _path) {
        this._name = name;
        this._path = _path;
        this._gid = gid;
        this._parentGid = parentGid;
    }

    @Override
    public int gid() {
        return _gid;
    }

    @Override
    public String name() {
        return _name;
    }

    @Override
    public int[] path() {
        return _path;
    }

    public int parentGid() {
        return _parentGid;
    }

    @Override
    public Group createSubGroup(int gid, String name) {
        int[] childPath = new int[this._path.length+1];
        System.arraycopy(this._path, 0, childPath, 0, this._path.length);
        childPath[this._path.length] = this._genIndex;
        _genIndex++;
        BaseGroup newGroup = new BaseGroup(gid, this._gid, name, childPath);
        return newGroup;
    }


    @Override
    public void save(EStructArray container) {
        EStruct root = container.root();
        if(root == null) {
            root = container.newEStruct();
        }
        root.set("gid", Type.INT, _gid);
        root.set("parent", Type.INT, _parentGid);
        root.set("name", Type.STRING, _name);
        ((IntArray)root.getOrCreate("path", Type.INT_ARRAY)).addAll(_path);
        root.set("genIndex", Type.INT, _genIndex);
    }

    static Group load(EStructArray container) {
        BaseGroup sg = new BaseGroup();
        EStruct root = container.root();
        if(root == null) {
            throw new RuntimeException("Nothing to load !");
        }
        sg._gid = (int)root.get("gid");
        sg._parentGid = (int)root.get("parent");
        sg._name = (String) root.get("name");
        sg._path = root.getIntArray("path").extract();
        sg._genIndex = (int) root.get("genIndex");
        return sg;
    }

    @Override
    public String toString() {
        return "{gid: "+ _gid +", parent: "+ _parentGid +", name: "+ _name +",_path: "+ Arrays.toString(_path)+"}";
    }
}
