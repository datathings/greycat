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

import org.mwg.Callback;
import org.mwg.Graph;
import org.mwg.Type;
import org.mwg.ml.AbstractMLNode;
import org.mwg.plugin.NodeState;
import org.mwg.utility.Enforcer;

import java.util.ArrayList;
import java.util.List;


//TODO So, which interface do we need?

//TODO Predict value?

//TODO Bootstrap mode management

/**
 * Autocorrelation-based periodicity detector.
 *
 * Dimensions are treated separately.
 */
public class AutoregressionBasedPeriodicityDetector extends AbstractMLNode{

    /**
     * Attribute key - sliding window of values
     */
    protected static final String INTERNAL_VALUE_BUFFER_KEY = "_valueBuffer";

    public static final String NAME = "AutoregressionBasedPeriodicityDetector";

    /**
     * Number of input dimensions
     */
    public static final String INPUT_DIM_KEY = "InputDimensions";
    /**
     * Number of input dimensions - default
     */
    public static final int INPUT_DIM_UNKNOWN = -1;
    /**
     * Number of input dimensions - default (unknown so far)
     */
    public static final int INPUT_DIM_DEF = INPUT_DIM_UNKNOWN;

    /*
    * Autoregression window length key
    */
    public static final String AUTOREGRESSION_LAG_KEY = "AutoregressionLag";

    /**
     * Buffer size
     */
    public static final String BUFFER_SIZE_KEY = "BufferSize";
    /**
     * Buffer size - default
     */
    public static final int BUFFER_SIZE_DEF = 50;
    /**
     * Buffer size
     */
    public static final String PERIODS_NUM = "NumberOfPeriods";
    /**
     * Buffer size - default
     */
    public static final int[] PERIODS_NUM_DEF = new int[0];
    /**
     * Buffer size
     */
    public static final String PERIODS = "Periods";
    /**
     * Buffer size - default
     */
    public static final int[] PERIODS_DEF = new int[0];


