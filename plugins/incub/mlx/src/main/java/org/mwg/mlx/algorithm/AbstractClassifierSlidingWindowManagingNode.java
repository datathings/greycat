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
import org.mwg.ml.ClassificationNode;
import org.mwg.plugin.NodeState;

/**
 * Created by andre on 5/4/2016.
 */
public abstract class AbstractClassifierSlidingWindowManagingNode extends AbstractAnySlidingWindowManagingNode implements ClassificationNode {

    protected static final int[] INTERNAL_RESULTS_BUFFER_DEF = new int[0];

    public AbstractClassifierSlidingWindowManagingNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    /**
     * @param state Node state to get/set value (e.g. model)
     * @param value Input parameters
     * @return Class value prediction for current model and specified input
     */
    protected abstract int predictValue(NodeState state, double value[]);

    /**
     * {@inheritDoc}
     */
    @Override
    public void classify(final Callback<Integer> callback) {
        extractFeatures(new Callback<double[]>() {
            @Override
            public void on(double[] result) {
                int predictedClass = predictValue(unphasedState(), result);
                callback.on(predictedClass);
            }
        });
    }

    /**
     * @param state Node state to get/set properties (e.g. model)
     * @param valueBuffer Input parameter values
     * @param resultBuffer Expected class lables, corresponding to value buffer
     * @return The number of classification error for current model, specified parameters and specified results
     */
    protected int getBufferErrorCount(NodeState state, double valueBuffer[], int resultBuffer[]) {
        //For each value in value buffer
        int startIndex = 0;
        if (resultBuffer.length == 0) {
            return 0;
        }

        final int dims = valueBuffer.length/resultBuffer.length;

        int errorCount = 0;
        int index = 0;
        while (startIndex + dims <= valueBuffer.length) {
            double curValue[] = new double[dims];
            System.arraycopy(valueBuffer, startIndex, curValue, 0, dims);
            int realClass = resultBuffer[index];
            int predictedClass = predictValue(state, curValue);
            errorCount += (realClass != predictedClass) ? 1 : 0;

            //Continue the loop
            startIndex += dims;
            index++;
        }
        return errorCount;
    }

    /**
     * Attribute key - List of known classes
     */
    public static final String KNOWN_CLASSES_LIST_KEY = "_knownClassesList";

    /**
     * Adds new class number to known clases list
     *
     * @param state Node state to get/set properties
     * @param classLabel New class number
     */
    protected void addToKnownClassesList(NodeState state, int classLabel) {
        int[] knownClasses = state.getFromKeyWithDefault(KNOWN_CLASSES_LIST_KEY, new int[0]);
        int[] newKnownClasses = new int[knownClasses.length + 1];
        for (int i = 0; i < knownClasses.length; i++) {
            if (classLabel == knownClasses[i]) {
                return; //Already known. No need to add
            }
            newKnownClasses[i] = knownClasses[i];
        }
        newKnownClasses[knownClasses.length] = classLabel;
        state.setFromKey(KNOWN_CLASSES_LIST_KEY, Type.INT_ARRAY, newKnownClasses);
    }

    /**
     * @param state Node state to get/set values
     * @param value Value under test
     * @param classNum Class under test
     * @return Likelihood estimation that current value belongs to specified class
     */
    protected abstract double getLikelihoodForClass(NodeState state, double value[], int classNum);

    /**
     * Adds value's contribution to total, sum and sum of squares of new model.
     * Does NOT build model yet.
     *
     * @param state Node state to get/set properties
     * @param valueBuffer Old parameter values
     * @param resultBuffer Old results
     * @param value New value
     * @param classNumber Class number corresponding to new value
     */
    protected abstract void updateModelParameters(NodeState state, double[] valueBuffer, int[] resultBuffer, double[] value, int classNumber);

    @Override
    protected void setBootstrapModeHook(NodeState state) {
        //It would have been easy if not for keeping the buffers
        removeAllClasses(state);

        //Now step-by-step build new models
        double valueBuffer[] = state.getFromKeyWithDefault(INTERNAL_VALUE_BUFFER_KEY, new double[0]);
        int resultBuffer[] = state.getFromKeyWithDefault(INTERNAL_RESULTS_BUFFER_KEY, new int[0]);
        int startIndex = 0;
        final int dims = state.getFromKeyWithDefault(INPUT_DIM_KEY, INPUT_DIM_UNKNOWN);
        int i = 0;
        while (startIndex + dims < valueBuffer.length) {
            double curValue[] = new double[dims];
            System.arraycopy(valueBuffer, startIndex, curValue, 0, dims);
            updateModelParameters(state, valueBuffer, resultBuffer, curValue, resultBuffer[i]);
            startIndex += dims;
            i++;
        }
    }

