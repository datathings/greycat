/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.blas;

import greycat.struct.DMatrix;
import greycat.struct.matrix.*;

public class HybridMatrixEngine implements MatrixEngine {

    private MatrixEngine blas;
    private MatrixEngine jama;

    public HybridMatrixEngine() {
        blas = new BlasMatrixEngine();
        jama = new PlainMatrixEngine();
    }

    /**
     * @native ts
     * private static MULT_LIMIT: number=12;
     */
    private final static int MULT_LIMIT = 9;

    /**
     * @native ts
     * private static INVERT_LIMIT: number=6;
     */
    private final static int INVERT_LIMIT = 10;

    /**
     * @native ts
     * private static PINV_LIMIT: number=5;
     */
    private final static int PINV_LIMIT = 8;

    /**
     * @native ts
     * private static SOLVELU_LIMIT: number=6;
     */
    private final static int SOLVELU_LIMIT = 8;

    /**
     * @native ts
     * private static SOLVEQR_LIMIT: number=10;
     */
    private final static int SOLVEQR_LIMIT = 8;

    /**
     * @native ts
     * private static SOLVESVD_LIMIT: number=8;
     */
    private final static int SOLVESVD_LIMIT = 35;


    @Override
    public DMatrix multiplyTransposeAlphaBeta(TransposeType transA, double alpha, DMatrix matA, TransposeType transB, DMatrix matB, double beta, DMatrix matC) {
        if (matA.leadingDimension() < MULT_LIMIT && matB.leadingDimension() < MULT_LIMIT) {
            return jama.multiplyTransposeAlphaBeta(transA, alpha, matA, transB, matB, beta, matC);
        } else {
            return blas.multiplyTransposeAlphaBeta(transA, alpha, matA, transB, matB, beta, matC);
        }
    }

    @Override
    public DMatrix invert(DMatrix mat, boolean invertInPlace) {
        if (mat.rows() < INVERT_LIMIT) {
            return jama.invert(mat, invertInPlace);
        } else {
            return blas.invert(mat, invertInPlace);
        }
    }

    @Override
    public DMatrix pinv(DMatrix mat, boolean invertInPlace) {
        /*if(mat.rows()<PINV_LIMIT){
            return jama.pinv(mat,invertInPlace);
        }
        else {
            return blas.pinv(mat,invertInPlace);
        }*/

        PInvSVD res = new PInvSVD();
        res.factor(mat, invertInPlace);
        return res.getPInv();
    }

    @Override
    public DMatrix solveLU(DMatrix matA, DMatrix matB, boolean workInPlace, TransposeType transB) {
        if (matA.leadingDimension() < SOLVELU_LIMIT && matB.leadingDimension() < SOLVELU_LIMIT) {
            return jama.solveLU(matA, matB, workInPlace, transB);
        } else {
            return blas.solveLU(matA, matB, workInPlace, transB);
        }
    }

    @Override
    public DMatrix solveQR(DMatrix matA, DMatrix matB, boolean workInPlace, TransposeType transB) {
        /*if(matA.leadingDimension()<SOLVEQR_LIMIT &&matB.leadingDimension()<SOLVEQR_LIMIT){
            return jama.solveQR(matA, matB, workInPlace, transB);
        }
        else {
            return blas.solveQR(matA, matB, workInPlace, transB);
        }*/
        return blas.solveQR(matA, matB, workInPlace, transB);
    }

    @Override
    public SVDDecompose decomposeSVD(DMatrix matA, boolean workInPlace) {
        return jama.decomposeSVD(matA, workInPlace);
//        if(matA.leadingDimension()<SOLVESVD_LIMIT){
//            return jama.decomposeSVD(matA,workInPlace);
//        }
//        else {
//            return blas.decomposeSVD(matA,workInPlace);
//        }
    }

    @Override
    public DMatrix solve(DMatrix A, DMatrix B) {
        return blas.solve(A, B);
    }
}
