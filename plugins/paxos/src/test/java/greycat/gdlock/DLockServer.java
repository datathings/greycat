package greycat.gdlock;

import greycat.Config;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.rocksdb.RocksDBStorage;
import greycat.websocket.WSSharedServer;
import io.atomix.AtomixReplica;
import io.atomix.catalyst.transport.Address;

public class DLockServer {

    public static void main(String[] args) {
        Graph g = GraphBuilder.newBuilder().withStorage(new RocksDBStorage("data")).withMemorySize(Config.cacheSize).build();
        g.connect(connectionResult -> {
            WSSharedServer wss = new WSSharedServer(g, 8090);
            wss.start();
            System.out.println("GreyCat WebSocket Started!");
        });
    }

}
