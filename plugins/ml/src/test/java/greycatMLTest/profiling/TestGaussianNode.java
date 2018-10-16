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
package greycatMLTest.profiling;

import greycat.*;
import greycat.ml.profiling.GaussianWrapper;
import greycat.struct.DMatrix;
import greycat.struct.EStruct;
import greycat.struct.EStructArray;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.JavaRandom;
import org.junit.Test;

public class TestGaussianNode {
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
                EStruct en2 = eg.newEStruct();

                GaussianWrapper gaussian = new GaussianWrapper(en);
                GaussianWrapper gaussianTest = new GaussianWrapper(en2);

                double[] key = {1.1, 2.2, 3.3, 4.4, 5.5};
                int n = 10;

                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < key.length; j++) {
                        key[j] += j * 0.5 - n / 2.0;
                    }
                    gaussian.learn(key);
                }

//                MatrixOps.print(gaussian.getCovariance(), "Covariance");
//                MatrixOps.printArray(gaussian.getAvg(), "avg");


//                System.out.println("");
//                System.out.println("");

                JavaRandom rnd = new JavaRandom();
                rnd.setSeed(1234);


                int sample = 1000;
                DMatrix rand = gaussian.drawMatrix(sample, rnd);
//                MatrixOps.print(rand,"drawn");

                for (int i = 0; i < rand.columns(); i++) {
                    gaussianTest.learn(rand.column(i));
                }


//                MatrixOps.print(gaussianTest.getCovariance(), "Covariance");
//                MatrixOps.printArray(gaussianTest.getAvg(), "avg");

                assert (MatrixOps.compare(gaussianTest.getCovariance(), gaussian.getCovariance())<7);

            }
        });

    }

}
