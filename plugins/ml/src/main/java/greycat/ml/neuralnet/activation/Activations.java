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
package greycat.ml.neuralnet.activation;

import greycat.ml.neuralnet.Activation;

public class Activations {
    public static final int LINEAR = 0;
    public static final int SIGMOID = 1;
    public static final int SINE = 2;
    public static final int TANH = 3;
    public static final int LECUN_TANH = 4;
    public static final int RECTIFIED_LINEAR = 5;

    public static final int DEFAULT = LINEAR;

    public static Activation getUnit(int activationUnit, double[] unitArgs) {
        switch (activationUnit) {
            case LINEAR:
                return Linear.instance();
            case SIGMOID:
                return Sigmoid.instance();
            case SINE:
                return Sine.instance();
            case TANH:
                return Tanh.instance();
            case LECUN_TANH:
                return LeCunTanh.instance();
            case RECTIFIED_LINEAR:
                return new RectifiedLinear(unitArgs[0]);
        }
        return getUnit(DEFAULT, null);
    }

}
