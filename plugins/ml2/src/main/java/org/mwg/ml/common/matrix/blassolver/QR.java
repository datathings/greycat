package org.mwg.ml.common.matrix.blassolver;


import org.mwg.ml.common.matrix.Matrix;
import org.mwg.ml.common.matrix.TransposeType;
import org.mwg.ml.common.matrix.blassolver.blas.KBlas;

/**
 * @ignore ts
 */
public class QR {

    /**
     * The orthogonal matrix
     */
    private Matrix Q;
    private Matrix R;
    private KBlas _blas;

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
    public QR(int rows, int columns, KBlas blas) {
        this._blas = blas;
        if (columns > rows)
            throw new RuntimeException("n > m");

        this.m = rows;
        this.n = columns;
        this.k = Math.min(m, n);
        tau = new double[k];
        R = new Matrix(null, n, n);

    }

    /**
     * Convenience method to compute a QR decomposition
     *
     * @param A Matrix to decompose. Not modified
     * @return Newly allocated decomposition
     */
    public static QR factorize(Matrix A, boolean workInPlace, KBlas blas) {
        return new QR(A.rows(), A.columns(), blas).factor(A, workInPlace);
    }

    public QR factor(Matrix matA, boolean workInPlace) {
        Matrix A;
        if (!workInPlace) {
            A = matA.clone();
        } else {
            A = matA;
        }

        int lwork;

        // CoreQuery optimal workspace. First for computing the factorization
        {
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
        }

        // Workspace needed for generating an explicit orthogonal matrix
        {
            workGen = new double[1];
            int[] info = new int[1];
            info[0] = 0;
            _blas.dorgqr(m, n, k, new double[0], 0, m, new double[0], 0, workGen, 0, -1, info);

            if (info[0] != 0)
                lwork = n;
            else
                lwork = (int) workGen[0];
            lwork = Math.max(1, lwork);
            workGen = new double[lwork];
        }

        /*
         * Calculate factorisation, and extract the triangular factor
         */
        int[] info = new int[1];
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


    public void solve(Matrix B, Matrix X) {
        int BnumCols = B.columns();

        Matrix temp=Matrix.multiplyTransposeAlphaBeta(TransposeType.TRANSPOSE,1.0,Q,TransposeType.NOTRANSPOSE,1.0,B);

        // solve each column one by one
        for (int colB = 0; colB < BnumCols; colB++) {
            for (int i = n - 1; i >= 0; i--) {
                double sum = temp.get(i,colB);
                for (int j = i + 1; j < n; j++) {
                    sum -= R.get(i, j) * temp.get(j,colB);
                }
                double res=sum / R.get(i, i);
                temp.set(i,colB, res);
                X.set(i,colB,res);
            }

            /*
            // make a copy of this column in the vector
            for (int i = 0; i < m; i++) {
                Y.set(i,0, B.get(i, colB));
            }
            // Solve Qa=b
            // a = Q'b
            Z = Matrix.multiplyTransposeAlphaBeta(TransposeType.TRANSPOSE, 1.0, Q, TransposeType.NOTRANSPOSE, 1.0, Y);

            // solve for Rx = b using the standard upper triangular blassolver
            solveU(R, Z.data(), n);
            // save the results
            for (int i = 0; i < n; i++) {
                X.set(i, colB, Z.getAtIndex(i));
            }*/
        }


    }

   /* private void solveU(Matrix U, double[] b, int n) {
        for (int i = n - 1; i >= 0; i--) {
            double sum = b[i];
            for (int j = i + 1; j < n; j++) {
                sum -= U.get(i, j) * b[j];
            }
            b[i] = sum / U.get(i, i);
        }
    }*/


    /**
     * Returns the upper triangular factor
     */
    public Matrix getR() {
        return R;
    }

    public Matrix getQ() {
        return Q;
    }
}