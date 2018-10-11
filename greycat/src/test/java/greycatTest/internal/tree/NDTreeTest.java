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
package greycatTest.internal.tree;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.Node;
import greycat.internal.custom.KDTreeNode;
import greycat.internal.custom.NDTreeNode;
import greycat.struct.TreeResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

public class NDTreeTest {
    @Test
    public void NDTest() {
        final Graph graph = new GraphBuilder()
                .withMemorySize(1000000)
                .build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                KDTreeNode kdTree = (KDTreeNode) graph.newTypedNode(0, 0, KDTreeNode.NAME);
                NDTreeNode ndTree = (NDTreeNode) graph.newTypedNode(0, 0, NDTreeNode.NAME);

                int dim = 5;
                double[] precisions = new double[dim];
                double[] boundMin = new double[dim];
                double[] boundMax = new double[dim];
                for (int i = 0; i < dim; i++) {
                    precisions[i] = 0.001;
                    boundMin[i] = 0;
                    boundMax[i] = 1;
                }
                ndTree.setMinBound(boundMin);
                ndTree.setMaxBound(boundMax);
                ndTree.setResolution(precisions);
                kdTree.setResolution(precisions);
                Random random = new Random();
                random.setSeed(125362l);
                int ins = 100;
                int test = 10;
                int nsearch = 4;


                Node[] nodes = new Node[ins];
                double[][] keys = new double[ins][];
                double[][] keysTest = new double[test][];

                for (int i = 0; i < ins; i++) {
                    //temp.setProperty("value", Type.DOUBLE, random.nextDouble());
                    double[] key = new double[dim];
                    for (int j = 0; j < dim; j++) {
                        key[j] = random.nextDouble();
                    }
                    keys[i] = key;
                    nodes[i] = graph.newNode(0, 0);
                }

                for (int i = 0; i < test; i++) {
                    double[] key = new double[dim];
                    for (int j = 0; j < dim; j++) {
                        key[j] = random.nextDouble();
                    }
                    keysTest[i] = key;
                }


                long ts = System.currentTimeMillis();
                for (int i = 0; i < ins; i++) {
                    ndTree.insert(keys[i], nodes[i].id());
                }
                long te = System.currentTimeMillis() - ts;

//                System.out.println("NDTree insert: " + te + " ms");

                ts = System.currentTimeMillis();
                for (int i = 0; i < ins; i++) {
                    kdTree.insert(keys[i], nodes[i].id());
                }
                te = System.currentTimeMillis() - ts;

//                System.out.println("KDTree insert: " + te + " ms");


                long[][] temp = new long[test][nsearch];
                ts = System.currentTimeMillis();
                for (int i = 0; i < test; i++) {
                    TreeResult res = ndTree.queryAround(keysTest[i], nsearch);
                    for (int j = 0; j < nsearch; j++) {
                        temp[i][j] = res.value(j);
                    }
                    res.free();
                }
                te = System.currentTimeMillis() - ts;
//                System.out.println("NDTree get all: " + te + " ms");

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
//                System.out.println("KDTree get all: " + te + " ms");


//                System.out.println();
//                System.out.println("KDTree size: "+kdTree.size());
//                System.out.println("KDTree tree size: "+kdTree.treeSize());
//                System.out.println("NDTree size: "+ndTree.size());
//                System.out.println("NDTree tree size: "+ ndTree.treeSize());
//                System.out.println();

                for (int i = 0; i < test; i++) {
                    for (int j = 0; j < nsearch; j++) {
                        if (temp[i][j] != tempkdtree[i][j]) {
                            throw new RuntimeException("Error! " + temp[i][j] + " != " + tempkdtree[i][j]);
                        }
                    }
                }


                double[] mins = new double[dim];
                double[] maxs = new double[dim];

                for (int i = 0; i < dim; i++) {
                    mins[i] = 0.2;
                    maxs[i] = 0.7;
                }

                ts = System.currentTimeMillis();
                TreeResult trangeND = ndTree.queryArea(mins, maxs);
                te = System.currentTimeMillis() - ts;
//                System.out.println("NDtree range: " + te + " ms");

                ts = System.currentTimeMillis();
                TreeResult trangeKD = kdTree.queryArea(mins, maxs);
                te = System.currentTimeMillis() - ts;
//                System.out.println("KDTree range: " + te + " ms");
//                System.out.println(trangeKD.size() + " , " + trangeND.size());
                Assert.assertTrue(trangeKD.size() == trangeND.size());


                for (int i = 0; i < trangeKD.size(); i++) {
                    Assert.assertTrue(trangeKD.value(i) == trangeND.value(i));
                }


//                System.out.println("test pass!");

            }
        });
    }

}
