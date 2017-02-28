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
package greycat.blasTest;

import greycat.blas.BlasMatrixEngine;
import greycat.blas.HybridMatrixEngine;
import greycat.struct.DMatrix;
import greycat.struct.matrix.*;
import org.junit.Assert;
import org.junit.Test;

public class OpsTest {

    int exec = 1000;
    boolean enablebench = false;
    int dimMax = 50;

    /**
     * @native ts
     */
    @Test
    public void optimize() {
        if (!enablebench) {
            return;
        }
        MatrixEngine blas = new BlasMatrixEngine();
        MatrixEngine jama = new PlainMatrixEngine();

        MatrixSVD(blas, dimMax);
        MatrixSVD(jama, dimMax);

        long start;
        long blastime, jamatime;
        double ratio;

        int dim;

        for (dim = 5; dim < dimMax; dim++) {
            start = System.currentTimeMillis();
            for (int z = 0; z < exec; z++) {
                MatrixMult(blas, dim);
            }
            blastime = System.currentTimeMillis() - start;

            start = System.currentTimeMillis();
            for (int z = 0; z < exec; z++) {
                MatrixMult(jama, dim);
            }
            jamatime = System.currentTimeMillis() - start;
            ratio = jamatime * 1.0 / blastime;
            if (jamatime < blastime) {
                //System.out.println("DIM " + dim + " Blas MULT " + blastime + " JAMA MULT " + jamatime + " ratio " + ratio);
            } else {
                System.out.println("DIM " + dim + " Blas MULT " + blastime + " JAMA MULT " + jamatime + " ratio " + ratio + " WIN FOR BLAS: " + dim);
            }
        }

        System.out.println("");


        for (dim = 5; dim < dimMax; dim++) {
            start = System.currentTimeMillis();
            for (int z = 0; z < exec; z++) {
                MatrixSVD(blas, dim);
            }
            blastime = System.currentTimeMillis() - start;

            start = System.currentTimeMillis();
            for (int z = 0; z < exec; z++) {
                MatrixSVD(jama, dim);
            }
            jamatime = System.currentTimeMillis() - start;
            ratio = jamatime * 1.0 / blastime;
            if (jamatime < blastime) {
                //System.out.println("DIM " + dim + " Blas SVD " + blastime + " JAMA SVD " + jamatime + " ratio " + ratio);
            } else {
                System.out.println("DIM " + dim + " Blas SVD " + blastime + " JAMA SVD " + jamatime + " ratio " + ratio + " WIN FOR BLAS: " + dim);
            }
        }

        System.out.println("");

        for (dim = 5; dim < dimMax; dim++) {
            start = System.currentTimeMillis();
            for (int z = 0; z < exec; z++) {
                MatrixQR(blas, dim);
            }
            blastime = System.currentTimeMillis() - start;

            start = System.currentTimeMillis();
            for (int z = 0; z < exec; z++) {
                MatrixQR(jama, dim);
            }
            jamatime = System.currentTimeMillis() - start;
            ratio = jamatime * 1.0 / blastime;
            if (jamatime < blastime) {
               // System.out.println("DIM " + dim + " Blas QR " + blastime + " JAMA QR " + jamatime + " ratio " + ratio);
            } else {
                System.out.println("DIM " + dim + " Blas QR " + blastime + " JAMA QR " + jamatime + " ratio " + ratio + " WIN FOR BLAS: " + dim);
            }
        }

        System.out.println("");

        for (dim = 5; dim < dimMax; dim++) {
            start = System.currentTimeMillis();
            for (int z = 0; z < exec; z++) {
                MatrixLU(blas, dim);
            }
            blastime = System.currentTimeMillis() - start;

            start = System.currentTimeMillis();
            for (int z = 0; z < exec; z++) {
                MatrixLU(jama, dim);
            }
            jamatime = System.currentTimeMillis() - start;
            ratio = jamatime * 1.0 / blastime;
            if (jamatime < blastime) {
                //      System.out.println("DIM " + dim + " Blas LU " + blastime + " JAMA LU " + jamatime +" ratio " + ratio );
            } else {
                System.out.println("DIM " + dim + " Blas LU " + blastime + " JAMA LU " + jamatime + " ratio " + ratio + " WIN FOR BLAS: " + dim);
            }
        }

        System.out.println("");

        for (dim = 5; dim < dimMax; dim++) {
            start = System.currentTimeMillis();
            for (int z = 0; z < exec; z++) {
                MatrixPseudoInv(blas, dim);
            }
            blastime = System.currentTimeMillis() - start;

            start = System.currentTimeMillis();
            for (int z = 0; z < exec; z++) {
                MatrixPseudoInv(jama, dim);
            }
            jamatime = System.currentTimeMillis() - start;
            ratio = jamatime * 1.0 / blastime;
            if (jamatime < blastime) {
                //     System.out.println("DIM " + dim + " Blas Pinv " + blastime + " JAMA Pinv " + jamatime +" ratio " + ratio );
            } else {
                System.out.println("DIM " + dim + " Blas Pinv " + blastime + " JAMA Pinv " + jamatime + " ratio " + ratio + " WIN FOR BLAS: " + dim);
            }
        }
        System.out.println("");


        for (dim = 5; dim < dimMax; dim++) {
            start = System.currentTimeMillis();
            for (int z = 0; z < exec; z++) {
                MatrixInvert(blas, dim);
            }
            blastime = System.currentTimeMillis() - start;

            start = System.currentTimeMillis();
            for (int z = 0; z < exec; z++) {
                MatrixInvert(jama, dim);
            }
            jamatime = System.currentTimeMillis() - start;
            ratio = jamatime * 1.0 / blastime;
            if (jamatime < blastime) {
                //     System.out.println("DIM " + dim + " Blas invert " + blastime + " JAMA invert " + jamatime +" ratio " + ratio );
            } else {
                System.out.println("DIM " + dim + " Blas invert " + blastime + " JAMA invert " + jamatime + " ratio " + ratio + " WIN FOR BLAS: " + dim);
            }
        }
    }

