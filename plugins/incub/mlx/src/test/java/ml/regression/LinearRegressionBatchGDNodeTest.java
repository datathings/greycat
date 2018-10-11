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
import org.mwg.Callback;
import org.mwg.Graph;
import org.mwg.GraphBuilder;
import org.mwg.Type;
import org.mwg.internal.scheduler.NoopScheduler;
import org.mwg.ml.AbstractMLNode;
import org.mwg.mlx.MLXPlugin;
import org.mwg.mlx.algorithm.AbstractLRGradientDescentNode;
import org.mwg.mlx.algorithm.AbstractLinearRegressionNode;
import org.mwg.mlx.algorithm.regression.LinearRegressionBatchGDNode;

import static org.junit.Assert.assertTrue;

public class LinearRegressionBatchGDNodeTest extends AbstractLinearRegressionTest {

    @Test
    public void testNormalBatchGDIterationCountStop() {
        final Graph graph = new GraphBuilder().withPlugin(new MLXPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                LinearRegressionBatchGDNode lrNode = (LinearRegressionBatchGDNode) graph.newTypedNode(0, 0, LinearRegressionBatchGDNode.NAME);
                lrNode.setProperty(AbstractLinearRegressionNode.BUFFER_SIZE_KEY, Type.INT, 50);
                lrNode.setProperty(AbstractLinearRegressionNode.LOW_ERROR_THRESH_KEY, Type.DOUBLE, 0.01);
                lrNode.setProperty(AbstractLinearRegressionNode.HIGH_ERROR_THRESH_KEY, Type.DOUBLE, 0.01);
                lrNode.setProperty(LinearRegressionBatchGDNode.GD_ITERATION_THRESH_KEY, Type.INT, 3000);
                lrNode.setProperty(AbstractLRGradientDescentNode.LEARNING_RATE_KEY, Type.DOUBLE, 0.001);
                lrNode.set(AbstractMLNode.FROM, FEATURE);

                AbstractLinearRegressionTest.RegressionJumpCallback rjc = runRandom(lrNode, 100);

                lrNode.free();
                graph.disconnect(null);

                assertTrue(Math.abs(rjc.coefs[0] - 2) < 5e-3);
                assertTrue(Math.abs(rjc.intercept - 1) < 1.6e-2);
                assertTrue(rjc.bufferError < 1e-4);
                assertTrue(rjc.l2Reg < 1e-4);
            }
        });

    }

    @Test
    public void testNormalBatchGDErrorThresholdStop() {
        final Graph graph = new GraphBuilder().withPlugin(new MLXPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                LinearRegressionBatchGDNode lrNode = (LinearRegressionBatchGDNode) graph.newTypedNode(0, 0, LinearRegressionBatchGDNode.NAME);
                lrNode.setProperty(AbstractLinearRegressionNode.BUFFER_SIZE_KEY, Type.INT, 10);
                lrNode.setProperty(AbstractLinearRegressionNode.LOW_ERROR_THRESH_KEY, Type.DOUBLE, 1e-4);
                lrNode.setProperty(AbstractLinearRegressionNode.HIGH_ERROR_THRESH_KEY, Type.DOUBLE, 1e-4);
                lrNode.setProperty(LinearRegressionBatchGDNode.GD_ITERATION_THRESH_KEY, Type.INT, -1);
                lrNode.setProperty(LinearRegressionBatchGDNode.GD_ERROR_THRESH_KEY, Type.DOUBLE, 5e-9);
                lrNode.setProperty(LinearRegressionBatchGDNode.LEARNING_RATE_KEY, Type.DOUBLE, 0.001);
                lrNode.set(AbstractMLNode.FROM, FEATURE);

                AbstractLinearRegressionTest.RegressionJumpCallback rjc = runRandom(lrNode, 16);

                graph.disconnect(null);

                assertTrue(Math.abs(rjc.coefs[0] - 2) < 1e-2);
                assertTrue(Math.abs(rjc.intercept - 1) < 1.5e-2);
                assertTrue(rjc.bufferError < 1e-4);
                assertTrue(rjc.l2Reg < 1e-5);
            }
        });

    }
}
