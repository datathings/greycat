package greycat.raft;

import greycat.Config;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.rocksdb.RocksDBStorage;
import greycat.websocket.WSSharedServer;
import io.atomix.AtomixReplica;
import io.atomix.catalyst.transport.Address;

public class RaftServer {

    public static void main(String[] args) {
        Graph g = GraphBuilder.newBuilder().withStorage(new RocksDBStorage("paxos-data")).withMemorySize(Config.cacheSize).build();
        g.connect(connectionResult -> {

            AtomixReplica replica = AtomixReplica.builder(new Address(Config.master, Config.masterPort)).build();
            replica.bootstrap().join();
            System.out.println("Atomix Server Started!");

            WSSharedServer wss = new WSSharedServer(g, 8090);
            wss.start();
            System.out.println("GreyCat WebSocket Started!");

        });
    }

}