    /**
     * {@inheritDoc}
     */
    public AutoregressionBasedPeriodicityDetector(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    private static final Enforcer abpdEnforcer = new Enforcer().asDouble(AUTOREGRESSION_LAG_KEY);

    @Override
    public void setProperty(String propertyName, byte propertyType, Object propertyValue) {
        if (INTERNAL_VALUE_BUFFER_KEY.equals(propertyName) || INPUT_DIM_KEY.equals(propertyName) ||
            PERIODS_NUM_DEF.equals(propertyName) || PERIODS_DEF.equals(propertyName)) {
            //Nothing. They are unsettable directly
        }else {
            abpdEnforcer.check(propertyName, propertyType, propertyValue);
            super.setProperty(propertyName, propertyType, propertyValue);
        }
    }

    /**
     * Updates model parameters with respect to new values
     *
     * @param state Node state to get/set properties
     * @param currentBuffer Current value buffer
     * @param value New value (assumed to be last value in the buffer as well)
     * @return Array of periods (second index) for each dimension (first index)
     */
    protected int[][] updateModelParameters(NodeState state, double[] currentBuffer, double[] value) {
        int dims = state.getFromKeyWithDefault(INPUT_DIM_KEY, INPUT_DIM_UNKNOWN);
        if (dims == INPUT_DIM_UNKNOWN) {
            dims = value.length;
            state.setFromKey(INPUT_DIM_KEY, Type.INT, dims);
        }

        if (currentBuffer.length < 2*dims){
            //We have only one column. Not enough to draw any periodicity conclusions
            //No periods detected
            return new int[dims][0];
        }

        //That's current window length. Might be less than max, if we just started.
        final int windowLength = currentBuffer.length / dims;

        //Requested lag. Real might be smaller at the start.
        final int lag = (Integer) state.getFromKey(AUTOREGRESSION_LAG_KEY);

        //Count number of rows and columns of autocorrelation matrix. Should be the same for each dimension.
        //How many columns? Equal to lag (or, if we don't have enough values, then as many as we have)
        //How many rows? (windowSize - lag) if we have enough values. Otherwise - 1.
        //What if we don't have enough values to cover the lag? Can easily happen at the beginning.
        final int realLag = (windowLength > lag) ? lag : windowLength;
        int periodsNumbersByDimensions[] = new int[dims];
        List<Integer> periods = new ArrayList<>();
        int[][] resultingPeriods = new int[dims][];

        for (int currentDimension=0; currentDimension < dims; currentDimension++) {//For each dimension separately
            //We'll take the whole length for autoregression. Assuming that sliding window will cut values.
            //We have the number of values equal to sliding window size

            //Single out current dimension.
            double singleDimensionValues[] = new double[windowLength];
            periodsNumbersByDimensions[currentDimension] = 0;
            for (int i=0;i<windowLength;i++){
                singleDimensionValues[i] = currentBuffer[currentDimension + i*dims];
            }
            double meanY = 0;
            double varY = 0;
            //Step 1.2. Now create matrix column-by-column
            for (int i=0;i<singleDimensionValues.length;i++){
                meanY += singleDimensionValues[i];
            }
            meanY /= singleDimensionValues.length;

            //Let's calculate std out of means
            for (int i=0;i<singleDimensionValues.length;i++){
                varY += (singleDimensionValues[i] - meanY)*(singleDimensionValues[i] - meanY);
            }
            varY /= singleDimensionValues.length;

            //TODO Save autoregression/autocorrelation coefficients for prediction purposes? Add some prediction?

            //Step 3. We got regression coefficients. Now let's transform them into autocorrelation.
            double resultCoefs[] = new double[realLag];
            for (int k=0;k<realLag;k++){
                resultCoefs[k] = 0;
                for (int i=0;i<singleDimensionValues.length-k;i++){
                    resultCoefs[k] += (singleDimensionValues[i] - meanY)*(singleDimensionValues[i+k] - meanY);
                }
                resultCoefs[k] /= (varY * (singleDimensionValues.length-k));
            }

            //Step 4. We got autocorrelation coefficients. Let's get periodicity out of them.
            //Keep an option to report autocorrelation. It might be useful.
            //TODO

            //Keep only positive periods? Do not count "antiperiods"?
            boolean inPeriodicity = false;
            int periodicityStarts = 0;
            int periodicityStops = 0;
            double threshold = 2/ Math.sqrt(realLag);
            for (int i=1;i<realLag;i++){
                //Autocorrelation with itself? Sure, but that's not what we want
                if (!inPeriodicity){
                    //Currently nothing periodic is detected. Let's see if coefficient's has 95% confidence of being non-zero
                    if (resultCoefs[i] > threshold){
                        periodicityStarts = i;
                        inPeriodicity = true;
                    }
                }else{
                    if ((resultCoefs[i] < threshold) || (i==(realLag-1))){
                        periodicityStops = i;
                        inPeriodicity = false;
                        int detectedPeriod = periodicityStarts;
                        for (int j=periodicityStarts+1;j<=periodicityStops;j++){
                            if (resultCoefs[j] > resultCoefs[detectedPeriod]){
                                detectedPeriod = j;
                            }
                        }
                        periods.add(detectedPeriod);
                        periodsNumbersByDimensions[currentDimension]++;
                    }
                }
            }
        }

        int periodsArray[] = new int[periods.size()];
        for (int i=0;i<periodsArray.length;i++){
            periodsArray[i] = periods.get(i);
        }
        state.setFromKey(PERIODS_NUM, Type.INT_ARRAY, periodsNumbersByDimensions);
        state.setFromKey(PERIODS, Type.INT_ARRAY, periodsArray);
        return transformPeriodsArrays(periodsNumbersByDimensions, periodsArray);
    }


    private static int[][] transformPeriodsArrays(int numPeriodsByDimension[], int periods[]){
        int result[][] = new int[numPeriodsByDimension.length][];
        int index = 0;
        for (int i=0;i<numPeriodsByDimension.length;i++){
            result[i] = new int[numPeriodsByDimension[i]];
            for (int j=0;j<numPeriodsByDimension[i];j++){
                result[i][j] = periods[index];
                index++;
            }
        }
        return result;
    }

    /**
     * Adjust buffer: adds value to the end of it, removes first value(s) if necessary.
     *
     * @param state Node state to get/set properties
     * @param value Newly received values
     * @param bootstrapMode Bootstrap mode value
     * @return New adjusted value buffer.
     */
    protected static double[] adjustValueBuffer(NodeState state, double value[], boolean bootstrapMode) {
        int dimensions = state.getFromKeyWithDefault(INPUT_DIM_KEY, INPUT_DIM_DEF);
        if (dimensions < 0) {
            dimensions = value.length;
            state.setFromKey(INPUT_DIM_KEY, Type.INT, value.length);
        }

        double buffer[] = state.getFromKeyWithDefault(INTERNAL_VALUE_BUFFER_KEY, new double[0]);

        final int bufferLength = buffer.length / dimensions; //Buffer is "unrolled" into 1D array.
        //So adding 1 value to the end and removing (currentBufferLength + 1) - maxBufferLength from the beginning.
        final int maxBufferLength = state.getFromKeyWithDefault(BUFFER_SIZE_KEY, BUFFER_SIZE_DEF);
        final int numValuesToRemoveFromBeginning = bootstrapMode ? 0 : Math.max(0, bufferLength + 1 - maxBufferLength);
        final int newBufferLength = bufferLength + 1 - numValuesToRemoveFromBeginning;

        double newBuffer[] = new double[newBufferLength * dimensions];
        System.arraycopy(buffer, numValuesToRemoveFromBeginning*dimensions, newBuffer, 0, newBuffer.length - dimensions);
        System.arraycopy(value, 0, newBuffer, newBuffer.length - dimensions, dimensions);

        state.setFromKey(INTERNAL_VALUE_BUFFER_KEY, Type.DOUBLE_ARRAY, newBuffer);
        return newBuffer;
    }

    /**
     * Main training function to learn from the the expected output,
     * The input features are defined through features extractions.
     *
     * @param callback Called when the learning is completed with the status of learning true/false
     */
    public void learn(final Callback<int[][]> callback) {
        extractFeatures(new Callback<double[]>() {
            @Override
            public void on(double[] result) {
                int outcome[][] = addValue(result);
                callback.on(outcome);
            }
        });
    }

    //TODO For now bootstrap mode is unused
    /**
     * Attribute key - whether the node is in bootstrap (re-learning) mode
     */
    public static final String BOOTSTRAP_MODE_KEY = "bootstrapMode";
    public static final boolean BOOTSTRAP_MODE_DEF = true;

    /**
     * Main infer function to give all detected periods,
     * The input features are defined through features extractions.
     *
     * @param callback Called when the infer is completed with the result of inferred periods (first index dimension -
     *                 second index - detected periods)
     */
    void inferPeriodsByDimensions(Callback<int[][]> callback){
        NodeState state = unphasedState();
        int numPeriodsByDimensions[] = (int[])state.getFromKey(PERIODS_NUM);
        int periods[] = (int[])state.getFromKey(PERIODS);
        int result[][] = transformPeriodsArrays(numPeriodsByDimensions, periods);
        callback.on(result);
    }

    /**
     * Adds new value to the buffer. Connotations change depending on whether the node is in bootstrap mode or not.
     *
     * @param value New value to add; {@code null} disallowed
     * @return Array of periods (second index) for each dimension (first index)
     */
    protected int[][] addValue(double value[]) {
        illegalArgumentIfFalse(value != null, "Value must be not null");

        NodeState state = unphasedState();

        double newBuffer[] = adjustValueBuffer(state, value, false);//TODO Bootstrap - false
        return updateModelParameters(state, newBuffer, value);
        //TODO bootstrap mode management. By now - constantly in bootstrap.
    }

    //TODO Step 2. Add management of thresholds for distances. Over threshold? New state.
    //TODO Gradually moving over threshold? Compare not to last state, but to "average" one?
    //TODO Returning to old states?
    //TODO Step 3. Java test with real London consumption

    /**
     * @param ar Array to copy/remove last element
     * @return Copy of that array w/o last element
     */
    protected static int[] withoutLastElement(int ar[]){
        int res[] = new int[ar.length-1];
        System.arraycopy(ar, 0, res, 0, res.length);
        return res;
    }

    /**
     *
     * Effectively, Levenstein distance with weights
     *
     * @param per1 Periods array 1 to count distance
     * @param per2 Periods array 2 to count distance
     * @param weight_added_period Weight associated with adding period (1 - as bad as shifting some period by one,
     *                            usually more than 1 - adding period is worse than just small shift)
     * @return How much different are those periods (in terms of shifted periods, added epriods, removed periods,etc.)
     */
    public static int periodsSequenceDistance(int per1[], int per2[], int weight_added_period){
        //Possible options: period added, period deleted, period shifted

        //One of the periods is empty. All others are "added"
        if (per1.length == 0){
            //Empty per1. All in per2 is "added"
            return per2.length*weight_added_period;
        }
        if (per2.length == 0){
            //Empty per2. All in per1 is "added"
            return per1.length*weight_added_period;
        }

        int per1NoLast[] = withoutLastElement(per1);
        int per2NoLast[] = withoutLastElement(per2);

        //Unlike Levenstein distance, it is not just a match or mismatch.
        //It also matters how far they are

        //return minimum of following options:
        //1. Count last period of per1 as non-existing in per2. Compare the rest.
        //2. Count last period of per2 as non-existing in per1. Compare the rest.
        //3. Count last period as the same in per1 and per2 (possibly shifted). Compare the rest.
        return Math.min(Math.min(periodsSequenceDistance(per1NoLast, per2, weight_added_period) + weight_added_period,
                                 periodsSequenceDistance(per1, per2NoLast,weight_added_period) + weight_added_period),
                periodsSequenceDistance(per1NoLast, per2NoLast, weight_added_period) + Math.abs(per1[per1.length-1] - per2[per2.length-1]));

    }
}
