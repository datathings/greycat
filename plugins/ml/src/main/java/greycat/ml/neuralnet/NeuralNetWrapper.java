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
package greycat.ml.neuralnet;

import greycat.Type;
import greycat.ml.neuralnet.layer.Layer;
import greycat.ml.neuralnet.layer.Layers;
import greycat.ml.neuralnet.loss.Loss;
import greycat.ml.neuralnet.loss.Losses;
import greycat.ml.neuralnet.optimiser.Optimiser;
import greycat.ml.neuralnet.optimiser.Optimisers;
import greycat.ml.neuralnet.process.ExMatrix;
import greycat.ml.neuralnet.process.ProcessGraph;
import greycat.struct.DMatrix;
import greycat.struct.EStruct;
import greycat.struct.EStructArray;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.RandomInterface;
import greycat.struct.matrix.VolatileDMatrix;

public class NeuralNetWrapper {

    private static final String TRAIN_LOSS = "train_loss";
    private static final String TEST_LOSS = "test_loss";
    private static final String LEARNER = "optimiser";


    private RandomInterface random = null;
    private EStructArray backend;
    private EStruct root;
    private Layer[] layers;
    private Loss tarinLoss;
    private Loss testLoss;
    private Optimiser learner;


    public NeuralNetWrapper(EStructArray p_backend) {
        backend = p_backend;
        int nb = backend.size() - 1;

        if (nb < 0) {
            //create configuration node
            root = p_backend.newEStruct();
            p_backend.setRoot(root);
            nb = 0;
        } else {
            root = p_backend.root();
        }

        //Load config with everything in default

        tarinLoss = Losses.getUnit(root.getWithDefault(TRAIN_LOSS, Losses.DEFAULT), null);
        testLoss = Losses.getUnit(root.getWithDefault(TEST_LOSS, Losses.DEFAULT), null);
        learner = Optimisers.getUnit(root.getWithDefault(LEARNER, Optimisers.DEFAULT), backend.root());

        if (nb > 0) {
            //load all layers
            layers = new Layer[nb];
            for (int i = 0; i < layers.length; i++) {
                layers[i] = Layers.loadLayer(backend.estruct(i + 1));
            }
        } else {
            layers = new Layer[0];
        }
    }

    public void setRandom(RandomInterface random) {
        this.random = random;
    }

    public void setTrainLoss(int trainLoss, double[] weights) {
        this.tarinLoss = Losses.getUnit(trainLoss, weights);
        root.set(TRAIN_LOSS, Type.INT, trainLoss);
    }


    public void setTestLoss(int testLoss, double[] weights) {
        this.testLoss = Losses.getUnit(testLoss, weights);
        root.set(TEST_LOSS, Type.INT, testLoss);
    }


    public void setOptimizer(int optimizer, double[] learnerParams, int frequency) {
        this.learner = Optimisers.getUnit(optimizer, root);
        if (learnerParams != null) {
            this.learner.setParams(learnerParams);
        }
        this.learner.setFrequency(frequency);
    }

    public void initAllLayers(int weightInitType, RandomInterface random, double std) {
        for (int i = 0; i < layers.length; i++) {
            layers[i].init(weightInitType, random, std);
        }
    }

    public void initSelectedLayer(int layerNumber, int weightInitType, RandomInterface random, double std) {
        if (layerNumber < layers.length) {
            layers[layerNumber].init(weightInitType, random, std);
        } else {
            throw new RuntimeException("Layer not found");
        }
    }

    public NeuralNetWrapper addLayer(int layerType, int inputs, int outputs, int activationUnit, double[] activationParams) {
        if (layers.length > 0) {
            if (layers[layers.length - 1].outputDimensions() != inputs) {
                throw new RuntimeException("Layers last output size is different that current layer input");
            }
        }
        Layer ff = Layers.createLayer(backend.newEStruct(), layerType);
        ff.create(inputs, outputs, activationUnit, activationParams);
        internal_add(ff);
        return this;
    }


