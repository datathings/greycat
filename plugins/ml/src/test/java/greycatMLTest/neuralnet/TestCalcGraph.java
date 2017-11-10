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

import greycat.ml.neuralnet.activation.Activations;
import greycat.ml.neuralnet.loss.Loss;
import greycat.ml.neuralnet.loss.Losses;
import greycat.ml.neuralnet.activation.Activation;
import greycat.ml.neuralnet.process.ProcessGraph;
import greycat.ml.neuralnet.process.ExMatrix;
import greycat.struct.DMatrix;
import greycat.struct.matrix.VolatileDMatrix;
import org.junit.Test;

public class TestCalcGraph {

    @Test
    public void testcalc() {

        //Simulating example found in: https://mattmazur.com/2015/03/17/a-step-by-step-backpropagation-example/

        //input matrix
        DMatrix wi = VolatileDMatrix.empty(2, 1);
        wi.set(0, 0, 0.05);
        wi.set(1, 0, 0.1);
        ExMatrix input = ExMatrix.createFromW(wi);

        //output matrix
        DMatrix wo1 = VolatileDMatrix.empty(2, 1);
        wo1.set(0, 0, 0.01);
        wo1.set(1, 0, 0.99);
        ExMatrix targetOutput = ExMatrix.createFromW(wo1);


        //weights matrix 1
        DMatrix ww1 = VolatileDMatrix.empty(2, 2);
        ww1.set(0, 0, 0.15);
        ww1.set(0, 1, 0.2);
        ww1.set(1, 0, 0.25);
        ww1.set(1, 1, 0.3);
        ExMatrix weights1 = ExMatrix.createFromW(ww1);

        //bias matrix 1
        DMatrix wb1 = VolatileDMatrix.empty(2, 1);
        wb1.set(0, 0, 0.35);
        wb1.set(1, 0, 0.35);
        ExMatrix bias1 = ExMatrix.createFromW(wb1);

        //weights matrix 2
        DMatrix ww2 = VolatileDMatrix.empty(2, 2);
        ww2.set(0, 0, 0.4);
        ww2.set(0, 1, 0.45);
        ww2.set(1, 0, 0.5);
        ww2.set(1, 1, 0.55);
        ExMatrix weights2 = ExMatrix.createFromW(ww2);

        //bias matrix 2
        DMatrix wb2 = VolatileDMatrix.empty(2, 1);
        wb2.set(0, 0, 0.6);
        wb2.set(1, 0, 0.6);
        ExMatrix bias2 = ExMatrix.createFromW(wb2);

        Loss sumsq = Losses.getUnit(Losses.SUM_OF_SQUARES);
        Activation sigmoid = Activations.getUnit(Activations.SIGMOID, null);


        //The calculation steps are here

        ProcessGraph g = new ProcessGraph(true);


        ExMatrix o1temp = g.mul(weights1, input);
        ExMatrix o1 = g.add(o1temp, bias1);
        ExMatrix o1a = g.activate(sigmoid, o1);

        ExMatrix o2temp = g.mul(weights2, o1a);
        ExMatrix o2 = g.add(o2temp, bias2);
        ExMatrix actualOutput = g.activate(sigmoid, o2);

        DMatrix err = g.applyLoss(sumsq, actualOutput, targetOutput, true);
        testdouble(Losses.sumOfLosses(err), 0.2983711087600027);

        //Now the backpropagation step:
        g.backpropagate();


        //Let's apply the learning rate: 0.5
        double learningRate = 0.5;

        applyLearningRate(weights1, learningRate);
        applyLearningRate(weights2, learningRate);
        applyLearningRate(bias1, learningRate);
        applyLearningRate(bias2, learningRate);

        testdouble(weights1.get(0, 0), 0.1497807161327628);
        testdouble(weights1.get(0, 1), 0.19956143226552567);
        testdouble(weights1.get(1, 0), 0.24975114363236958);
        testdouble(weights1.get(1, 1), 0.29950228726473915);

        testdouble(bias1.get(0, 0), 0.3456143226552565);
        testdouble(bias1.get(1, 0), 0.3450228726473914);

        testdouble(weights2.get(0, 0), 0.35891647971788465);
        testdouble(weights2.get(0, 1), 0.4086661860762334);
        testdouble(weights2.get(1, 0), 0.5113012702387375);
        testdouble(weights2.get(1, 1), 0.5613701211079891);

        testdouble(bias2.get(0, 0), 0.5307507191857215);
        testdouble(bias2.get(1, 0), 0.6190491182582781);


    }

    private static double EPS = 1e-16;

    private static void testdouble(double d1, double d2) {
        if (Math.abs(d1 - d2) > EPS) {
            System.out.println("d1: " + d1 + " d2: " + d2);
            throw new RuntimeException("d1 != d2");
        }
    }

    private static void applyLearningRate(ExMatrix mat, double learningRate) {
        int len = mat.length();
        DMatrix dw = mat.getDw();
        for (int i = 0; i < len; i++) {
            mat.unsafeSet(i, mat.unsafeGet(i) - learningRate * dw.unsafeGet(i));
        }
        //Empty the learning
        dw.fill(0);
    }


}
