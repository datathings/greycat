package greycat.ac.sessions;

import greycat.*;
import greycat.plugin.NodeState;
import greycat.struct.EStructArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */
public class BaseSessionsManager implements SessionManager {

    private Graph _graph;
    private String _acIndexName;
    private long _inactivityDelay = 10 * 60 * 1000; // 10 minutes

    private Map<Long, BaseSession> _usersSessions = new HashMap<>();
    private Map<String, BaseSession> _sessionIdSessions = new HashMap<>();


    public BaseSessionsManager(Graph _graph, String acIndexName) {
        this._graph = _graph;
        this._acIndexName = acIndexName;
    }

    public BaseSessionsManager setInactivityDelay(long delay) {
        this._inactivityDelay = delay;
        return this;
    }

    @Override
    public BaseSession getOrCreateSession(long uid) {
        BaseSession session = _usersSessions.get(uid);
        if (session == null) {
            session = new BaseSession(uid, UUID.randomUUID().toString(), _inactivityDelay);
            _usersSessions.put(session.uid(), session);
            _sessionIdSessions.put(session.sessionId(), session);
        }
        return session;
    }

    public Session sessionCheck(String sessionId) {
        BaseSession session = _sessionIdSessions.get(sessionId);
        if (session == null) {
            return null;
        } else {
            if (session.isExpired()) {
                _sessionIdSessions.remove(sessionId);
                _usersSessions.remove(session.uid());
                return null;
            } else {
                return session;
            }
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
                    BaseSession session = BaseSession.load((EStructArray) elem);
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
                    sessionsNode.set("name", Type.STRING, "sessions");
                    acIndex.update(sessionsNode);
                } else {
                    sessionsNode = sessions[0];
                }
                int i = 0;
                for (BaseSession session : _sessionIdSessions.values()) {
                    EStructArray securityGroupContainer = (EStructArray) sessionsNode.getOrCreateAt(i++, Type.ESTRUCT_ARRAY);
                    session.save(securityGroupContainer);
                }
                _graph.save(done);
            }, "sessions");
        });
    }

}
