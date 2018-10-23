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
package greycat.ml.neuralnet.layer;

import greycat.Type;
import greycat.ml.neuralnet.activation.Activation;
import greycat.ml.neuralnet.activation.Activations;
import greycat.ml.neuralnet.process.ExMatrix;
import greycat.ml.neuralnet.process.ProcessGraph;
import greycat.ml.neuralnet.process.WeightInit;
import greycat.struct.DoubleArray;
import greycat.struct.EStruct;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.RandomInterface;

class RNN implements Layer {
    private static String WEIGHTS = "weights";
    private static String BIAS = "bias";
    private static String ACTIVATION = "activation";
    private static String ACTIVATION_PARAM = "activation_param";
    private static String CONTEXT = "context";

    private ExMatrix weights;
    private ExMatrix bias;

    private ExMatrix context;
    private ExMatrix internalContext;

    private Activation activation;
    private EStruct host;
    private ExMatrix[] params = null;

    RNN(EStruct hostnode) {
        if (hostnode == null) {
            throw new RuntimeException("Host node can't be null");
        }
        weights = new ExMatrix(hostnode, WEIGHTS);
        bias = new ExMatrix(hostnode, BIAS);
        internalContext = new ExMatrix(hostnode, CONTEXT);
        context = ExMatrix.deepCopy(internalContext);
        activation = Activations.getUnit(hostnode.getWithDefault(ACTIVATION, Activations.SIGMOID),  ((DoubleArray)hostnode.getOrCreate(ACTIVATION_PARAM, Type.DOUBLE_ARRAY)).extract());
        this.host = hostnode;
    }


    @Override
    public Layer create(int inputs, int outputs, int activationUnit, double[] activationParams) {
        //First always set the type
        host.set(Layers.LAYER_TYPE, Type.INT, Layers.RNN_LAYER);
        weights.init(outputs, inputs + outputs);
        bias.init(outputs, 1);
        context.init(outputs, 1);
        activation = Activations.getUnit(activationUnit, activationParams);
        host.set(ACTIVATION, Type.INT, activationUnit);
        if (activationParams != null) {
            ((DoubleArray) host.getOrCreate(ACTIVATION_PARAM, Type.DOUBLE_ARRAY)).initWith(activationParams);
        }
        return this;

    }


    @Override
    public Layer init(int weightInitType, RandomInterface random, double std) {
        WeightInit.init(weights, weightInitType, random, std);
//        WeightInit.init(bias, weightInitType, random, std);
        return this;
    }

    @Override
    public ExMatrix forward(ExMatrix input, ProcessGraph g) {

        ExMatrix output;
        if (input.columns() != 1) {
            ExMatrix[] inSplit = new ExMatrix[input.columns()];
            ExMatrix[] outSplit = new ExMatrix[input.columns()];

            for (int i = 0; i < input.columns(); i++) {
                inSplit[i] = g.extractColumn(input, i);
            }

            for (int i = 0; i < input.columns(); i++) {
                outSplit[i] = internalForward(inSplit[i], g);
            }
            output = g.concatColumns(outSplit);

        } else {
            output = internalForward(input, g);
        }
        MatrixOps.copy(context.getW(), internalContext.getW());
        return output;
    }

    public ExMatrix internalForward(ExMatrix input, ProcessGraph g) {
        if (input.columns() != 1) {
            throw new RuntimeException("RNN can't process more than 1 input vector at a time!");
        }
        ExMatrix concat = g.concatVectors(input, context);
        ExMatrix sum = g.mul(weights, concat);
        sum = g.add(sum, bias);
        ExMatrix output = g.activate(activation, sum);
        //rollover activations for next iteration
        context = g.assign(output);

        return output;
    }


    @Override
    public ExMatrix[] getLayerParameters() {
        if (params == null) {
            params = new ExMatrix[]{weights, bias};
        }
        return params;
    }

    @Override
    public void resetState() {
        context.getW().fill(0);
        context.getDw().fill(0);
        context.getStepCache().fill(0);

        internalContext.getW().fill(0);
        internalContext.getDw().fill(0);
        internalContext.getStepCache().fill(0);
    }

    @Override
    public int inputDimensions() {
        return weights.columns() - weights.rows();
    }

    @Override
    public int outputDimensions() {
        return weights.rows();
    }

    @Override
    public void print() {
        System.out.println("Layer RNN");
        MatrixOps.print(weights, "weights");
        MatrixOps.print(bias, "bias");
        MatrixOps.print(context, "context");
    }
}
