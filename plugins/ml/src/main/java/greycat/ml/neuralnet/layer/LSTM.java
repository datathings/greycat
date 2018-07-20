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
import greycat.ml.neuralnet.activation.Activation;
import greycat.ml.neuralnet.activation.Activations;
import greycat.ml.neuralnet.process.ExMatrix;
import greycat.ml.neuralnet.process.ProcessGraph;
import greycat.struct.EStruct;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.RandomGenerator;

class LSTM implements Layer {

    private static String WIX = "wix";
    private static String WIH = "wih";
    private static String BI = "bi";

    private static String WFX = "wfx";
    private static String WFH = "wfh";
    private static String BF = "bf";

    private static String WOX = "wox";
    private static String WOH = "woh";
    private static String BO = "bo";

    private static String WCX = "wcx";
    private static String WCH = "wch";
    private static String BC = "bc";

    private static String HIDDEN_CONTEXT = "hiddencontext";
    private static String CELL_CONTEXT = "cellcontext";


    private ExMatrix wix, wih, bi;
    private ExMatrix wfx, wfh, bf;
    private ExMatrix wox, woh, bo;
    private ExMatrix wcx, wch, bc;

    private ExMatrix hiddenContext;
    private ExMatrix cellContext;

    private Activation fInputGate = Activations.getUnit(Activations.SIGMOID, null);
    private Activation fForgetGate = Activations.getUnit(Activations.SIGMOID, null);
    private Activation fOutputGate = Activations.getUnit(Activations.SIGMOID, null);
    private Activation fCellInput = Activations.getUnit(Activations.TANH, null);
    private Activation fCellOutput = Activations.getUnit(Activations.TANH, null);

    private EStruct host;
    private ExMatrix[] params = null;


    LSTM(EStruct hostnode) {
        if (hostnode == null) {
            throw new RuntimeException("Host node can't be null");
        }
        wix = new ExMatrix(hostnode, WIX);
        wih = new ExMatrix(hostnode, WIH);
        bi = new ExMatrix(hostnode, BI);

        wfx = new ExMatrix(hostnode, WFX);
        wfh = new ExMatrix(hostnode, WFH);
        bf = new ExMatrix(hostnode, BF);

        wox = new ExMatrix(hostnode, WOX);
        woh = new ExMatrix(hostnode, WOH);
        bo = new ExMatrix(hostnode, BO);

        wcx = new ExMatrix(hostnode, WCX);
        wch = new ExMatrix(hostnode, WCH);
        bc = new ExMatrix(hostnode, BC);

        hiddenContext = new ExMatrix(hostnode, HIDDEN_CONTEXT);
        cellContext = new ExMatrix(hostnode, CELL_CONTEXT);
        this.host = hostnode;
    }


    @Override
    public Layer init(int inputs, int outputs, int activationUnit, double[] activationParams, RandomGenerator random, double std) {
        host.set(Layers.TYPE, Type.INT, Layers.LSTM_LAYER);

        wix.init(outputs, inputs);
        wih.init(outputs, outputs);
        bi.init(outputs, 1);

        wfx.init(outputs, inputs);
        wfh.init(outputs, outputs);
        bf.init(outputs, 1);

        wox.init(outputs, inputs);
        woh.init(outputs, outputs);
        bo.init(outputs, 1);

        wcx.init(outputs, inputs);
        wch.init(outputs, outputs);
        bc.init(outputs, 1);

        hiddenContext.init(outputs, 1);
        cellContext.init(outputs, 1);

        return reInit(random, std);

    }

    @Override
    public Layer reInit(RandomGenerator random, double std) {
        //todo check why bias are not initialized randomly
        if (random != null && std != 0) {
            MatrixOps.fillWithRandomStd(wix, random, std);
            MatrixOps.fillWithRandomStd(wih, random, std);
            // MatrixOps.fillWithRandomStd(bi, random, std);

            MatrixOps.fillWithRandomStd(wfx, random, std);
            MatrixOps.fillWithRandomStd(wfh, random, std);
            //set forget bias to 1.0, as described here: http://jmlr.org/proceedings/papers/v37/jozefowicz15.pdf
            bf.fill(1.0);

            MatrixOps.fillWithRandomStd(wox, random, std);
            MatrixOps.fillWithRandomStd(woh, random, std);
            //MatrixOps.fillWithRandomStd(bo, random, std);

            MatrixOps.fillWithRandomStd(wcx, random, std);
            MatrixOps.fillWithRandomStd(wch, random, std);
            //MatrixOps.fillWithRandomStd(bc, random, std);
        }

        return this;
    }

    @Override
    public ExMatrix forward(ExMatrix input, ProcessGraph g) {

        if (input.columns() != 1) {
            ExMatrix[] outSplit = new ExMatrix[input.columns()];
            for (int i = 0; i < input.columns(); i++) {
                outSplit[i] = internalForward(g.extractColumn(input, i), g);
            }
            return g.concatColumns(outSplit);

        } else {
            return internalForward(input, g);
        }
    }


    private ExMatrix internalForward(ExMatrix input, ProcessGraph g) {
        //input gate
        ExMatrix sum0 = g.mul(wix, input);
        ExMatrix sum1 = g.mul(wih, hiddenContext);
        ExMatrix inputGate = g.activate(fInputGate, g.add(g.add(sum0, sum1), bi));

        //forget gate
        ExMatrix sum2 = g.mul(wfx, input);
        ExMatrix sum3 = g.mul(wfh, hiddenContext);
        ExMatrix forgetGate = g.activate(fForgetGate, g.add(g.add(sum2, sum3), bf));

        //output gate
        ExMatrix sum4 = g.mul(wox, input);
        ExMatrix sum5 = g.mul(woh, hiddenContext);
        ExMatrix outputGate = g.activate(fOutputGate, g.add(g.add(sum4, sum5), bo));

        //write operation on cells
        ExMatrix sum6 = g.mul(wcx, input);
        ExMatrix sum7 = g.mul(wch, hiddenContext);
        ExMatrix cellInput = g.activate(fCellInput, g.add(g.add(sum6, sum7), bc));

        //compute new cell activation
        ExMatrix retainCell = g.elmul(forgetGate, cellContext);
        ExMatrix writeCell = g.elmul(inputGate, cellInput);
        ExMatrix cellAct = g.add(retainCell, writeCell);

        //compute hidden state as gated, saturated cell activations
        ExMatrix output = g.elmul(outputGate, g.activate(fCellOutput, cellAct));

        //rollover activations for next iteration
        hiddenContext = output;
        cellContext = cellAct;

        return output;
    }


    @Override
    public ExMatrix[] getLayerParameters() {
        if (params == null) {
            params = new ExMatrix[]{
                    wix, wih, bi,
                    wfx, wfh, bf,
                    wox, woh, bo,
                    wcx, wch, bc
            };
        }
        return params;
    }

    @Override
    public void resetState() {
        hiddenContext.getW().fill(0);
        hiddenContext.getDw().fill(0);
        hiddenContext.getStepCache().fill(0);

        cellContext.getW().fill(0);
        cellContext.getDw().fill(0);
        cellContext.getStepCache().fill(0);
    }

    @Override
    public int inputDimensions() {
        return wix.columns();
    }

    @Override
    public int outputDimensions() {
        return wix.rows();
    }
}
