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
package greycatMLTest.preprocessing;

import greycat.*;
import greycat.ml.MLPlugin;
import greycat.ml.preprocessing.PCAWrapper;
import greycat.ml.profiling.Gaussian;
import greycat.ml.profiling.GaussianWrapper;
import greycat.struct.DMatrix;
import greycat.struct.EStructArray;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.VolatileDMatrix;
import org.junit.Assert;

import java.util.Random;

public class TestPCAENode {


    public static void main(String[] arg) {


        int dim = 10;                  // Total dimensions in the data
        int realdim = 4;               // Actual real dimensions in the data, the rest are linear correlation plus some noise
        double randomness = 0;       // Strength of the noise from 0 to 1 on the non real dimensions. if randomness ->1 they become somehow real dimension


        int len = dim * 100;  //Number of data point to generate
        double maxsignal = 20; //Maximum signal strength

        Random random = new Random();
        random.setSeed(1234);


        double[][] trainingData = new double[len][dim];
        DMatrix trainMatrix = VolatileDMatrix.empty(dim, len);
        DMatrix backup = VolatileDMatrix.empty(dim, len);

        for (int i = 0; i < len; i++) {
            for (int j = 0; j < realdim; j++) {
                trainingData[i][j] = random.nextDouble() * maxsignal;
            }
        }

        for (int i = 0; i < len; i++) {
            for (int j = realdim; j < dim; j++) {
                trainingData[i][j] = trainingData[i][0] * (1 - randomness) + random.nextDouble() * randomness * maxsignal;
            }
        }

        for (int i = 0; i < len; i++) {
            for (int j = 0; j < dim; j++) {
                trainMatrix.set(j, i, trainingData[i][j]);
                backup.set(j, i, trainingData[i][j]);
            }
        }


        Graph g = GraphBuilder.newBuilder().withPlugin(new MLPlugin()).build();

        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                Node host = g.newNode(0, 0);

                EStructArray hostarray = (EStructArray) host.getOrCreate("ml", Type.ESTRUCT_ARRAY);

                GaussianWrapper gaussianNode = new GaussianWrapper(hostarray.newEStruct());
                PCAWrapper pca = new PCAWrapper(hostarray.newEStruct());


                for (int i = 0; i < len; i++) {
                    gaussianNode.learn(trainingData[i]);
                }

                pca.setCovariance(gaussianNode.getCovariance(), gaussianNode.getAvg(), gaussianNode.getTotal(), false);

                int bestdim = pca.getBestDim();
                System.out.println("best dim:" + bestdim);
                Assert.assertTrue(bestdim == realdim);
                pca.setDimension(bestdim);

                Gaussian.normaliseMatrix(trainMatrix, gaussianNode.getAvg(), gaussianNode.getSTD());
                DMatrix converted = pca.convertMatrix(trainMatrix, false);
                DMatrix originalback = pca.invertMatrix(converted, false);
                Gaussian.inverseNormaliseMatrix(originalback, gaussianNode.getAvg(), gaussianNode.getSTD());

                double err = MatrixOps.compare(originalback, backup);
                Assert.assertTrue(err < 1e-5);
                System.out.println("pca error: " + err);


            }
        });


    }
}