    /**
     * Called automatically before all known classes are removed
     *
     * @param state Node state to get/set properties
     */
    protected abstract void removeAllClassesHook(NodeState state);

    /**
     * Removes information about known clasess
     *
     * @param state Node state to get/set properties
     */
    private void removeAllClasses(NodeState state) {
        removeAllClassesHook(state);
        state.setFromKey(KNOWN_CLASSES_LIST_KEY, Type.INT_ARRAY, new int[0]);
    }

    /**
     * Adds new value to the buffer. Connotations change depending on whether the node is in bootstrap mode or not.
     *
     * @param value New value to add; {@code null} disallowed
     * @param result New result
     * @return New value of bootstrap mode
     */
    protected boolean addValue(double value[], int result) {
        illegalArgumentIfFalse(value != null, "Value must be not null");
        NodeState state = unphasedState();
        boolean bootstrapMode = state.getFromKeyWithDefault(BOOTSTRAP_MODE_KEY, BOOTSTRAP_MODE_DEF);

        if (bootstrapMode) {
            return addValueBootstrap(state, value, result);
        }
        return addValueNoBootstrap(state, value, result);
    }

    /**
     * Adds new value to reuslt buffer. Removes value(s) from beginning (if necessary).
     *
     * @param state Node state to get/set proeprties
     * @param result New class label to be added to result buffer
     * @param bootstrapMode New bootstrap mode
     * @return New reuslt buffer
     */
    protected static int[] adjustResultBuffer(NodeState state, int result, boolean bootstrapMode){
        int resultBuffer[] = state.getFromKeyWithDefault(INTERNAL_RESULTS_BUFFER_KEY, INTERNAL_RESULTS_BUFFER_DEF);;

        //So adding 1 value to the end and removing (currentBufferLength + 1) - maxBufferLength from the beginning.
        final int maxResultBufferLength = state.getFromKeyWithDefault(BUFFER_SIZE_KEY, BUFFER_SIZE_DEF);
        final int numValuesToRemoveFromBeginning = bootstrapMode? 0 : Math.max(0, resultBuffer.length + 1 - maxResultBufferLength);

        int newBuffer[] = new int[resultBuffer.length + 1 - numValuesToRemoveFromBeginning];
        //Setting first values
        System.arraycopy(resultBuffer, numValuesToRemoveFromBeginning, newBuffer, 0, newBuffer.length - 1);
        newBuffer[newBuffer.length-1] = result;
        state.setFromKey(INTERNAL_RESULTS_BUFFER_KEY, Type.INT_ARRAY, newBuffer);
        return newBuffer;
    }

    /**
     * Adds value assuming we are not in bootstrap mode
     *
     * @param state Node state to get/set properties
     * @param value New value to add; {@code null} disallowed
     * @param result Expected class label corresponding to new value.
     * @return New bootstrap mode value
     */
    protected boolean addValueNoBootstrap(NodeState state, double value[], int result) {
        double newBuffer[] = adjustValueBuffer(state, value, false);
        int newResultBuffer[] = AbstractClassifierSlidingWindowManagingNode.adjustResultBuffer(state, result, false);

        //Predict for each value in the buffer. Calculate percentage of errors.
        double errorInBuffer = ((double) getBufferErrorCount(state, newBuffer, newResultBuffer)) / newResultBuffer.length;
        double higherErrorThreshold = state.getFromKeyWithDefault(HIGH_ERROR_THRESH_KEY, HIGH_ERROR_THRESH_DEF);
        if (errorInBuffer > higherErrorThreshold) {
            NodeState newState = setBootstrapMode(state, true); //If number of errors is above higher threshold, get into the bootstrap
            updateModelParameters(newState, newBuffer, newResultBuffer, value, result);
            return true;
        }
        return false;
    }

    /**
     * Adds new value to the buffer. Gaussian model is regenerated. Assuming that we are in bootstrap mode.
     *
     * @param state Node state to get/set properties
     * @param value New value to add; {@code null} disallowed
     * @param result Proper class label
     * @return new bootstrap mode value
     */
    protected boolean addValueBootstrap(NodeState state, double value[], int result) {
        double newBuffer[] = adjustValueBuffer(state, value, true);
        int newResultBuffer[] = AbstractClassifierSlidingWindowManagingNode.adjustResultBuffer(state, result, true);
        boolean newBootstrap = true;

        if (newResultBuffer.length >= getMaxBufferLength()) {
            //Predict for each value in the buffer. Calculate percentage of errors.
            double errorInBuffer = ((double) getBufferErrorCount(state, newBuffer, newResultBuffer)) / newResultBuffer.length;
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
    public void learn(final int expectedClass, final Callback<Boolean> callback) {
        extractFeatures(new Callback<double[]>() {
            @Override
            public void on(double[] result) {
                boolean outcome = addValue(result, expectedClass);
                callback.on(outcome);
            }
        });
    }

}
