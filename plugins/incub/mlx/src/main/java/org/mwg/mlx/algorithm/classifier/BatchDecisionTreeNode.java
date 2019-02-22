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

import org.mwg.Graph;
import org.mwg.Type;
import org.mwg.ml.common.DecisionTreeNode;
import org.mwg.mlx.algorithm.AbstractClassifierSlidingWindowManagingNode;
import org.mwg.plugin.NodeState;

import java.util.*;

public class BatchDecisionTreeNode extends AbstractClassifierSlidingWindowManagingNode {
    public static final String NAME = "BatchDecisionTreeClassifier";

    //TODO what to do after node split? Re-create decision tree from buffer? Now - yes

    //TODO Make criterion configurable (entropy vs Giny index vs accuracy improvement vs ... think of other options)

    //TODO Configure for built-in pruning?

    DecisionTreeNode rootNode = null;

    public BatchDecisionTreeNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    protected int predict(DecisionTreeNode node, double value[]) {
        if ((node.left == null) && (node.right == null)) {
            return node.classNum;
        }
        if (value[node.featureNum] >= node.boundary) {
            return predict(node.right, value);
        }
        return predict(node.left, value);
    }

    protected static final String INTERNAL_DECISION_TREE_KEY = "_decisionTreeSerialized";

    @Override
    protected int predictValue(NodeState state, double[] value) {
        if (rootNode == null) {
            rootNode = DecisionTreeNode.deserializeFromDoubleArray((double[])state.getFromKey(INTERNAL_DECISION_TREE_KEY));
        }
        return predict(rootNode, value);
    }

    @Override
    protected double getLikelihoodForClass(NodeState state, double[] value, int classNum) {
        final int predictedClass = predictValue(state, value);
        //No real likelihood. Just yes or no.
        return (classNum == predictedClass) ? 1.0 : 0.0;
    }

    private static double[] extractColumn(double[][] values, int columnNumber) {
        double result[] = new double[values.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = values[i][columnNumber];
        }
        return result;
    }

    private static double getBoundary(double values[]) {
        //TODO: for now it is only median. Add more sophisticated things.
        //When counting median, leave only unique elements.
        //Otherwise we can end up with everything above or below the boundary.
        Set<Double> uniqueValuesSet = new HashSet<Double>();
        for (double d : values) {
            uniqueValuesSet.add(d);
        }
        Double sortedValues[] = uniqueValuesSet.toArray(new Double[0]);
        Arrays.sort(sortedValues);
        final int midArray = sortedValues.length / 2;
        if (values.length % 2 == 0)
            return (sortedValues[midArray] + sortedValues[midArray - 1]) / 2; //Mind that index start with 0
        return sortedValues[midArray]; //Again, mind that index start with 0
    }

    private static double[] getAllPossibleBoundaries(double values[]) {
        //When counting median, leave only unique elements.
        //Otherwise we can end up with everything above or below the boundary.
        Set<Double> uniqueValuesSet = new HashSet<Double>();
        for (int i = 0; i < values.length; i++) {
            uniqueValuesSet.add(values[i]);
        }
        if (uniqueValuesSet.size() == 1) {
            return new double[]{values[0]};
        }
        Double sortedValues[] = uniqueValuesSet.toArray(new Double[0]);
        Arrays.sort(sortedValues);
        double result[] = new double[sortedValues.length - 1];
        for (int i = 0; i < result.length; i++) {
            result[i] = (sortedValues[i] + sortedValues[i + 1]) / 2;
        }
        return result;
    }

    private static int[] toIntArray(List<Integer> intList) {
        int result[] = new int[intList.size()];
        int index = 0;
        for (int i = 0; i < intList.size(); i++) {
            result[index] = intList.get(i);
            index++;
        }
        return result;
    }

    private static double[][] toDouble2DArray(List<double[]> doubleList) {
        double result[][] = new double[doubleList.size()][];
        int index = 0;
        for (int i = 0; i < doubleList.size(); i++) {
            double[] curArray = doubleList.get(i);
            result[index] = new double[curArray.length];
            System.arraycopy(curArray, 0, result[index], 0, curArray.length);
            index++;
        }
        return result;
    }

