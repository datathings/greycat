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
package greycat.ml.math;

import greycat.struct.DMatrix;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.TransposeType;
import greycat.struct.matrix.VolatileDMatrix;

public class PolynomialFit {

    private DMatrix coef;
    private int degree = 0;

    public PolynomialFit(int degree) {
        this.degree = degree;
    }

    public static double extrapolate(double time, double[] weights) {
        double result = 0;
        double power = 1;
        for (int j = 0; j < weights.length; j++) {
            result += weights[j] * power;
            power = power * time;
        }
        return result;
    }

    public static double rmse(double[] time, double[] values, double[] coef) {
        double rmse = 0;
        double ex;
        for (int i = 0; i < time.length; i++) {
            ex = extrapolate(time[i], coef);
            rmse += (ex - values[i]) * (ex - values[i]);
        }
        rmse = rmse / time.length;
        return Math.sqrt(rmse);
    }

    public double[] getCoef() {
        return coef.data();
    }

    public void fit(double samplePoints[], double[] observations) {
        DMatrix y = VolatileDMatrix.wrap(observations, observations.length, 1);
        DMatrix a = VolatileDMatrix.empty(y.rows(), degree + 1);
        // cset up the A matrix
        for (int i = 0; i < observations.length; i++) {
            double obs = 1;
            for (int j = 0; j < degree + 1; j++) {
                a.set(i, j, obs);
                obs *= samplePoints[i];
            }
        }
        // processValues the A matrix and see if it failed
        coef = MatrixOps.defaultEngine().solveQR(a, y, true, TransposeType.NOTRANSPOSE);
    }


}