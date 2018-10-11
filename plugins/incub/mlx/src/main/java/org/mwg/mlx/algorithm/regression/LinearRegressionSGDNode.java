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
package org.mwg.mlx.algorithm.regression;

import org.mwg.Graph;
import org.mwg.Type;
import org.mwg.mlx.algorithm.AbstractLRGradientDescentNode;
import org.mwg.plugin.NodeState;

/**
 * Linear regression node based on stochastic gradient descent.
 * Likelz to be much faster than non-SGD, but might take longer time to converge.
 * <p>
 * Created by andre on 4/29/2016.
 */
public class LinearRegressionSGDNode extends AbstractLRGradientDescentNode {

    public static final String NAME = "LinearRegressionStochasticGradientDescent";

    public LinearRegressionSGDNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    //We don't need large buffer. Gradient algorithm uses sliding window only for evaluations.
    //Therefore, we have to keep the window size even in bootstrap mode
    @Override
    protected boolean addValueBootstrap(NodeState state, double value[], double result) {
        //ONLY DIFFERENCE: adjusting as if there was no bootstrap
        double newBuffer[] = LinearRegressionSGDNode.adjustValueBuffer(state, value, false);
        double newResultBuffer[] = LinearRegressionSGDNode.adjustResultBuffer(state, result, false);
        boolean newBootstrap = true;

        if (newResultBuffer.length >= getMaxBufferLength()) {
            //Predict for each value in the buffer. Calculate percentage of errors.
            double errorInBuffer = getBufferError(state, newBuffer, newResultBuffer);
            double lowerErrorThreshold = state.getFromKeyWithDefault(LOW_ERROR_THRESH_KEY, LOW_ERROR_THRESH_DEF);
            if (errorInBuffer <= lowerErrorThreshold) {
                setBootstrapMode(state, false); //If number of errors is below lower threshold, get out of bootstrap
                newBootstrap = false;
            }
        }
        updateModelParameters(state, newBuffer, newResultBuffer, value, result);

        return newBootstrap;
    }

    @Override
    protected void updateModelParameters(NodeState state, double valueBuffer[], double resultBuffer[], double value[], double response) {
        //Value should be already added to buffer by that time
        int dims = state.getFromKeyWithDefault(INPUT_DIM_KEY, INPUT_DIM_UNKNOWN);
        if (dims == INPUT_DIM_UNKNOWN) {
            dims = value.length;
            state.setFromKey(INPUT_DIM_KEY, Type.INT, dims);
        }
        final double alpha = state.getFromKeyWithDefault(LEARNING_RATE_KEY, DEFAULT_LEARNING_RATE);
        double lambda = state.getFromKeyWithDefault(L2_COEF_KEY, L2_COEF_DEF);

        //Get coefficients. If they are of length 0, initialize with random.
        double coefs[] = state.getFromKeyWithDefault(COEFFICIENTS_KEY, COEFFICIENTS_DEF);
        double intercept = state.getFromKeyWithDefault(INTERCEPT_KEY, INTERCEPT_DEF);
        if (coefs.length == 0) {
            coefs = new double[dims];
        }

        //For batch gradient descent:
        //Theta_j = theta_j - alpha * (1/m * sum( h(X_i) - y_i )*X_j_i + lambda/m * theta_j)

        double h = 0;
        for (int j = 0; j < dims; j++) {
            h += coefs[j] * value[j];
        }
        h += intercept;

        //For stochastic gradient descent:
        //Theta_j = theta_j - alpha * ( (h(X_i) - y_i )*X_j + lambda * theta_j)
        for (int j = 0; j < dims; j++) {
            coefs[j] -= alpha * ((h - response) * value[j] + lambda * coefs[j]);
        }
        //Intercept: value is 1, L2 reg-n not used.
        intercept -= alpha * (h - response);
        state.setFromKey(INTERCEPT_KEY, Type.DOUBLE, intercept);
        state.setFromKey(COEFFICIENTS_KEY, Type.DOUBLE_ARRAY, coefs);
    }

}

