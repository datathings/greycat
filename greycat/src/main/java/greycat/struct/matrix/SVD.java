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
 * Singular Value Decomposition.
 * <p>
 * For an m-by-n matrix A with m >= n, the singular value decomposition is
 * an m-by-n orthogonal matrix U, an n-by-n diagonal matrix S, and
 * an n-by-n orthogonal matrix V so that A = U*S*V'.
 * <p>
 * The singular values, sigma[k] = S.get(k,k), are ordered so that
 * sigma[0] >= sigma[1] >= ... >= sigma[n-1].
 * <p>
 * The singular value decompostion always exists, so the constructor will
 * never fail.  The matrix condition number and the effective numerical
 * rank can be computed from this decomposition.
 */

class SVD implements SVDDecompose {

/* ------------------------
   Class variables
 * ------------------------ */

    /**
     * Arrays for internal storage of U and V.
     *
     * @serial internal storage of U.
     * @serial internal storage of V.
     */
    private DMatrix U, V;

    /**
     * Array for internal storage of singular values.
     *
     * @serial internal storage of singular values.
     */
    private double[] s;

    /**
     * Row and column dimensions.
     *
     * @serial row dimension.
     * @serial column dimension.
     */
    private int m, n;

/* ------------------------
   Constructor
 * ------------------------ */

    /**
     * Construct the singular value decomposition
     * Structure to access U, S and V.
     *
     * @param Arg Rectangular matrix
     */

