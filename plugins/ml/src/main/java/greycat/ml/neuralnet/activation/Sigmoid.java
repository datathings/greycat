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

class Sigmoid extends Activation {

    private static Sigmoid static_unit= null;

    public static Sigmoid instance() {
        if (static_unit == null) {
            static_unit = new Sigmoid();
        }
        return static_unit;
    }

    @Override
    public double forward(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    @Override
    public double backward(double x, double fct) {
        return fct * (1 - fct);
    }
}
