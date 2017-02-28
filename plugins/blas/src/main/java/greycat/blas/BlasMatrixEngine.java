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
package greycat.blas;

import greycat.struct.DMatrix;
import greycat.struct.matrix.*;

public class BlasMatrixEngine implements MatrixEngine {

    private Blas _blas;

    /**
     * @native ts
     * this._blas = new greycat.blas.JSBlas();
     */
    public BlasMatrixEngine() {
        // _blas = new F2JBlas();
        _blas = new NetlibBlas();
    }

    public void setBlas(Blas p_blas) {
        this._blas = p_blas;
    }

    public Blas getBlas() {
        return _blas;
    }


    //C=alpha*A + beta * B (with possible transpose for A or B)
    @Override
    public DMatrix multiplyTransposeAlphaBeta(TransposeType transA, double alpha, DMatrix matA, TransposeType transB, DMatrix matB, double beta, DMatrix matC) {
        if (MatrixOps.testDimensionsAB(transA, transB, matA, matB)) {
            int k = 0;
            int[] dimC = new int[2];
            if (transA.equals(TransposeType.NOTRANSPOSE)) {
                k = matA.columns();
                if (transB.equals(TransposeType.NOTRANSPOSE)) {
                    dimC[0] = matA.rows();
                    dimC[1] = matB.columns();
                } else {
                    dimC[0] = matA.rows();
                    dimC[1] = matB.rows();
                }
            } else {
                k = matA.rows();
                if (transB.equals(TransposeType.NOTRANSPOSE)) {
                    dimC[0] = matA.columns();
                    dimC[1] = matB.columns();
                } else {
                    dimC[0] = matA.columns();
                    dimC[1] = matB.rows();
                }
            }
            if (beta == 0 || matC == null) {
                matC = VolatileDMatrix.empty(dimC[0], dimC[1]);
            }
            _blas.dgemm(transA, transB, matC.rows(), matC.columns(), k, alpha, matA.data(), 0, matA.rows(), matB.data(), 0, matB.rows(), beta, matC.data(), 0, matC.rows());
            return matC;
        } else {
            throw new RuntimeException("Dimensions mismatch between A,B and C");
        }
    }

    @Override
    public DMatrix invert(DMatrix mat, boolean invertInPlace) {
        if (mat.rows() != mat.columns()) {
            return null;
        }
        if (invertInPlace) {
            LU dlu = new LU(mat.rows(), mat.columns(), _blas);
            if (dlu.invert(mat)) {
                return mat;
            } else {
                return null;
            }
        } else {
            DMatrix result = VolatileDMatrix.empty(mat.rows(), mat.columns());
            DMatrix A_temp = VolatileDMatrix.empty(mat.rows(), mat.columns());
            System.arraycopy(mat.data(), 0, A_temp.data(), 0, mat.columns() * mat.rows());
            LU dlu = new LU(A_temp.rows(), A_temp.columns(), _blas);
            if (dlu.invert(A_temp)) {
                result.fillWith(A_temp.data());
                return result;
            } else {
                return null;
            }
        }
    }

    @Override
    public DMatrix pinv(DMatrix mat, boolean invertInPlace) {
        return solve(mat, VolatileDMatrix.identity(mat.rows(), mat.rows()));
        /*PInvSVD pinvsvd = new PInvSVD();
        pinvsvd.factor(mat, invertInPlace);
        return pinvsvd.getPInv();*/
    }

    @Override
    public DMatrix solveQR(DMatrix matA, DMatrix matB, boolean workInPlace, TransposeType transB) {
        QR solver = QR.factorize(matA, workInPlace, _blas);
        DMatrix coef = VolatileDMatrix.empty(matA.columns(), matB.columns());
        if (transB != TransposeType.NOTRANSPOSE) {
            matB = MatrixOps.transpose(matB);
        }
        solver.solve(matB, coef);
        return coef;
    }

    @Override
    public SVDDecompose decomposeSVD(DMatrix matA, boolean workInPlace) {
        SVD svd = new SVD(matA.rows(), matA.columns(), _blas);
        svd.factor(matA, workInPlace);
        return svd;
    }

    @Override
    public DMatrix solveLU(DMatrix matA, DMatrix matB, boolean workInPlace, TransposeType transB) {
        if (!workInPlace) {
            DMatrix A_temp = VolatileDMatrix.empty(matA.rows(), matA.columns());
            System.arraycopy(matA.data(), 0, A_temp.data(), 0, matA.columns() * matA.rows());

            LU dlu = new LU(A_temp.rows(), A_temp.columns(), _blas);
            dlu.factor(A_temp, true);

            if (dlu.isSingular()) {
                return null;
            }
            DMatrix B_temp = VolatileDMatrix.empty(matB.rows(), matB.columns());
            System.arraycopy(matB.data(), 0, B_temp.data(), 0, matB.columns() * matB.rows());
            dlu.transSolve(B_temp, transB);
            return B_temp;
        } else {
            LU dlu = new LU(matA.rows(), matA.columns(), _blas);
            dlu.factor(matA, true);
            if (dlu.isSingular()) {
                return null;
            }
            dlu.transSolve(matB, transB);
            return matB;
        }
    }

    @Override
    public DMatrix solve(DMatrix A, DMatrix B) {
        return (A.rows() == A.columns() ? (new LU(A.rows(), A.columns(), _blas).factor(A, false)).solve(B) :
                solveQR(A, B, false, TransposeType.NOTRANSPOSE));
    }


}