    public SVD(DMatrix Arg) {

        // Derived from LINPACK code.
        // Initialize.
        DMatrix A = VolatileDMatrix.cloneFrom(Arg);
        m = Arg.rows();
        n = Arg.columns();

      /* Apparently the failing cases are only a proper subset of (m<n), 
     so let's not throw error.  Correct fix to come later?
      if (m<n) {
	  throw new IllegalArgumentException("Jama SVD only works for m >= n"); }
      */
        int nu = Math.min(m, n);
        s = new double[Math.min(m + 1, n)];
        U = VolatileDMatrix.empty(m, nu);
        V = VolatileDMatrix.empty(n, n);
        double[] e = new double[n];
        double[] work = new double[m];
        // Reduce A to bidiagonal form, storing the diagonal elements
        // in s and the super-diagonal elements in e.

        int nct = Math.min(m - 1, n);
        int nrt = Math.max(0, Math.min(n - 2, m));
        for (int k = 0; k < Math.max(nct, nrt); k++) {
            if (k < nct) {

                // Compute the transformation for the k-th column and
                // place the k-th diagonal in s[k].
                // Compute 2-norm of k-th column without under/overflow.
                s[k] = 0;
                for (int i = k; i < m; i++) {
                    s[k] = Utils.hypot(s[k], A.get(i, k));
                }
                if (s[k] != 0.0) {
                    if (A.get(k, k) < 0.0) {
                        s[k] = -s[k];
                    }
                    for (int i = k; i < m; i++) {
                        A.set(i, k, A.get(i, k) / s[k]);
                    }
                    A.add(k, k, 1.0);
                }
                s[k] = -s[k];
            }
            for (int j = k + 1; j < n; j++) {
                if ((k < nct) && (s[k] != 0.0)) {

                    // Apply the transformation.

                    double t = 0;
                    for (int i = k; i < m; i++) {
                        t += A.get(i, k) * A.get(i, j);
                    }
                    t = -t / A.get(k, k);
                    for (int i = k; i < m; i++) {
                        A.add(i, j, +t * A.get(i, k));
                    }
                }

                // Place the k-th row of A into e for the
                // subsequent calculation of the row transformation.

                e[j] = A.get(k, j);
            }
            if (k < nct) {

                // Place the transformation in U for subsequent back
                // multiplication.

                for (int i = k; i < m; i++) {
                    U.set(i, k, A.get(i, k));
                }
            }
            if (k < nrt) {

                // Compute the k-th row transformation and place the
                // k-th super-diagonal in e[k].
                // Compute 2-norm without under/overflow.
                e[k] = 0;
                for (int i = k + 1; i < n; i++) {
                    e[k] = Utils.hypot(e[k], e[i]);
                }
                if (e[k] != 0.0) {
                    if (e[k + 1] < 0.0) {
                        e[k] = -e[k];
                    }
                    for (int i = k + 1; i < n; i++) {
                        e[i] /= e[k];
                    }
                    e[k + 1] += 1.0;
                }
                e[k] = -e[k];
                if ((k + 1 < m) && (e[k] != 0.0)) {

                    // Apply the transformation.

                    for (int i = k + 1; i < m; i++) {
                        work[i] = 0.0;
                    }
                    for (int j = k + 1; j < n; j++) {
                        for (int i = k + 1; i < m; i++) {
                            work[i] += e[j] * A.get(i, j);
                        }
                    }
                    for (int j = k + 1; j < n; j++) {
                        double t = -e[j] / e[k + 1];
                        for (int i = k + 1; i < m; i++) {
                            A.add(i, j, t * work[i]);
                        }
                    }
                }
                // Place the transformation in V for subsequent
                // back multiplication.

                for (int i = k + 1; i < n; i++) {
                    V.set(i, k, e[i]);
                }
            }
        }

        // Set up the final bidiagonal matrix or order p.

        int p = Math.min(n, m + 1);
        if (nct < n) {
            s[nct] = A.get(nct, nct);
        }
        if (m < p) {
            s[p - 1] = 0.0;
        }
        if (nrt + 1 < p) {
            e[nrt] = A.get(nrt, p - 1);
        }
        e[p - 1] = 0.0;

        // If required, generate U.

        for (int j = nct; j < nu; j++) {
            for (int i = 0; i < m; i++) {
                U.set(i, j, 0.0);
            }
            U.set(j, j, 1.0);
        }
        for (int k = nct - 1; k >= 0; k--) {
            if (s[k] != 0.0) {
                for (int j = k + 1; j < nu; j++) {
                    double t = 0;
                    for (int i = k; i < m; i++) {
                        t += U.get(i, k) * U.get(i, j);
                    }
                    t = -t / U.get(k, k);
                    for (int i = k; i < m; i++) {
                        U.add(i, j, t * U.get(i, k));
                    }
                }
                for (int i = k; i < m; i++) {
                    U.set(i, k, -U.get(i, k));
                }
                U.set(k, k, 1.0 + U.get(k, k));
                for (int i = 0; i < k - 1; i++) {
                    U.set(i, k, 0.0);
                }
            } else {
                for (int i = 0; i < m; i++) {
                    U.set(i, k, 0.0);
                }
                U.set(k, k, 1.0);
            }
        }

        // If required, generate V.

        for (int k = n - 1; k >= 0; k--) {
            if ((k < nrt) && (e[k] != 0.0)) {
                for (int j = k + 1; j < nu; j++) {
                    double t = 0;
                    for (int i = k + 1; i < n; i++) {
                        t += V.get(i, k) * V.get(i, j);
                    }
                    t = -t / V.get(k + 1, k);
                    for (int i = k + 1; i < n; i++) {
                        V.add(i, j, t * V.get(i, k));
                    }
                }
            }
            for (int i = 0; i < n; i++) {
                V.set(i, k, 0.0);
            }
            V.set(k, k, 1.0);
        }

        // Main iteration loop for the singular values.

        int pp = p - 1;
        int iter = 0;
        double eps = Math.pow(2.0, -52.0);
        double tiny = Math.pow(2.0, -966.0);
        while (p > 0) {
            int k, kase;

            // Here is where a test for too many iterations would go.

            // This section of the program inspects for
            // negligible elements in the s and e arrays.  On
            // completion the variables kase and k are set as follows.

            // kase = 1     if s(p) and e[k-1] are negligible and k<p
            // kase = 2     if s(k) is negligible and k<p
            // kase = 3     if e[k-1] is negligible, k<p, and
            //              s(k), ..., s(p) are not negligible (qr step).
            // kase = 4     if e(p-1) is negligible (convergence).

            for (k = p - 2; k >= -1; k--) {
                if (k == -1) {
                    break;
                }
                if (Math.abs(e[k]) <=
                        tiny + eps * (Math.abs(s[k]) + Math.abs(s[k + 1]))) {
                    e[k] = 0.0;
                    break;
                }
            }
            if (k == p - 2) {
                kase = 4;
            } else {
                int ks;
                for (ks = p - 1; ks >= k; ks--) {
                    if (ks == k) {
                        break;
                    }
                    double t = (ks != p ? Math.abs(e[ks]) : 0.) +
                            (ks != k + 1 ? Math.abs(e[ks - 1]) : 0.);
                    if (Math.abs(s[ks]) <= tiny + eps * t) {
                        s[ks] = 0.0;
                        break;
                    }
                }
                if (ks == k) {
                    kase = 3;
                } else if (ks == p - 1) {
                    kase = 1;
                } else {
                    kase = 2;
                    k = ks;
                }
            }
            k++;

            // Perform the task indicated by kase.

            switch (kase) {

                // Deflate negligible s(p).

                case 1: {
                    double f = e[p - 2];
                    e[p - 2] = 0.0;
                    for (int j = p - 2; j >= k; j--) {
                        double t = Utils.hypot(s[j], f);
                        double cs = s[j] / t;
                        double sn = f / t;
                        s[j] = t;
                        if (j != k) {
                            f = -sn * e[j - 1];
                            e[j - 1] = cs * e[j - 1];
                        }
                        for (int i = 0; i < n; i++) {
                            t = cs * V.get(i, j) + sn * V.get(i, p - 1);
                            V.set(i, p - 1, -sn * V.get(i, j) + cs * V.get(i, p - 1));
                            V.set(i, j, t);
                        }
                    }
                }
                break;

                // Split at negligible s(k).

                case 2: {
                    double f = e[k - 1];
                    e[k - 1] = 0.0;
                    for (int j = k; j < p; j++) {
                        double t = Utils.hypot(s[j], f);
                        double cs = s[j] / t;
                        double sn = f / t;
                        s[j] = t;
                        f = -sn * e[j];
                        e[j] = cs * e[j];
                        for (int i = 0; i < m; i++) {
                            t = cs * U.get(i, j) + sn * U.get(i, k - 1);
                            U.set(i, k - 1, -sn * U.get(i, j) + cs * U.get(i, k - 1));
                            U.set(i, j, t);
                        }
                    }
                }
                break;

                // Perform one qr step.

                case 3: {

                    // Calculate the shift.

                    double scale = Math.max(Math.max(Math.max(Math.max(
                            Math.abs(s[p - 1]), Math.abs(s[p - 2])), Math.abs(e[p - 2])),
                            Math.abs(s[k])), Math.abs(e[k]));
                    double sp = s[p - 1] / scale;
                    double spm1 = s[p - 2] / scale;
                    double epm1 = e[p - 2] / scale;
                    double sk = s[k] / scale;
                    double ek = e[k] / scale;
                    double b = ((spm1 + sp) * (spm1 - sp) + epm1 * epm1) / 2.0;
                    double c = (sp * epm1) * (sp * epm1);
                    double shift = 0.0;
                    if ((b != 0.0) || (c != 0.0)) {
                        shift = Math.sqrt(b * b + c);
                        if (b < 0.0) {
                            shift = -shift;
                        }
                        shift = c / (b + shift);
                    }
                    double f = (sk + sp) * (sk - sp) + shift;
                    double g = sk * ek;

                    // Chase zeros.

                    for (int j = k; j < p - 1; j++) {
                        double t = Utils.hypot(f, g);
                        double cs = f / t;
                        double sn = g / t;
                        if (j != k) {
                            e[j - 1] = t;
                        }
                        f = cs * s[j] + sn * e[j];
                        e[j] = cs * e[j] - sn * s[j];
                        g = sn * s[j + 1];
                        s[j + 1] = cs * s[j + 1];
                        for (int i = 0; i < n; i++) {
                            t = cs * V.get(i, j) + sn * V.get(i, j + 1);
                            V.set(i, j + 1, -sn * V.get(i, j) + cs * V.get(i, j + 1));
                            V.set(i, j, t);
                        }
                        t = Utils.hypot(f, g);
                        cs = f / t;
                        sn = g / t;
                        s[j] = t;
                        f = cs * e[j] + sn * s[j + 1];
                        s[j + 1] = -sn * e[j] + cs * s[j + 1];
                        g = sn * e[j + 1];
                        e[j + 1] = cs * e[j + 1];
                        if (j < m - 1) {
                            for (int i = 0; i < m; i++) {
                                t = cs * U.get(i, j) + sn * U.get(i, j + 1);
                                U.set(i, j + 1, -sn * U.get(i, j) + cs * U.get(i, j + 1));
                                U.set(i, j, t);
                            }
                        }
                    }
                    e[p - 2] = f;
                    iter = iter + 1;
                }
                break;
                // Convergence.
                case 4: {
                    // Make the singular values positive.
                    if (s[k] <= 0.0) {
                        s[k] = (s[k] < 0.0 ? -s[k] : 0.0);
                        for (int i = 0; i <= pp; i++) {
                            V.set(i, k, -V.get(i, k));
                        }
                    }
                    // Order the singular values.
                    while (k < pp) {
                        if (s[k] >= s[k + 1]) {
                            break;
                        }
                        double t = s[k];
                        s[k] = s[k + 1];
                        s[k + 1] = t;
                        if (k < n - 1) {
                            for (int i = 0; i < n; i++) {
                                t = V.get(i, k + 1);
                                V.set(i, k + 1, V.get(i, k));
                                V.set(i, k, t);
                            }
                        }
                        if (k < m - 1) {
                            for (int i = 0; i < m; i++) {
                                t = U.get(i, k + 1);
                                U.set(i, k + 1, U.get(i, k));
                                U.set(i, k, t);
                            }
                        }
                        k++;
                    }
                    iter = 0;
                    p--;
                }
                break;
            }
        }
    }

/* ------------------------
   Public Methods
 * ------------------------ */

