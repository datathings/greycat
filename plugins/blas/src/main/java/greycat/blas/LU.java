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
import greycat.struct.matrix.TransposeType;
import greycat.struct.matrix.VolatileDMatrix;

class LU {

    /**
     * Holds the LU factors
     */
    private DMatrix LU;
    private Blas _blas;

    public DMatrix getLU() {
        return LU;
    }

    /**
     * Row pivotations
     */
    private int[] piv;

    /**
     * True if the matrix was singular
     */
    private boolean singular;

    /**
     * Constructor for DenseLU
     *
     * @param m Number of rows
     * @param n Number of columns
     */
    public LU(int m, int n, Blas blas) {
        this._blas = blas;
        LU = VolatileDMatrix.empty(m, n);
        piv = new int[Math.min(m, n)];
    }

    /**
     * Creates an LU decomposition of the given matrix
     *
     * @param A DMatrix to decompose. Not modified
     * @return The current decomposition
     */
    public static LU factorize(DMatrix A, Blas blas) {
        return new LU(A.rows(), A.columns(), blas).factor(A, false);
    }

    /**
     * Creates an LU decomposition of the given matrix
     *
     * @param A DMatrix to decompose. Overwritten with the decomposition
     * @return The current decomposition
     */
    public LU factor(DMatrix A, boolean factorInPlace) {
        if (factorInPlace) {
            singular = false;

            int[] info = new int[1];
            info[0] = 0;
            _blas.dgetrf(A.rows(), A.columns(), A.data(), 0, A.rows(), piv, 0, info);

            if (info[0] > 0)
                singular = true;
            else if (info[0] < 0)
                throw new RuntimeException();

            LU.fillWith(A.data());
            return this;
        } else {
            singular = false;
            DMatrix B = VolatileDMatrix.cloneFrom(A);
            int[] info = new int[1];
            info[0] = 0;
            _blas.dgetrf(B.rows(), B.columns(), B.data(), 0, B.rows(), piv, 0, info);

            if (info[0] > 0)
                singular = true;
            else if (info[0] < 0)
                throw new RuntimeException();

            LU.fillWith(B.data());
            return this;
        }
    }

    public DMatrix getL() {
        int numRows = LU.rows();
        int numCols = LU.rows() < LU.columns() ? LU.rows() : LU.columns();
        DMatrix lower = VolatileDMatrix.empty(numRows, numCols);
        for (int i = 0; i < numCols; i++) {
            lower.set(i, i, 1.0);
            for (int j = 0; j < i; j++) {
                lower.set(i, j, LU.get(i, j));
            }
        }
        if (numRows > numCols) {
            for (int i = numCols; i < numRows; i++) {
                for (int j = 0; j < numCols; j++) {
                    lower.set(i, j, LU.get(i, j));
                }
            }
        }
        return lower;
    }

    /*
    public DMatrix getP() {
        return DMatrix.fromPartialPivots(piv, true);
    }*/

    public DMatrix getU() {
        int numRows = LU.rows() < LU.columns() ? LU.rows() : LU.columns();
        int numCols = LU.columns();
        DMatrix upper = VolatileDMatrix.empty(numRows, numCols);
        for (int i = 0; i < numRows; i++) {
            for (int j = i; j < numCols; j++) {
                upper.set(i, j, LU.get(i, j));
            }
        }
        return upper;
    }


    /**
     * Returns the row pivots
     */
    public int[] getPivots() {
        return piv;
    }

    /**
     * Checks for singularity
     */
    public boolean isSingular() {
        return singular;
    }

    /**
     * Computes <code>A\B</code>, overwriting <code>B</code>
     */
    public DMatrix solve(DMatrix B) {
        return transSolve(B, TransposeType.NOTRANSPOSE);
    }

    public DMatrix transSolve(DMatrix B, TransposeType trans) {
        /*
        if (singular) {
         //   throw new MatrixSingularException();
        }
        */
        if (B.rows() != LU.rows())
            throw new RuntimeException("B.numRows() != LU.numRows()");

        int[] info = new int[1];
        _blas.dgetrs(trans, LU.rows(),
                B.columns(), LU.data(), 0, LU.rows(), piv, 0,
                B.data(), 0, B.rows(), info);

        if (info[0] < 0)
            throw new RuntimeException();

        return B;
    }

    public boolean invert(DMatrix A) {
        int[] info = new int[1];
        info[0] = 0;
        _blas.dgetrf(A.rows(), A.columns(), A.data(), 0, A.rows(), piv, 0, info);

       /* System.out.println("After f");
        for(int i=0;i<A.rows()*A.columns(); i++){
            System.out.print(A.getAtIndex(i)+" ");
        }
        System.out.println();

        System.out.println("PIV");
        for(int i=0;i<piv.length; i++){
            System.out.print(piv[i]+" ");
        }
        System.out.println();*/

        if (info[0] > 0) {
            singular = true;
            return false;
        } else if (info[0] < 0)
            throw new RuntimeException();

        int lwork = A.rows() * A.rows();
        double[] work = new double[lwork];
        for (int i = 0; i < lwork; i++) {
            work[i] = 0;
        }
        _blas.dgetri(A.rows(), A.data(), 0, A.rows(), piv, 0, work, 0, lwork, info);
        if (info[0] != 0) {
            return false;
        } else {
            return true;
        }
    }

}