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
import greycat.ml.math.MultivariateNormalDistribution;
import greycat.ml.profiling.GaussianWrapper;
import greycat.struct.DMatrix;
import greycat.struct.EStructArray;
import greycat.struct.matrix.MatrixOps;

public class TestNDG {

    //@Test
    public void TestNDG() {

        Graph g = GraphBuilder.newBuilder().build();

        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                double[] vec = new double[4];
                vec[0] = 3;
                vec[1] = 5;
                vec[2] = 7;
                vec[3] = 11;

                Node node = g.newNode(0, 0);
                EStructArray struct = (EStructArray) node.getOrCreate("temp", Type.ESTRUCT_ARRAY);
                GaussianWrapper gw = new GaussianWrapper(struct.newEStruct());

                System.out.println("learning: \t" + vec[0] + " \t" + vec[1] + " \t" + vec[2] + " \t" + vec[3]);
                gw.learn(vec);

                for (int i = 0; i < 10; i++) {
                    for (int j = 0; j < 4; j++) {
                        vec[j] = (vec[j] * vec[(j + 1) % 4]) % 11;
                    }
                    vec[3]--;
                    vec[2]++;
                    vec[1] = vec[1] * 2;

                    System.out.println("learning: \t" + vec[0] + " \t" + vec[1] + " \t" + vec[2] + " \t" + vec[3]);
                    gw.learn(vec);
                }

                double[] avg = gw.getAvg();
                DMatrix cov = gw.getCovariance();

                MultivariateNormalDistribution mnd = new MultivariateNormalDistribution(avg, cov, true);

                MatrixOps.print(cov, "cov");
                MatrixOps.printArray(avg, " avg");

                double[] val = new double[]{1.7, 1.5, 0.3, -1};
                double res = mnd.getExponentTerm(val);
                double fullres = mnd.density(val, false);
                System.out.println("exponent: " + res + " fullres: " + fullres);

            }
        });


    }

}
