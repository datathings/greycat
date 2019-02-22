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
package greycatMLTest.profiling;

import greycat.*;
import greycat.internal.custom.NDTree;
import greycat.ml.profiling.GaussianWrapper;
import greycat.ml.profiling.GmmManager;
import greycat.struct.EStructArray;
import greycat.struct.ProfileResult;

import java.util.Random;

/**
 * Created by assaad on 21/06/2017.
 */
public class TestGmmManager {
    public static void main(String[] args) {

        Graph graph = GraphBuilder
                .newBuilder()
                .build();

        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                Node host = graph.newNode(0, 0);

                EStructArray ndTree = (EStructArray) host.getOrCreate("graphNDTree", Type.ESTRUCT_ARRAY);
                // EStructArray gmmTree = (EStructArray) host.getOrCreate("graphgmm", Type.ESTRUCT_ARRAY);

                GmmManager manager = new GmmManager(ndTree);

                NDTree tree = new NDTree(ndTree, manager);

                int len = 1000;
                double[][] keys = new double[len][4];

                Random rand = new Random();
                for (int i = 0; i < len; i++) {
                    for (int j = 0; j < 4; j++) {
                        keys[i][j] = rand.nextDouble();
                    }
                }

                tree.setMinBound(new double[]{0, 0, 0, 0});
                tree.setMaxBound(new double[]{1, 1, 1, 1});
                tree.setResolution(new double[]{0.1, 0.1, 0.1, 0.1});

                for (int i = 0; i < len; i++) {
                    tree.insert(keys[i], 1);
                }

                ProfileResult res = tree.queryAround(new double[]{0.4, 0.5, 0.6, 0.7}, 4);

                System.out.println("result: " + res.size());
                for (int i = 0; i < res.size(); i++) {
                    int ind = (int) res.value(i);
                    //System.out.println(ind);
                    GaussianWrapper gn = new GaussianWrapper(ndTree.estruct(ind));
                    double[] av1 = gn.getAvg();
                    double[] k1 = res.keys(i);
                    System.out.println("Id: " + ind + " distance: " + res.distance(i) + " keys: [" + k1[0] + " " + k1[1] + " " + k1[2] + " " + k1[3] + " " + "] gmm avg:[" + av1[0] + " " + av1[1] + " " + av1[2] + " " + av1[3] + " " + "] gmm Total: " + gn.getTotal());
                    //gn.print();
                }


            }
        });

    }
}
