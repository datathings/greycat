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
import org.mwg.mlx.algorithm.AbstractLinearRegressionNode;
import org.mwg.mlx.algorithm.regression.LinearRegressionBatchGDNode;
import org.mwg.mlx.algorithm.regression.LinearRegressionSGDNode;

import static org.junit.Assert.assertTrue;

/**
 * Created by andre on 5/10/2016.
 */
public class LinearRegressionSGDNodeTest extends AbstractLinearRegressionTest{

    @Test
    public void testNormalSGD() {
        final Graph graph = new GraphBuilder()
                //.withOffHeapMemory()
                //.withMemorySize(20_000)
                //.withAutoSave(10000)
                //.withStorage(new LevelDBStorage("data"))
                .withPlugin(new MLXPlugin()).withScheduler(new NoopScheduler())
                .build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                LinearRegressionSGDNode lrNode = (LinearRegressionSGDNode) graph.newTypedNode(0, 0, LinearRegressionSGDNode.NAME);

                final int BUFFER_SIZE = 3000;
                lrNode.setProperty(AbstractLinearRegressionNode.BUFFER_SIZE_KEY, Type.INT, BUFFER_SIZE);
                lrNode.setProperty(AbstractLinearRegressionNode.LOW_ERROR_THRESH_KEY, Type.DOUBLE, 0.1);
                lrNode.setProperty(AbstractLinearRegressionNode.HIGH_ERROR_THRESH_KEY, Type.DOUBLE, 0.001);
                lrNode.setProperty(LinearRegressionBatchGDNode.LEARNING_RATE_KEY, Type.DOUBLE, 0.01);
                lrNode.set(AbstractMLNode.FROM, FEATURE);

                AbstractLinearRegressionTest.RegressionJumpCallback rjc = runRandom(lrNode, BUFFER_SIZE+500);

                lrNode.free();
                graph.disconnect(null);

                assertTrue(Math.abs(rjc.coefs[0] - 2) < 1e-3);
                assertTrue(Math.abs(rjc.intercept - 1) < 2e-3);
                assertTrue(rjc.bufferError < eps);
                assertTrue(rjc.l2Reg < eps);
            }
        });
    }
}
