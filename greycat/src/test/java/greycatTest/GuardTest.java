package greycatTest;

import greycat.*;
import greycat.scheduler.NoopScheduler;
import greycatTest.internal.MockStorage;
import org.junit.Assert;
import org.junit.Test;

public class GuardTest {

    @Test
    public void test() {
        MockStorage storage = new MockStorage();
        Graph g = GraphBuilder.newBuilder().withMemorySize(1000).withScheduler(new NoopScheduler()).withStorage(storage).build();
        g.connect(null);

        for (int i = 0; i < 100; i++) {
            Node n = g.newNode(0, 0);
            n.free();
        }
        g.save(null);

        long cleaned = g.space().clean(25);
        Assert.assertEquals(cleaned, 249);
        
        g.disconnect(null);
    }

}
