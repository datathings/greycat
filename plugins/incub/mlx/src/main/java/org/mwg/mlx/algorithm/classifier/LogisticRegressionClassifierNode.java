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
import org.mwg.ml.ClassificationNode;
import org.mwg.mlx.algorithm.AbstractClassifierSlidingWindowManagingNode;
import org.mwg.plugin.NodeState;
import org.mwg.utility.Enforcer;

/**
 * Created by andrey.boytsov on 17/05/16.
 */
public class LogisticRegressionClassifierNode extends AbstractClassifierSlidingWindowManagingNode implements ClassificationNode {

    /**
     * Regression coefficients
     */
    public static final String COEFFICIENTS_KEY = "regressionCoefficients";
    /**
     * Regression coefficients - default
     */
    public static final double[] COEFFICIENTS_DEF = new double[0];
    /**
     * Regression intercept
     */
    public static final String INTERCEPT_KEY = "regressionIntercept";
    /**
     * Regression intercept - default
     */
    public static final double INTERCEPT_DEF = 0.0;

    /**
     * L2 regularization coefficient
     */
    public static final String L2_COEF_KEY = "L2Coefficient";
    /**
     * L2 regularization coefficient - default
     */
    public static final double L2_COEF_DEF = 0.0;

    public static final String NAME = "LogisticRegressionBatch";

    public LogisticRegressionClassifierNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    public static final String GD_DIFFERENCE_THRESH_KEY = "gdDifferenceThreshold";
    public static final String GD_ITERATION_THRESH_KEY = "gdIterationThreshold";

    public static final int DEFAULT_GD_ITERATIONS_COUNT = 10000;

    public static final double DEFAULT_LEARNING_RATE = 0.0001;

    /**
     * Attribute key - Learning rate
     */
    public static final String LEARNING_RATE_KEY = "LearningRate";

    private static final Enforcer logRegrEnforcer = new Enforcer()
            .asDouble(GD_DIFFERENCE_THRESH_KEY)
            .asDouble(GD_ITERATION_THRESH_KEY)
            .asPositiveDouble(LEARNING_RATE_KEY)
            .asNonNegativeDouble(L2_COEF_KEY);


    @Override
    public void setProperty(String propertyName, byte propertyType, Object propertyValue) {
        if ((propertyName.lastIndexOf(COEFFICIENTS_KEY, 0) == 0) || (propertyName.lastIndexOf(INTERCEPT_KEY, 0) == 0)) {
            //Nothing. Those cannot be set.
        } else {
            logRegrEnforcer.check(propertyName, propertyType, propertyValue);
            super.setProperty(propertyName, propertyType, propertyValue);
        }
    }

    private static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    private static double dot(double vector1[], double vector2[]) {
        assert vector1 != null : "vector1 must be not null";
        assert vector2 != null : "vector2 must be not null";
        assert vector1.length == vector2.length : "vectors must be of equal length";
        double result = 0;
        for (int i = 0; i < vector1.length; i++) {
            result += vector1[i] * vector2[i];
        }
        return result;
    }

    @Override
    protected int predictValue(NodeState state, double[] value) {
        int classes[] = state.getFromKeyWithDefault(KNOWN_CLASSES_LIST_KEY, new int[0]);
        double maxLikelihood = -1.0; //Guaranteed to change. No real likelihood is less than 0.
        int maxClass = 0;
        for (int cl : classes) {
            double likelihoodPerClass = getLikelihoodForClass(state, value, cl);
            if (likelihoodPerClass > maxLikelihood) {
                maxLikelihood = likelihoodPerClass;
                maxClass = cl;
            }
        }
        return maxClass;
    }

    @Override
    protected double getLikelihoodForClass(NodeState state, double[] value, int classNum) {
        double coefs[] = state.getFromKeyWithDefault(COEFFICIENTS_KEY + classNum, COEFFICIENTS_DEF);
        double intercept = state.getFromKeyWithDefault(INTERCEPT_KEY + classNum, INTERCEPT_DEF);
        return sigmoid(dot(value, coefs) + intercept);
    }

