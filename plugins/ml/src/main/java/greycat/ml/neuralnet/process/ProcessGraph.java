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
package greycat.ml.neuralnet.process;

import greycat.ml.neuralnet.Activation;
import greycat.ml.neuralnet.loss.Loss;
import greycat.struct.DMatrix;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.TransposeType;
import greycat.struct.matrix.VolatileDMatrix;

import java.util.ArrayList;
import java.util.List;

public class ProcessGraph {

    private boolean applyBackprop;
    private List<ProcessStep> backprop = new ArrayList<ProcessStep>();

    public ProcessGraph(boolean applyBackprop) {
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
        if (this.applyBackprop) {
            ProcessStep bp = new ProcessStep() {
                public void execute() {
                    DMatrix dwatemp = MatrixOps.multiplyTranspose(TransposeType.NOTRANSPOSE, out.getDw(), TransposeType.TRANSPOSE, matB.getW());
                    DMatrix dwbtemp = MatrixOps.multiplyTranspose(TransposeType.TRANSPOSE, matA.getW(), TransposeType.NOTRANSPOSE, out.getDw());

                    MatrixOps.addtoMatrix(matA.getDw(), dwatemp);
                    MatrixOps.addtoMatrix(matB.getDw(), dwbtemp);
                }
            };
            backprop.add(bp);
        }
        return out;
    }


    public final ExMatrix expand(final ExMatrix matA, final int numOfCol) {
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
        if (this.applyBackprop) {
            ProcessStep bp = new ProcessStep() {
                //the derivative is distributive over the add operator
                public void execute() {
                    MatrixOps.addtoMatrix(matA.getDw(), out.getDw());
                    MatrixOps.addtoMatrix(matB.getDw(), out.getDw());
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
                }
            };
            backprop.add(bp);
        }
        return output;
    }

    public final DMatrix applyLoss(final Loss lossUnit, final ExMatrix actualOutput, final ExMatrix targetOutput, final boolean calcForwardLoss) {

        if (this.applyBackprop) {
            ProcessStep bp = new ProcessStep() {
                public void execute() {
                    lossUnit.backward(actualOutput, targetOutput);
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

    public ExMatrix elmul(final ExMatrix matA, final ExMatrix matB) {
        final ExMatrix out = ExMatrix.createFromW(MatrixOps.HadamardMult(matA, matB));

        if (this.applyBackprop) {
            ProcessStep bp = new ProcessStep() {
                public void execute() {
                    MatrixOps.addtoMatrix(matA.getDw(), MatrixOps.HadamardMult(matB.getW(), out.getDw()));
                    MatrixOps.addtoMatrix(matB.getDw(), MatrixOps.HadamardMult(matA.getW(), out.getDw()));
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
        for (int i = 0; i < matA.length(); i++) {
            out.unsafeSet(i, 1 - matA.unsafeGet(i));
        }

        if (this.applyBackprop) {
            ProcessStep bp = new ProcessStep() {
                public void execute() {
                    MatrixOps.scaleThenAddtoMatrix(matA.getDw(), out.getDw(), -1);
                }
            };
            backprop.add(bp);
        }

        return null;
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
                            adw.set(i, j, outdw.get(i, j));
                        }
                    }

                    int r = matA.rows();

                    for (int i = 0; i < matB.rows(); i++) {
                        for (int j = 0; j < matB.columns(); j++) {
                            bdw.set(i, j, outdw.get(i + r, j));
                        }
                    }
                }

            };
            backprop.add(bp);
        }
        return out;


    }
}
