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
package greycat.struct.matrix;

import greycat.struct.DMatrix;

public class MatrixOps {

    public static double PROBA_EPS = 1e-15;
    private static MatrixEngine _defaultEngine = null;

    public static MatrixEngine defaultEngine() {
        if (_defaultEngine == null) {
            _defaultEngine = new PlainMatrixEngine(); //todo return back to HybridMatrixEngine once blas is working in JS
        }
        return _defaultEngine;
    }

    public static void setDefaultEngine(MatrixEngine engine) {
        _defaultEngine = engine;
    }

    public static DMatrix copyMatrix(DMatrix source, DMatrix destination) {
        for (int row = 0; row < source.rows(); row++) {
            for (int col = 0; col < source.columns(); col++) {
                destination.set(row, col, source.get(row, col));
            }
        }
        return destination;
    }

    public static void appendBtoA(DMatrix A, DMatrix B, int step) {
        for (int i = 0; i < B.columns(); i += step) {
            A.appendColumn(B.column(i));
        }
    }

    public static DMatrix fillWithRandom(DMatrix matrix, RandomGenerator random, double min, double max) {
        int len = matrix.length();
        double d = max - min;
        for (int i = 0; i < len; i++) {
            matrix.unsafeSet(i, random.nextDouble() * (d) + min);
        }
        return matrix;
    }

    public static DMatrix fillWithRandomStd(DMatrix matrix, RandomInterface random, double std) {
        int len = matrix.length();
        for (int i = 0; i < len; i++) {
            matrix.unsafeSet(i, random.nextGaussian() * std);
        }
        return matrix;
    }


    public static DMatrix multiply(DMatrix matA, DMatrix matB) {
        return defaultEngine().multiplyTransposeAlphaBeta(TransposeType.NOTRANSPOSE, 1d, matA, TransposeType.NOTRANSPOSE, matB, 0, null);
    }

    public static DMatrix multiplyTranspose(TransposeType transA, DMatrix matA, TransposeType transB, DMatrix matB) {
        return defaultEngine().multiplyTransposeAlphaBeta(transA, 1.0, matA, transB, matB, 0, null);
    }

    public static DMatrix multiplyTransposeAlpha(TransposeType transA, double alpha, DMatrix matA, TransposeType transB, DMatrix matB) {
        return defaultEngine().multiplyTransposeAlphaBeta(transA, alpha, matA, transB, matB, 0, null);
    }

    public static DMatrix multiplyTransposeAlphaBeta(TransposeType transA, double alpha, DMatrix matA, TransposeType transB, DMatrix matB, double beta, DMatrix matC) {
        return defaultEngine().multiplyTransposeAlphaBeta(transA, alpha, matA, transB, matB, beta, matC);
    }

    public static DMatrix invert(DMatrix mat, boolean invertInPlace) {
        return defaultEngine().invert(mat, invertInPlace);
    }

    public static DMatrix pinv(DMatrix mat, boolean invertInPlace) {
        return defaultEngine().pinv(mat, invertInPlace);
    }

    public static DMatrix transpose(DMatrix matA) {
        DMatrix result = VolatileDMatrix.empty(matA.columns(), matA.rows());
        int TRANSPOSE_SWITCH = 375;
        if (matA.columns() == matA.rows()) {
            transposeSquare(matA, result);
        } else if (matA.columns() > TRANSPOSE_SWITCH && matA.rows() > TRANSPOSE_SWITCH) {
            transposeBlock(matA, result);
        } else {
            transposeStandard(matA, result);
        }
        return result;
    }

    private static void transposeSquare(DMatrix matA, DMatrix result) {
        int index = 1;
        int indexEnd = matA.columns();
        for (int i = 0; i < matA.rows(); i++) {
            int indexOther = (i + 1) * matA.columns() + i;
            int n = i * (matA.columns() + 1);
            result.unsafeSet(n, matA.unsafeGet(n));
            for (; index < indexEnd; index++) {
                result.unsafeSet(index, matA.unsafeGet(indexOther));
                result.unsafeSet(indexOther, matA.unsafeGet(index));
                indexOther += matA.columns();
            }
            index += i + 2;
            indexEnd += matA.columns();
        }
    }

    private static void transposeStandard(DMatrix matA, DMatrix result) {
        int index = 0;
        for (int i = 0; i < result.columns(); i++) {
            int index2 = i;
            int end = index + result.rows();
            while (index < end) {
                result.unsafeSet(index++, matA.unsafeGet(index2));
                index2 += matA.rows();
            }
        }
    }