    private static double[] getImprovementAndBoundary(double[] values, int classNumbers[]) {
        //TODO for now using only accuracy improvement. Add more criteria.
        double boundariesList[] = getAllPossibleBoundaries(values);
        double maxImprovement = -1; //Improvement cannot be <0 ,so we'll replace that immediately
        double bestBoundary = Double.NaN;
        for (int k = 0; k < boundariesList.length; k++) {
            double boundary = boundariesList[k];
            int originalErrors = getMajorityErrors(classNumbers);
            List<Integer> aboveBoundaryClassNumbers = new ArrayList<Integer>();
            List<Integer> belowBoundaryClassNumbers = new ArrayList<Integer>();
            for (int i = 0; i < values.length; i++) {
                //= can go either way.
                //It is a median, so something is likely to be on the boundary.
                if (values[i] >= boundary) {
                    aboveBoundaryClassNumbers.add(classNumbers[i]);
                } else {
                    belowBoundaryClassNumbers.add(classNumbers[i]);
                }
            }
            int errorsAbove = getMajorityErrors(toIntArray(aboveBoundaryClassNumbers));
            int errorsBelow = getMajorityErrors(toIntArray(belowBoundaryClassNumbers));
            double errorPercentageBefore = ((double) originalErrors) / classNumbers.length;
            double errorPercentageAfter = ((double) errorsAbove + errorsBelow) / classNumbers.length;
            double improvement = errorPercentageBefore - errorPercentageAfter;
            if (improvement > maxImprovement) {
                maxImprovement = improvement;
                bestBoundary = boundary;
            }
        }

        //double boundary = getBoundary(values);
        return new double[]{maxImprovement, bestBoundary};
    }

    private static int getMajorityErrors(int classNumbers[]) {
        int mostPopularClass = getMostFrequentElement(classNumbers);
        int errors = 0;
        for (int i = 0; i < classNumbers.length; i++) {
            errors += (classNumbers[i] == mostPopularClass) ? 0 : 1;
        }
        return errors;
    }

    private static int getMostFrequentElement(int[] classNumbers) {
        assert classNumbers.length > 0; //Should ot reach that spot if we have empty set
        int sortedClassNumbers[] = new int[classNumbers.length];
        System.arraycopy(classNumbers, 0, sortedClassNumbers, 0, classNumbers.length);
        Arrays.sort(sortedClassNumbers);
        //Actually, we don't need sorting itself
        //We only need to group same class numbers together for detecting the majority
        //This way we can have majority detection in O(n*log(n)) + O(n) = O(n*log(n))
        //As opposed to O(n^2) for straightforward algorithm
        int maxClass = 0;
        int maxCount = -1;
        int currentClass = 0;
        int currentCount = 0;
        for (int i = 0; i < sortedClassNumbers.length; i++) {
            if ((sortedClassNumbers[i] != currentClass) || (i == 0)) { //Class has changed
                if (currentCount > maxCount) {
                    maxClass = currentClass;
                    maxCount = currentCount;
                }
                currentClass = sortedClassNumbers[i];
                currentCount = 0;
            }
            currentCount++;
        }
        if (currentCount > maxCount) {
            maxClass = currentClass;
        }
        return maxClass;
    }

    private DecisionTreeNode createLeaf(int classNum) {
        DecisionTreeNode leaf = new DecisionTreeNode();
        leaf.featureNum = -1;
        leaf.boundary = Double.NaN;
        leaf.classNum = classNum;
        leaf.left = null;
        leaf.right = null;
        return leaf;
    }

    private double[][] unpackValues(double[] values, int dims) {
        final int numValues = values.length/dims;
        double result[][] = new double[numValues][dims];
        int indexX = 0;
        int indexY = 0;
        for (int i = 0; i < values.length; i++) {
            result[indexX][indexY] = values[i];
            indexY = (indexY + 1) % dims;
            if (indexY == 0) {
                indexX++;
            }
        }
        return result;
    }

