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

import greycat.struct.DMatrix;
import greycat.struct.matrix.VolatileDMatrix;

public class Losses {
    public static final int SUM_OF_SQUARES = 0;
    public static final int ABSTRACT_VALUE = 1;
    public static final int LOG_LOSS = 2;
    public static final int ARG_MAX = 3;
    public static final int MULTI_DIM_BINARY = 4;
    public static final int SOFTMAX_LOG_LOSS = 5;


    public static final int DEFAULT = SUM_OF_SQUARES;

    public static Loss getUnit(int lossUnit, double[] weights) {
        if (weights == null) {
            switch (lossUnit) {
                case SUM_OF_SQUARES:
                    return SumOfSquares.instance();
                case ABSTRACT_VALUE:
                    return AbstractValue.instance();
                case LOG_LOSS:

                    return LogLoss.instance();
                case ARG_MAX:
                    return ArgMax.instance();
                case MULTI_DIM_BINARY:
                    return MultiDimensionalBinary.instance();
                case SOFTMAX_LOG_LOSS:
                    return SoftMaxLogLoss.instance();
            }
        } else {
            switch (lossUnit) {
                case SUM_OF_SQUARES:
                    return new SumOfSquares(weights);
                case ABSTRACT_VALUE:
                    return new AbstractValue(weights);
                case LOG_LOSS:
                    return new LogLoss(weights);
                case ARG_MAX:
                    return ArgMax.instance();
                case MULTI_DIM_BINARY:
                    return MultiDimensionalBinary.instance();
                case SOFTMAX_LOG_LOSS:
                    return new SoftMaxLogLoss(weights);
            }
        }
        return getUnit(DEFAULT, null);
    }


    public static DMatrix sumOverOutputsMatrix(DMatrix losses) {
        DMatrix res = VolatileDMatrix.empty(losses.rows(), 1);
        for (int i = 0; i < losses.rows(); i++) {
            for (int j = 0; j < losses.columns(); j++) {
                res.add(i, 0, losses.get(i, j));
            }
        }
        return res;
    }


    public static void processRMSE(DMatrix err, int counter) {
        for (int i = 0; i < err.rows(); i++) {
            for (int j = 0; j < err.columns(); j++) {
                err.set(i, j, Math.sqrt(err.get(i, j) * 2 / counter));
            }
        }
    }

    public static void processAVGErr(DMatrix err, int counter) {
        for (int i = 0; i < err.rows(); i++) {
            for (int j = 0; j < err.columns(); j++) {
                err.set(i, j, err.get(i, j) / counter);
            }
        }
    }

    public static void inverseNormalizeError(DMatrix error, double[] std) {
        for (int j = 0; j < error.rows(); j++) {
            double factor = std[j] * std[j];
            for (int i = 0; i < error.columns(); i++) {
                error.set(j, i, error.get(j, i) * factor);
            }
        }
    }


    public static double[] sumOverOutputs(DMatrix losses) {
        double[] res = new double[losses.columns()];
        for (int i = 0; i < losses.columns(); i++) {
            for (int j = 0; j < losses.rows(); j++) {
                res[i] += losses.get(j, i);
            }
        }
        return res;
    }

    public static double sumOfLosses(DMatrix losses) {
        double res = 0;
        int len = losses.length();
        for (int i = 0; i < len; i++) {
            res += losses.unsafeGet(i);
        }
        return res;
    }

    public static double sumOfLosses(DMatrix[] losses) {
        double res = 0;
        for (DMatrix loss : losses) {
            if (loss != null) {
                int len = loss.length();
                for (int i = 0; i < len; i++) {
                    res += loss.unsafeGet(i);
                }
            }
        }
        return res;
    }

    public static double avgOfLosses(DMatrix[] losses) {
        int total = 0;
        for (DMatrix loss : losses) {
            if (loss != null) {
                total += loss.length();
            }
        }
        return sumOfLosses(losses) / total;
    }

    public static double avgOfLosses(DMatrix losses) {
        return sumOfLosses(losses) / losses.length();
    }

    public static double[] avgLossPerOutput(DMatrix losses) {
        double[] res = new double[losses.rows()];
        for (int i = 0; i < losses.rows(); i++) {
            for (int j = 0; j < losses.columns(); j++) {
                res[i] += losses.get(i, j);
            }
            res[i] = res[i] / losses.columns();
        }
        return res;
    }
}
