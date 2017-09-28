package greycat;

import greycat.chunk.ChunkSpace;
import greycat.internal.MWResolver;
import greycat.plugin.Storage;
import io.atomix.AtomixReplica;
import io.atomix.catalyst.transport.Address;
import io.atomix.concurrent.DistributedLock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PaxosResolver extends MWResolver {

    public PaxosResolver(Storage p_storage, ChunkSpace p_space, Graph p_graph) {
        super(p_storage, p_space, p_graph);
    }

    private AtomixReplica replica;

    private Map<Long, DistributedLock> locks = new ConcurrentHashMap<Long, DistributedLock>();

    @Override
    public void init() {
        super.init();
        AtomixReplica.Builder builder = AtomixReplica.builder(new Address("localhost", 8700));
        replica = builder.build();
        replica.bootstrap().join();
    }

    @Override
    public void externalLock(Node node) {
        super.externalLock(node);
        String key = node.id() + "_lock";
        locks.put(node.id(), replica.getLock(key).join());
    }

    @Override
    public void externalUnlock(Node node) {
        super.externalUnlock(node);
        locks.get(node.id()).unlock().join();
    }
}