    private DecisionTreeNode split(NodeState state, double[][] values, int classNumbers[]) {
        assert classNumbers.length > 0;
        //Step 1. Select the feature to split upon
        // For each feature:
        //   1.1. Get the boundary
        //   1.2. Calculate the criterion
        // Select feature with maximum criterion.
        // If improvement is not worth it, just stop here and make a leaf.
        // Otherwise - make a split, recursively call this method.
        if (allClassNumbersAreTheSame(classNumbers)) {
            return createLeaf(classNumbers[0]);
        }

        if (allValuesAreTheSame(values)) {
            return createLeaf(getMostFrequentElement(classNumbers));
        }

        final int dims = state.getFromKeyWithDefault(INPUT_DIM_KEY, INPUT_DIM_DEF);
        int chosenFeature = -1;
        double maxImprovement = 0; //TODO initialize with threshold
        double bestBoundary = Double.NaN;
        for (int i = 0; i < dims; i++) {
            double valuesOfFeature[] = extractColumn(values, i);
            double improvementAndBoundary[] = getImprovementAndBoundary(valuesOfFeature, classNumbers);
            double improvement = improvementAndBoundary[0];
            double boundary = improvementAndBoundary[1];
            if (improvement > maxImprovement) {
                maxImprovement = improvement;
                bestBoundary = boundary;
                chosenFeature = i;
            }
        }
        if (chosenFeature < 0) {
            return createLeaf(getMostFrequentElement(classNumbers));
        }

        //So, we chose the feature.
        //TODO Do not duplicate the code. We did all of that when choosing the features
        double valuesByFeature[] = extractColumn(values, chosenFeature);
        double boundary = bestBoundary;

        List<Integer> aboveBoundaryClassNumbers = new ArrayList<Integer>();
        List<double[]> aboveBoundaryValues = new ArrayList<double[]>();
        List<Integer> belowBoundaryClassNumbers = new ArrayList<Integer>();
        List<double[]> belowBoundaryValues = new ArrayList<double[]>();
        for (int i = 0; i < values.length; i++) {
            //= can go either way.
            //It is a median, so something is likely to be on the boundary.
            if (values[i][chosenFeature] >= boundary) {
                aboveBoundaryClassNumbers.add(classNumbers[i]);
                aboveBoundaryValues.add(values[i]);
            } else {
                belowBoundaryClassNumbers.add(classNumbers[i]);
                belowBoundaryValues.add(values[i]);
            }
        }
        double bbValuesArray[][] = toDouble2DArray(belowBoundaryValues);
        int bbClasses[] = toIntArray(belowBoundaryClassNumbers);
        double abValuesArray[][] = toDouble2DArray(aboveBoundaryValues);
        int abClasses[] = toIntArray(aboveBoundaryClassNumbers);

        DecisionTreeNode split = new DecisionTreeNode();
        split.featureNum = chosenFeature;
        split.boundary = boundary;
        //split.classNum does not matter
        split.left = split(state, bbValuesArray, bbClasses);
        split.right = split(state, abValuesArray, abClasses);
        return split;
    }

    protected static boolean allClassNumbersAreTheSame(int[] classNumbers) {
        final int curClass = classNumbers[0];
        for (int i = 1; i < classNumbers.length; i++) {
            if (classNumbers[i] != curClass) {
                return false;
            }
        }
        return true;
    }

    //TODO Limit precision? Introduce epsilon?
    protected static boolean allValuesAreTheSame(double[][] values) {
        final double curValue[] = values[0];
        for (int i = 1; i < values.length; i++) {
            for (int j = 0; j < curValue.length; j++) {
                if (values[i][j] != curValue[j]) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void updateModelParameters(NodeState state, double valueBuffer[], int resultBuffer[], double[] value, int classNumber) {
        if (state.getFromKeyWithDefault(INPUT_DIM_KEY, INPUT_DIM_DEF) == INPUT_DIM_UNKNOWN) {
            state.setFromKey(INPUT_DIM_KEY, Type.INT, value.length);
        }

        rootNode = split(state, unpackValues(valueBuffer, valueBuffer.length/resultBuffer.length), resultBuffer);
        state.setFromKey(INTERNAL_DECISION_TREE_KEY, Type.DOUBLE_ARRAY, rootNode.serializeToDoubleArray());
    }

    @Override
    protected void removeAllClassesHook(NodeState state) {
        //Nothing to do here
    }
}