    @Override
    protected void updateModelParameters(NodeState state, double[] valueBuffer, int resultBuffer[], double value[], int classNumber) {
        if (state.getFromKeyWithDefault(INPUT_DIM_KEY, INPUT_DIM_DEF) == INPUT_DIM_UNKNOWN) {
            state.setFromKey(INPUT_DIM_KEY, Type.INT, value.length);
        }
        initializeClassIfNecessary(state, classNumber);
        final int dims = state.getFromKeyWithDefault(INPUT_DIM_KEY, INPUT_DIM_DEF);

        final double gdDifferenceThresh = state.getFromKeyWithDefault(GD_DIFFERENCE_THRESH_KEY, 0.0);
        final int gdIterThresh = state.getFromKeyWithDefault(GD_ITERATION_THRESH_KEY, DEFAULT_GD_ITERATIONS_COUNT);

        final double alpha = state.getFromKeyWithDefault(LEARNING_RATE_KEY, DEFAULT_LEARNING_RATE);
        final double lambda = state.getFromKeyWithDefault(L2_COEF_KEY, L2_COEF_DEF);

        int classes[] = state.getFromKeyWithDefault(KNOWN_CLASSES_LIST_KEY, new int[0]);
        for (final int cl : classes) {
            double coefs[] = state.getFromKeyWithDefault(COEFFICIENTS_KEY + cl, COEFFICIENTS_DEF);
            double intercept = state.getFromKeyWithDefault(INTERCEPT_KEY + cl, INTERCEPT_DEF);
            if (coefs.length == 0) { //TODO should not happen? If necessary, remove unnecessary check & replace with assertion
                coefs = new double[dims];
                state.setFromKey(COEFFICIENTS_KEY + cl, Type.DOUBLE_ARRAY, coefs);
            }

            int iterCount = 0;
            boolean exitCase = false;
            while (!exitCase) {
                iterCount++;

                int startIndex = 0;
                int index = 0;
                double oldCoefs[] = new double[coefs.length];
                System.arraycopy(coefs, 0, oldCoefs, 0, coefs.length);
                final double oldIntercept = intercept;
                while (startIndex + dims <= valueBuffer.length) {
                    double curValue[] = new double[dims];
                    System.arraycopy(valueBuffer, startIndex, curValue, 0, dims);

                    double h = sigmoid(dot(value, oldCoefs) + intercept);
                    int y = (resultBuffer[index] == cl) ? 1 : 0;

                    for (int j = 0; j < dims; j++) {
                        coefs[j] += alpha * ((y - h) * curValue[j]) / resultBuffer.length;
                    }
                    intercept += alpha * (y - h) / resultBuffer.length;

                    startIndex += dims;
                    index++;
                }
                for (int j = 0; j < dims; j++) {
                    coefs[j] -= alpha * lambda * oldCoefs[j];
                }


                double maxDiff = 0.0;
                for (int j = 0; j < dims; j++) {
                    maxDiff = Math.max(Math.abs(coefs[j] - oldCoefs[j]), maxDiff);
                }
                maxDiff = Math.max(Math.abs(intercept - oldIntercept), maxDiff);

                state.setFromKey(COEFFICIENTS_KEY + cl, Type.DOUBLE_ARRAY, coefs);
                state.setFromKey(INTERCEPT_KEY + cl, Type.DOUBLE, intercept);
                if (gdDifferenceThresh > 0) {
                    exitCase = exitCase || maxDiff < gdDifferenceThresh;
                }

                if (gdIterThresh > 0) {
                    exitCase = exitCase || (iterCount >= gdIterThresh);
                }

                if ((!(gdDifferenceThresh > 0)) && (!(gdIterThresh > 0))) {
                    //Protection against infinite loops. If neither error threshold, nor iteration thresholds are used,
                    //run loops once, do not go infinite.
                    exitCase = true;
                }
            }

        }

    }

    @Override
    protected void removeAllClassesHook(NodeState state) {
        int classes[] = state.getFromKeyWithDefault(KNOWN_CLASSES_LIST_KEY, new int[0]);
        for (int curClass : classes) {
            state.setFromKey(INTERCEPT_KEY + curClass, Type.DOUBLE, INTERCEPT_DEF);
            state.setFromKey(COEFFICIENTS_KEY + curClass, Type.DOUBLE_ARRAY, COEFFICIENTS_DEF);
        }
    }

    protected void initializeClassIfNecessary(NodeState state, int classNum) {
        Object oldCoefsObj = state.getFromKey(COEFFICIENTS_KEY + classNum);
        if (oldCoefsObj != null) {
            //Is there, but could be deleted
            double oldCoefs[] = (double[]) oldCoefsObj;
            if (oldCoefs.length > 0) { //Is the class deleted?
                //Already initialized
                return;
            }
        }
        int dims = state.getFromKeyWithDefault(INPUT_DIM_KEY, INPUT_DIM_DEF);

        addToKnownClassesList(state, classNum);
        state.setFromKey(COEFFICIENTS_KEY + classNum, Type.DOUBLE_ARRAY, new double[dims]);
        state.setFromKey(INTERCEPT_KEY + classNum, Type.DOUBLE, INTERCEPT_DEF);
    }

}
