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
package greycat.ml.neuralnet;

import greycat.ml.neuralnet.process.ExMatrix;
import greycat.ml.neuralnet.process.ProcessGraph;
import greycat.struct.matrix.RandomGenerator;

public abstract class Layer {

    public abstract ExMatrix forward(ExMatrix input, ProcessGraph g);

    public abstract ExMatrix[] getLayerParameters();

    public abstract Layer init(int inputs, int outputs, int activationUnit, double[] activationParams, RandomGenerator random, double std);

    public abstract Layer reInit(RandomGenerator random, double std);

    public abstract void resetState();

    public abstract int inputDimensions();

    public abstract int outputDimensions();

}
