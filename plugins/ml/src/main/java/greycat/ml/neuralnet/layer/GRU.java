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
import greycat.struct.EStruct;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.RandomGenerator;
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

        context = new ExMatrix(hostnode, CONTEXT);
        this.host = hostnode;
    }


    @Override
    public Layer init(int inputs, int outputs, int activationUnit, double[] activationParams, RandomInterface random, double std) {
        host.set(Layers.TYPE, Type.INT, Layers.GRU_LAYER);

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

        return reInit(random, std);
    }

    @Override
    public Layer reInit(RandomInterface random, double std) {
        //todo check why bias are not initialized randomly
        if (random != null && std != 0) {
            MatrixOps.fillWithRandomStd(ihmix, random, std);
            MatrixOps.fillWithRandomStd(hhmix, random, std);
            //MatrixOps.fillWithRandomStd(bmix,random,std);

            MatrixOps.fillWithRandomStd(ihnew, random, std);
            MatrixOps.fillWithRandomStd(hhnew, random, std);
            // MatrixOps.fillWithRandomStd(bnew,random,std);

            MatrixOps.fillWithRandomStd(ihreset, random, std);
            MatrixOps.fillWithRandomStd(hhreset, random, std);
            // MatrixOps.fillWithRandomStd(breset,random,std);
        }
        return this;
    }


    @Override
    public ExMatrix forward(ExMatrix input, ProcessGraph g) {
        if (input.columns() != 1) {
            throw new RuntimeException("GRU can't process more than 1 input vector at a time!");
        }

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
        context = output;

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
    }

    @Override
    public int inputDimensions() {
        return ihmix.columns();
    }

    @Override
    public int outputDimensions() {
        return ihmix.rows();
    }
}
