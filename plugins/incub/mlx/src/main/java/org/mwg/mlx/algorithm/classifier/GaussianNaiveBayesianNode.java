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
package org.mwg.mlx.algorithm.classifier;

import org.mwg.Constants;
import org.mwg.Graph;
import org.mwg.Type;
import org.mwg.ml.ClassificationNode;
import org.mwg.ml.common.matrix.operation.Gaussian1D;
import org.mwg.mlx.algorithm.AbstractGaussianClassifierNode;
import org.mwg.plugin.NodeState;

public class GaussianNaiveBayesianNode extends AbstractGaussianClassifierNode implements ClassificationNode {

    //TODO Any synchronization?

    //TODO Try out changing parameters on the fly

    public static final String NAME = "GaussianNaiveBayesian";

    /**
     * {@inheritDoc}
     */
    public GaussianNaiveBayesianNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }


    protected void initializeClassIfNecessary(NodeState state, int classNum) {
        Object oldSumsObj = state.getFromKey(INTERNAL_SUM_KEY_PREFIX + classNum);
        if (oldSumsObj != null) {
            //Is there, but could be deleted
            double oldSums[] = (double[]) oldSumsObj;
            if (oldSums.length > 0) { //Is the class deleted?
                //Already initialized
                return;
            }
        }

        addToKnownClassesList(state, classNum);
        state.setFromKey(INTERNAL_TOTAL_KEY_PREFIX + classNum, Type.INT, 0);
        final int dimensions = state.getFromKeyWithDefault(INPUT_DIM_KEY, INPUT_DIM_DEF);
        state.setFromKey(INTERNAL_SUM_KEY_PREFIX + classNum, Type.DOUBLE_ARRAY, new double[dimensions]);
        state.setFromKey(INTERNAL_SUMSQUARE_KEY_PREFIX + classNum, Type.DOUBLE_ARRAY, new double[dimensions]);
        //Model can stay uninitialized until total is at least <TODO 2? 1?>
    }


    @Override
    protected void updateModelParameters(NodeState state, double valueBuffer[], int resultBuffer[], double value[], int classNum) {
        //Rebuild Gaussian for mentioned class
        //Update sum, sum of squares and total
        if (state.getFromKeyWithDefault(INPUT_DIM_KEY, INPUT_DIM_DEF) == INPUT_DIM_UNKNOWN) {
            state.setFromKey(INPUT_DIM_KEY, Type.INT, value.length);
        }
        initializeClassIfNecessary(state, classNum);
        int curTotal = (Integer)state.getFromKey(INTERNAL_TOTAL_KEY_PREFIX + classNum);
        state.setFromKey(INTERNAL_TOTAL_KEY_PREFIX + classNum, Type.INT, curTotal + 1);

        double currentSum[] = (double[])state.getFromKey(INTERNAL_SUM_KEY_PREFIX + classNum);
        double currentSumSquares[] = (double[])state.getFromKey(INTERNAL_SUMSQUARE_KEY_PREFIX + classNum);
        for (int i = 0; i < value.length; i++) {
            currentSum[i] += value[i];
            currentSumSquares[i] += value[i] * value[i];
        }
        state.setFromKey(INTERNAL_SUM_KEY_PREFIX + classNum, Type.DOUBLE_ARRAY, currentSum);
        state.setFromKey(INTERNAL_SUMSQUARE_KEY_PREFIX + classNum, Type.DOUBLE_ARRAY, currentSumSquares);
        //Need to re-put
    }

    protected double getLikelihoodForClass(NodeState state, double value[], int classNum) {
        //It is assumed that real class is removed and replaces with 0
        initializeClassIfNecessary(state, classNum); //TODO should not be necessary. Double-check.
        int total = (Integer)state.getFromKey(INTERNAL_TOTAL_KEY_PREFIX + classNum);
        if (total < 2) {
            //Not enough data to build model for that class.
            return 0;
        }

        //For each dimension
        //Step 1. Get sum
        //Step 2. Get sum of squares.
        //Step 3. Multiply for each dimension
        double likelihood = 1;
        double sums[] = (double[])state.getFromKey(INTERNAL_SUM_KEY_PREFIX + classNum);
        double sumSquares[] = (double[])state.getFromKey(INTERNAL_SUMSQUARE_KEY_PREFIX + classNum);
        for (int i = 0; i < state.getFromKeyWithDefault(INPUT_DIM_KEY, INPUT_DIM_DEF); i++) {
            likelihood *= Gaussian1D.getDensity(sums[i], sumSquares[i], total, value[i]);
        }
        //TODO Use log likelihood? Can be better for underflows.
        return likelihood;
    }

    @Override
    protected int predictValue(NodeState state, double value[]) {
        int classes[] = state.getFromKeyWithDefault(KNOWN_CLASSES_LIST_KEY, new int[0]);
        if (classes.length == 1) {
            return classes[0];
        }
        double curMaxLikelihood = Constants.BEGINNING_OF_TIME; //Even likelihood 0 should surpass it
        int curMaxLikelihoodClass = -1;
        for (int curClass : classes) {
            double curLikelihood = getLikelihoodForClass(state, value, curClass);
            if (curLikelihood > curMaxLikelihood) {
                curMaxLikelihood = curLikelihood;
                curMaxLikelihoodClass = curClass;
            }
        }
        return curMaxLikelihoodClass;
    }

    @Override
    public String toString() {
        NodeState state = unphasedState();
        String result = "";
        int allClasses[] = state.getFromKeyWithDefault(KNOWN_CLASSES_LIST_KEY, new int[0]);
        if (allClasses.length == 0) {
            return "No classes";
        }
        for (int classNum : allClasses) {
            int total = (Integer)state.getFromKey(INTERNAL_TOTAL_KEY_PREFIX + classNum);
            if (total < 2) {
                //Not enough data to build model for that class.
                result += classNum + ": Not enough data(" + total + ")\n";
            } else {
                double sums[] = (double[])state.getFromKey(INTERNAL_SUM_KEY_PREFIX + classNum);
                double means[] = (double[])state.getFromKey(INTERNAL_SUM_KEY_PREFIX + classNum);
                for (int i = 0; i < means.length; i++) {
                    means[i] = means[i] / total;
                }
                double sumSquares[] = (double[])state.getFromKey(INTERNAL_SUMSQUARE_KEY_PREFIX + classNum);
                result += classNum + ": mean = ["; //TODO For now - cannot report variance from distribution
                for (int i = 0; i < means.length; i++) {
                    result += means[i] + ", ";
                }
                result += "]\nCovariance:\n";
                result += "[";
                for (int j = 0; j < means.length; j++) {
                    result += Gaussian1D.getCovariance(sums[j], sumSquares[j], total) + ", ";
                }
                result += "]\n";
            }
        }
        return result;//TODO Normalize on average?
    }
}
