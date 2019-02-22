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
import org.mwg.ml.AbstractMLNode;
import org.mwg.ml.common.matrix.MatrixOps;
import org.mwg.ml.common.matrix.TransposeType;
import org.mwg.ml.common.matrix.VolatileMatrix;
import org.mwg.mlx.algorithm.AbstractLinearRegressionNode;
import org.mwg.plugin.NodeState;
import org.mwg.struct.Matrix;
import org.mwg.utility.Enforcer;

public class LinearRegressionWithPeriodicityNode extends AbstractLinearRegressionNode {

    public static final String NAME = "LinearRegressionWithPeriodicComponentsBatch";

    /**
     * Double array of periodic components
     */
    public static final String PERIODS_LIST_KEY = "ListOfPeriods";
    /**
     * Time feature
     */
    public static final String TIME_FEATURE_KEY = "TimeFeature";
    /**
     * Sinus components
     */
    public static final String SINUS_KEY = "SinusComponents";
    /**
     * Cosine components
     */
    public static final String COSINE_KEY = "CosineComponents";


    private static final Enforcer lrwpEnforcer = new Enforcer().asDoubleArray(PERIODS_LIST_KEY).asString(TIME_FEATURE_KEY);

    @Override
    public void setProperty(String propertyName, byte propertyType, Object propertyValue) {
        lrwpEnforcer.check(propertyName, propertyType, propertyValue);
        super.setProperty(propertyName, propertyType, propertyValue);
    }

    //TODO Retrieve current periods separately

    public LinearRegressionWithPeriodicityNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    //TODO Add time for jumpings
    @Override
    protected void updateModelParameters(NodeState state, double currentBuffer[], double resultBuffer[], double value[], double currentResult) {
        int dims = state.getFromKeyWithDefault(INPUT_DIM_KEY, INPUT_DIM_UNKNOWN);
        if (dims == INPUT_DIM_UNKNOWN) {
            dims = value.length;
            state.setFromKey(INPUT_DIM_KEY, Type.INT, dims);
        }
        double periodsList[] = state.getFromKeyWithDefault(PERIODS_LIST_KEY, new double[0]);
        //Adding place for intercepts (1 value * resultBuffer.length rows)
        //Adding place for periodic components (2 value (sin & cos) * number of periodic components * resultBuffer.length rows)
        final double reshapedValue[] = new double[currentBuffer.length + resultBuffer.length + 2*periodsList.length*resultBuffer.length];

        final double l2 = state.getFromKeyWithDefault(L2_COEF_KEY, L2_COEF_DEF);

        String fromString = (String)state.getFromKey(AbstractMLNode.FROM);
        String fromFeatures[] = fromString.split(AbstractMLNode.FROM_SEPARATOR);
        String timeName = (String)state.getFromKey(TIME_FEATURE_KEY);
        int timeColumnIndex = 0;
        for (int i=1;i<fromFeatures.length;i++){
            if (fromFeatures[i].equals(timeName)){
                timeColumnIndex = i;
                break ;
            }
        }

        //Step 1. Re-arrange to column-based format.
        for (int i = 0; i < resultBuffer.length; i++) {
            reshapedValue[i] = 1; //First column is 1 (for intercepts)
        }
        //
        for (int i=0;i<periodsList.length;i++){
            //Sinus and cosine for this periodic component
            for (int j = 0; j < resultBuffer.length; j++) {
                double time = currentBuffer[timeColumnIndex+dims*j];
                reshapedValue[(i+1)*resultBuffer.length + j] = Math.sin(time*2*Math.PI/periodsList[i]);
                reshapedValue[(i+1+periodsList.length)*resultBuffer.length + j] = Math.cos(time*2*Math.PI/periodsList[i]);
            }
        }
        for (int i = 0; i < resultBuffer.length; i++) {
            for (int j = 0; j < dims; j++) {
                reshapedValue[(j + 1 + 2*periodsList.length) * resultBuffer.length + i] = currentBuffer[i * dims + j];
            }
        }

        Matrix xMatrix = VolatileMatrix.wrap(reshapedValue, resultBuffer.length, 2*periodsList.length + dims + 1);
        Matrix yVector = VolatileMatrix.wrap(resultBuffer, resultBuffer.length, 1);

        // inv(Xt * X - lambda*I) * Xt * ys
        // I - almost identity, but with 0 for intercept term
        Matrix xtMulX = MatrixOps.multiplyTranspose(TransposeType.TRANSPOSE, xMatrix, TransposeType.NOTRANSPOSE, xMatrix);

        for (int i = 1; i <= dims; i++) {
            xtMulX.add(i, i, l2);
        }

        //PInvSVD pinvsvd = new PInvSVD();
        //pinvsvd.factor(xtMulX, false);
        //Matrix pinv = pinvsvd.getPInv();
        Matrix pinv = MatrixOps.pinv(xtMulX, false);
        Matrix invMulXt = MatrixOps.multiplyTranspose(TransposeType.NOTRANSPOSE, pinv, TransposeType.TRANSPOSE, xMatrix);
        Matrix result = MatrixOps.multiplyTranspose(TransposeType.NOTRANSPOSE, invMulXt, TransposeType.NOTRANSPOSE, yVector);

        final double newSinusCoefficients[] = new double[periodsList.length];
        for (int i = 0; i < periodsList.length; i++) {
            newSinusCoefficients[i] = result.get(i + 1, 0);
        }
        final double newCosinusCoefficients[] = new double[periodsList.length];
        for (int i = 0; i < periodsList.length; i++) {
            newCosinusCoefficients[i] = result.get(i + periodsList.length + 1, 0);
        }
        final double newCoefficients[] = new double[dims];
        for (int i = 0; i < dims; i++) {
            newCoefficients[i] = result.get(i + 2*periodsList.length + 1, 0);
        }
        state.setFromKey(INTERCEPT_KEY, Type.DOUBLE, result.get(0, 0));
        state.setFromKey(COEFFICIENTS_KEY, Type.DOUBLE_ARRAY, newCoefficients);
        state.setFromKey(SINUS_KEY, Type.DOUBLE_ARRAY, newSinusCoefficients);
        state.setFromKey(COSINE_KEY, Type.DOUBLE_ARRAY, newCosinusCoefficients);
    }

