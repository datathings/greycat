package greycat.storage.ac;

import greycat.Type;
import greycat.struct.*;

import java.util.Arrays;

import static greycat.storage.ac.PermissionsManager.*;

/**
 * Created by Gregory NAIN on 03/08/2017.
 */
public class Permission {

    private long _uid;
    private long[] _roots = new long[0];
    private int[] _r = new int[0];
    private int[] _w = new int[0];
    private int[] _nr = new int[0];
    private int[] _nw = new int[0];

    private Permission() {}

    Permission(long uid) {
        this._uid = uid;
    }

    public long uid() {
        return _uid;
    }

    public int[] read() {
        return _r;
    }

    public int[] write() {
        return _w;
    }

    public int[] notRead() {
        return _nr;
    }

    public int[] notWrite() {
        return _nw;
    }
    boolean add(int permType, int gid) {
        if (!consistencyCheck(permType, gid)) {
            return false;
        }
        switch (permType) {
            case READ_ALLOWED: {
                int[] newTable = new int[_r.length + 1];
                System.arraycopy(_r, 0, newTable, 0, _r.length);
                newTable[_r.length] = gid;
                Arrays.sort(newTable);
                this._r = newTable;
            }
            break;
            case WRITE_ALLOWED: {
                int[] newTable = new int[_w.length + 1];
                System.arraycopy(_w, 0, newTable, 0, _w.length);
                newTable[_w.length] = gid;
                Arrays.sort(newTable);
                this._w = newTable;
            }
            break;
            case READ_DENIED: {
                int[] newTable = new int[_nr.length + 1];
                System.arraycopy(_nr, 0, newTable, 0, _nr.length);
                newTable[_nr.length] = gid;
                Arrays.sort(newTable);
                this._nr = newTable;
            }
            break;
            case WRITE_DENIED: {
                int[] newTable = new int[_nw.length + 1];
                System.arraycopy(_nw, 0, newTable, 0, _nw.length);
                newTable[_nw.length] = gid;
                Arrays.sort(newTable);
                this._nw = newTable;
            }
            break;
        }
        return true;
    }

    boolean add(int permType, int[] gids) {
        for (int gid : gids) {
            if (!consistencyCheck(permType, gid)) {
                return false;
            }
        }
        switch (permType) {
            case READ_ALLOWED: {
                int[] newTable = new int[_r.length + gids.length];
                System.arraycopy(_r, 0, newTable, 0, _r.length);
                for (int i = 0; i < gids.length; i++) {
                    newTable[_r.length + i] = gids[i];
                }
                Arrays.sort(newTable);
                this._r = newTable;
            }
            break;
            case WRITE_ALLOWED: {
                int[] newTable = new int[_w.length + gids.length];
                System.arraycopy(_w, 0, newTable, 0, _w.length);
                for (int i = 0; i < gids.length; i++) {
                    newTable[_w.length + i] = gids[i];
                }
                Arrays.sort(newTable);
                this._w = newTable;
            }
            break;
            case READ_DENIED: {
                int[] newTable = new int[_nr.length + gids.length];
                System.arraycopy(_nr, 0, newTable, 0, _nr.length);
                for (int i = 0; i < gids.length; i++) {
                    newTable[_nr.length + i] = gids[i];
                }
                Arrays.sort(newTable);
                this._nr = newTable;
            }
            break;
            case WRITE_DENIED: {
                int[] newTable = new int[_nw.length + gids.length];
                System.arraycopy(_nw, 0, newTable, 0, _nw.length);
                for (int i = 0; i < gids.length; i++) {
                    newTable[_nw.length + i] = gids[i];
                }
                Arrays.sort(newTable);
                this._nw = newTable;
            }
            break;
        }
        return true;
    }

    private boolean consistencyCheck(int permType, int gid) {
        return true;
    }


    public void save(EStructArray container) {
        EStruct root = container.root();
        if(root == null) {
            root = container.newEStruct();
        }
        root.set("uid", Type.LONG, _uid);
        ((LongArray)root.getOrCreate("roots", Type.LONG_ARRAY)).addAll(_roots);
        ((IntArray)root.getOrCreate("read", Type.INT_ARRAY)).addAll(_r);
        ((IntArray)root.getOrCreate("write", Type.INT_ARRAY)).addAll(_w);
        ((IntArray)root.getOrCreate("!read", Type.INT_ARRAY)).addAll(_nr);
        ((IntArray)root.getOrCreate("!write", Type.INT_ARRAY)).addAll(_nw);
    }

    public static Permission load(EStructArray container) {
        Permission perm = new Permission();
        EStruct root = container.root();
        if(root == null) {
            throw new RuntimeException("Nothing to load !");
        }
        perm._uid = (long)root.get("uid");
        perm._roots = root.getLongArray("roots").extract();
        perm._r = root.getIntArray("read").extract();
        perm._w = root.getIntArray("write").extract();
        perm._nr = root.getIntArray("!read").extract();
        perm._nw = root.getIntArray("!write").extract();
        return perm;
    }

    @Override
    public String toString() {
        return "{uid: "+_uid+", read: "+Arrays.toString(_r)+", write: "+Arrays.toString(_w)+", !read: "+Arrays.toString(_nr)+", !write: "+Arrays.toString(_nw)+"}";
    }
}
