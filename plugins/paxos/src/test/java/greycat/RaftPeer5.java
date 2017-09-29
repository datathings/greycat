package greycat;

import io.atomix.AtomixReplica;
import io.atomix.catalyst.transport.Address;

public class RaftPeer5 {

    public static void main(String[] args) {
        AtomixReplica.Builder builder = AtomixReplica.builder(new Address("10.187.80.30", 8174));
        AtomixReplica replica = builder.build();
        replica.bootstrap(new Address(Config.master, Config.masterPort)).join();
    }

}
