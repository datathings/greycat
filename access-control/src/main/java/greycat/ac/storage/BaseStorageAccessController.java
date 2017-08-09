/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.ac.storage;

import greycat.*;
import greycat.ac.AccessControlManager;
import greycat.ac.Session;
import greycat.internal.heap.HeapBuffer;
import greycat.plugin.Storage;
import greycat.struct.Buffer;
import greycat.utility.Base64;

/**
 * Created by Gregory NAIN on 03/08/2017.
 */
public class BaseStorageAccessController implements Storage {

    private static final String AUTH_PARAM_KEY = "gc-auth-key";

    private Storage _secured;
    private Graph _graph;
    private AccessControlManager _acm;

    public BaseStorageAccessController(Storage toSecure, AccessControlManager manager) {
        if (!Validator.validate()) {
            System.exit(-1);
        }
        this._secured = toSecure;
        this._acm = manager;
    }

    @Override
    public void get(Buffer keys, Callback<Buffer> callback) {
        _secured.get(keys, proxyResult -> {
            final Buffer filtered = filter_get(proxyResult);
            proxyResult.free();
            callback.on(filtered);
        });
    }

    @Override
    public final void put(Buffer stream, Callback<Boolean> callback) {
        final Buffer filtered = filter_put(stream);
        _secured.put(filtered, result -> {
            filtered.free();
            callback.on(result);
        });
    }

    @Override
    public final void putSilent(Buffer stream, Callback<Buffer> callback) {
        final Buffer filtered = filter_put(stream);
        _secured.putSilent(filtered, result -> {
            filtered.free();
            callback.on(result);
        });
    }

    @Override
    public void remove(Buffer buffer, Callback<Boolean> callback) {
        _secured.remove(buffer, callback);
    }

    @Override
    public void connect(Graph graph, Callback<Boolean> callback) {
        if (graph.getProperty(AUTH_PARAM_KEY) != null) {
            this._graph = graph;
            callback.on(true);
        } else {
            System.err.println("Session ID not found on graph. Impossible to connect");
            callback.on(false);
        }
    }

    @Override
    public void lock(Callback<Buffer> callback) {
        _secured.lock(callback);
    }

    @Override
    public void unlock(Buffer buffer, Callback<Boolean> callback) {
        _secured.unlock(buffer, callback);
    }

    @Override
    public void disconnect(Callback<Boolean> callback) {
        _graph = null;
        callback.on(true);
    }

    @Override
    public void listen(Callback<Buffer> callback) {
        _secured.listen(callback);
    }

    private Buffer filter_get(Buffer in) {
        Session session = _acm.getSessionsManager().getSession((String) _graph.getProperty(AUTH_PARAM_KEY));
        long uid = session.uid();
        final Buffer result = new HeapBuffer();
        long max = in.length();
        long cursor = 0;
        int group = 0;
        long previous = 0;
        while (cursor < max) {
            byte elem = in.read(cursor);
            switch (elem) {
                case Constants.CHUNK_META_SEP:
                    group = Base64.decodeToIntWithBounds(in, previous, cursor);
                    break;
                case Constants.BUFFER_SEP:
                    if (_acm.canRead(uid, group)) {
                        result.writeAll(in.slice(previous, cursor));
                    } else {
                        result.write(Constants.BUFFER_SEP);
                    }
                    group = 0;
                    previous = cursor + 1;
                    break;
            }
            cursor++;
        }
        if (_acm.canRead(uid, group)) {
            if (cursor > previous) {
                result.writeAll(in.slice(previous, cursor - 1));
            }
        } else {
            result.write(Constants.BUFFER_SEP);
        }
        return result;
    }

    private Buffer filter_put(Buffer in) {
        Session session = _acm.getSessionsManager().getSession((String) _graph.getProperty(AUTH_PARAM_KEY));
        long uid = session.uid();
        final Buffer result = new HeapBuffer();
        long max = in.length();
        long cursor = 0;
        int group = 0;
        long previous = 0;

        long previous_key = -1;

        while (cursor < max) {
            byte elem = in.read(cursor);
            switch (elem) {
                case Constants.CHUNK_META_SEP:
                    group = Base64.decodeToIntWithBounds(in, previous, cursor);
                    break;
                case Constants.BUFFER_SEP:
                    if (previous_key == -1) {
                        previous_key = previous;
                    } else {

                        if (_acm.canWrite(uid, group)) {
                            result.writeAll(in.slice(previous_key, cursor));
                        }
                        previous_key = -1;
                    }
                    group = 0;
                    previous = cursor + 1;
                    break;
            }
            cursor++;
        }

        if (previous_key != -1 && _acm.canWrite(uid, group)) {
            result.writeAll(in.slice(previous_key, cursor - 1));
        }
        return result;
    }
}