    public DMatrix[] learn(double[] inputs, double[] outputs, boolean reportLoss) {
        ProcessGraph cg = new ProcessGraph(random, true);
        ExMatrix input = ExMatrix.createFromW(VolatileDMatrix.wrap(inputs, inputs.length, 1));
        ExMatrix targetOutput = ExMatrix.createFromW(VolatileDMatrix.wrap(outputs, outputs.length, 1));
        ExMatrix actualOutput = internalForward(cg, input, 0, layers.length);
        DMatrix error = cg.applyLoss(tarinLoss, actualOutput, targetOutput, reportLoss);
        cg.backpropagate();
        learner.stepUpdate(layers);
        return new DMatrix[]{actualOutput, error};
    }


    public DMatrix[] learnVec(DMatrix inputs, DMatrix outputs, boolean reportLoss) {
        ProcessGraph cg = new ProcessGraph(random, true);
        ExMatrix input = ExMatrix.createFromW(inputs);
        ExMatrix targetOutput = ExMatrix.createFromW(outputs);
        ExMatrix actualOutput = internalForward(cg, input, 0, layers.length);
        DMatrix error = cg.applyLoss(tarinLoss, actualOutput, targetOutput, reportLoss);
        cg.backpropagate();
        learner.setBatchSize(inputs.columns());
        learner.stepUpdate(layers);
        return new DMatrix[]{actualOutput, error};
    }

    public DMatrix[] testVec(DMatrix inputs, DMatrix outputs) {
        ProcessGraph cg = new ProcessGraph(random, false);
        ExMatrix input = ExMatrix.createFromW(inputs);
        ExMatrix targetOutput = ExMatrix.createFromW(outputs);
        ExMatrix actualOutput = internalForward(cg, input, 0, layers.length);
        return new DMatrix[]{actualOutput, cg.applyLoss(testLoss, actualOutput, targetOutput, true)};
    }

    public DMatrix predictVec(DMatrix inputs) {
        ProcessGraph cg = new ProcessGraph(random, false);
        ExMatrix input = ExMatrix.createFromW(inputs);
        ExMatrix actualOutput = internalForward(cg, input, 0, layers.length);
        return actualOutput.getW();
    }


    public final void finalLearn() {
        learner.finalUpdate(layers);
    }

    public void resetState() {
        for (int i = 0; i < layers.length; i++) {
            layers[i].resetState();
        }
    }


    public double[] predict(double[] inputs) {
        ProcessGraph cg = new ProcessGraph(random, false);
        ExMatrix input = ExMatrix.createFromW(VolatileDMatrix.wrap(inputs, inputs.length, 1));
        ExMatrix actualOutput = internalForward(cg, input, 0, layers.length);
        return actualOutput.data();
    }

    public double[] forward(double[] inputs, int offset, int numOfLayers) {
        ProcessGraph cg = new ProcessGraph(random, false);
        ExMatrix input = ExMatrix.createFromW(VolatileDMatrix.wrap(inputs, inputs.length, 1));
        ExMatrix actualOutput = internalForward(cg, input, offset, numOfLayers);
        return actualOutput.data();
    }

    public DMatrix forwardVec(DMatrix inputs, int offset, int numOfLayers) {
        ProcessGraph cg = new ProcessGraph(random, false);
        ExMatrix input = ExMatrix.createFromW(inputs);
        ExMatrix actualOutput = internalForward(cg, input, offset, numOfLayers);
        return actualOutput.getW();
    }

    private ExMatrix internalForward(ProcessGraph cg, ExMatrix input, int offset, int numOfLayers) {
        ExMatrix nextInput = input;
        for (int i = offset; i < offset + numOfLayers; i++) {
            nextInput = layers[i].forward(nextInput, cg);
        }
        return nextInput;
    }

    private void internal_add(Layer layer) {
        Layer[] temp = new Layer[layers.length + 1];
        System.arraycopy(layers, 0, temp, 0, layers.length);
        temp[layers.length] = layer;
        layers = temp;
    }

    public Layer[] getLayers() {
        return this.layers;
    }

    public void printNN(boolean details) {
        for (Layer l : layers) {
            l.print(details);
        }
    }
}