    @Override
    public double getBufferError(NodeState state, double valueBuffer[], double results[]) {
        //For each value in value buffer
        int startIndex = 0;
        final int dims = valueBuffer.length / results.length;

        final int numValues = valueBuffer.length / dims;//TODO What if there are not enough values?
        if (numValues == 0) {
            return 0;
        }

        String fromString = (String)state.getFromKey(AbstractMLNode.FROM);
        String fromFeatures[] = fromString.split(AbstractMLNode.FROM_SEPARATOR);
        String timeName = (String)state.getFromKey(TIME_FEATURE_KEY);
        int timeIndex = 0;
        for (int i=1;i<fromFeatures.length;i++){
            if (fromFeatures[i].equals(timeName)){
                timeIndex = i;
                break ;
            }
        }
        double periodsList[] = state.getFromKeyWithDefault(PERIODS_LIST_KEY, new double[0]);

        double coefficients[] = state.getFromKeyWithDefault(COEFFICIENTS_KEY, COEFFICIENTS_DEF);
        double sinComponent[] = state.getFromKeyWithDefault(SINUS_KEY, new double[0]);
        double cosComponent[] = state.getFromKeyWithDefault(COSINE_KEY, new double[0]);
        int index = 0;
        double intercept = state.getFromKeyWithDefault(INTERCEPT_KEY, INTERCEPT_DEF);
        double sqrResidualSum = 0;
        while (startIndex + dims <= valueBuffer.length) { //For each value
            double curValue[] = new double[dims];
            System.arraycopy(valueBuffer, startIndex, curValue, 0, dims);
            double response = 0;
            for (int i=0;i<curValue.length;i++){
                response += coefficients[i]*curValue[i];
            }
            //Sinus for this periodic component
            for (int i = 0; i < periodsList.length; i++) {
                double time = curValue[timeIndex];
                response += sinComponent[i]*Math.sin(time*2*Math.PI/periodsList[i]);
                response += cosComponent[i]*Math.cos(time*2*Math.PI/periodsList[i]);
            }
            response += intercept;

            sqrResidualSum += (response - results[index])*(response - results[index]);

            //Continue the loop
            startIndex += dims;
            index++;
        }
        return sqrResidualSum / numValues;
    }
}
