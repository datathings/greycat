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

import greycat.Type;
import greycat.ml.neuralnet.layer.Layer;
import greycat.ml.neuralnet.layer.Layers;
import greycat.ml.neuralnet.optimiser.Optimiser;
import greycat.ml.neuralnet.optimiser.Optimisers;
import greycat.ml.neuralnet.loss.Loss;
import greycat.ml.neuralnet.loss.Losses;
import greycat.ml.neuralnet.process.ExMatrix;
import greycat.ml.neuralnet.process.ProcessGraph;
import greycat.struct.DMatrix;
import greycat.struct.EGraph;
import greycat.struct.ENode;
import greycat.struct.matrix.RandomGenerator;
import greycat.struct.matrix.VolatileDMatrix;

public class NeuralNet {

    private static final String TRAIN_LOSS = "train_loss";
    private static final String REPORTING_LOSS = "reporting_loss";
    private static final String LEARNER = "optimiser";
    private static final String SEED = "seed";
    private static final String STD = "std";
    private static final double STD_DEF = 0.08;

    private EGraph backend;
    private ENode root;
    private Layer[] layers;
    private Loss tarinLoss;
    private Loss testLoss;
    private Optimiser learner;

    private RandomGenerator random;
    private double std;

    public NeuralNet(EGraph p_backend) {
        backend = p_backend;
        int nb = backend.size() - 1;

        if (nb < 0) {
            //create configuration node
            root = p_backend.newNode();
            p_backend.setRoot(root);
            nb = 0;
        } else {
            root = p_backend.root();
        }

        //Load config with everything in default

        tarinLoss = Losses.getUnit(root.getWithDefault(TRAIN_LOSS, Losses.DEFAULT));
        testLoss = Losses.getUnit(root.getWithDefault(REPORTING_LOSS, Losses.DEFAULT));
        learner = Optimisers.getUnit(root.getWithDefault(LEARNER, Optimisers.DEFAULT), backend.root());
        random = new RandomGenerator();
        random.setSeed(root.getWithDefault(SEED, System.currentTimeMillis()));
        std = root.getWithDefault(STD, STD_DEF);


        if (nb > 0) {
            //load all layers
            layers = new Layer[nb];
            for (int i = 0; i < layers.length; i++) {
                layers[i] = Layers.loadLayer(backend.node(i));
            }
        } else {
            layers = new Layer[0];
        }
    }


    public void setTrainLoss(int trainLoss) {
        this.tarinLoss = Losses.getUnit(trainLoss);
        root.set(TRAIN_LOSS, Type.INT, trainLoss);
    }


    public void setTestLoss(int testLoss) {
        this.testLoss = Losses.getUnit(testLoss);
        root.set(REPORTING_LOSS, Type.INT, testLoss);
    }


    public void setOptimizer(int optimizer, double[] learnerParams, int frequency) {
        this.learner = Optimisers.getUnit(optimizer, root);
        if (learnerParams != null) {
            this.learner.setParams(learnerParams);
        }
        this.learner.setFrequency(frequency);
    }


    public void setRandom(long seed, double std) {
        this.random.setSeed(seed);
        this.std = std;
        root.set(SEED, Type.LONG, seed);
        root.set(STD, Type.DOUBLE, std);

        for (int i = 0; i < layers.length; i++) {
            layers[i].reInit(random,std);
        }

    }

    public NeuralNet addLayer(int layerType, int inputs, int outputs, int activationUnit, double[] activationParams) {
        if (layers.length > 0) {
            if (layers[layers.length - 1].outputDimensions() != inputs) {
                throw new RuntimeException("Layers last output size is different that current layer input");
            }
        }
        Layer ff = Layers.createLayer(backend.newNode(), layerType);
        ff.init(inputs, outputs, activationUnit, activationParams, random, std);
        internal_add(ff);
        return this;
    }


    public DMatrix learn(double[] inputs, double[] outputs, boolean reportLoss) {
        ProcessGraph cg = new ProcessGraph(true);
        ExMatrix input = ExMatrix.createFromW(VolatileDMatrix.wrap(inputs, inputs.length, 1));
        ExMatrix targetOutput = ExMatrix.createFromW(VolatileDMatrix.wrap(outputs, outputs.length, 1));
        ExMatrix actualOutput = internalForward(cg, input);
        DMatrix error = cg.applyLoss(tarinLoss, actualOutput, targetOutput, reportLoss);
        cg.backpropagate();
        learner.stepUpdate(layers);
        return error;
    }


    public DMatrix learnVec(DMatrix inputs, DMatrix outputs, boolean reportLoss) {
        ProcessGraph cg = new ProcessGraph(true);
        ExMatrix input = ExMatrix.createFromW(inputs);
        ExMatrix targetOutput = ExMatrix.createFromW(outputs);
        ExMatrix actualOutput = internalForward(cg, input);
        DMatrix error = cg.applyLoss(tarinLoss, actualOutput, targetOutput, reportLoss);
        cg.backpropagate();
        learner.setBatchSize(inputs.columns());
        learner.stepUpdate(layers);
        return error;
    }

    public DMatrix testVec(DMatrix inputs, DMatrix outputs) {
        ProcessGraph cg = new ProcessGraph(false);
        ExMatrix input = ExMatrix.createFromW(inputs);
        ExMatrix targetOutput = ExMatrix.createFromW(outputs);
        ExMatrix actualOutput = internalForward(cg, input);
        return cg.applyLoss(testLoss, actualOutput, targetOutput, true);
    }

    public DMatrix predictVec(DMatrix inputs) {
        ProcessGraph cg = new ProcessGraph(false);
        ExMatrix input = ExMatrix.createFromW(inputs);
        ExMatrix actualOutput = internalForward(cg, input);
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
        ProcessGraph cg = new ProcessGraph(false);
        ExMatrix input = ExMatrix.createFromW(VolatileDMatrix.wrap(inputs, inputs.length, 1));
        ExMatrix actualOutput = internalForward(cg, input);
        return actualOutput.data();
    }

    private ExMatrix internalForward(ProcessGraph cg, ExMatrix input) {
        ExMatrix nextInput = input;
        for (int i = 0; i < layers.length; i++) {
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

}
