/**
 * Copyright 2017-2018 The GreyCat Authors.  All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package common.matrix;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.junit.Assert;
import org.junit.Test;
import org.mwg.Callback;
import org.mwg.Graph;
import org.mwg.GraphBuilder;
import org.mwg.Node;
import org.mwg.internal.scheduler.NoopScheduler;
import org.mwg.ml.algorithm.profiling.GaussianMixtureNode;
import org.mwg.ml.common.matrix.operation.Gaussian1D;
import org.mwg.mlx.MLXPlugin;
import org.mwg.struct.Matrix;

import java.util.Random;

public class GaussianProbaTest {
    @Test
    public void Gaussian1D() {
        final Graph graph = new GraphBuilder().withPlugin(new MLXPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                final GaussianMixtureNode gaussianNode = (GaussianMixtureNode) graph.newTypedNode(0, 0, GaussianMixtureNode.NAME);
                final double eps = 1e-7;


                final int total = 16;
                final double[][] train = new double[total][1];
                final Random rand = new Random();

                double sum = 0;
                double sumsquare = 0;
                for (int i = 0; i < 16; i++) {
                    train[i][0] = rand.nextDouble() * 100;
                    sum += train[i][0];
                    sumsquare += train[i][0] * train[i][0];

                    final int finalI = i;
                    gaussianNode.jump(i, new Callback<Node>() {
                        @Override
                        public void on(Node result) {
                            gaussianNode.learnVector(train[finalI], new Callback<Boolean>() {
                                @Override
                                public void on(Boolean result) {

                                }
                            });
                            result.free();
                        }
                    });

                }

                final double finalSum = sum;
                final double finalSumsquare = sumsquare;
                gaussianNode.jump(16, new Callback<GaussianMixtureNode>() {
                    @Override
                    public void on(GaussianMixtureNode result) {
                        double[] avgBatch = result.getAvg();
                        Matrix covBatch = result.getCovariance(avgBatch, null);

                        //System.out.println("Avg: " + avgBatch[0] + " " + sum / total);
                        //System.out.println("Var: " + covBatch[0][0] + " " + Gaussian1D.getCovariance(sum, sumsquare, total));
                        Assert.assertTrue(Math.abs(avgBatch[0] - finalSum / total) < eps);
                        Assert.assertTrue(Math.abs(covBatch.get(0, 0) - Gaussian1D.getCovariance(finalSum, finalSumsquare, total)) < eps);

                        double testvec = rand.nextDouble() * 100;
                        System.out.println("Prob: " + Gaussian1D.getDensity(finalSum, finalSumsquare, total, testvec) + " " + result.getProbability(new double[]{testvec}, null, false));
                        Assert.assertTrue(Math.abs(Gaussian1D.getDensity(finalSum, finalSumsquare, total, testvec) - result.getProbability(new double[]{testvec}, null, false)) < eps);

                    }
                });


            }
        });

    }

    @Test
    public void MultinomialTest() {
        final Graph graph = new GraphBuilder().withPlugin(new MLXPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                GaussianMixtureNode gaussianNodeLive = (GaussianMixtureNode) graph.newTypedNode(0, 0, GaussianMixtureNode.NAME);

                gaussianNodeLive.set(GaussianMixtureNode.FROM, "f1;f2");

                int test = 100;
                int feat = 2;

                double[][] b = new double[test][feat];
                double[] v = new double[feat];
                Random random = new Random();

                for (int i = 0; i < test; i++) {
                    for (int j = 0; j < feat; j++) {
                        v[j] = random.nextDouble() * (1 + 100 * j);
                        b[i][j] = v[j];
                    }
                    gaussianNodeLive.set("f1", b[i][0]);
                    gaussianNodeLive.set("f2", b[i][1]);

                    gaussianNodeLive.learn(new Callback<Boolean>() {
                        @Override
                        public void on(Boolean result) {

                        }
                    });
                }

                double[] ravg = gaussianNodeLive.getAvg();
                Matrix rcovData = gaussianNodeLive.getCovariance(ravg, null);
                
                double[][] temp = new double[rcovData.rows()][];
                for (int i = 0; i < rcovData.rows(); i++) {
                    temp[i] = new double[rcovData.columns()];
                    for (int j = 0; j < rcovData.columns(); j++) {
                        temp[i][j] = rcovData.get(i, j);
                    }
                }

                //Test probability calculation.
                MultivariateNormalDistribution apache = new MultivariateNormalDistribution(ravg, temp);

                double eps = 1e-8;
                double d = apache.density(v);
                //System.out.println("apache: " + d);

                double y = gaussianNodeLive.getProbability(v, null, false);
                //System.out.println("live: " + y);


                Assert.assertTrue(Math.abs(d - y) < eps);

            }
        });
    }

    @Test
    public void Singularity() {
        final Graph graph = new GraphBuilder().withPlugin(new MLXPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                double[] data = new double[3];
                double[] datan = new double[4];

                Random rand = new Random();

                GaussianMixtureNode node1 = (GaussianMixtureNode) graph.newTypedNode(0, 0, GaussianMixtureNode.NAME);
                GaussianMixtureNode node2 = (GaussianMixtureNode) graph.newTypedNode(0, 0, GaussianMixtureNode.NAME);

                node1.set(GaussianMixtureNode.FROM, "f1;f2;f3");
                node2.set(GaussianMixtureNode.FROM, "f1;f2;f3;f4");

                for (int i = 0; i < 1000; i++) {
                    data[0] = 8 + rand.nextDouble() * 4; //avg =10, [8,12]
                    data[1] = 90 + rand.nextDouble() * 20; //avg=100 [90,110]
                    data[2] = -60 + rand.nextDouble() * 20; //avg=-50 [-60,-40]

                    datan[0] = data[0];
                    datan[1] = data[1];
                    datan[2] = data[2];
                    datan[3] = 0 * data[0] + 0 * data[1] + 0 * data[2];

                    node1.set("f1", data[0]);
                    node1.set("f2", data[1]);
                    node1.set("f3", data[2]);

                    node2.set("f1", datan[0]);
                    node2.set("f2", datan[1]);
                    node2.set("f3", datan[2]);
                    node2.set("f4", datan[3]);


                    node1.learn(new Callback<Boolean>() {
                        @Override
                        public void on(Boolean result) {

                        }
                    });

                    node2.learn(new Callback<Boolean>() {
                        @Override
                        public void on(Boolean result) {

                        }
                    });
                }

                double[] avg = node1.getAvg();
                double[] avg2 = node2.getAvg();

                //printd(avg);
                //printd(avg2);

                data[0] = 10;
                data[1] = 100;
                data[2] = -60;

                datan[0] = data[0];
                datan[1] = data[1];
                datan[2] = data[2];
                datan[3] = 0 * data[0] + 0 * data[1] + 0 * data[2];

                double p = node1.getProbability(avg, null, false);
                double p2 = node2.getProbability(avg2, null, false);
                Assert.assertTrue(Math.abs(p - p2) < 1e-5);
                //System.out.println("p1: " + p);
                // System.out.println("p2: " + p2);


            }

            private void printd(double[] avg) {
                for (double d : avg) {
                    System.out.print(d + " ");
                }
                System.out.println();

            }
        });
    }
}
