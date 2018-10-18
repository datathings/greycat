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
package greycat.ml.neuralnet.process;

import greycat.ml.neuralnet.activation.Activation;
import greycat.ml.neuralnet.loss.Loss;
import greycat.struct.DMatrix;
import greycat.struct.matrix.*;

import java.util.ArrayList;
import java.util.List;

public class ProcessGraph {

    private static boolean DEBUG = true;
    private boolean applyBackprop;
    private List<ProcessStep> backprop = new ArrayList<ProcessStep>();
    private RandomInterface random = null;

    public ProcessGraph(RandomInterface random, boolean applyBackprop) {
        this.random = random;
        this.applyBackprop = applyBackprop;
    }


    public final void backpropagate() {
        for (int i = backprop.size() - 1; i >= 0; i--) {
            backprop.get(i).execute();
        }
        backprop.clear();
    }

    public final void setBackPropagation(boolean applyBackprop) {
        this.applyBackprop = applyBackprop;
        backprop.clear();
    }

    //Multiply two matrices
    public final ExMatrix mul(final ExMatrix matA, final ExMatrix matB) {
        final ExMatrix out = ExMatrix.createFromW(MatrixOps.multiply(matA, matB));
        if(DEBUG){
            System.out.println("MUL");
            MatrixOps.print(matA.getW(),"matA");
            MatrixOps.print(matB.getW(),"matB");
            MatrixOps.print(out.getW(),"out");
        }
        if (this.applyBackprop) {
            ProcessStep bp = new ProcessStep() {
                public void execute() {
                    DMatrix dwatemp = MatrixOps.multiplyTranspose(TransposeType.NOTRANSPOSE, out.getDw(), TransposeType.TRANSPOSE, matB.getW());
                    DMatrix dwbtemp = MatrixOps.multiplyTranspose(TransposeType.TRANSPOSE, matA.getW(), TransposeType.NOTRANSPOSE, out.getDw());

                    MatrixOps.addtoMatrix(matA.getDw(), dwatemp);
                    MatrixOps.addtoMatrix(matB.getDw(), dwbtemp);
                    if(DEBUG){
                        System.out.println("-- MUL");
                        MatrixOps.print(out.getDw(),"out dw");
                        MatrixOps.print(matA.getDw(),"matA dw");
                        MatrixOps.print(matB.getDw(),"matB dw");
                    }
                }
            };
            backprop.add(bp);
        }
        return out;
    }


    public final ExMatrix expand(final ExMatrix matA, final int numOfCol) {
        if(DEBUG){
            System.out.println("EXPAND");
        }
        if (numOfCol == 1) {
            return matA;
        } else {
            if (matA.columns() != 1) {
                throw new RuntimeException("This method does not support expansion for matrices with more than 1 column! ");
            }
            DMatrix ones = VolatileDMatrix.empty(1, numOfCol);
            ones.fill(1);
            final ExMatrix one = ExMatrix.createFromW(ones);
            return mul(matA, one);
        }
    }


    //Add two matrices
    public final ExMatrix add(final ExMatrix matA, final ExMatrix matB) {
        final ExMatrix out = ExMatrix.createFromW(MatrixOps.add(matA, matB));
        if(DEBUG){
            System.out.println("ADD");
            MatrixOps.print(matA.getW(),"matA");
            MatrixOps.print(matB.getW(),"matB");
            MatrixOps.print(out.getW(),"out");
        }
        if (this.applyBackprop) {
            ProcessStep bp = new ProcessStep() {
                //the derivative is distributive over the add operator
                public void execute() {
                    MatrixOps.addtoMatrix(matA.getDw(), out.getDw());
                    MatrixOps.addtoMatrix(matB.getDw(), out.getDw());
                    if(DEBUG){
                        System.out.println("-- ADD");
                        MatrixOps.print(out.getDw(),"out dw");
                        MatrixOps.print(matA.getDw(),"matA dw");
                        MatrixOps.print(matB.getDw(),"matB dw");

                    }
                }
            };
            backprop.add(bp);
        }
        return out;
    }




    //Apply activation function
    public final ExMatrix activate(final Activation activation, final ExMatrix input) {
        final ExMatrix output = ExMatrix.empty(input.rows(), input.columns());
        final int len = input.length();
        //todo [opt] all activation functions can be vectorized as well
        for (int i = 0; i < len; i++) {
            output.unsafeSet(i, activation.forward(input.unsafeGet(i)));
        }
        if(DEBUG){
            System.out.println("ACTIVATE");
            MatrixOps.print(input.getW(),"matA");
            MatrixOps.print(output.getW(),"out");
        }
        if (this.applyBackprop) {
            ProcessStep bp = new ProcessStep() {
                public void execute() {
                    DMatrix inputDw = input.getDw();
                    DMatrix inputW = input.getW();
                    DMatrix outputDW = output.getDw();
                    DMatrix outputW = output.getW();
                    //todo [opt] can be optimized in // or blas using Hadamard product
                    //Backpropa assigned is: inputDw += derivation of activation * outputDw
                    for (int i = 0; i < len; i++) {
                        inputDw.unsafeSet(i, inputDw.unsafeGet(i) + (activation.backward(inputW.unsafeGet(i), outputW.unsafeGet(i)) * outputDW.unsafeGet(i)));
                    }
                    if(DEBUG){
                        System.out.println("-- ACTIVATE");
                        MatrixOps.print(output.getDw(),"out dw");
                        MatrixOps.print(input.getDw(),"matA dw");
                    }
                }
            };
            backprop.add(bp);
        }
        return output;
    }

