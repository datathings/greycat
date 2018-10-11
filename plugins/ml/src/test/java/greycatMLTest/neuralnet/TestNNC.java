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
import greycat.ml.CRandomGenerator;
import greycat.ml.neuralnet.NeuralNetWrapper;
import greycat.ml.neuralnet.activation.Activations;
import greycat.ml.neuralnet.layer.Layers;
import greycat.ml.neuralnet.loss.Losses;
import greycat.ml.neuralnet.optimiser.Optimisers;
import greycat.struct.DMatrix;
import greycat.struct.EStructArray;
import greycat.struct.matrix.VolatileDMatrix;
import org.junit.Test;

public class TestNNC {

    @Test
    public void testLinearNN() {
        Graph g = GraphBuilder.newBuilder().build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                int input = 5;
                int output = 2;

                int setsize = 7;

                double learningrate = 0.3;
                double regularisation = 0.0001;

                Node node = g.newNode(0, 0);
                EStructArray egraph = (EStructArray) node.getOrCreate("nn", Type.ESTRUCT_ARRAY);

                NeuralNetWrapper net = new NeuralNetWrapper(egraph);
                net.setRandom(1234, 0.1);

                net.addLayer(Layers.FEED_FORWARD_LAYER, input, output, Activations.LINEAR, null);
                net.setOptimizer(Optimisers.GRADIENT_DESCENT, new double[]{learningrate, regularisation}, 1);
                net.setTrainLoss(Losses.SUM_OF_SQUARES);

                CRandomGenerator random = new CRandomGenerator();
                random.setSeed(123456789);

                net.setRandomGenerator(random, 0.08);


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

//                MatrixOps.print(inputs,"inputs");
//                MatrixOps.print(outputs,"outputs");


            }
        });


    }
}