    private static void transposeBlock(DMatrix matA, DMatrix result) {
        int BLOCK_WIDTH = 60;
        for (int j = 0; j < matA.columns(); j += BLOCK_WIDTH) {
            int blockWidth = Math.min(BLOCK_WIDTH, matA.columns() - j);
            int indexSrc = j * matA.rows();
            int indexDst = j;

            for (int i = 0; i < matA.rows(); i += BLOCK_WIDTH) {
                int blockHeight = Math.min(BLOCK_WIDTH, matA.rows() - i);
                int indexSrcEnd = indexSrc + blockHeight;

                for (; indexSrc < indexSrcEnd; indexSrc++) {
                    int colSrc = indexSrc;
                    int colDst = indexDst;
                    int end = colDst + blockWidth;
                    for (; colDst < end; colDst++) {
                        result.unsafeSet(colDst, matA.unsafeGet(colSrc));
                        colSrc += matA.rows();
                    }
                    indexDst += result.rows();
                }
            }
        }
    }


    public static boolean testDimensionsAB(TransposeType transA, TransposeType transB, DMatrix matA, DMatrix matB) {
        if (transA == TransposeType.NOTRANSPOSE) {
            if (transB == TransposeType.NOTRANSPOSE) {
                return (matA.columns() == matB.rows());
            } else {
                return (matA.columns() == matB.columns());
            }
        } else {
            if (transB == TransposeType.NOTRANSPOSE) {
                return (matA.rows() == matB.rows());
            } else {
                return (matA.rows() == matB.columns());
            }
        }
    }


    public static boolean testDim(DMatrix x, DMatrix y) {
        if (x.rows() != y.rows() || x.columns() != y.columns()) {
            throw new RuntimeException("Matrices original and destination have different dimensions");
        }
        return true;
    }

    public static DMatrix sub(DMatrix matA, DMatrix matB) {
        testDim(matA, matB);
        DMatrix result = VolatileDMatrix.empty(matA.rows(), matA.columns());
        int total = matA.length();
        for (int i = 0; i < total; i++) {
            result.unsafeSet(i, matA.unsafeGet(i) - matB.unsafeGet(i));
        }
        return result;
    }


    //todo can be //
    public static void scaleInPlace(double alpha, DMatrix matA) {
        if (alpha == 0) {
            matA.fill(0);
            return;
        }
        for (int i = 0; i < matA.rows() * matA.columns(); i++) {
            matA.unsafeSet(i, alpha * matA.unsafeGet(i));
        }
    }


    //todo can be //
    //MatA= alpha * matA+ beta * matB
    public static void addInPlace(DMatrix matA, double alpha, DMatrix matB, double beta) {
        testDim(matA, matB);
        int total = matA.length();
        if (alpha == 0) {
            if (beta == 0) {
                matA.fill(0);
            } else if (beta == 1) {
                matA.fillWith(matB.data());
            } else {
                for (int i = 0; i < total; i++) {
                    matA.unsafeSet(i, matB.unsafeGet(i) * beta);
                }
            }

        } else if (alpha == 1) {
            if (beta != 0) {
                if (beta == 1) {
                    for (int i = 0; i < total; i++) {
                        matA.unsafeSet(i, matA.unsafeGet(i) + matB.unsafeGet(i));
                    }
                } else {
                    for (int i = 0; i < total; i++) {
                        matA.unsafeSet(i, matA.unsafeGet(i) + matB.unsafeGet(i) * beta);
                    }
                }
            }
        } else {
            if (beta == 0) {
                for (int i = 0; i < total; i++) {
                    matA.unsafeSet(i, matA.unsafeGet(i) * alpha);
                }
            } else if (beta == 1) {
                for (int i = 0; i < total; i++) {
                    matA.unsafeSet(i, matA.unsafeGet(i) * alpha + matB.unsafeGet(i));
                }
            } else {
                for (int i = 0; i < total; i++) {
                    matA.unsafeSet(i, matA.unsafeGet(i) * alpha + matB.unsafeGet(i) * beta);
                }
            }
        }
    }


    //todo can be replaced by system array copy if possible
    public static void copy(DMatrix from, DMatrix to) {
        int total = from.length();
        if (to.length() == 0) {
            to.init(from.rows(), from.columns());
        }
        for (int i = 0; i < total; i++) {
            to.unsafeSet(i, from.unsafeGet(i));
        }
    }


