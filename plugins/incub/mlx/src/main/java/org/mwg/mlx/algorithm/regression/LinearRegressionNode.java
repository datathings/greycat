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
import org.mwg.ml.common.matrix.MatrixOps;
import org.mwg.ml.common.matrix.TransposeType;
import org.mwg.ml.common.matrix.VolatileMatrix;
import org.mwg.mlx.algorithm.AbstractLinearRegressionNode;
import org.mwg.plugin.NodeState;
import org.mwg.struct.Matrix;

public class LinearRegressionNode extends AbstractLinearRegressionNode {

    public static final String NAME = "LinearRegressionBatch";

    public LinearRegressionNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    @Override
    protected void updateModelParameters(NodeState state, double currentBuffer[], double resultBuffer[], double value[], double currentResult) {
        int dims = state.getFromKeyWithDefault(INPUT_DIM_KEY, INPUT_DIM_UNKNOWN);
        if (dims == INPUT_DIM_UNKNOWN) {
            dims = value.length;
            state.setFromKey(INPUT_DIM_KEY, Type.INT, dims);
        }
        //Adding place for intercepts
        final double reshapedValue[] = new double[currentBuffer.length + resultBuffer.length];

        final double l2 = state.getFromKeyWithDefault(L2_COEF_KEY, L2_COEF_DEF);

        //Step 1. Re-arrange to column-based format.
        for (int i = 0; i < resultBuffer.length; i++) {
            reshapedValue[i] = 1; //First column is 1 (for intercepts)
        }
        for (int i = 0; i < resultBuffer.length; i++) {
            for (int j = 0; j < dims; j++) {
                reshapedValue[(j + 1) * resultBuffer.length + i] = currentBuffer[i * dims + j];
            }
        }

        Matrix xMatrix = VolatileMatrix.wrap(reshapedValue, resultBuffer.length, dims + 1);
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

        final double newCoefficients[] = new double[dims];
        for (int i = 0; i < dims; i++) {
            newCoefficients[i] = result.get(i + 1, 0);
        }
        state.setFromKey(INTERCEPT_KEY, Type.DOUBLE, result.get(0, 0));
        state.setFromKey(COEFFICIENTS_KEY, Type.DOUBLE_ARRAY, newCoefficients);
    }
}
