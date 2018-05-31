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

class Tanh implements Activation {

    private static Tanh static_unit = null;

    public static Tanh instance() {
        if (static_unit == null) {
            static_unit = new Tanh();
        }
        return static_unit;
    }

    /**
     * {@native ts
     * return (Math.exp(x) + Math.exp(-x)) / 2;
     * }
     */
    private static double cosh(double x) {
        return Math.cosh(x);
    }

    /**
     * {@native ts
     * if (x === Infinity) {return 1;}
     * if (x === -Infinity) {return -1;}
     * let y = Math.exp(x * 2);
     * return (y - 1) / (y + 1);
     * }
     */
    @Override
    public double forward(double x) {
        return Math.tanh(x);
    }

    @Override
    public double backward(double x, double fct) {
        return 1 - fct * fct;
    }


}
