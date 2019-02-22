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
package greycatMLTest;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.Node;
import greycat.internal.custom.KDTreeNode;
import greycat.struct.TreeResult;
import org.junit.Test;

import java.util.Random;

public class GPSIndex {


    @Test
    public void testgps() {

        final Graph graph = new GraphBuilder()
                .withMemorySize(1000000)
                .build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                //Can be changed to NDTree as well
                KDTreeNode kdTree = (KDTreeNode) graph.newTypedNode(0, 0, KDTreeNode.NAME);

                int dim = 2; //0 for lat and 1 for lng
                double[] precisions = new double[dim];
                double[] boundMin = new double[dim];
                double[] boundMax = new double[dim];

                precisions[0] = 0.00000001;
                precisions[1] = 0.00000001;
                boundMin[0] = -90;
                boundMin[1] = -180;
                boundMax[0] = 90;
                boundMax[1] = 180;
                kdTree.setResolution(precisions);
                kdTree.setMinBound(boundMin);
                kdTree.setMaxBound(boundMax);


                Random random = new Random();
                random.setSeed(125362l);
                int inserts = 10000;
                int test = 100;
                int nsearch = 4;


                Node[] nodes = new Node[inserts];
                double[][] keys = new double[inserts][];
                double[][] keysTest = new double[test][];

                for (int i = 0; i < inserts; i++) {
                    double[] key = new double[dim];
                    //generate random gps points between -90,-180 and +90,+180
                    key[0] = random.nextDouble() * 180 - 90;
                    key[1] = random.nextDouble() * 360 - 180;

                    keys[i] = key;
                    nodes[i] = graph.newNode(0, 0);
                }

                for (int i = 0; i < test; i++) {
                    double[] key = new double[dim];
                    //generate test gps points between -90,-180 and +90,+180
                    key[0] = random.nextDouble() * 180 - 90;
                    key[1] = random.nextDouble() * 360 - 180;
                    keysTest[i] = key;
                }


                long ts = System.currentTimeMillis();
                for (int i = 0; i < inserts; i++) {
                    kdTree.insert(keys[i], nodes[i].id());
                }
                long te = System.currentTimeMillis() - ts;

                System.out.println("KDTree insert: " + te + " ms");


                long[][] tempkdtree = new long[test][nsearch];
                ts = System.currentTimeMillis();
                for (int i = 0; i < test; i++) {
                    TreeResult res = kdTree.queryAround(keysTest[i], nsearch);
                    for (int j = 0; j < nsearch; j++) {
                        tempkdtree[i][j] = res.value(j);
                    }
                    res.free();
                }
                te = System.currentTimeMillis() - ts;
                System.out.println("KDTree get all: " + te + " ms");


                System.out.println("");
                System.out.println("KDTree size: " + kdTree.size());
                System.out.println("");


                double[] mins = new double[dim];
                double[] maxs = new double[dim];

                mins[0] = -20;
                mins[1] = -20;

                maxs[0] = 20;
                maxs[1] = 20;


                ts = System.currentTimeMillis();
                TreeResult trangeKD = kdTree.queryArea(mins, maxs);
                te = System.currentTimeMillis() - ts;
//                System.out.println("KDTree range: " + te + " ms");
//                System.out.println("found: " + trangeKD.size() + " result in this area");

                graph.disconnect(new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {

                    }
                });

            }
        });
    }
}
