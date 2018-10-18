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

                int input = 5;
                int output = 2;

                int setsize = 7;
                int nbRounds = 5;

                double learningrate = 0.003;
                double regularisation = 0.0001;

                Node node = g.newNode(0, 0);
                EStructArray egraph = (EStructArray) node.getOrCreate("nn", Type.ESTRUCT_ARRAY);

                NeuralNetWrapper nn = new NeuralNetWrapper(egraph);

                nn.addLayer(Layers.FEED_FORWARD_LAYER, input, output, Activations.LINEAR, null);
                nn.setOptimizer(Optimisers.GRADIENT_DESCENT, new double[]{learningrate, regularisation}, 1);
                nn.setTrainLoss(Losses.SUM_OF_SQUARES, null);

                CRandom random = new CRandom();
                random.setSeed(123456789);
                nn.setRandom(random);
                nn.initAllLayers(WeightInit.GAUSSIAN, random, 0.08);


                DMatrix inputs = VolatileDMatrix.empty(input, setsize);
                DMatrix outputs = VolatileDMatrix.empty(output, setsize);
                for (int i = 0; i < setsize; i++) {
                    //generate input randomly:

                    for (int j = 0; j < input; j++) {
                        inputs.set(j, i, random.nextDouble());
                        outputs.add(0, i, inputs.get(j, i) * j);
                        outputs.add(1, i, -2 * inputs.get(j, i) * j + 5);
                    }
                }

                MatrixOps.print(inputs, "inputs");
                MatrixOps.print(outputs, "outputs");

                System.out.println("BEFORE training");
                nn.printNN();
                for (int i = 0; i < nbRounds; i++) {
                    System.out.println("AFTER round "+i);
                    nn.learnVec(inputs, outputs, false);
                    nn.printNN();

                }


            }
        });


    }
}