    /**
     * @native ts
     */
    @Test
    public void decompose_blas() {
        MatrixEngine engine = new BlasMatrixEngine();
        int dim = dimMax;
        MatrixSVD(engine, dim);
        MatrixInvert(engine, dim);
        MatrixLU(engine, dim);
        MatrixQR(engine, dim);
        MatrixPseudoInv(engine, dim);
    }

    /**
     * @native ts
     */
    @Test
    public void decompose_jama() {
        MatrixEngine engine = new PlainMatrixEngine();
        int dim = dimMax;
        MatrixSVD(engine, dim);
        MatrixInvert(engine, dim);
        MatrixLU(engine, dim);
        MatrixQR(engine, dim);
        MatrixPseudoInv(engine, dim);
    }

    /**
     * @native ts
     */
    @Test
    public void decompose_Hybrid() {
        MatrixEngine engine = new HybridMatrixEngine();
        int dim = dimMax;
        MatrixSVD(engine, dim);
        MatrixInvert(engine, dim);
        MatrixLU(engine, dim);
        MatrixQR(engine, dim);
        MatrixPseudoInv(engine, dim);
    }

    public void MatrixMult(MatrixEngine engine, int dim) {
        double eps = 1e-7;
        DMatrix matA = VolatileDMatrix.random(dim, dim, 0, 0, 100);
        DMatrix matB = VolatileDMatrix.random(dim, dim, 0, 0, 100);
        DMatrix res = engine.multiplyTransposeAlphaBeta(TransposeType.NOTRANSPOSE, 1.0, matA, TransposeType.NOTRANSPOSE, matB, 0, null);
    }

    public void MatrixInvert(MatrixEngine engine, int dim) {
        double eps = 1e-7;

        DMatrix matA = VolatileDMatrix.random(dim, dim, 0, 0, 100);
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

    public void MatrixLU(MatrixEngine engine, int dim) {
        int m = dim;
        int n = dim;
        int p = dim;
        double eps = 1e-7;

        DMatrix matA = VolatileDMatrix.random(m, n, 0, 0, 100);
        DMatrix matB = VolatileDMatrix.random(m, p, 0, 0, 100);
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


    public void MatrixQR(MatrixEngine engine, int dim) {
        int m = dim;
        int n = dim;
        int p = dim;
        double eps = 1e-6;

        DMatrix matA = VolatileDMatrix.random(m, n, 0, 0, 100);
        DMatrix matB = VolatileDMatrix.random(m, p, 0, 0, 100);

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


    public void MatrixPseudoInv(MatrixEngine engine, int dim) {
        int m = dim;
        int n = dim;
        double eps = 1e-6;

        DMatrix matA = VolatileDMatrix.random(m, n, 0, 0, 100);
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


    public void MatrixSVD(MatrixEngine engine, int dim) {
        int m = dim;
        int n = dim;
        double eps = 1e-7;
        DMatrix matA = VolatileDMatrix.random(m, n, 0, 0, 100);
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
