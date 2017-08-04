package greycat.storage.ac;

import greycat.Callback;
import greycat.Graph;
import greycat.Validator;
import greycat.auth.IdentityManager;
import greycat.plugin.Storage;
import greycat.struct.Buffer;

/**
 * Created by Gregory NAIN on 03/08/2017.
 */
public class StorageAccessControler implements Storage {

    private static final String AUTH_PARAM_KEY = "gc-auth-key";

    private Storage _secured;
    private Graph _graph;
    private IdentityManager _identityManager;
    private AccessControlManager _accessControlManager;

    public StorageAccessControler(Storage toSecure, IdentityManager identityManager, AccessControlManager manager) {
        if (!Validator.validate()) {
            System.exit(-1);
        }
        this._secured = toSecure;
        this._identityManager = identityManager;
        this._accessControlManager = manager;
    }

    @Override
    public void get(Buffer buffer, Callback<Buffer> callback) {

    }

    @Override
    public void put(Buffer buffer, Callback<Boolean> callback) {

    }

    @Override
    public void putSilent(Buffer buffer, Callback<Buffer> callback) {

    }

    @Override
    public void remove(Buffer buffer, Callback<Boolean> callback) {

    }

    @Override
    public void connect(Graph graph, Callback<Boolean> callback) {
        if (graph.getProperty(AUTH_PARAM_KEY) != null) {
            this._graph = graph;
            _accessControlManager.init(callback);
        } else {
            System.err.println("Session ID not found on graph. Impossible to connect");
            callback.on(false);
        }
    }

    @Override
    public void lock(Callback<Buffer> callback) {

    }

    @Override
    public void unlock(Buffer buffer, Callback<Boolean> callback) {

    }

    @Override
    public void disconnect(Callback<Boolean> callback) {

    }

    @Override
    public void listen(Callback<Buffer> callback) {

    }
}
