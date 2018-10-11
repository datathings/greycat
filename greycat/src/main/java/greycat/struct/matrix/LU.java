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

/**
 * LU Decomposition.
 * <p>
 * For an m-by-n matrix A with m >= n, the LU decomposition is an m-by-n
 * unit lower triangular matrix L, an n-by-n upper triangular matrix U,
 * and a permutation vector piv of length m so that A(piv,:) = L*U.
 * If m < n, then L is m-by-m and U is m-by-n.
 * <p>
 * The LU decompostion with pivoting always exists, even if the matrix is
 * singular, so the constructor will never fail.  The primary use of the
 * LU decomposition is in the solution of square systems of simultaneous
 * linear equations.  This will fail if isNonsingular() returns false.
 */

class LU {

/* ------------------------
   Class variables
 * ------------------------ */

    /**
     * Array for internal storage of decomposition.
     *
     * @serial internal array storage.
     */
    private DMatrix LU;

    /**
     * Row and column dimensions, and pivot sign.
     *
     * @serial column dimension.
     * @serial row dimension.
     * @serial pivot sign.
     */
    private int m, n, pivsign;

    /**
     * Internal storage of pivot vector.
     *
     * @serial pivot vector.
     */
    private int[] piv;

/* ------------------------
   Constructor
 * ------------------------ */

    /**
     * LU Decomposition
     * Structure to access L, U and piv.
     *
     * @param A Rectangular matrix
     */

    public LU(DMatrix A) {
        // Use a "left-looking", dot-product, Crout/Doolittle algorithm.

        LU = VolatileDMatrix.cloneFrom(A);
        m = A.rows();
        n = A.columns();
        piv = new int[m];
        for (int i = 0; i < m; i++) {
            piv[i] = i;
        }
        pivsign = 1;

        double[] LUcolj = new double[m];

        // Outer loop.

        for (int j = 0; j < n; j++) {

            // Make a copy of the j-th column to localize references.

            for (int i = 0; i < m; i++) {
                LUcolj[i] = LU.get(i, j);
            }

            // Apply previous transformations.

            for (int i = 0; i < m; i++) {


                // Most of the time is spent in the following dot product.

                int kmax = Math.min(i, j);
                double s = 0.0;
                for (int k = 0; k < kmax; k++) {
                    s += LU.get(i, k) * LUcolj[k];
                }

                LUcolj[i] -= s;
                LU.set(i, j, LUcolj[i]);
            }

            // Find pivot and exchange if necessary.

            int p = j;
            for (int i = j + 1; i < m; i++) {
                if (Math.abs(LUcolj[i]) > Math.abs(LUcolj[p])) {
                    p = i;
                }
            }
            if (p != j) {
                for (int k = 0; k < n; k++) {
                    double t = LU.get(p, k);
                    LU.set(p, k, LU.get(j, k));
                    LU.set(j, k, t);
                }
                int k = piv[p];
                piv[p] = piv[j];
                piv[j] = k;
                pivsign = -pivsign;
            }

            // Compute multipliers.

            if (j < m && LU.get(j, j) != 0.0) {
                for (int i = j + 1; i < m; i++) {
                    LU.set(i, j, LU.get(i, j) / LU.get(j, j));
                }
            }
        }
    }

/* ------------------------
   Temporary, experimental code.
   ------------------------ *\

   \** LU Decomposition, computed by Gaussian elimination.
   <P>
   This constructor computes L and U with the "daxpy"-based elimination
   algorithm used in LINPACK and MATLAB.  In Java, we suspect the dot-product,
   Crout algorithm will be faster.  We have temporarily included this
   constructor until timing experiments confirm this suspicion.
   <P>
   @param  A             Rectangular matrix
   @param  linpackflag   Use Gaussian elimination.  Actual value ignored.
   @return               Structure to access L, U and piv.
   *\

   public LUDecomposition (DMatrix A, int linpackflag) {
      // Initialize.
      LU = A.getArrayCopy();
      m = A.getRowDimension();
      n = A.getColumnDimension();
      piv = new int[m];
      for (int i = 0; i < m; i++) {
         piv[i] = i;
      }
      pivsign = 1;
      // Main loop.
      for (int k = 0; k < n; k++) {
         // Find pivot.
         int p = k;
         for (int i = k+1; i < m; i++) {
            if (Math.abs(LU.get(i,k)) > Math.abs(LU[p][k])) {
               p = i;
            }
         }
         // Exchange if necessary.
         if (p != k) {
            for (int j = 0; j < n; j++) {
               double t = LU[p][j]; LU[p][j] = LU[k][j]; LU[k][j] = t;
            }
            int t = piv[p]; piv[p] = piv[k]; piv[k] = t;
            pivsign = -pivsign;
         }
         // Compute multipliers and eliminate k-th column.
         if (LU.get(k,k) != 0.0) {
            for (int i = k+1; i < m; i++) {
               LU.get(i,k) /= LU.get(k,k);
               for (int j = k+1; j < n; j++) {
                  LU.get(i,j) -= LU.get(i,k)*LU[k][j];
               }
            }
         }
      }
   }

\* ------------------------
   End of temporary code.
 * ------------------------ */

/* ------------------------
   Public Methods
 * ------------------------ */

