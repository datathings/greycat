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

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.Node;
import greycat.ml.MLPlugin;

import java.util.Random;

/**
 * Created by assaad on 28/10/2016.
 */
public class NeuralNodeTest {

    private static double generate(double[] inputVec, double[][] conv, double[] weights) {
        double res = 0;

        double[] temp = new double[conv.length];

        for (int i = 0; i < conv.length; i++) {
            for (int j = 0; j < inputVec.length; j++) {
                temp[i] += conv[i][j] * inputVec[j];
            }
            temp[i] += conv[i][inputVec.length];
        }

        for (int j = 0; j < temp.length; j++) {
            res += temp[j] * weights[j];
        }
        res += weights[temp.length];
        return res;
    }

    public static void main(String[] arg) {
        Graph g = new GraphBuilder().withPlugin(new MLPlugin()).build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                Node root = g.newNode(0, 0);

                NeuralNodeEmpty nn = (NeuralNodeEmpty) g.newTypedNode(0, 0, NeuralNodeEmpty.NAME);
                int input = 10;
                int hidden = 3;

                nn.configure(input, 1, 1, hidden);
                root.addToRelation("ml", nn);

                g.index(0, 0, "TREE", result1 -> {
                    result1.addToIndex(root, "name");
                });

//                WSServer ws=new WSServer(g,5678);
//                ws.start();


                Random random = new Random();


                //Initialize a model
                double[][] conv = new double[hidden][input + 1];
                double[] weights = new double[hidden + 1];

                for (int i = 0; i < hidden; i++) {
                    for (int j = 0; j < input + 1; j++) {
                        conv[i][j] = random.nextDouble() * 2 - 1;
                    }
                }
                for (int i = 0; i < weights.length; i++) {
                    weights[i] = random.nextDouble() * 2 - 1;
                }


                double[] inputVec;
                double[] outputVec;


                for (int r = 0; r < 2; r++) {
                    inputVec = new double[input];
                    outputVec = new double[1];

                    for (int i = 0; i < inputVec.length; i++) {
                        inputVec[i] = random.nextDouble() * 2 - 1;
                    }

                    outputVec[0] = generate(inputVec, conv, weights);


                    nn.learn(inputVec, outputVec, new Callback<double[]>() {
                        @Override
                        public void on(double[] result) {
                            System.out.print("error: ");
                            for (int i = 0; i < result.length; i++) {
                                System.out.print(result[i] + ", ");
                            }
                            System.out.println("");
                        }
                    });

                }


            }
        });
    }
}
