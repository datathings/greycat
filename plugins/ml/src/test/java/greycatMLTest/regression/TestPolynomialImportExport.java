package greycatMLTest.regression;

import greycat.*;
import greycat.ml.MLPlugin;
import greycat.ml.regression.PolynomialNode;
import greycat.scheduler.NoopScheduler;
import org.junit.Assert;

import java.util.Random;

public class TestPolynomialImportExport {
    public static void main(String[] args) {

        final Graph graph = new GraphBuilder().withPlugin(new MLPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                double precision = 0.00001;
                int size = 10000;

                long seed = 1545436547678348l;
                //Random random = new Random(seed);
                Random random = new Random();
                final double[] values = new double[size];
                final double[] error = new double[size];
                final double[] poly = new double[size];

                PolynomialNode polynomialNode = (PolynomialNode) graph.newTypedNode(0, 1, PolynomialNode.NAME);
                polynomialNode.set(PolynomialNode.PRECISION, Type.DOUBLE, precision);

                long start = System.currentTimeMillis();
                for (int i = 0; i < size; i++) {
                    values[i] = random.nextDouble();
                    final int finalI = i;
                    polynomialNode.travelInTime(i + 1, new Callback<Node>() {
                        @Override
                        public void on(Node result) {
                            PolynomialNode x = (PolynomialNode) result;
                            x.learn(values[finalI], null);
                        }
                    });
                }
                long end = System.currentTimeMillis() - start;
                //System.out.println("total time: " + end + " ms");


                final double[] res = new double[3];


                for (int i = 0; i < size; i++) {
                    final int finalI = i;
                    polynomialNode.travelInTime(i + 1, new Callback<Node>() {
                        @Override
                        public void on(Node result) {
                            PolynomialNode x = (PolynomialNode) result;
                            x.extrapolate(new Callback<Double>() {
                                @Override
                                public void on(Double result) {
                                    poly[finalI] = result;
                                    error[finalI] = Math.abs(result - values[finalI]);
                                    if (error[finalI] > res[0]) {
                                        res[0] = error[finalI];
                                    }
                                    res[1] += error[finalI];
                                }
                            });
                        }
                    });
                }

                polynomialNode.timepoints(0, size + 3, new Callback<long[]>() {
                    @Override
                    public void on(long[] result) {
                        res[2] = result.length;
                    }
                });

                res[1] = res[1] / size;

                Assert.assertTrue(res[0] <= precision);
                Assert.assertTrue(res[2] < size);

                TaskContext ctx = PolynomialNode.export.prepare(graph, polynomialNode, new Callback<TaskResult>() {
                    @Override
                    public void on(TaskResult result) {
                        System.out.println("Done");
                    }
                });

                PolynomialNode.export.executeUsing(ctx);






            }
        });

    }
}
