package greycat.ac.groups;


import greycat.Type;
import greycat.struct.EStruct;
import greycat.struct.EStructArray;
import greycat.struct.IntArray;

import java.util.Arrays;

/**
 * Created by Gregory NAIN on 03/08/2017.
 */
public class Group {

    private int gid;
    private String name;
    private int[] path;
    private int genIndex = 1;

    private Group(){};

    Group(int gid, String name, int[] path) {
        this.name = name;
        this.path = path;
        this.gid = gid;
    }

    public int gid() {
        return gid;
    }

    public String name() {
        return name;
    }

    public int[] path() {
        return path;
    }

    Group createSubGroup(int gid, String name) {
        int[] childPath = new int[this.path.length+1];
        System.arraycopy(this.path, 0, childPath, 0, this.path.length);
        childPath[this.path.length] = this.genIndex;
        genIndex++;
        Group newGroup = new Group(gid, name, childPath);
        return newGroup;
    }

    void save(EStructArray container) {
        EStruct root = container.root();
        if(root == null) {
            root = container.newEStruct();
        }
        root.set("gid", Type.INT, gid);
        root.set("name", Type.STRING, name);
        ((IntArray)root.getOrCreate("path", Type.INT_ARRAY)).addAll(path);
        root.set("genIndex", Type.INT, genIndex);
    }

    static Group load(EStructArray container) {
        Group sg = new Group();
        EStruct root = container.root();
        if(root == null) {
            throw new RuntimeException("Nothing to load !");
        }
        sg.gid = (int)root.get("gid");
        sg.name = (String) root.get("name");
        sg.path = root.getIntArray("path").extract();
        sg.genIndex = (int) root.get("genIndex");
        return sg;
    }

    @Override
    public String toString() {
        return "{gid: "+gid+", name: "+name+", path: "+ Arrays.toString(path)+"}";
    }
}