    public final DMatrix applyLoss(final Loss lossUnit, final ExMatrix actualOutput, final ExMatrix targetOutput, final boolean calcForwardLoss) {
        if(DEBUG){
            System.out.println("LOSS");
        }
        if (this.applyBackprop) {
            ProcessStep bp = new ProcessStep() {
                public void execute() {
                    lossUnit.backward(actualOutput, targetOutput);
                    if(DEBUG){
                        System.out.println("-- LOSS");
                        MatrixOps.print(actualOutput.getDw(),"out dw");
                    }
                }
            };
            backprop.add(bp);
        }
        if (calcForwardLoss) {
            DMatrix err = lossUnit.forward(actualOutput, targetOutput);
            return err;
        } else {
            return null;
        }
    }


    public ExMatrix extractColumn(final ExMatrix input, final int colNumber) {
        final ExMatrix out = ExMatrix.empty(input.rows(), 1);

        for (int i = 0; i < input.rows(); i++) {
            out.set(i, 0, input.get(i, colNumber));
        }

        if (this.applyBackprop) {
            ProcessStep bp = new ProcessStep() {
                public void execute() {
                    for (int i = 0; i < input.rows(); i++) {
                        input.getDw().add(i, colNumber, out.getDw().get(i, 0));
                    }
                }
            };
            backprop.add(bp);
        }

        return out;
    }

    public ExMatrix concatColumns(final ExMatrix[] input) {
        final ExMatrix out = ExMatrix.empty(input[0].rows(), input.length);

        for (int i = 0; i < input[0].rows(); i++) {
            for (int j = 0; j < input.length; j++) {
                out.set(i, j, input[j].get(i, 0));
            }
        }

        if (this.applyBackprop) {
            ProcessStep bp = new ProcessStep() {
                public void execute() {
                    for (int j = 0; j < input.length; j++) {
                        for (int i = 0; i < input[0].rows(); i++) {
                            input[j].getDw().add(i, 0, out.getDw().get(i, j));
                        }
                    }
                }
            };
            backprop.add(bp);
        }
        return out;
    }

    public ExMatrix elmul(final ExMatrix matA, final ExMatrix matB) {
        final ExMatrix out = ExMatrix.createFromW(MatrixOps.HadamardMult(matA, matB));
        if(DEBUG){
            System.out.println("ELMUL");
            MatrixOps.print(matA.getW(),"matA");
            MatrixOps.print(matB.getW(),"matB");
            MatrixOps.print(out.getW(),"out");
        }
        if (this.applyBackprop) {
            ProcessStep bp = new ProcessStep() {
                public void execute() {
                    MatrixOps.addtoMatrix(matA.getDw(), MatrixOps.HadamardMult(matB.getW(), out.getDw()));
                    MatrixOps.addtoMatrix(matB.getDw(), MatrixOps.HadamardMult(matA.getW(), out.getDw()));
                    if(DEBUG){
                        System.out.println("-- ELMUL");
                        MatrixOps.print(out.getDw(),"out dw");
                        MatrixOps.print(matA.getDw(),"matA dw");
                        MatrixOps.print(matB.getDw(),"matB dw");

                    }
                }
            };
            backprop.add(bp);
        }
        return out;
    }

    public ExMatrix oneMinus(final ExMatrix matA) {
        final ExMatrix out = new ExMatrix(null, null);
        out.init(matA.rows(), matA.columns());
        final int len = matA.length();
        for (int i = 0; i < len; i++) {
            out.unsafeSet(i, 1 - matA.unsafeGet(i));
        }
        if(DEBUG){
            System.out.println("one minus");
            MatrixOps.print(matA.getW(),"matA");
            MatrixOps.print(out.getW(),"out");
        }
        if (this.applyBackprop) {
            ProcessStep bp = new ProcessStep() {
                public void execute() {
                    MatrixOps.scaleThenAddtoMatrix(matA.getDw(), out.getDw(), -1);
                    if(DEBUG){
                        System.out.println("-- one minus");
                        MatrixOps.print(out.getDw(),"out");
                        MatrixOps.print(matA.getDw(),"matA");
                    }
                }
            };
            backprop.add(bp);
        }

        return out;
    }

