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
package ml;

import org.mwg.Callback;
import org.mwg.Graph;
import org.mwg.GraphBuilder;
import org.mwg.internal.scheduler.NoopScheduler;
import org.mwg.ml.algorithm.profiling.GaussianMixtureNode;
import org.mwg.mlx.MLXPlugin;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Scanner;

/**
 * @ignore ts
 */
public class GaussianMixtureSim {
    public static void main(String[] arg) {
        final Graph graph = new GraphBuilder().withPlugin(new MLXPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                boolean exit = false;
                String command;

                GaussianMixtureNode node1 = (GaussianMixtureNode) graph.newTypedNode(0, 0, GaussianMixtureNode.NAME);
                node1.set(GaussianMixtureNode.LEVEL,2);
                node1.set(GaussianMixtureNode.WIDTH,3);

                while (!exit) {
                    Scanner scanIn = new Scanner(System.in);
                    command = scanIn.nextLine();
                    String[] args = command.split(" ");
                    if (args[0].equals("exit")) {
                        exit = true;
                    }
                    if (args[0].equals("add")) {
                        double[] data = new double[args.length - 1];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = Double.parseDouble(args[i + 1]);
                        }
                        //to set data here
                        node1.learnVector(data,new Callback<Boolean>() {
                            @Override
                            public void on(Boolean result) {

                            }
                        });

                        long[] sublev = node1.getSubGraph();
                        for (int i = 0; i < sublev.length; i++) {
                            System.out.println("->" + sublev[i]);
                        }
                    }
                    if (args[0].equals("avg")) {
                        print(node1.getAvg());
                    }
                    if (args[0].equals("min")) {
                        print(node1.getMin());
                    }
                    if (args[0].equals("max")) {
                        print(node1.getMax());
                    }
                    if (args[0].equals("draw")) {
                        long[] sublev = node1.getSubGraph();
                        for (int i = 0; i < sublev.length; i++) {
                            System.out.println("->" + sublev[i]);
                        }
                    }


                }
            }
        });
    }

    public static void print(double[] val) {
        NumberFormat formatter = new DecimalFormat("#0.00");
        if (val == null) {
            return;
        }

        for (int i = 0; i < val.length; i++) {
            System.out.print(formatter.format(val[i]) + " ");
        }
        System.out.println();

    }
}