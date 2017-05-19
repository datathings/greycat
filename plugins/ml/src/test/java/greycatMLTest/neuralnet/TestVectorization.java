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
package greycatMLTest.neuralnet;

import greycat.*;
import greycat.ml.MLPlugin;
import greycat.ml.neuralnet.NeuralNet;
import greycat.ml.neuralnet.activation.Activations;
import greycat.ml.neuralnet.layer.Layers;
import greycat.ml.neuralnet.loss.Losses;
import greycat.ml.neuralnet.optimiser.Optimisers;
import greycat.struct.DMatrix;
import greycat.struct.EGraph;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.RandomGenerator;
import greycat.struct.matrix.VolatileDMatrix;
import org.junit.Test;

public class TestVectorization {

    @Test
    public void vectorize() {
        Graph g = GraphBuilder.newBuilder().withPlugin(new MLPlugin()).build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                //set number of input to outputDimensions
                int inputdim = 5;
                int outputdim = 2;

                //number of training set to generate
                int trainset = 1000;
                int rounds = 100;

                double learningrate = 0.1;
                double regularisation = 0;
                boolean display=false;
                RandomGenerator randomGenerator=new RandomGenerator();
                randomGenerator.setSeed(1234);

                DMatrix inputs = VolatileDMatrix.random(inputdim, trainset, randomGenerator, -1, 1);
                DMatrix linearsys = VolatileDMatrix.random(outputdim, inputdim, randomGenerator,-2, 2);
                DMatrix outputs = MatrixOps.multiply(linearsys, inputs);
                //System.out.println(outputDimensions.rows() + " , " + outputDimensions.columns());

                Node node1 = g.newNode(0, 0);
                EGraph egraph1 = (EGraph) node1.getOrCreate("nn1", Type.EGRAPH);
                NeuralNet net1 = new NeuralNet(egraph1);
                net1.setRandom(1234, 0.1);
                net1.addLayer(Layers.LINEAR_LAYER, inputdim, outputdim, Activations.LINEAR, null);
                net1.setOptimizer(Optimisers.GRADIENT_DESCENT, new double[]{learningrate/trainset, regularisation}, 1);
                net1.setTrainLoss(Losses.SUM_OF_SQUARES);


                Node node2 = g.newNode(0, 0);
                EGraph egraph2 = (EGraph) node2.getOrCreate("nn2", Type.EGRAPH);
                NeuralNet net2 = new NeuralNet(egraph2);
                net2.setRandom(1234, 0.1);
                net2.addLayer(Layers.LINEAR_LAYER, inputdim, outputdim, Activations.LINEAR, null);
                net2.setOptimizer(Optimisers.GRADIENT_DESCENT, new double[]{learningrate, regularisation}, 0);
                net2.setTrainLoss(Losses.SUM_OF_SQUARES);



                long start=System.currentTimeMillis();
                for (int j = 0; j < rounds; j++) {
                    DMatrix[] err = net1.learnVec(inputs, outputs, true);
                    double[] reserr = Losses.avgLossPerOutput(err[1]);
//                    if(display||j==rounds-1) {
//                        System.out.print("error Vectorized NN at round " + (j+1) + ": ");
//                        for (int i = 0; i < reserr.length; i++) {
//                            System.out.print(reserr[i] + " ");
//                        }
//                        System.out.println("");
//                    }
                }
                long end=System.currentTimeMillis();
//                System.out.println("time Vectorized: "+(end-start)+" ms");

//                System.out.println("");

                start=System.currentTimeMillis();
                for (int j = 0; j < rounds; j++) {
                    double[] lossround=new double[outputdim];
                    for(int i=0;i<trainset;i++){
                        DMatrix[] res= net2.learn(inputs.column(i),outputs.column(i),true);
                        for(int k=0;k<outputdim;k++){
                            lossround[k]+=res[1].get(k,0);
                        }
                    }
                    if(display||j==rounds-1) {
//                        System.out.print("error nonVectorized at round " + (j+1) + ": ");
                        for (int k = 0; k < outputdim; k++) {
                            lossround[k] = lossround[k] / trainset;
                            //System.out.print(lossround[k] + " ");
                        }
//                        System.out.println("");
                    }
                    net2.finalLearn();
                }
                end=System.currentTimeMillis();
//                System.out.println("time nonVectorized: "+(end-start)+" ms");



            }
        });


    }

}
