package greycat.ac.auth;


import greycat.Type;
import greycat.struct.EStruct;
import greycat.struct.EStructArray;

import java.util.Arrays;
import java.util.Date;

/**
 * Created by Gregory NAIN on 03/08/2017.
 */
public class PasswordKey {

    private static final int UID_IDX = 0;
    private static final int AUTH_KEY_IDX = 1;
    private static final int DEADLINE_IDX = 2;

    private long _uid;
    private String _authKey;
    private long _deadline;

    private PasswordKey(){}

    public PasswordKey(long uid, String authKey, long deadline) {
        this._uid = uid;
        this._authKey = authKey;
        this._deadline = deadline;
    }

    public long uid() {
        return _uid;
    }

    public String authKey() {
        return _authKey;
    }

    public long deadline() {
        return _deadline;
    }

    void save(EStructArray container) {
        EStruct root = container.root();
        if(root == null) {
            root = container.newEStruct();
        }
        root.setAt(UID_IDX, Type.LONG, _uid);
        root.setAt(AUTH_KEY_IDX, Type.STRING, _authKey);
        root.setAt(DEADLINE_IDX, Type.LONG, _deadline);
    }

    static PasswordKey load(EStructArray container) {
        PasswordKey pk = new PasswordKey();
        EStruct root = container.root();
        if(root == null) {
            throw new RuntimeException("Nothing to load !");
        }
        pk._uid = (long) root.getAt(UID_IDX);
        pk._authKey = (String) root.getAt(AUTH_KEY_IDX);
        pk._deadline = (long)root.getAt(DEADLINE_IDX);

        return pk;
    }

    @Override
    public String toString() {
        return "{uid: "+_uid+", authKey: "+_authKey+", deadline: "+ new Date(_deadline).toInstant().toString()+"}";
    }
}
