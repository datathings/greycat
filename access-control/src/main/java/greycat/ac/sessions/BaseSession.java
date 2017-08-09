/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.ac.sessions;

import greycat.ac.Session;
import greycat.Type;
import greycat.struct.EStruct;
import greycat.struct.EStructArray;

import java.util.Date;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */
public class BaseSession implements Session {

    private static final int UID_IDX = 0;
    private static final int SESSION_ID_IDX = 1;
    private static final int LAST_HIT_IDX = 2;
    private static final int VALIDITY_IDX = 3;

    private long _uid;
    private String _sessionId;
    private long _lastHit;
    private long _validityPeriod;

    private BaseSession(){}

    public BaseSession(long uid, String sessionId, long validityPeriod) {
        this._uid = uid;
        this._sessionId = sessionId;
        this._validityPeriod = validityPeriod;
        this._lastHit = System.currentTimeMillis();
    }

    @Override
    public long uid() {
        return _uid;
    }

    @Override
    public String sessionId() {
        return _sessionId;
    }

    @Override
    public long deadline() {
        return  _lastHit + _validityPeriod;
    }

    @Override
    public boolean setLastHit(long lastHit) {
        if(lastHit > deadline()) {
            return false;
        } else {
            this._lastHit = lastHit;
            return true;
        }
    }

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() > deadline();
    }

    @Override
    public void save(EStructArray container) {
        EStruct root = container.root();
        if(root == null) {
            root = container.newEStruct();
        }
        root.setAt(UID_IDX, Type.LONG, _uid);
        root.setAt(SESSION_ID_IDX, Type.STRING, _sessionId);
        root.setAt(LAST_HIT_IDX, Type.LONG, _lastHit);
        root.setAt(VALIDITY_IDX, Type.LONG, _validityPeriod);
    }

    static BaseSession load(EStructArray container) {
        BaseSession session = new BaseSession();
        EStruct root = container.root();
        if(root == null) {
            throw new RuntimeException("Nothing to load !");
        }
        session._uid = (long) root.getAt(UID_IDX);
        session._sessionId = (String) root.getAt(SESSION_ID_IDX);
        session._lastHit = (long)root.getAt(LAST_HIT_IDX);
        session._validityPeriod = (long)root.getAt(VALIDITY_IDX);

        return session;
    }

    @Override
    public String toString() {
        return "{uid: "+_uid+", sessionId: "+_sessionId+", lastHit: "+_lastHit+", validity: "+_validityPeriod+", deadline:"+ new Date(deadline()).toString()+"}";
    }

}
