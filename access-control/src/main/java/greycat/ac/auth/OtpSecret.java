package greycat.ac.auth;


import greycat.Node;
import greycat.Type;
import greycat.struct.EStruct;
import greycat.struct.EStructArray;

import java.util.Date;

/**
 * Created by Gregory NAIN on 03/08/2017.
 */
public class OtpSecret {

    private static final int SECRET_IDX = 0;

    private long _uid;
    private String _secret;

    private OtpSecret(){}

    public OtpSecret(long uid, String secret) {
        this._uid = uid;
        this._secret = secret;
    }

    public long uid() {
        return _uid;
    }

    public String secret() {
        return _secret;
    }


    void save(Node container) {
        container.set("uid", Type.LONG, _uid);
        container.setAt(SECRET_IDX, Type.STRING, _secret);
    }

    static OtpSecret load(Node container) {
        OtpSecret secret = new OtpSecret();
        secret._uid = (long) container.get("uid");
        secret._secret = (String) container.getAt(SECRET_IDX);

        return secret;
    }

    @Override
    public String toString() {
        return "{uid: "+_uid+", secret: "+_secret+"}";
    }
}
