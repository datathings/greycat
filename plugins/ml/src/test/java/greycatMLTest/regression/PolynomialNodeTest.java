/**
 * Copyright 2017-2019 The GreyCat Authors.  All rights reserved.
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
package greycatMLTest.regression;

import greycat.*;
import greycat.ml.MLPlugin;
import greycat.ml.regression.PolynomialNode;
import greycat.scheduler.NoopScheduler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class PolynomialNodeTest {

    private Graph graph;

    public static void testPoly(long[] times, final double[] values, final int numOfPoly, final Graph graph) {
        final double precision = Defaults.PRECISION;
        final int size = Defaults.SIZE;
        PolynomialNode polynomialNode = (PolynomialNode) graph.newTypedNode(0, times[0], PolynomialNode.NAME);
        polynomialNode.set(PolynomialNode.PRECISION, Type.DOUBLE, precision);

        for (int i = 0; i < size; i++) {
            final int ia = i;
            polynomialNode.travelInTime(times[ia], new Callback<PolynomialNode>() {
                @Override
                public void on(PolynomialNode result) {
                    result.learn(values[ia], new Callback<Boolean>() {
                        @Override
                        public void on(Boolean result) {

                        }
                    });
                }
            });
        }

        //System.out.println(polynomialNode);

        for (int i = 0; i < size; i++) {
            final int ia = i;
            polynomialNode.travelInTime(times[ia], new Callback<PolynomialNode>() {
                @Override
                public void on(PolynomialNode result) {
                    result.extrapolate(new Callback<Double>() {
                        @Override
                        public void on(Double v) {
                            Assert.assertTrue(Math.abs(values[ia] - v) <= precision);
                        }
                    });
                }
            });
        }

        polynomialNode.timepoints(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, new Callback<long[]>() {
            @Override
            public void on(long[] result) {
                Assert.assertTrue(result.length <= numOfPoly);
            }
        });
    }

    /**
     * Create a {@link PolynomialNode} by setting all {@link Defaults} values
     *
     * @param graph the {@link Graph} from within the {@link PolynomialNode} has to be created
     * @return a {@link PolynomialNode} based on default test values
     */
    private static PolynomialNode createDefaultPolynomialNode(final Graph graph) {
        return (PolynomialNode) graph.newTypedNode(Defaults.WORLD, Defaults.TIME, PolynomialNode.NAME)
                .set(PolynomialNode.MAX_DEGREE, Type.INT, Defaults.MAX_DEGREE)
                .set(PolynomialNode.PRECISION, Type.DOUBLE, Defaults.PRECISION);
    }

    @Before
    public void setup() {
        graph = new GraphBuilder().withPlugin(new MLPlugin()).withScheduler(new NoopScheduler()).build();
    }

    /**
     * @native ts
     */
    @Test
    public void testConstant() {
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                /*
                try {
                    BlasMatrixEngine bme = (BlasMatrixEngine) Matrix.defaultEngine();
                    bme.setBlas(new F2JBlas());
                } catch (Exception ignored) {
                }*/
                final int size = Defaults.SIZE;
                long[] times = new long[size];
                double[] values = new double[size];
                //test degree 0
                for (int i = 0; i < size; i++) {
                    times[i] = i * 10 + 5000;
                    values[i] = 42.0;
                }
                testPoly(times, values, 1, graph);

                //test degree 1
                for (int i = 0; i < size; i++) {
                    values[i] = 3 * i - 20;
                }
                testPoly(times, values, 1, graph);

                //test degree 2
                for (int i = 0; i < size; i++) {
                    values[i] = 3 * i * i - 99 * i - 20;

                }
                testPoly(times, values, 1, graph);

                //test degree 5
                for (int i = 0; i < size; i++) {
                    values[i] = 2 * i * i * i * i * i - 1000 * i - 100000;
                }
                testPoly(times, values, 8, graph);

            }
        });
    }

    /**
     * @native ts
     */
    @Test
    public void testWithoutFuturePrediction() {
        graph.connect(isConnected -> {
            // Given a PolynomialNode with future prediction disabled
            final PolynomialNode node = createDefaultPolynomialNode(graph);

            // When PolynomialNode is trained to learn the 0.0 + t * 2.0 polynomial
            Arrays
                    .asList(0, 1, 2, 5, 8, 10)
                    .forEach(time -> node.travelInTime(
                            time,
                            result -> result.set(PolynomialNode.VALUE, Type.DOUBLE, time * 2.0)
                    ));

            // Then PolynomialNode should not predict future values and keep the last known value
            node.travelInTime(20, result -> Assert.assertEquals(20.0, (double) result.get(PolynomialNode.VALUE), 0.000001));
        });
    }

    /**
     * @native ts
     */
    @Test
    public void testWithFuturePrediction() {
        graph.connect(isConnected -> {
            // Given a PolynomialNode with future prediction enabled
            final PolynomialNode node = createDefaultPolynomialNode(graph);
            node.set(PolynomialNode.FUTURE_PREDICTION, Type.BOOL, true);

            // When PolynomialNode is trained to learn the 0.0 + t * 2.0 polynomial
            Arrays
                    .asList(0, 1, 2, 5, 8, 10)
                    .forEach(time -> node.travelInTime(
                            time,
                            result -> result.set(PolynomialNode.VALUE, Type.DOUBLE, time * 2.0)
                    ));

            // Then PolynomialNode should predict future values
            node.travelInTime(20, result -> Assert.assertEquals(40.0, (double) result.get(PolynomialNode.VALUE), 0.000001));
        });
    }

    /**
     * Default values to apply in test if not precised
     */
    private static class Defaults {
        public static final long WORLD = 0;
        public static final long TIME = 0;
        public static final int SIZE = 100;
        public static final double PRECISION = 0.5;
        public static final int MAX_DEGREE = PolynomialNode.MAX_DEGREE_DEF;
    }

}
