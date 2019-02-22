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
package greycat.ml.neuralnet.loss;

import greycat.ml.neuralnet.process.ExMatrix;
import greycat.struct.DMatrix;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.VolatileDMatrix;

class ArgMax implements Loss {

    private static ArgMax static_unit = null;

    public static ArgMax instance() {
        if (static_unit == null) {
            static_unit = new ArgMax();
        }
        return static_unit;
    }


    @Override
    public void backward(ExMatrix actualOutput, ExMatrix targetOutput) {
        throw new RuntimeException("not implemented");

    }

    //return wether or not for each training example, the maximum was the same 1 x training samples
    //if actual arg max = target arg max -> 0
    //else there is a loss -> 1
    @Override
    public DMatrix forward(DMatrix actualOutput, DMatrix targetOutput) {
        MatrixOps.testDim(actualOutput, targetOutput);
        double maxActual;
        double maxTarget;
        int indxMaxActual;
        int indxMaxTarget;
        double aw;
        double tw;
        DMatrix res = VolatileDMatrix.empty(1, actualOutput.columns());
        if (actualOutput.rows() == 1) {
            res.fill(0);
            return res;
        }
        for (int i = 0; i < actualOutput.columns(); i++) {
            maxActual = actualOutput.get(0, i);
            maxTarget = targetOutput.get(0, i);
            indxMaxActual = 0;
            indxMaxTarget = 0;
            for (int j = 1; j < actualOutput.rows(); j++) {
                aw = actualOutput.get(j, i);
                tw = targetOutput.get(j, i);
                if (aw > maxActual) {
                    maxActual = aw;
                    indxMaxActual = j;
                }
                if (tw > maxTarget) {
                    maxTarget = tw;
                    indxMaxTarget = j;
                }
            }
            if (indxMaxActual == indxMaxTarget) {
                res.set(0, i, 0);
            } else {
                res.set(0, i, 1);
            }
        }
        return res;
    }

}
