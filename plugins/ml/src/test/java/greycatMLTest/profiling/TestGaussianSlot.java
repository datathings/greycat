package greycatMLTest.profiling;

import greycat.*;
import greycat.struct.EStructArray;
import org.junit.Test;

public class TestGaussianSlot {
    @Test
    public void TestGaussianSlot(){
        Graph g = GraphBuilder.newBuilder().build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                Node n = g.newNode(0,0);
                EStructArray es= (EStructArray) n.getOrCreate("ss", Type.ESTRUCT_ARRAY);





            }
        });

    }
}