    //todo can be vectorized
    //MatA=MatA+matB
    public static void addtoMatrix(DMatrix original, DMatrix values) {
        testDim(original, values);
        for (int i = 0; i < original.length(); i++) {
            original.unsafeSet(i, original.unsafeGet(i) + values.unsafeGet(i));
        }
    }

    public static void scaleThenAddtoMatrix(DMatrix original, DMatrix values, double scale) {
        testDim(original, values);
        int total = original.length();
        for (int i = 0; i < total; i++) {
            original.unsafeSet(i, original.unsafeGet(i) + values.unsafeGet(i) * scale);
        }
    }

    //todo can be vectorized
    //MatC=MatA+matB
    public static DMatrix add(DMatrix matA, DMatrix matB) {
        testDim(matA, matB);

        DMatrix result = VolatileDMatrix.empty(matA.rows(), matA.columns());
        int total = matA.length();
        for (int i = 0; i < total; i++) {
            result.unsafeSet(i, matA.unsafeGet(i) + matB.unsafeGet(i));
        }
        return result;
    }

    //todo can be vectorized
    public static double compare(DMatrix matA, DMatrix matB) {
        testDim(matA, matB);

        double max = 0;
        double temp;
        int total = matA.length();
        for (int i = 0; i < total; i++) {
            temp = Math.abs(matA.unsafeGet(i) - matB.unsafeGet(i));
            if (temp > max) {
                max = temp;
            }
        }
        return max;
    }

    //todo can be vectorized
    //Hadamard Multiplication multiply 2 matrices elementwise A=[a,b,c] B=[x,y,z] -> result=[a.x, b.y, c.z]
    public static DMatrix HadamardMult(DMatrix matA, DMatrix matB) {
        testDim(matA, matB);
        DMatrix result = VolatileDMatrix.empty(matA.rows(), matA.columns());
        int total = matA.length();
        for (int i = 0; i < total; i++) {
            result.unsafeSet(i, matA.unsafeGet(i) * matB.unsafeGet(i));
        }
        return result;
    }


    public static void print(DMatrix matA, String name) {
        System.out.println("Matrix " + name);
        for (int i = 0; i < matA.rows(); i++) {
            for (int j = 0; j < matA.columns(); j++) {
                System.out.print(matA.get(i, j) + "\t");
            }
            System.out.println("");
        }
        System.out.println("");
    }


    public static void printArray(double[] arrayA, String name) {
        System.out.println("Matrix " + name);
        for (int j = 0; j < arrayA.length; j++) {
            System.out.print(arrayA[j] + "\t");
        }
        System.out.println("");
    }


    public static DMatrix cloneMatrix(DMatrix source) {
        return VolatileDMatrix.cloneFrom(source);
    }


    public static DMatrix cropMatrix(DMatrix original, int rows, int columns) {
        if (rows > original.rows() || columns > original.columns()) {
            throw new RuntimeException("Rows and columns must be less or equal to the original matrix rows and columns");
        }
        VolatileDMatrix res = VolatileDMatrix.empty(rows, columns);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                res.set(i, j, original.get(i, j));
            }
        }
        return res;
    }

    public static DMatrix expandMatrix(DMatrix original, int rows, int columns) {
        if (rows < original.rows() || columns < original.columns()) {
            throw new RuntimeException("Rows and columns must be more or equal to the original matrix rows and columns");
        }
        VolatileDMatrix res = VolatileDMatrix.empty(rows, columns);
        for (int i = 0; i < original.rows(); i++) {
            for (int j = 0; j < original.columns(); j++) {
                res.set(i, j, original.get(i, j));
            }
        }
        return res;
    }

    public static void softmax(DMatrix input, DMatrix output) {
        double maxval = Double.NEGATIVE_INFINITY;
        int len = input.length();
        for (int i = 0; i < len; i++) {
            if (input.unsafeGet(i) > maxval) {
                maxval = input.unsafeGet(i);
            }
        }

        double p;
        for (int col = 0; col < input.columns(); col++) {
            double sum = 0;
            for (int row = 0; row < input.rows(); row++) {
                p = Math.exp(input.get(row, col) - maxval);
                output.set(row, col, p);
                sum += p;
            }
            for (int row = 0; row < input.rows(); row++) {
                p = output.get(row, col) / sum;
                p = Math.min(p, 1 - PROBA_EPS);
                p = Math.max(p, PROBA_EPS);
                output.set(row, col, p);
            }
        }
    }
}
