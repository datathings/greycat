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
package greycat.ml.neuralnet.loss;

import greycat.ml.neuralnet.process.ExMatrix;
import greycat.struct.DMatrix;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.VolatileDMatrix;

public class LogLoss implements Loss {
    private double[] weights;
    private static LogLoss static_unit = null;

    public static Loss instance() {
        if (static_unit == null) {
            static_unit = new LogLoss(null);
        }
        return static_unit;
    }

    public LogLoss(double[] weights) {
        this.weights = weights;
    }

    @Override
    public void backward(ExMatrix actualOutput, ExMatrix targetOutput) {
        if (weights == null) {
            final int len = targetOutput.length();
            for (int i = 0; i < len; i++) {
                double errDelta = -targetOutput.unsafeGet(i) / Math.max(actualOutput.unsafeGet(i), MatrixOps.PROBA_EPS);  //double errDelta = 2*(actualOutput.w[i] - targetOutput.w[i]);
                actualOutput.getDw().unsafeSet(i, actualOutput.getDw().unsafeGet(i) + errDelta); //actualOutput.dw[i] += errDelta;
            }
        } else {
            for (int row = 0; row < targetOutput.rows(); row++) {
                for (int col = 0; col < targetOutput.columns(); col++) {
                    double errDelta = -targetOutput.get(row, col) * weights[row] / Math.max(actualOutput.get(row, col), MatrixOps.PROBA_EPS);
                    actualOutput.getDw().add(row, col, errDelta);
                }
            }
        }
    }

    //todo divide by batch size
    @Override
    public DMatrix forward(DMatrix actualOutput, DMatrix targetOutput) {
        MatrixOps.testDim(actualOutput, targetOutput);
        DMatrix res = VolatileDMatrix.empty(actualOutput.rows(), actualOutput.columns());

        if (weights == null) {
            int len = targetOutput.length();
            for (int i = 0; i < len; i++) {
                res.unsafeSet(i, -targetOutput.unsafeGet(i) * Math.log(Math.max(actualOutput.unsafeGet(i), MatrixOps.PROBA_EPS)));
            }
        } else {
            for (int row = 0; row < res.rows(); row++) {
                for (int col = 0; col < res.columns(); col++) {
                    res.set(row, col, -targetOutput.get(row, col) * Math.log(Math.max(actualOutput.get(row, col), MatrixOps.PROBA_EPS)) * weights[row]);
                }
            }
        }
        return res;
    }
}
