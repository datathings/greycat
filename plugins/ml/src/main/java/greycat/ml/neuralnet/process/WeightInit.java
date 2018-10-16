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
package greycat.ml.neuralnet.process;

import greycat.struct.DMatrix;
import greycat.struct.matrix.RandomInterface;

public class WeightInit {
//   Follow  https://deeplearning4j.org/api/latest/org/deeplearning4j/nn/weights/WeightInit.html for doc

    public final static int GAUSSIAN = 0;
    public final static int ZERO = 1;
    public final static int ONE = 2;
    public final static int SIGMOID_UNIFORM = 3;
    public final static int NORMAL = 4;
    public final static int LECUN_UNIFORM = 5;
    public final static int UNIFORM = 6;
    public final static int XAVIER = 7;
    public final static int XAVIER_UNIFORM = 8;
    public final static int RELU = 9;
    public final static int RELU_UNIFORM = 10;
    public final static int IDENTITY = 11;

    public final static int NORMAL_IN = 12;
    public final static int NORMAL_OUT = 13;
    public final static int NORMAL_AVG = 14;

    public final static int UNIFORM_IN = 15;
    public final static int UNIFORM_OUT = 16;
    public final static int UNIFORM_AVG = 17;
    public final static int DEFAULT = GAUSSIAN;


    public static void init(DMatrix weights, int initType, RandomInterface random, double std) {
        int len = weights.length();
        if (len == 0) {
            throw new RuntimeException("Set the weight dimension first");
        }
        double range = 1;
        boolean uniform = false;

        //in is weights.columns()
        //out is weights.rows()

        switch (initType) {
            case GAUSSIAN:
                range = std;
                uniform = false;
                break;
            case ZERO:
                weights.fill(0);
                return;
            case ONE:
                weights.fill(1);
                return;
            case SIGMOID_UNIFORM:
                uniform = true;
                range = 4 * Math.sqrt(6.0 / (weights.columns() + weights.rows()));
                break;
            case NORMAL:
                uniform = false;
                range = Math.sqrt(1.0 / weights.columns());
                break;
            case UNIFORM:
                uniform = true;
                range = Math.sqrt(1.0 / weights.columns());
                break;

// LECUN_UNIFORM Uniform U[-a,a] with a=3/sqrt(fanIn).
            case LECUN_UNIFORM:
                uniform = true;
                range = 3 * Math.sqrt(1.0 / weights.columns());
                break;
// XAVIER: As per Glorot and Bengio 2010: Gaussian distribution with mean 0, variance 2.0/(fanIn + fanOut)
            case XAVIER:
                uniform = false;
                range = Math.sqrt(2.0 / (weights.columns() + weights.rows()));
                break;

// XAVIER_UNIFORM: As per Glorot and Bengio 2010: Uniform distribution U(-s,s) with s = sqrt(6/(fanIn + fanOut))
            case XAVIER_UNIFORM:
                uniform = true;
                range = Math.sqrt(6.0 / (weights.columns() + weights.rows()));
                break;
// RELU: He et al. (2015), "Delving Deep into Rectifiers". Normal distribution with variance 2.0/nIn
            case RELU:
                uniform = false;
                range = Math.sqrt(2.0 / weights.columns());
                break;
// RELU_UNIFORM: He et al. (2015), "Delving Deep into Rectifiers". Uniform distribution U(-s,s) with s = sqrt(6/fanIn)
            case RELU_UNIFORM:
                uniform = true;
                range = Math.sqrt(6.0 / weights.columns());
                break;
            case IDENTITY:
                weights.fill(0);
                int ld = Math.min(weights.columns(), weights.rows());
                for (int i = 0; i < ld; i++) {
                    weights.set(i, i, 1.0);
                }
                return;
//  VAR_SCALING_NORMAL_FAN_IN Gaussian distribution with mean 0, variance 1.0/(fanIn)
            case NORMAL_IN:
                uniform = false;
                range = Math.sqrt(1.0 / weights.columns());
                break;
            case NORMAL_OUT:
//  VAR_SCALING_NORMAL_FAN_OUT Gaussian distribution with mean 0, variance 1.0/(fanOut)
                uniform = false;
                range = Math.sqrt(1.0 / weights.rows());
                break;
            case NORMAL_AVG:
//  VAR_SCALING_NORMAL_FAN_AVG Gaussian distribution with mean 0, variance 1.0/((fanIn + fanOut)/2)
                uniform = false;
                range = Math.sqrt(2.0 / (weights.columns() + weights.rows()));
                break;
//  VAR_SCALING_UNIFORM_FAN_IN Uniform U[-a,a] with a=3.0/(fanIn)
            case UNIFORM_IN:
                uniform = true;
                range = 3.0 / weights.columns();
                break;
//  VAR_SCALING_UNIFORM_FAN_OUT Uniform U[-a,a] with a=3.0/(fanOut)
            case UNIFORM_OUT:
                uniform = true;
                range = 3.0 / weights.rows();
                break;
//  VAR_SCALING_UNIFORM_FAN_AVG Uniform U[-a,a] with a=3.0/((fanIn + fanOut)/2)
            case UNIFORM_AVG:
                uniform = true;
                range = 6.0 / (weights.columns() + weights.rows());
                break;
        }


        if (uniform) {
            for (int i = 0; i < len; i++) {
                weights.unsafeSet(i, random.nextDouble() * range);
            }
        } else {
            for (int i = 0; i < len; i++) {
                weights.unsafeSet(i, random.nextGaussian() * range);
            }
        }
    }
}
