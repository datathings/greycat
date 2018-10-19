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
import greycat.struct.EStruct;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.RandomInterface;

class GRU implements Layer {

    private static String IHMIX = "ihmix";
    private static String HHMIX = "hhmix";
    private static String BMIX = "bmix";

    private static String IHNEW = "ihnew";
    private static String HHNEW = "hhnew";
    private static String BNEW = "bnew";

    private static String IHRESET = "ihreset";
    private static String HHRESET = "hhreset";
    private static String BRESET = "breset";

    private static String CONTEXT = "context";

    private ExMatrix ihmix, hhmix, bmix;
    private ExMatrix ihnew, hhnew, bnew;
    private ExMatrix ihreset, hhreset, breset;

    private ExMatrix context;
    private ExMatrix internalContext;


    private Activation fMix = Activations.getUnit(Activations.SIGMOID, null);
    private Activation fReset = Activations.getUnit(Activations.SIGMOID, null);
    private Activation fNew = Activations.getUnit(Activations.TANH, null);


    private EStruct host;
    private ExMatrix[] params = null;


    GRU(EStruct hostnode) {
        if (hostnode == null) {
            throw new RuntimeException("Host node can't be null");
        }
        ihmix = new ExMatrix(hostnode, IHMIX);
        hhmix = new ExMatrix(hostnode, HHMIX);
        bmix = new ExMatrix(hostnode, BMIX);

        ihnew = new ExMatrix(hostnode, IHNEW);
        hhnew = new ExMatrix(hostnode, HHNEW);
        bnew = new ExMatrix(hostnode, BNEW);

        ihreset = new ExMatrix(hostnode, IHRESET);
        hhreset = new ExMatrix(hostnode, HHRESET);
        breset = new ExMatrix(hostnode, BRESET);

        context = new ExMatrix(null, null);
        internalContext = new ExMatrix(hostnode, CONTEXT);
        this.host = hostnode;
    }


    @Override
    public Layer create(int inputs, int outputs, int activationUnit, double[] activationParams) {
        host.set(Layers.LAYER_TYPE, Type.INT, Layers.GRU_LAYER);

        ihmix.init(outputs, inputs);
        hhmix.init(outputs, outputs);
        bmix.init(outputs, 1);

        ihnew.init(outputs, inputs);
        hhnew.init(outputs, outputs);
        bnew.init(outputs, 1);

        ihreset.init(outputs, inputs);
        hhreset.init(outputs, outputs);
        breset.init(outputs, 1);

        context.init(outputs, 1);
        internalContext.init(outputs, 1);
        return this;
    }

    @Override
    public Layer init(int weightInitType, RandomInterface random, double std) {
        WeightInit.init(ihmix, weightInitType, random, std);
        WeightInit.init(hhmix, weightInitType, random, std);
//        WeightInit.init(bmix, weightInitType, random, std);

        WeightInit.init(ihnew, weightInitType, random, std);
        WeightInit.init(hhnew, weightInitType, random, std);
//        WeightInit.init(bnew, weightInitType, random, std);

        WeightInit.init(ihreset, weightInitType, random, std);
        WeightInit.init(hhreset, weightInitType, random, std);
//        WeightInit.init(breset, weightInitType, random, std);

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

    private ExMatrix internalForward(ExMatrix input, ProcessGraph g) {
        ExMatrix sum0 = g.mul(ihmix, input);
        ExMatrix sum1 = g.mul(hhmix, context);
        ExMatrix actMix = g.activate(fMix, g.add(g.add(sum0, sum1), bmix));

        ExMatrix sum2 = g.mul(ihreset, input);
        ExMatrix sum3 = g.mul(hhreset, context);
        ExMatrix actReset = g.activate(fReset, g.add(g.add(sum2, sum3), breset));

        ExMatrix sum4 = g.mul(ihnew, input);
        ExMatrix gatedContext = g.elmul(actReset, context);
        ExMatrix sum5 = g.mul(hhnew, gatedContext);
        ExMatrix actNewPlusGatedContext = g.activate(fNew, g.add(g.add(sum4, sum5), bnew));

        ExMatrix memvals = g.elmul(actMix, context);
        ExMatrix newvals = g.elmul(g.oneMinus(actMix), actNewPlusGatedContext);
        ExMatrix output = g.add(memvals, newvals);

        //rollover activations for next iteration
        context = g.assign(output);

        return output;
    }


    @Override
    public ExMatrix[] getLayerParameters() {
        if (params == null) {
            params = new ExMatrix[]{ihmix, hhmix, bmix, ihnew, hhnew, bnew, ihreset, hhreset, breset};
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
        return ihmix.columns();
    }

    @Override
    public int outputDimensions() {
        return ihmix.rows();
    }

    @Override
    public void print() {
        System.out.println("Layer GRU");
        MatrixOps.print(ihmix, "ihmix");
        MatrixOps.print(hhmix, "hhmix");
        MatrixOps.print(bmix, "bmix");

        MatrixOps.print(ihnew, "ihnew");
        MatrixOps.print(hhnew, "hhnew");
        MatrixOps.print(bnew, "bnew");

        MatrixOps.print(ihreset, "ihreset");
        MatrixOps.print(hhreset, "hhreset");
        MatrixOps.print(breset, "breset");

        MatrixOps.print(context, "context");
    }
}
