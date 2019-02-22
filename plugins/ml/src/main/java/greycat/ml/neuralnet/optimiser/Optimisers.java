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
package greycat.ml.neuralnet.optimiser;

import greycat.struct.EStruct;

public class Optimisers {
    public static final int GRADIENT_DESCENT = 0;
    public static final int RMSPROP = 1;
    public static final int MOMENTUM = 2;
    public static final int NESTEROV = 3;


    public static final int DEFAULT = GRADIENT_DESCENT;


    public static Optimiser getUnit(int learnerUnit, EStruct root) {
        switch (learnerUnit) {
            case GRADIENT_DESCENT:
                return new GradientDescent(root);
            case RMSPROP:
                return new RMSProp(root);
            case MOMENTUM:
                return new Momentum(root);
            case NESTEROV:
                return new Nesterov(root);
        }
        return getUnit(DEFAULT, root);
    }
}
