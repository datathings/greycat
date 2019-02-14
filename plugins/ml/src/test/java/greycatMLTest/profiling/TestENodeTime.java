package greycatMLTest.profiling;

import greycat.*;
import greycat.ml.HelperForTest;
import greycat.ml.profiling.GaussianWrapper;
import greycat.plugin.Job;
import greycat.struct.DoubleArray;
import greycat.struct.EStruct;
import greycat.struct.EStructArray;
import org.junit.Assert;
import org.junit.Test;

public class TestENodeTime {
    @Test
    public void Test() {
        Graph graph = GraphBuilder
                .newBuilder()
                .withProxyUsage(false)
                .build();

        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                double[] key = {1.1};
                int dataset =12;

                Node node = graph.newNode(0, 0);
                node.setTimeSensitivity(3,0);


                DeferCounter dc = graph.newCounter(dataset);

                for (int i = 0; i < dataset; i++) {
                    int finalI = i;
                    node.travel(0, i, new Callback<Node>() {
                        @Override
                        public void on(Node result) {
                            result = result.rephase();

                            EStructArray eg = (EStructArray) result.getOrCreate("profile", Type.ESTRUCT_ARRAY);
                            if (eg.root() == null) {
                                eg.setRoot(eg.newEStruct());
                            }

                            GaussianWrapper gw = new GaussianWrapper(eg.root());
                            key[0]+=1.1;
                            gw.learn(key);


                            result.free();
                            dc.count();
                        }
                    });
                }

                dc.then(new Job() {
                    @Override
                    public void run() {
                        DeferCounter dc2 = graph.newCounter(dataset);

                        for (int i = 0; i < dataset; i++) {
                            node.travel(0, i, new Callback<Node>() {
                                @Override
                                public void on(Node result) {
                                    EStructArray eg = (EStructArray) result.getOrCreate("profile", Type.ESTRUCT_ARRAY);
                                    GaussianWrapper gw = new GaussianWrapper(eg.root());

                                    System.out.println(gw.getTotal() + " "+gw.getSum()[0]);
                                    result.free();
                                    dc2.count();
                                }
                            });
                        }

                        dc2.then(new Job() {
                            @Override
                            public void run() {
                                graph.disconnect(new Callback<Boolean>() {
                                    @Override
                                    public void on(Boolean result) {
                                        System.out.println("test done");
                                    }
                                });
                            }
                        });
                    }
                });


            }
        });

    }
}