    public ExMatrix concatVectors(final ExMatrix matA, final ExMatrix matB) {
        if (matA.columns() != matB.columns()) {
            throw new RuntimeException("Expected same column size");
        }

        final ExMatrix out = new ExMatrix(null, null);
        out.init(matA.rows() + matB.rows(), matA.columns());

        if (matA.hasStepCache() || matB.hasStepCache()) {
            DMatrix outw = out.getW();
            DMatrix outdw = out.getDw();
            DMatrix outsc = out.getStepCache();
            DMatrix aw = matA.getW();
            DMatrix adw = matA.getDw();
            DMatrix asc = matA.getStepCache();
            DMatrix bw = matB.getW();
            DMatrix bdw = matB.getDw();
            DMatrix bsc = matB.getStepCache();

            for (int i = 0; i < matA.rows(); i++) {
                for (int j = 0; j < matA.columns(); j++) {
                    outw.set(i, j, aw.get(i, j));
                    outdw.set(i, j, adw.get(i, j));
                    outsc.set(i, j, asc.get(i, j));
                }
            }

            int r = matA.rows();

            for (int i = 0; i < matB.rows(); i++) {
                for (int j = 0; j < matB.columns(); j++) {
                    outw.set(i + r, j, bw.get(i, j));
                    outdw.set(i + r, j, bdw.get(i, j));
                    outsc.set(i + r, j, bsc.get(i, j));
                }
            }

        } else {
            DMatrix outw = out.getW();
            DMatrix outdw = out.getDw();
            DMatrix aw = matA.getW();
            DMatrix adw = matA.getDw();
            DMatrix bw = matB.getW();
            DMatrix bdw = matB.getDw();

            for (int i = 0; i < matA.rows(); i++) {
                for (int j = 0; j < matA.columns(); j++) {
                    outw.set(i, j, aw.get(i, j));
                    outdw.set(i, j, adw.get(i, j));
                }
            }

            int r = matA.rows();

            for (int i = 0; i < matB.rows(); i++) {
                for (int j = 0; j < matB.columns(); j++) {
                    outw.set(i + r, j, bw.get(i, j));
                    outdw.set(i + r, j, bdw.get(i, j));
                }
            }
        }


        if (this.applyBackprop) {
            ProcessStep bp = new ProcessStep() {
                public void execute() {
                    DMatrix outdw = out.getDw();
                    DMatrix adw = matA.getDw();
                    DMatrix bdw = matB.getDw();

                    for (int i = 0; i < matA.rows(); i++) {
                        for (int j = 0; j < matA.columns(); j++) {
                            adw.add(i, j, outdw.get(i, j));
                        }
                    }

                    int r = matA.rows();

                    for (int i = 0; i < matB.rows(); i++) {
                        for (int j = 0; j < matB.columns(); j++) {
                            bdw.add(i, j, outdw.get(i + r, j));
                        }
                    }
                }

            };
            backprop.add(bp);
        }
        return out;
    }

    public ExMatrix assign(final ExMatrix in) {
        ExMatrix out = ExMatrix.createFromW(in.getW());
        System.out.println("ASSIGN");
        if (this.applyBackprop) {
            ProcessStep bp = new ProcessStep() {
                public void execute() {
                    MatrixOps.addtoMatrix(in.getDw(), out.getDw());
                    System.out.println("ASSIGN --");
                    MatrixOps.print(out.getDw(), "out dw");
                    MatrixOps.print(in.getDw(), "in dw");
//            out.getDw().fill(0);
                }
            };
            backprop.add(bp);
        }
        return out;
    }

    public ExMatrix softmax(ExMatrix input) {
        final ExMatrix out = new ExMatrix(null, null);
        out.init(input.rows(), input.columns());
        MatrixOps.softmax(input.getW(), out.getW());

        if (this.applyBackprop) {
            ProcessStep bp = new ProcessStep() {
                public void execute() {
                    double p;
                    DMatrix da = input.getDw();
                    DMatrix dS = out.getDw();
                    DMatrix S = out.getW();
                    for (int col = 0; col < input.columns(); col++) {
                        for (int row = 0; row < input.rows(); row++) {
                            p = S.get(row, col);
                            for (int k = 0; k < input.rows(); k++) {
                                if (k == row) {
                                    da.add(row, col, S.get(k, col) * (1 - p) * dS.get(k, col));
                                } else {
                                    da.add(row, col, -S.get(k, col) * p * dS.get(k, col));
                                }
                            }
                        }
                    }


                }
            };
            backprop.add(bp);
        }
        return out;
    }

    public ExMatrix dropout(ExMatrix input, double dropout) {
        final ExMatrix out = new ExMatrix(null, null);
        final ExMatrix temp = new ExMatrix(null, null);
        out.init(input.rows(), input.columns());
        temp.init(input.rows(), input.columns());
        MatrixOps.copy(input.getW(), out.getW());

        if (random == null) {
            random = new JavaRandom();
        }
        int len = input.length();
        for (int i = 0; i < len; i++) {
            if (random.nextDouble() < dropout) {
                out.unsafeSet(i, 0.0);
                temp.unsafeSet(i, 1);
            }
        }

        if (this.applyBackprop) {
            ProcessStep bp = new ProcessStep() {
                public void execute() {
                    DMatrix inDw = input.getDw();
                    DMatrix outDw = out.getDw();
                    for (int i = 0; i < len; i++) {
                        if (temp.unsafeGet(i) != 1.0) {
                            inDw.unsafeAdd(i, outDw.unsafeGet(i));
                        }
                    }
                }
            };
            backprop.add(bp);
        }
        return out;
    }
}
