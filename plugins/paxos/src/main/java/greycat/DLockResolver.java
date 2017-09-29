package greycat;

import greycat.chunk.ChunkSpace;
import greycat.internal.MWResolver;
import greycat.plugin.Storage;
import greycat.websocket.WSClient;

import java.util.concurrent.CountDownLatch;

public class DLockResolver extends MWResolver {

    private WSClient wsLock;

    public DLockResolver(Storage p_storage, ChunkSpace p_space, Graph p_graph) {
        super(p_storage, p_space, p_graph);
        wsLock = (WSClient) p_storage;
    }


    @Override
    public void init() {
        super.init();
    }

    @Override
    public void free() {
        super.free();
        System.out.println("Exited Paxos Consensus");
    }

    @Override
    public void externalLock(Node node) {
        super.externalLock(node);

        CountDownLatch wait = new CountDownLatch(1);
        wsLock.extLock(node.id(), new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                wait.countDown();
            }
        });
        try {
            wait.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void externalUnlock(Node node) {
        super.externalUnlock(node);

        CountDownLatch wait = new CountDownLatch(1);
        wsLock.extUnlock(node.id(), new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                wait.countDown();
            }
        });
        try {
            wait.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
