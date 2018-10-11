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
package greycat.ml.neuralnet.activation;

class LeCunTanh implements Activation {

    private static LeCunTanh static_unit = null;

    public static LeCunTanh instance() {
        if (static_unit == null) {
            static_unit = new LeCunTanh();
        }
        return static_unit;
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
        return 1.7159 * Math.tanh(2 * x / 3);
    }

    @Override
    public double backward(double x, double fct) {
        return 1.143933333333333 * (1 - (fct * fct / 2.94431281));
    }

}
