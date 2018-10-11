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
package org.mwg.mlx.algorithm;

import org.mwg.Callback;
import org.mwg.Graph;
import org.mwg.Type;
import org.mwg.ml.RegressionNode;
import org.mwg.plugin.NodeState;


/**
 * Created by andre on 5/4/2016.
 */
public abstract class AbstractAnySlidingWindowManagingRegressionNode extends AbstractAnySlidingWindowManagingNode implements RegressionNode {

    protected static final double[] INTERNAL_RESULTS_BUFFER_DEF = new double[0];

    public AbstractAnySlidingWindowManagingRegressionNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    protected abstract double predictValue(NodeState state, double value[]);

    /**
     * Adds new value to the buffer. Connotations change depending on whether the node is in bootstrap mode or not.
     *
     * @param value New value to add; {@code null} disallowed
     * @param result New result value
     * @return New value of bootsrap mode
     */
    protected boolean addValue(double value[], double result) {
        illegalArgumentIfFalse(value != null, "Value must be not null");

        NodeState state = unphasedState();
        boolean bootstrapMode = state.getFromKeyWithDefault(BOOTSTRAP_MODE_KEY, BOOTSTRAP_MODE_DEF);

        if (bootstrapMode) {
            return addValueBootstrap(state, value, result);
        }
        return addValueNoBootstrap(state, value, result);

    }

    /**
     * Adds value to result buffer, deletes first value(s) if necessary.
     *
     * @param state Current state to get properties and set result buffer
     * @param result New result (Y) value
     * @param bootstrapMode Whether we are in bootstrap mode
     * @return new result buffer
     */
    protected static double[] adjustResultBuffer(NodeState state, double result, boolean bootstrapMode){
        double resultBuffer[] = state.getFromKeyWithDefault(INTERNAL_RESULTS_BUFFER_KEY, INTERNAL_RESULTS_BUFFER_DEF);

        //So adding 1 value to the end and removing (currentBufferLength + 1) - maxBufferLength from the beginning.
        final int maxResultBufferLength = state.getFromKeyWithDefault(BUFFER_SIZE_KEY, BUFFER_SIZE_DEF);
        final int numValuesToRemoveFromBeginning = bootstrapMode ? 0 : Math.max(0, resultBuffer.length + 1 - maxResultBufferLength);

        double newBuffer[] = new double[resultBuffer.length + 1 - numValuesToRemoveFromBeginning];
        //Setting first values
        System.arraycopy(resultBuffer, numValuesToRemoveFromBeginning, newBuffer, 0, newBuffer.length - 1);
        newBuffer[newBuffer.length-1] = result;
        state.setFromKey(INTERNAL_RESULTS_BUFFER_KEY, Type.DOUBLE_ARRAY, newBuffer);
        return newBuffer;
    }

    /**
     * Gets error metrics by comparing expected reuslts (from results buffer) and actual results (from model)
     *
     * @param state State to get properties
     * @param valueBuffer Previous values (X)
     * @param resultBuffer Previous results (Y)
     * @return Error measure
     */
    protected abstract double getBufferError(NodeState state, double valueBuffer[], double resultBuffer[]);

    /**
     * Adds value to buffer, assuming there is no bootstrap mode currently.
     *
     * @param state State to get/set properties
     * @param value New parameter values
     * @param result new result (Y)
     * @return New bootstrap state
     */
    protected boolean addValueNoBootstrap(NodeState state, double value[], double result) {
        double newBuffer[] = adjustValueBuffer(state, value, false);
        double newResultBuffer[] = AbstractAnySlidingWindowManagingRegressionNode.adjustResultBuffer(state, result, false);

        //Predict for each value in the buffer. Calculate percentage of errors.
        double errorInBuffer = getBufferError(state, newBuffer, newResultBuffer);
        double higherErrorThreshold = state.getFromKeyWithDefault(HIGH_ERROR_THRESH_KEY, HIGH_ERROR_THRESH_DEF);
        if (errorInBuffer > higherErrorThreshold) {
            setBootstrapMode(state, true); //If number of errors is above higher threshold, get into the bootstrap
            updateModelParameters(state, newBuffer, newResultBuffer, value, result);
            return true;
        }
        return false;
    }

    /**
     * Adds new value to the buffer. Gaussian model is regenerated.
     *
     * @param state State to get/set properties
     * @param value New value to add; {@code null} disallowed
     * @param result New result.
     * @return New bootstrap state
     */
    protected boolean addValueBootstrap(NodeState state, double value[], double result) {
        double newBuffer[] = adjustValueBuffer(state, value, true);
        double newResultBuffer[] = AbstractAnySlidingWindowManagingRegressionNode.adjustResultBuffer(state, result, true);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void learn(final double output, final Callback<Boolean> callback) {
        extractFeatures(new Callback<double[]>() {
            @Override
            public void on(double[] result) {
                boolean outcome = addValue(result, output);
                callback.on(outcome);
            }
        });
    }


    /**
     * Updates ML model parameters with respect to new values.
     *
     * @param state State to get/set properties
     * @param valueBuffer Old parameter values
     * @param resultBuffer Old results
     * @param value New parameter values
     * @param outcome New result
     */
    protected abstract void updateModelParameters(NodeState state, double valueBuffer[], double resultBuffer[], double value[], double outcome);

    public void extrapolate(final Callback<Double> callback) {
        extractFeatures(new Callback<double[]>() {
            @Override
            public void on(double[] result) {
                double outcome = predictValue(unphasedState(), result);
                callback.on(outcome);
            }
        });
    }
}
