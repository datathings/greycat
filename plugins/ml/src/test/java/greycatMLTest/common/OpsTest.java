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
package greycatMLTest.common;

import greycat.struct.DMatrix;
import greycat.struct.matrix.*;
import org.junit.Assert;
import org.junit.Test;

public class OpsTest {

    JavaRandom rand = new JavaRandom();
    int exec = 1000;
    boolean enablebench = false;
    int dim = 10;

    /**
     * @native ts
     */
    @Test
    public void decompose() {
        rand.setSeed(0);
        MatrixEngine engine = new PlainMatrixEngine();
        MatrixSVD(engine);
        MatrixInvert(engine);
        MatrixLU(engine);
        MatrixQR(engine);
        MatrixPseudoInv(engine);
    }

    public void MatrixMult(MatrixEngine engine) {
        double eps = 1e-7;

        DMatrix matA = VolatileDMatrix.random(dim, dim, rand, 0, 100);
        DMatrix matB = VolatileDMatrix.random(dim, dim, rand, 0, 100);
        DMatrix res = engine.multiplyTransposeAlphaBeta(TransposeType.NOTRANSPOSE, 1.0, matA, TransposeType.NOTRANSPOSE, matB, 0, null);
    }

    public void MatrixInvert(MatrixEngine engine) {
        double eps = 1e-7;

        DMatrix matA = VolatileDMatrix.random(dim, dim, rand, 0, 100);
        DMatrix res = engine.invert(matA, false);

        if (!enablebench) {
            DMatrix id = MatrixOps.multiply(matA, res);
            for (int i = 0; i < dim; i++) {
                for (int j = 0; j < dim; j++) {
                    double x;
                    if (i == j) {
                        x = 1;
                    } else {
                        x = 0;
                    }
                    Assert.assertTrue(Math.abs(id.get(i, j) - x) < eps);
                }
            }
        }
    }

    public void MatrixLU(MatrixEngine engine) {
        int m = dim;
        int n = dim;
        int p = dim;
        double eps = 1e-7;

        DMatrix matA = VolatileDMatrix.random(m, n, rand, 0, 100);
        DMatrix matB = VolatileDMatrix.random(m, p, rand, 0, 100);
        DMatrix res = engine.solveLU(matA, matB, false, TransposeType.NOTRANSPOSE);
        if (!enablebench) {
            DMatrix temp = MatrixOps.multiply(matA, res);
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    Assert.assertTrue(Math.abs(matB.get(i, j) - temp.get(i, j)) < eps);
                }
            }
        }
    }


    public void MatrixQR(MatrixEngine engine) {
        int m = dim;
        int n = dim;
        int p = dim;
        double eps = 1e-6;

        DMatrix matA = VolatileDMatrix.random(m, n, rand, 0, 100);
        DMatrix matB = VolatileDMatrix.random(m, p, rand, 0, 100);

        DMatrix res = engine.solveQR(matA, matB, false, TransposeType.NOTRANSPOSE);
        if (!enablebench) {
            DMatrix temp = MatrixOps.multiply(matA, res);
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < p; j++) {
                    Assert.assertTrue(Math.abs(matB.get(i, j) - temp.get(i, j)) < eps);
                }
            }
        }
    }


    public void MatrixPseudoInv(MatrixEngine engine) {
        int m = dim;
        int n = dim;
        double eps = 1e-6;

        DMatrix matA = VolatileDMatrix.random(m, n, rand, 0, 100);
        DMatrix res = engine.pinv(matA, false);
        if (!enablebench) {
            DMatrix id = MatrixOps.multiply(res, matA);
            for (int i = 0; i < id.rows(); i++) {
                for (int j = 0; j < id.columns(); j++) {
                    double x;
                    if (i == j) {
                        x = 1;
                    } else {
                        x = 0;
                    }
                    Assert.assertTrue(Math.abs(id.get(i, j) - x) < eps);
                }
            }
        }
    }


    public void MatrixSVD(MatrixEngine engine) {
        int m = dim;
        int n = dim;
        double eps = 1e-7;
        DMatrix matA = VolatileDMatrix.random(m, n, rand, 0, 100);
        SVDDecompose svd = engine.decomposeSVD(matA, false);
        if (!enablebench) {
            DMatrix U = svd.getU();
            DMatrix S = svd.getSMatrix();
            DMatrix Vt = svd.getVt();

            DMatrix res = MatrixOps.multiply(U, S);
            res = MatrixOps.multiply(res, Vt);

            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    Assert.assertTrue(Math.abs(res.get(i, j) - matA.get(i, j)) < eps);
                }
            }
        }
    }
}
