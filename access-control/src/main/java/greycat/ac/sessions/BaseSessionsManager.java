/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.ac.sessions;

import greycat.*;
import greycat.ac.Session;
import greycat.ac.SessionManager;
import greycat.plugin.NodeState;
import greycat.struct.EStructArray;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */
public class BaseSessionsManager implements SessionManager {

    private Graph _graph;
    private String _acIndexName;
    private long _inactivityDelay = 10 * 60 * 1000; // 10 minutes

    private Map<Long, Session> _usersSessions = new HashMap<>();
    private Map<String, Session> _sessionIdSessions = new HashMap<>();

    public BaseSessionsManager(Graph _graph, String acIndexName) {
        this._graph = _graph;
        this._acIndexName = acIndexName;
    }

    @Override
    public SessionManager setInactivityDelay(long delay) {
        this._inactivityDelay = delay;
        return this;
    }

    @Override
    public long getInactivityDelay() {
        return _inactivityDelay;
    }

    @Override
    public Session getOrCreateSession(long uid) {
        Session session = _usersSessions.get(uid);
        if (session == null) {
            session = new BaseSession(uid, UUID.randomUUID().toString(), _inactivityDelay);
            _usersSessions.put(session.uid(), session);
            _sessionIdSessions.put(session.sessionId(), session);
        }
        return session;
    }

    @Override
    public Session getSession(String sessionId) {
        return _sessionIdSessions.get(sessionId);
    }

    @Override
    public void clearSession(String sessionId) {
        Session session = _sessionIdSessions.remove(sessionId);
        if (session != null) {
            _usersSessions.remove(session.uid());
        }
    }

    @Override
    public void load(Callback<Boolean> done) {
        _graph.index(-1, System.currentTimeMillis(), _acIndexName, acIndex -> {
            acIndex.findFrom(sessions -> {
                Node sessionsNode;
                if (sessions == null || sessions.length == 0) {
                    throw new RuntimeException("Should not load if never saved !");
                } else {
                    sessionsNode = sessions[0];
                }
                NodeState ns = _graph.resolver().resolveState(sessionsNode);
                ArrayList<Integer> attKeys = new ArrayList<>();
                ns.each((attributeKey, elemType, elem) -> {
                    if (elemType == Type.STRING) {
                        return;
                    }
                    attKeys.add(attributeKey);
                    Session session = BaseSession.load((EStructArray) elem);
                    if (!session.isExpired()) {
                        _sessionIdSessions.put(session.sessionId(), session);
                        _usersSessions.put(session.uid(), session);
                    }
                });
                for (int attKey : attKeys) {
                    sessionsNode.removeAt(attKey);
                }
                _graph.save(done);
            }, "sessions");
        });
    }

    @Override
    public void save(Callback<Boolean> done) {
        _graph.index(-1, System.currentTimeMillis(), _acIndexName, acIndex -> {
            acIndex.findFrom(sessions -> {
                Node sessionsNode;
                if (sessions == null || sessions.length == 0) {
                    sessionsNode = _graph.newNode(acIndex.world(), acIndex.time());
                    sessionsNode.setGroup(1);
                    sessionsNode.set("name", Type.STRING, "sessions");
                    acIndex.update(sessionsNode);
                } else {
                    sessionsNode = sessions[0];
                }
                int i = 0;
                for (Session session : _sessionIdSessions.values()) {
                    if (!session.isExpired()) {
                        EStructArray securityGroupContainer = (EStructArray) sessionsNode.getOrCreateAt(i++, Type.ESTRUCT_ARRAY);
                        session.save(securityGroupContainer);
                    }
                }
                _graph.save(done);
            }, "sessions");
        });
    }

}
