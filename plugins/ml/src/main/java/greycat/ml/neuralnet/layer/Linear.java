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
package greycat.ml.neuralnet.layer;

import greycat.Type;
import greycat.ml.neuralnet.process.ExMatrix;
import greycat.ml.neuralnet.process.ProcessGraph;
import greycat.struct.EStruct;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.RandomGenerator;

// Returns Weights*Input
// Can be used as PCA or dimensionality reduction of data, since here we are combining linearly outputDimensions from input
// There is no non-linearity here, since there is no activation function.

class Linear implements Layer {
    private static String WEIGHTS = "weights";
    private EStruct host;

    private ExMatrix weights;
    private ExMatrix[] params = null;

    Linear(EStruct hostnode) {
        if (hostnode == null) {
            throw new RuntimeException("Host node can't be null");
        }
        weights = new ExMatrix(hostnode, WEIGHTS);
        this.host = hostnode;
    }

    @Override
    public Layer init(int inputs, int outputs, int activationUnit, double[] activationParams, RandomGenerator random, double std) {
        //First always set the type
        host.set(Layers.TYPE, Type.INT, Layers.LINEAR_LAYER);
        weights.init(outputs, inputs);
        return reInit(random, std);

    }

    @Override
    public Layer reInit(RandomGenerator random, double std) {
        if (random != null && std != 0) {
            MatrixOps.fillWithRandomStd(weights, random, std);
        }
        return this;
    }

    @Override
    public ExMatrix forward(ExMatrix input, ProcessGraph g) {
        return g.mul(weights, input);
    }


    @Override
    public ExMatrix[] getLayerParameters() {
        if (params == null) {
            params = new ExMatrix[]{weights};
        }
        return params;
    }

    @Override
    public void resetState() {

    }

    @Override
    public int inputDimensions() {
        return weights.columns();
    }

    @Override
    public int outputDimensions() {
        return weights.rows();
    }
}
