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

import greycat.ml.neuralnet.activation.Activation;
import greycat.ml.neuralnet.activation.Activations;
import greycat.ml.neuralnet.loss.Loss;
import greycat.ml.neuralnet.loss.Losses;
import greycat.ml.neuralnet.process.ExMatrix;
import greycat.struct.DMatrix;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.TransposeType;
import greycat.struct.matrix.VolatileDMatrix;

public class TestCalc {
    public static void main(String[] arg) {

        //Simulating example found in: https://mattmazur.com/2015/03/17/a-step-by-step-backpropagation-example/
        //self contained example useful for debug

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
        ExMatrix o1temp = mul(weights1, input);
        ExMatrix o1 = add(o1temp, bias1);
        ExMatrix o1a = activation(sigmoid, o1);

        ExMatrix o2temp = mul(weights2, o1a);
        ExMatrix o2 = add(o2temp, bias2);
        ExMatrix actualOutput = activation(sigmoid, o2);

        DMatrix err = sumsq.forward(actualOutput, targetOutput);
        //till here

        System.out.println("");
        System.out.println("Step 1, before activation: [" + o1.get(0, 0) + " , " + o1.get(1, 0) + "]");
        System.out.println("Step 2, after activation: [" + o1a.get(0, 0) + " , " + o1a.get(1, 0) + "]");
        System.out.println("Step 3, before activation: [" + o2.get(0, 0) + " , " + o2.get(1, 0) + "]");
        System.out.println("Step 4, after activation, actual output: [" + actualOutput.get(0, 0) + " , " + actualOutput.get(1, 0) + "]");
        System.out.println("Total Error: " + err);
        System.out.println("");

        //Now the backpropagation step:
        sumsq.backward(actualOutput, targetOutput);
        backpropActivation(sigmoid, o2, actualOutput);
        backpropAdd(o2temp, bias2, o2);
        backpropMult(weights2, o1a, o2temp);

        backpropActivation(sigmoid, o1, o1a);
        backpropAdd(o1temp, bias1, o1);
        backpropMult(weights1, input, o1temp);

        //Let's apply the learning rate: 0.5
        double learningRate = 0.5;

        applyLearningRate(weights1, learningRate);
        applyLearningRate(weights2, learningRate);
        applyLearningRate(bias1, learningRate);
        applyLearningRate(bias2, learningRate);

        System.out.println("After learning: ");
        System.out.println("w1: " + weights1.get(0, 0));
        System.out.println("w2: " + weights1.get(0, 1));
        System.out.println("w3: " + weights1.get(1, 0));
        System.out.println("w4: " + weights1.get(1, 1));
        System.out.println("Bias: ");
        System.out.println("b1-a: " + bias1.get(0, 0));
        System.out.println("b1-b: " + bias1.get(1, 0));

        System.out.println("");
        System.out.println("w5: " + weights2.get(0, 0));
        System.out.println("w6: " + weights2.get(0, 1));
        System.out.println("w7: " + weights2.get(1, 0));
        System.out.println("w8: " + weights2.get(1, 1));
        System.out.println("Bias: ");
        System.out.println("b2-a: " + bias2.get(0, 0));
        System.out.println("b2-b: " + bias2.get(1, 0));


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

    private static ExMatrix mul(ExMatrix matA, ExMatrix matB) {
        ExMatrix out = ExMatrix.createFromW(MatrixOps.multiply(matA, matB));
        return out;
    }


    private static ExMatrix add(ExMatrix matA, ExMatrix matB) {
        ExMatrix out = ExMatrix.createFromW(MatrixOps.add(matA, matB));
        return out;
    }

    private static void backpropAdd(ExMatrix matA, ExMatrix matB, ExMatrix result) {

        MatrixOps.addtoMatrix(matA.getDw(), result.getDw());
        MatrixOps.addtoMatrix(matB.getDw(), result.getDw());

    }

    private static void backpropMult(ExMatrix matA, ExMatrix matB, ExMatrix result) {
        DMatrix dwatemp = MatrixOps.multiplyTranspose(TransposeType.NOTRANSPOSE, result.getDw(), TransposeType.TRANSPOSE, matB.getW());
        DMatrix dwbtemp = MatrixOps.multiplyTranspose(TransposeType.TRANSPOSE, matA.getW(), TransposeType.NOTRANSPOSE, result.getDw());

        MatrixOps.addtoMatrix(matA.getDw(), dwatemp);
        MatrixOps.addtoMatrix(matB.getDw(), dwbtemp);

    }


    private static ExMatrix activation(Activation activation, ExMatrix input) {
        final ExMatrix output = ExMatrix.empty(input.rows(), input.columns());
        final int len = input.length();

        //todo all activation functions can be vectorized as well
        for (int i = 0; i < len; i++) {
            output.unsafeSet(i, activation.forward(input.unsafeGet(i)));
        }
        return output;
    }


    public static void backpropActivation(Activation activation, ExMatrix input, ExMatrix output) {
        DMatrix inputDw = input.getDw();
        DMatrix inputW = input.getW();
        DMatrix outputDW = output.getDw();
        DMatrix outputW = output.getW();
        final int len = input.length();

        //toDo can be optimized in // or blas using Hadamard product
        //Backpropa assigned is: inputDw += derivation of activation
        for (int i = 0; i < len; i++) {
            inputDw.unsafeSet(i, inputDw.unsafeGet(i) + (activation.backward(inputW.unsafeGet(i), outputW.unsafeGet(i)) * outputDW.unsafeGet(i)));
        }
    }

}