    @Override
    public SVD factor(DMatrix A, boolean workInPlace) {
        return new SVD(A);
    }

    /**
     * Return the left singular vectors
     *
     * @return U
     */

    @Override
    public DMatrix getU() {
        return U;
    }

    @Override
    public DMatrix getVt() {
        return MatrixOps.transpose(getV());
    }

    /**
     * Return the right singular vectors
     *
     * @return V
     */
    public DMatrix getV() {
        return V;
    }

    /**
     * Return the one-dimensional array of singular values
     *
     * @return diagonal of S.
     */

    public double[] getSingularValues() {
        return s;
    }

    /**
     * Return the diagonal matrix of singular values
     *
     * @return S
     */

    @Override
    public DMatrix getSMatrix() {
        DMatrix X = VolatileDMatrix.empty(Math.min(m, n), n);
        for (int i = 0; i < this.s.length; i++) {
            if (i < m && i < n) {
                X.set(i, i, this.s[i]);
            }
        }
        return X;
    }

    @Override
    public double[] getS() {
        return s;
    }

    /**
     * Two norm
     *
     * @return max(S)
     */

    public double norm2() {
        return s[0];
    }

    /**
     * Two norm condition number
     *
     * @return max(S)/min(S)
     */

    public double cond() {
        return s[0] / s[Math.min(m, n) - 1];
    }

    /**
     * Effective numerical matrix rank
     *
     * @return Number of nonnegligible singular values.
     */

    public int rank() {
        double eps = Math.pow(2.0, -52.0);
        double tol = Math.max(m, n) * s[0] * eps;
        int r = 0;
        for (int i = 0; i < s.length; i++) {
            if (s[i] > tol) {
                r++;
            }
        }
        return r;
    }
}
