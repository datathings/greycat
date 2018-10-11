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
package greycat.ml.temp.anomalydetector;

import greycat.Callback;
import greycat.Graph;
import greycat.Node;
import greycat.Type;
import greycat.ml.AnomalyDetectionNode;
import greycat.ml.BaseMLNode;
import greycat.plugin.NodeState;
import greycat.utility.Enforcer;

import java.util.Arrays;

/**
 * Anomaly is detected if at least for one of the dimensions the value is &lt;...&gt; interquartile ranges away from the mean.
 * <p>
 * Created by andrey.boytsov on 14/06/16.
 */
public class InterquartileRangeOutlierDetectorNode extends BaseMLNode implements AnomalyDetectionNode {

    public static final int MAX_BUFFER_LIMIT = 100000;

    public static final String NAME = "InterquartileRangeAnomalyDetection";
    /**
     * Buffer size
     */
    public static final String BUFFER_SIZE_KEY = "BufferSize";
    /**
     * Buffer size - default
     */
    public static final int BUFFER_SIZE_DEF = 50;
    /**
     * Number of input dimensions
     */
    public static final String INPUT_DIM_KEY = "InputDimensions";
    /**
     * Number of input dimensions - default (unknown so far)
     */
    public static final int INPUT_DIM_DEF = -1;
    public static final double UPPER_PERCENTILE = 0.75;
    public static final double LOWER_PERCENTILE = 0.25;
    public static final double RANGE_COEF = 1.5;
    /**
     * Upper bound for some dimension (dimension added to the key)
     */
    public static final String UPPER_BOUND_KEY_PREFIX = "UpperBoundDimension";
    /**
     * Lower bound for some dimension (dimension added to the key)
     */
    public static final String LOWER_BOUND_KEY_PREFIX = "LowerBoundDimension";
    /**
     * Attribute key - sliding window of values
     */
    private static final String INTERNAL_VALUE_BUFFER_KEY = "_valueBuffer";
    private static final Enforcer enforcer = new Enforcer()
            .asIntWithin(BUFFER_SIZE_KEY, 1, MAX_BUFFER_LIMIT);

    public InterquartileRangeOutlierDetectorNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    /**
     * Adds new value to the buffer. Connotations change depending on whether the node is in bootstrap mode or not.
     *
     * @param value New value to add; {@code null} disallowed
     */
    protected boolean addValue(double value[]) {
        illegalArgumentIfFalse(value != null, "Value must be not null");

        NodeState state = unphasedState();
        int dimensions = state.getFromKeyWithDefault(INPUT_DIM_KEY, INPUT_DIM_DEF);
        if (dimensions < 0) {
            dimensions = value.length;
            state.setFromKey(INPUT_DIM_KEY, Type.INT, value.length);
        }

        double buffer[] = state.getFromKeyWithDefault(INTERNAL_VALUE_BUFFER_KEY, new double[0]);

        final int bufferLength = buffer.length / dimensions; //Buffer is "unrolled" into 1D array.
        //So adding 1 value to the end and removing (currentBufferLength + 1) - maxBufferLength from the beginning.
        final int maxBufferLength = state.getFromKeyWithDefault(BUFFER_SIZE_KEY, BUFFER_SIZE_DEF);
        final int numValuesToRemoveFromBeginning = Math.max(0, bufferLength + 1 - maxBufferLength);
        final int newBufferLength = bufferLength + 1 - numValuesToRemoveFromBeginning;

        double newBuffer[] = new double[newBufferLength * dimensions];
        System.arraycopy(buffer, numValuesToRemoveFromBeginning * dimensions, newBuffer, 0, newBuffer.length - dimensions);
        System.arraycopy(value, 0, newBuffer, newBuffer.length - dimensions, dimensions);

        double upperBounds[] = new double[dimensions];
        double lowerBounds[] = new double[dimensions];
        for (int i = 0; i < dimensions; i++) {
            //Get column
            double column[] = new double[newBufferLength];
            int index = i;
            for (int j = 0; j < newBufferLength; j++) {
                column[j] = newBuffer[index];
                index += dimensions;
            }
            //Sort.
            Arrays.sort(column);
            //Get 25 and 75 percentile
            double upperPercentile = column[Math.min((int) Math.round(newBufferLength * UPPER_PERCENTILE), newBufferLength - 1)];
            double lowerPercentile = column[Math.max((int) Math.round(newBufferLength * LOWER_PERCENTILE), 0)];
            double interquartileRange = upperPercentile - lowerPercentile;
            upperBounds[i] = upperPercentile + RANGE_COEF * interquartileRange;
            lowerBounds[i] = lowerPercentile - RANGE_COEF * interquartileRange;
            state.setFromKey(UPPER_BOUND_KEY_PREFIX + i, Type.DOUBLE, upperBounds[i]);
            state.setFromKey(LOWER_BOUND_KEY_PREFIX + i, Type.DOUBLE, lowerBounds[i]);
        }
        state.setFromKey(INTERNAL_VALUE_BUFFER_KEY, Type.DOUBLE_ARRAY, newBuffer);

        return checkValue(value, upperBounds, lowerBounds);
    }


    protected boolean checkValue(double value[], double lowerBounds[], double upperBounds[]) {
        for (int i = 0; i < value.length; i++) {
            //A bit strange condition to make sure that NaNs are counted as anomalies
            if (!((value[i] <= upperBounds[i]) && (value[i] >= lowerBounds[i]))) {
                return true; //Not within bounds? Anomaly
            }
        }
        return false;
    }

    @Override
    public void learn(final Callback<Boolean> callback) {
        extractFeatures(new Callback<double[]>() {
            @Override
            public void on(double[] result) {
                boolean outcome = addValue(result);
                callback.on(outcome);
            }
        });
    }

    @Override
    public void classify(final Callback<Boolean> callback) {
        extractFeatures(new Callback<double[]>() {
            @Override
            public void on(double[] result) {
                double lowerBounds[] = new double[result.length];
                double upperBounds[] = new double[result.length];
                NodeState state = unphasedState();
                for (int i = 0; i < result.length; i++) {
                    lowerBounds[i] = (Double) state.getFromKey(LOWER_BOUND_KEY_PREFIX + i);
                    upperBounds[i] = (Double) state.getFromKey(UPPER_BOUND_KEY_PREFIX + i);
                }
                boolean isAnomaly = checkValue(result, lowerBounds, upperBounds);
                callback.on(isAnomaly);
            }
        });
    }

    @Override
    public Node set(String propertyName, byte propertyType, Object propertyValue) {
        if (INTERNAL_VALUE_BUFFER_KEY.equals(propertyName) || INPUT_DIM_KEY.equals(propertyName)) {
            //Nothing. They are unsettable directly
        } else {
            enforcer.check(propertyName, propertyType, propertyValue);
            super.set(propertyName, propertyType, propertyValue);
        }
        return this;
    }
}
