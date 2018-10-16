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

public class AbstractValue implements Loss {
    private static AbstractValue static_unit = null;
    private double[] weights;

    public static AbstractValue instance() {
        if (static_unit == null) {
            static_unit = new AbstractValue(null);
        }
        return static_unit;
    }


    public AbstractValue(double[] weights) {
        this.weights = weights;
    }

    @Override
    public void backward(ExMatrix actualOutput, ExMatrix targetOutput) {
        final int len = targetOutput.length();

        for (int i = 0; i < len; i++) {
            double errDelta = actualOutput.unsafeGet(i) - targetOutput.unsafeGet(i);  //double errDelta = actualOutput.w[i] - targetOutput.w[i];
            //the derivation of Abstract value is either 1 or -1 |X| = x or -x.
            if (errDelta > 0) {
                errDelta = 1;
            } else {
                errDelta = -1;
            }
            actualOutput.getDw().unsafeSet(i, actualOutput.getDw().unsafeGet(i) + errDelta); //actualOutput.dw[i] += errDelta;
        }

        if (weights != null) {
            for (int row = 0; row < actualOutput.getDw().rows(); row++) {
                for (int col = 0; col < actualOutput.getDw().columns(); col++) {
                    actualOutput.getDw().set(row, col, actualOutput.getDw().get(row, col) * weights[row]);
                }
            }
        }

    }

    @Override
    public DMatrix forward(DMatrix actualOutput, DMatrix targetOutput) {
        MatrixOps.testDim(actualOutput, targetOutput);
        DMatrix res = VolatileDMatrix.empty(actualOutput.rows(), actualOutput.columns());
        int len = targetOutput.length();
        double errDelta;
        for (int i = 0; i < len; i++) {
            errDelta = actualOutput.unsafeGet(i) - targetOutput.unsafeGet(i);
            res.unsafeSet(i, Math.abs(errDelta));
        }
        if (weights != null) {
            for (int row = 0; row < res.rows(); row++) {
                for (int col = 0; col < res.columns(); col++) {
                    res.set(row, col, res.get(row, col) * weights[row]);
                }
            }
        }
        return res;
    }
}
