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
package greycatMLTest.neuralnet;

import greycat.*;
import greycat.ml.CRandom;
import greycat.ml.neuralnet.NeuralNetWrapper;
import greycat.ml.neuralnet.activation.Activations;
import greycat.ml.neuralnet.layer.Layers;
import greycat.ml.neuralnet.loss.Losses;
import greycat.ml.neuralnet.optimiser.Optimisers;
import greycat.ml.neuralnet.process.WeightInit;
import greycat.struct.DMatrix;
import greycat.struct.EStructArray;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.VolatileDMatrix;

public class TestNNC {

    public static void main(String[] args) {
        Graph g = GraphBuilder.newBuilder().build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                int input = 8;
                int output = 5;
                int setsize = 3;
                int nbRounds = 10;

                double learningrate = 0.3;
                double regularisation = 0.1;

                Node node = g.newNode(0, 0);
                EStructArray egraph = (EStructArray) node.getOrCreate("nn", Type.ESTRUCT_ARRAY);

                NeuralNetWrapper nn = new NeuralNetWrapper(egraph);

                nn.addLayer(Layers.FEED_FORWARD_LAYER, input, output, Activations.SIGMOID, null);
                nn.addLayer(Layers.LINEAR_LAYER, output, output, Activations.LINEAR, null);
                nn.addLayer(Layers.DROPOUT_LAYER, output, output, Activations.LINEAR, new double[]{0.01});
                nn.addLayer(Layers.LSTM_LAYER, output, output, 0, null);
                nn.setOptimizer(Optimisers.GRADIENT_DESCENT, new double[]{learningrate, regularisation}, 1);
                nn.setTrainLoss(Losses.SUM_OF_SQUARES, null);

                CRandom random = new CRandom();
                random.setSeed(123456789);
                nn.setRandom(random);
                nn.initAllLayers(WeightInit.GAUSSIAN, random, 0.08);


                DMatrix inputs = VolatileDMatrix.empty(input, setsize);
                DMatrix outputs = VolatileDMatrix.empty(output, setsize);

                double value;
                for (int i = 0; i < setsize; i++) {
                    //generate input randomly:
                    for (int j = 0; j < input; j++) {
                        value = random.nextDouble();
                        inputs.set(j, i, value);
                        for (int k = 0; k < output; k++) {
                            outputs.add(k, i, -k * value * j + k);
                        }
                    }
                }

                MatrixOps.print(inputs, "inputs");
                MatrixOps.print(outputs, "outputs");

                //System.out.println("BEFORE training");
                //nn.printNN();


                long before = System.currentTimeMillis();

                for (int i = 0; i < nbRounds; i++) {
                    // System.out.println("AFTER round "+i);
                    nn.learnVec(inputs, outputs, false);
                    // nn.printNN();
                }

                long after = System.currentTimeMillis();

                inputs = VolatileDMatrix.empty(input, setsize/2);
                outputs = VolatileDMatrix.empty(output, setsize/2);

                for (int i = 0; i < setsize/2; i++) {
                    //generate input randomly:
                    for (int j = 0; j < input; j++) {
                        value = random.nextDouble();
                        inputs.set(j, i, value);
                        for (int k = 0; k < output; k++) {
                            outputs.add(k, i, -k * value * j + k);
                        }
                    }
                }
                nn.learnVec(inputs, outputs, false);
                System.out.println("FINAL");
                nn.printNN();

                System.out.println("task test: " + (after - before) / (nbRounds * 1.0) + " ms/cycle");


            }
        });


    }
}
