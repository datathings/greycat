/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycatBlasTest;

import greycat.blas.BlasMatrixEngine;
import greycat.struct.DMatrix;
import greycat.struct.matrix.MatrixEngine;
import greycat.struct.matrix.PlainMatrixEngine;
import greycat.struct.matrix.TransposeType;
import greycat.struct.matrix.VolatileDMatrix;
import org.junit.Assert;
import org.junit.Test;

public class MultiplyTest {

    @Test
    public void MatrixMultBlas() {
        InternalManualMult(new BlasMatrixEngine());
    }

    @Test
    public void MatrixMultJama() {
        InternalManualMult(new PlainMatrixEngine());
    }

    public DMatrix manualMultpily(DMatrix matA, DMatrix matB) {
        DMatrix matC = VolatileDMatrix.empty(matA.rows(), matB.columns());
        for (int i = 0; i < matA.rows(); i++) {
            for (int j = 0; j < matB.columns(); j++) {
                for (int k = 0; k < matA.columns(); k++) {
                    matC.add(i, j, matA.get(i, k) * matB.get(k, j));
                }
            }
        }
        return matC;
    }

    public void InternalManualMult(MatrixEngine engine) {

        //  long current = System.currentTimeMillis();

        //Test matrix mult
        int r = 30;
        int o = 30;
        int p = 30;
        DMatrix matA = VolatileDMatrix.random(r, o, 0, 0, 100);
        DMatrix matB = VolatileDMatrix.random(o, p, 0, 0, 100);

        DMatrix result = engine.multiplyTransposeAlphaBeta(TransposeType.NOTRANSPOSE, 1, matA, TransposeType.NOTRANSPOSE, matB, 0, null);
        DMatrix matD = manualMultpily(matA, matB);

        double eps = 1e-7;

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < p; j++) {
                Assert.assertTrue(Math.abs(result.get(i, j) - matD.get(i, j)) < eps);
            }
        }

    }
}
