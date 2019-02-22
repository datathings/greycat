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
package org.mwg.mlx.algorithm.regression;

import org.mwg.Graph;
import org.mwg.Type;
import org.mwg.mlx.algorithm.AbstractLRGradientDescentNode;
import org.mwg.plugin.NodeState;

/**
 * Linear regression node based on stochastic gradient descent.
 * Likelz to be much faster than non-SGD, but might take longer time to converge.
 * Created by andre on 4/29/2016.
 */
public class LinearRegressionBatchGDNode extends AbstractLRGradientDescentNode {

    public static final String NAME = "LinearRegressionBatchGradientDescent";

    public LinearRegressionBatchGDNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    @Override
    protected void updateModelParameters(NodeState state, double valueBuffer[], double resultBuffer[], double value[], double result) {
        //Value should be already added to buffer by that time
        int dims = state.getFromKeyWithDefault(INPUT_DIM_KEY, INPUT_DIM_UNKNOWN);
        if (dims == INPUT_DIM_UNKNOWN) {
            dims = value.length;
            state.setFromKey(INPUT_DIM_KEY, Type.INT, dims);
        }
        final double alpha = state.getFromKeyWithDefault(LEARNING_RATE_KEY, DEFAULT_LEARNING_RATE);
        final double lambda = state.getFromKeyWithDefault(L2_COEF_KEY, L2_COEF_DEF);

        final double gdErrorThresh = state.getFromKeyWithDefault(GD_ERROR_THRESH_KEY, Double.NaN);
        final int gdIterThresh = state.getFromKeyWithDefault(GD_ITERATION_THRESH_KEY, DEFAULT_GD_ITERATIONS_COUNT);

        //Get coefficients. If they are of length 0, initialize with random.
        double coefs[] = state.getFromKeyWithDefault(COEFFICIENTS_KEY, COEFFICIENTS_DEF);
        double intercept = state.getFromKeyWithDefault(INTERCEPT_KEY, INTERCEPT_DEF);
        if (coefs.length == 0) {
            coefs = new double[dims];
            state.setFromKey(COEFFICIENTS_KEY, Type.DOUBLE_ARRAY, coefs);
        }

        //For batch gradient descent:
        //Theta_j = theta_j - alpha * (1/m * sum( h(X_i) - y_i )*X_j_i - lambda/m * theta_j)

        int iterCount = 0;
        boolean exitCase = false;
        double prevError = getBufferError(state, valueBuffer, resultBuffer);
        while (!exitCase) {
            iterCount++;

            int startIndex = 0;
            int index = 0;
            double oldCoefs[] = new double[coefs.length];
            System.arraycopy(coefs, 0, oldCoefs, 0, coefs.length);
            while (startIndex + dims <= valueBuffer.length) {
                double curValue[] = new double[dims];
                System.arraycopy(valueBuffer, startIndex, curValue, 0, dims);

                double h = 0;
                for (int j = 0; j < dims; j++) {
                    h += oldCoefs[j] * curValue[j];
                }
                h += intercept;

                double outcome = resultBuffer[index];
                for (int j = 0; j < dims; j++) {
                    coefs[j] -= alpha * ((h - outcome) * curValue[j]) / resultBuffer.length;
                }
                intercept -= alpha * (h - outcome) / resultBuffer.length;

                startIndex += dims;
                index++;
            }
            for (int j = 0; j < dims; j++) {
                coefs[j] += alpha * lambda * oldCoefs[j];
            }

            state.setFromKey(INTERCEPT_KEY, Type.DOUBLE, intercept);
            state.setFromKey(COEFFICIENTS_KEY, Type.DOUBLE_ARRAY, coefs);
            if (gdErrorThresh > 0) {
                double newError = getBufferError(state, valueBuffer, resultBuffer);
                exitCase = exitCase || ((prevError - newError) < gdErrorThresh);
                prevError = newError;
            }

            if (gdIterThresh > 0) {
                exitCase = exitCase || (iterCount >= gdIterThresh);
            }

            if ((!(gdErrorThresh > 0)) && (!(gdIterThresh > 0))) {
                //Protection against infinite loops. If neither error threshold, nor iteration thresholds are used,
                //run loops once, do not go infinite.
                exitCase = true;
            }
        }
    }


}

