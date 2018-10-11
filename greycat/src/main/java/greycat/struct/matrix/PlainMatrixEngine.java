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
package greycat.struct.matrix;

import greycat.struct.DMatrix;

public class PlainMatrixEngine implements MatrixEngine {

    @Override
    public DMatrix multiplyTransposeAlphaBeta(TransposeType transA, double alpha, DMatrix matA, TransposeType transB, DMatrix matB, double beta, DMatrix matC) {
        if (MatrixOps.testDimensionsAB(transA, transB, matA, matB)) {
            int[] dimC = new int[3];
            if (transA == TransposeType.NOTRANSPOSE) {
                if (transB == TransposeType.NOTRANSPOSE) {
                    dimC[0] = matA.rows();
                    dimC[1] = matB.columns();
                    dimC[2] = matA.columns();
                } else {
                    dimC[0] = matA.rows();
                    dimC[1] = matB.rows();
                    dimC[2] = matA.columns();
                }
            } else {
                if (transB == TransposeType.NOTRANSPOSE) {
                    dimC[0] = matA.columns();
                    dimC[1] = matB.columns();
                    dimC[2] = matA.rows();
                } else {
                    dimC[0] = matA.columns();
                    dimC[1] = matB.rows();
                    dimC[2] = matA.rows();
                }
            }
            if (beta == 0 || matC == null) {
                matC = VolatileDMatrix.empty(dimC[0], dimC[1]);
            }
            //perform mult here
            double temp = 0;
            if (transA == TransposeType.NOTRANSPOSE && transB == TransposeType.NOTRANSPOSE) {
                for (int i = 0; i < dimC[0]; i++) {
                    for (int j = 0; j < dimC[1]; j++) {
                        temp = 0;
                        for (int k = 0; k < dimC[2]; k++) {
                            temp += alpha * matA.get(i, k) * matB.get(k, j);
                        }
                        if (beta != 0) {
                            temp = temp + beta * matC.get(i, j);
                        }
                        matC.set(i, j, temp);
                    }
                }

            } else if (transA == TransposeType.NOTRANSPOSE && transB == TransposeType.TRANSPOSE) {
                for (int i = 0; i < dimC[0]; i++) {
                    for (int j = 0; j < dimC[1]; j++) {
                        temp = 0;
                        for (int k = 0; k < dimC[2]; k++) {
                            temp += alpha * matA.get(i, k) * matB.get(j, k);
                        }
                        if (beta != 0) {
                            temp = temp + beta * matC.get(i, j);
                        }
                        matC.set(i, j, temp);
                    }
                }
            } else if (transA == TransposeType.TRANSPOSE && transB == TransposeType.NOTRANSPOSE) {
                for (int i = 0; i < dimC[0]; i++) {
                    for (int j = 0; j < dimC[1]; j++) {
                        temp = 0;
                        for (int k = 0; k < dimC[2]; k++) {
                            temp += alpha * matA.get(k, i) * matB.get(k, j);
                        }
                        if (beta != 0) {
                            temp = temp + beta * matC.get(i, j);
                        }
                        matC.set(i, j, temp);
                    }
                }
            } else if (transA == TransposeType.TRANSPOSE && transB == TransposeType.TRANSPOSE) {
                for (int i = 0; i < dimC[0]; i++) {
                    for (int j = 0; j < dimC[1]; j++) {
                        temp = 0;
                        for (int k = 0; k < dimC[2]; k++) {
                            temp += alpha * matA.get(k, i) * matB.get(j, k);
                        }
                        if (beta != 0) {
                            temp = temp + beta * matC.get(i, j);
                        }
                        matC.set(i, j, temp);
                    }
                }
            }
            return matC;
        } else {
            throw new RuntimeException("Dimensions mismatch between A,B and C");
        }
    }

    @Override
    public DMatrix invert(DMatrix mat, boolean invertInPlace) {
        return solve(mat, VolatileDMatrix.identity(mat.rows(), mat.rows()));
    }

    @Override
    public DMatrix pinv(DMatrix mat, boolean invertInPlace) {
        return solve(mat, VolatileDMatrix.identity(mat.rows(), mat.rows()));
    }

    @Override
    public DMatrix solveLU(DMatrix matA, DMatrix matB, boolean workInPlace, TransposeType transB) {
        DMatrix btem;
        if (transB == TransposeType.TRANSPOSE) {
            btem = MatrixOps.transpose(matB);
        } else {
            btem = matB;
        }
        return (new LU(matA)).solve(btem);

    }

    @Override
    public DMatrix solveQR(DMatrix matA, DMatrix matB, boolean workInPlace, TransposeType transB) {
        DMatrix btem;
        if (transB == TransposeType.TRANSPOSE) {
            btem = MatrixOps.transpose(matB);
        } else {
            btem = matB;
        }
        return (new QR(matA)).solve(btem);
    }

    @Override
    public SVDDecompose decomposeSVD(DMatrix matA, boolean workInPlace) {
        return new SVD(matA);
    }

    @Override
    public EVDDecompose decomposeEVD(DMatrix matA, boolean workInPlace) {
        return EVD.decompose(matA,workInPlace);
    }


    /**
     * Solve A*X = B
     *
     * @param B right hand side
     * @return solution if A is square, least squares solution otherwise
     */

    @Override
    public DMatrix solve(DMatrix A, DMatrix B) {
        return (A.rows() == A.columns() ? (new LU(A)).solve(B) :
                (new QR(A)).solve(B));
    }
}