    /**
     * Is the matrix nonsingular?
     *
     * @return true if U, and hence A, is nonsingular.
     */

    public boolean isNonsingular() {
        for (int j = 0; j < n; j++) {
            if (LU.get(j, j) == 0)
                return false;
        }
        return true;
    }

    /**
     * Return lower triangular factor
     *
     * @return L
     */

    public DMatrix getL() {
        DMatrix L = VolatileDMatrix.empty(m, n);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (i > j) {
                    L.set(i, j, LU.get(i, j));
                } else if (i == j) {
                    L.set(i, j, 1.0);
                } else {
                    L.set(i, j, 0.0);
                }
            }
        }
        return L;
    }

    /**
     * Return upper triangular factor
     *
     * @return U
     */

    public DMatrix getU() {
        DMatrix U = VolatileDMatrix.empty(n, n);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i <= j) {
                    U.set(i, j, LU.get(i, j));
                } else {
                    U.set(i, j, 0.0);
                }
            }
        }
        return U;
    }

    /**
     * Return pivot permutation vector
     *
     * @return piv
     */

    public int[] getPivot() {
        int[] p = new int[m];
        for (int i = 0; i < m; i++) {
            p[i] = piv[i];
        }
        return p;
    }

    /**
     * Return pivot permutation vector as a one-dimensional double array
     *
     * @return (double) piv
     */

    public double[] getDoublePivot() {
        double[] vals = new double[m];
        for (int i = 0; i < m; i++) {
            vals[i] = (double) piv[i];
        }
        return vals;
    }

    /**
     * Determinant
     *
     * @return det(A)
     * @throws IllegalArgumentException DMatrix must be square
     */

    public double det() {
        if (m != n) {
            throw new IllegalArgumentException("DMatrix must be square.");
        }
        double d = (double) pivsign;
        for (int j = 0; j < n; j++) {
            d *= LU.get(j, j);
        }
        return d;
    }

    /**
     * Solve A*X = B
     *
     * @param B A DMatrix with as many rows as A and any number of columns.
     * @return X so that L*U*X = B(piv,:)
     * @throws IllegalArgumentException DMatrix row dimensions must agree.
     * @throws RuntimeException         DMatrix is singular.
     */

    public DMatrix solve(DMatrix B) {
        if (B.rows() != m) {
            throw new IllegalArgumentException("DMatrix row dimensions must agree.");
        }
        if (!this.isNonsingular()) {
            throw new RuntimeException("DMatrix is singular.");
        }
        // Copy right hand side with pivoting
        int nx = B.columns();
        DMatrix X = getMatrix(B, piv, 0, nx - 1);
        // Solve L*Y = B(piv,:)
        for (int k = 0; k < n; k++) {
            for (int i = k + 1; i < n; i++) {
                for (int j = 0; j < nx; j++) {
                    X.add(i, j, -X.get(k, j) * LU.get(i, k));
                }
            }
        }
        // Solve U*X = Y;
        for (int k = n - 1; k >= 0; k--) {
            for (int j = 0; j < nx; j++) {
                X.set(k, j, X.get(k, j) / LU.get(k, k));
            }
            for (int i = 0; i < k; i++) {
                for (int j = 0; j < nx; j++) {
                    X.add(i, j, -X.get(k, j) * LU.get(i, k));
                }
            }
        }
        return X;
    }

    private DMatrix getMatrix(DMatrix A, int[] r, int j0, int j1) {
        DMatrix B = VolatileDMatrix.empty(r.length, j1 - j0 + 1);
        try {
            for (int i = 0; i < r.length; i++) {
                for (int j = j0; j <= j1; j++) {
                    B.set(i, j - j0, A.get(r[i], j));
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return B;
    }

}
