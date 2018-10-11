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
package ml.regression;

import org.junit.Test;
import org.mwg.*;
import org.mwg.internal.scheduler.NoopScheduler;
import org.mwg.mlx.MLXPlugin;
import org.mwg.mlx.algorithm.AbstractLinearRegressionNode;
import org.mwg.mlx.algorithm.regression.LinearRegressionNode;

import static org.junit.Assert.assertTrue;

public class LinearRegressionNodeTest extends AbstractLinearRegressionTest {

    @Test
    public void testNormalPrecise() {
        final Graph graph = new GraphBuilder().withPlugin(new MLXPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                LinearRegressionNode lrNode = (LinearRegressionNode) graph.newTypedNode(0, 0, LinearRegressionNode.NAME);
                standardSettings(lrNode);
                RegressionJumpCallback rjc = runThroughDummyDataset(lrNode, false);
                lrNode.free();
                graph.disconnect(null);

                assertTrue(Math.abs(rjc.coefs[0] - 2) < eps);
                assertTrue(Math.abs(rjc.intercept - 1) < eps);
                assertTrue(rjc.bufferError < eps);
                assertTrue(rjc.l2Reg < eps);
            }
        });

    }


    @Test
    public void testNormalPrecise2() {
        final Graph graph = new GraphBuilder().withPlugin(new MLXPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                LinearRegressionNode lrNode = (LinearRegressionNode) graph.newTypedNode(0, 0, LinearRegressionNode.NAME);
                standardSettings(lrNode);
                RegressionJumpCallback rjc = runThroughDummyDataset(lrNode, true);
                lrNode.free();
                graph.disconnect(null);

                assertTrue(Math.abs(rjc.intercept + 0.5) < eps);
                assertTrue(Math.abs(rjc.coefs[0] - 0.5) < eps);
                assertTrue(rjc.bufferError < eps);
            }
        });

    }


    @Test
    public void testSuddenError() {
        //This test fails only on crash. Otherwise, it is just for
        final Graph graph = new GraphBuilder().withPlugin(new MLXPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                LinearRegressionNode lrNode = (LinearRegressionNode) graph.newTypedNode(0, 0, LinearRegressionNode.NAME);
                standardSettings(lrNode);
                RegressionJumpCallback rjc = runThroughDummyDataset(lrNode, false);

                rjc.value = new double[]{6};
                rjc.response = 1013;
                lrNode.jump(dummyDataset1.length, new Callback<Node>() {
                    @Override
                    public void on(Node result) {
                        rjc.on((AbstractLinearRegressionNode) result);
                    }
                });
                assertTrue(rjc.bootstrapMode);

                lrNode.free();
                graph.disconnect(null);

                assertTrue(Math.abs(rjc.coefs[0]  - 144.9) < 1);
                assertTrue(Math.abs(rjc.intercept + 332.8) < 1);
                assertTrue(Math.abs(rjc.bufferError - 79349.32) < 20);
            }
        });
    }

    @Test
    public void testTooLargeRegularization() {
        //This test fails only on crash. Otherwise, it is just for
        final Graph graph = new GraphBuilder().withPlugin(new MLXPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                double resid = 0;
                for (int i = 0; i < dummyDataset1.length; i++) {
                    resid += (dummyDataset1[i][1] - 6) * (dummyDataset1[i][1] - 6);
                }

                LinearRegressionNode lrNode = (LinearRegressionNode) graph.newTypedNode(0, 0, LinearRegressionNode.NAME);
                standardSettings(lrNode);
                lrNode.setProperty(LinearRegressionNode.L2_COEF_KEY, Type.DOUBLE, 1000000000.0);

                RegressionJumpCallback rjc = runThroughDummyDataset(lrNode, false);

                lrNode.free();
                graph.disconnect(null);

                assertTrue(Math.abs(rjc.coefs[0] - 0) < eps);
                assertTrue(Math.abs(rjc.intercept - 6) < eps);
                assertTrue(Math.abs(rjc.bufferError - (resid / 6)) < eps);
            }
        });
    }

}
