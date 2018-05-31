/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
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

public class SoftMaxLogLoss implements Loss {
    private static SoftMaxLogLoss static_unit = null;

    public static Loss instance() {
        if (static_unit == null) {
            static_unit = new SoftMaxLogLoss();
        }
        return static_unit;
    }

    @Override
    public void backward(ExMatrix actualOutput, ExMatrix targetOutput) {
        DMatrix prob = VolatileDMatrix.empty(actualOutput.rows(), actualOutput.columns());
        MatrixOps.softmax(actualOutput.getW(), prob);

        for (int col = 0; col < actualOutput.columns(); col++) {
            for (int row = 0; row < actualOutput.rows(); row++) {
                if (targetOutput.get(row, col) > 0) {
                    actualOutput.getDw().set(row, col, prob.get(row, col) - 1);
                } else {
                    actualOutput.getDw().set(row, col, prob.get(row, col));
                }
            }
        }
    }

    @Override
    public DMatrix forward(DMatrix actualOutput, DMatrix targetOutput) {
        MatrixOps.testDim(actualOutput, targetOutput);
        DMatrix res = VolatileDMatrix.empty(actualOutput.rows(), actualOutput.columns());


        double maxval = Double.NEGATIVE_INFINITY;
        int len = actualOutput.length();
        for (int i = 0; i < len; i++) {
            if (actualOutput.unsafeGet(i) > maxval) {
                maxval = actualOutput.unsafeGet(i);
            }
        }

        double p;
        for (int col = 0; col < actualOutput.columns(); col++) {
            double sum = 0;
            for (int row = 0; row < actualOutput.rows(); row++) {
                p = Math.exp(actualOutput.get(row, col) - maxval);
                sum += p;
            }
            sum = Math.log(sum);
            for (int row = 0; row < actualOutput.rows(); row++) {
                if (targetOutput.get(row, col) > 0) {
                    res.set(row, col, -targetOutput.get(row, col) * (actualOutput.get(row, col) - maxval - sum));
                }
            }
        }

        return res;
    }
}
