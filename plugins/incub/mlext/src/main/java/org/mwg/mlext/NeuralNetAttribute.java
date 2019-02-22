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
package org.mwg.mlext;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.mwg.plugin.AbstractExternalAttribute;
import org.mwg.plugin.Job;
import org.mwg.struct.Buffer;
import org.mwg.utility.Base64;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.ByteArrayOutputStream;

public class NeuralNetAttribute implements AbstractExternalAttribute {

    public static final String NAME = "NeuralNetAttribute";

    private MultiLayerNetwork model;
    private Job dirty;

    public void reconf() {

        int seed = 123;
        double learningRate = 0.01;
        int numInputs = 2;
        int numOutputs = 2;
        int numHiddenNodes = 5;

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(1)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(learningRate)
                .updater(Updater.NESTEROVS).momentum(0.9)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes)
                        .weightInit(WeightInit.XAVIER)
                        .activation("relu")
                        .build())
                .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .weightInit(WeightInit.XAVIER)
                        .activation("softmax").weightInit(WeightInit.XAVIER)
                        .nIn(numHiddenNodes).nOut(numOutputs).build())
                .pretrain(false).backprop(true).build();

        model = new MultiLayerNetwork(conf);

        System.out.println("Ready :-)");

        if (dirty != null) {
            dirty.run();
        }

    }

    @Override
    public void save(Buffer buffer) {
        System.out.println("ToSave");
        //boolean saveUpdater = true;                                     //Updater: i.e., the state for Momentum, RMSProp, Adagrad etc. Save this if you want to train your network more in the future
        Base64.encodeStringToBuffer(NAME, buffer);
        ByteArrayOutputStream oo = new ByteArrayOutputStream();
        /*
        try {
            System.out.println(model.params());

            ModelSerializer.writeModel(model, oo, saveUpdater);
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
        //TODO save the oo in MWG buffer
    }

    @Override
    public void load(Buffer buffer) {

    }

    @Override
    public AbstractExternalAttribute clone() {
        return null;
    }

    @Override
    public void notifyDirty(Job dirtyNotifier) {
        dirty = dirtyNotifier;
    }

}
