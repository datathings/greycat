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
import greycat.ml.profiling.GaussianWrapper;
import greycat.struct.DMatrix;
import greycat.struct.EStruct;
import greycat.struct.EStructArray;
import org.junit.Test;

import java.util.Random;

public class TestGaussianPvalue {
    @Test
    public void Test() {
        Graph graph = GraphBuilder
                .newBuilder()
                .build();

        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                Node node = graph.newNode(0, 0);

                EStructArray eg = (EStructArray) node.getOrCreate("graph", Type.ESTRUCT_ARRAY);
                EStruct en = eg.newEStruct();

                GaussianWrapper gaussian = new GaussianWrapper(en);
                double[] key = {1.1, 2.2, 3.3, 4.4, 5.5};
                int n = 10;

                Random rand = new Random();
                rand.setSeed(1234);

                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < key.length; j++) {
                        key[j] = j - rand.nextDouble() * 500;
                    }
                    gaussian.learn(key);
                }

//                MatrixOps.print(gaussian.getCovariance(), "Covariance");
//                MatrixOps.printArray(gaussian.getAvg(), "avg");


//                System.out.println("");
//                System.out.println("");

                DMatrix pvalue = gaussian.getPValue();

//                System.out.println("Total: " + gaussian.getTotal());
//                MatrixOps.print(gaussian.getCorrelation(), "Correlation");
//                MatrixOps.print(pvalue, "P-value");
                double EPS = 0.00001;

                assert (Math.abs(pvalue.get(0, 0)) < EPS);
                assert (Math.abs(pvalue.get(0, 1) - 0.710050) < EPS);



            }
        });

    }

}
