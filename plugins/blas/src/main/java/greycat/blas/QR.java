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
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.TransposeType;
import greycat.struct.matrix.VolatileDMatrix;

class QR {

    /**
     * The orthogonal matrix
     */
    private DMatrix Q;
    private DMatrix R;
    private Blas _blas;

    /**
     * Factorisation sizes
     */
    int m, n, k;

    /**
     * Work arrays
     */
    double[] work, workGen;

    /**
     * Scales for the reflectors
     */
    double[] tau;

    /**
     * Constructs an empty QR decomposition
     *
     * @param rows    Number of rows. Must be larger than or equal the number of
     *                columns
     * @param columns Number of columns
     */
    public QR(int rows, int columns, Blas blas) {
        this._blas = blas;
        if (columns > rows)
            throw new RuntimeException("n > m");

        this.m = rows;
        this.n = columns;
        this.k = Math.min(m, n);
        tau = new double[k];
        R = VolatileDMatrix.empty(n, n);
    }

    /**
     * Convenience method to compute a QR decomposition
     *
     * @param A DMatrix to decompose. Not modified
     * @return Newly allocated decomposition
     */
    public static QR factorize(DMatrix A, boolean workInPlace, Blas blas) {
        return new QR(A.rows(), A.columns(), blas).factor(A, workInPlace);
    }

    public QR factor(DMatrix matA, boolean workInPlace) {
        DMatrix A;
        if (!workInPlace) {
            A = VolatileDMatrix.cloneFrom(matA);
        } else {
            A = matA;
        }
        int lwork;

        // CoreQuery optimal workspace. First for computing the factorization
        work = new double[1];
        int[] info = new int[1];
        info[0] = 0;
        _blas.dgeqrf(m, n, new double[0], 0, m,
                new double[0], 0, work, 0, -1, info);

        if (info[0] != 0)
            lwork = n;
        else
            lwork = (int) work[0];
        lwork = Math.max(1, lwork);
        work = new double[lwork];

        // Workspace needed for generating an explicit orthogonal matrix
        workGen = new double[1];
        info[0] = 0;
        _blas.dorgqr(m, n, k, new double[0], 0, m, new double[0], 0, workGen, 0, -1, info);

        if (info[0] != 0)
            lwork = n;
        else
            lwork = (int) workGen[0];
        lwork = Math.max(1, lwork);
        workGen = new double[lwork];

        /*
         * Calculate factorisation, and extract the triangular factor
         */
        info[0] = 0;
        _blas.dgeqrf(m, n, A.data(), 0, m, tau, 0, work, 0, work.length, info);

        if (info[0] < 0)
            throw new RuntimeException("" + info[0]);

        for (int col = 0; col < A.columns(); col++) {
            for (int row = 0; row <= col; row++) {
                R.set(row, col, A.get(row, col));
            }
        }

        /*
         * Generate the orthogonal matrix
         */
        info[0] = 0;
        _blas.dorgqr(m, n, k, A.data(), 0, m, tau, 0, workGen, 0, workGen.length, info);

        if (info[0] < 0)
            throw new RuntimeException();

        Q = A;

        return this;
    }

    public void solve(DMatrix B, DMatrix X) {
        int BnumCols = B.columns();
        DMatrix Y = VolatileDMatrix.empty(m, 1);
        DMatrix Z;
        // solve each column one by one
        for (int colB = 0; colB < BnumCols; colB++) {
            // make a copy of this column in the vector
            for (int i = 0; i < m; i++) {
                Y.unsafeSet(i, B.get(i, colB));
            }
            // Solve Qa=b
            // a = Q'b
            Z = MatrixOps.multiplyTranspose(TransposeType.TRANSPOSE, Q, TransposeType.NOTRANSPOSE, Y);
            // solve for Rx = b using the standard upper triangular blassolver
            solveU(R, Z.data(), n, m);
            // save the results
            for (int i = 0; i < n; i++) {
                X.set(i, colB, Z.unsafeGet(i));
            }
        }
    }

    private void solveU(DMatrix U, double[] b, int n, int m) {
        for (int i = n - 1; i >= 0; i--) {
            double sum = b[i];
            for (int j = i + 1; j < n; j++) {
                sum -= U.get(i, j) * b[j];
            }
            b[i] = sum / U.get(i, i);
        }
    }

    /**
     * Returns the upper triangular factor
     */
    public DMatrix getR() {
        return R;
    }

    public DMatrix getQ() {
        return Q;
    }
}
