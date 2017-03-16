/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.blas;

import greycat.struct.matrix.TransposeType;
import org.netlib.blas.Dgemm;
import org.netlib.lapack.*;
import org.netlib.util.intW;

//Fortran code converted to java class, slower but good for debug

/**
 * @ignore ts
 */
public class F2JBlas implements Blas {
    @Override
    public void dgemm(TransposeType paramString1, TransposeType paramString2, int paramInt1, int paramInt2, int paramInt3, double paramDouble1, double[] paramArrayOfDouble1, int paramInt4, int paramInt5, double[] paramArrayOfDouble2, int paramInt6, int paramInt7, double paramDouble2, double[] paramArrayOfDouble3, int paramInt8, int paramInt9) {
        Dgemm.dgemm(transTypeToChar(paramString1), transTypeToChar(paramString2), paramInt1, paramInt2, paramInt3, paramDouble1, paramArrayOfDouble1, paramInt4, paramInt5, paramArrayOfDouble2, paramInt6, paramInt7, paramDouble2, paramArrayOfDouble3, paramInt8, paramInt9);
    }

    @Override
    public void dgetrs(TransposeType paramString, int paramInt1, int paramInt2, double[] paramArrayOfDouble1, int paramInt3, int paramInt4, int[] paramArrayOfInt, int paramInt5, double[] paramArrayOfDouble2, int paramInt6, int paramInt7, int[] paramintW) {
        intW newint = new intW(paramintW[0]);
        Dgetrs.dgetrs(transTypeToChar(paramString), paramInt1, paramInt2, paramArrayOfDouble1, paramInt3, paramInt4, paramArrayOfInt, paramInt5, paramArrayOfDouble2, paramInt6, paramInt7, newint);
        paramintW[0] = newint.val;
      /*  public void dgetrs(java.lang.String trans, int n, int nrhs, double[] a, int _a_offset, int lda, int[] ipiv, int _ipiv_offset, double[] b, int _b_offset, int ldb, org.netlib.common.intW info) {
            dgetrs_offsets(trans, n, nrhs, a, _a_offset, lda, ipiv, _ipiv_offset, b, _b_offset, ldb, info);
        }*/
    }

    @Override
    public void dgetri(int paramInt1, double[] paramArrayOfDouble1, int paramInt2, int paramInt3, int[] paramArrayOfInt, int paramInt4, double[] paramArrayOfDouble2, int paramInt5, int paramInt6, int[] paramintW) {
        intW newint = new intW(paramintW[0]);
        Dgetri.dgetri(paramInt1, paramArrayOfDouble1, paramInt2, paramInt3, paramArrayOfInt, paramInt4, paramArrayOfDouble2, paramInt5, paramInt6, newint);
        paramintW[0] = newint.val;
    }

    @Override
    public void dgetrf(int paramInt1, int paramInt2, double[] paramArrayOfDouble, int paramInt3, int paramInt4, int[] paramArrayOfInt, int paramInt5, int[] paramintW) {
        intW newint = new intW(paramintW[0]);
        Dgetrf.dgetrf(paramInt1, paramInt2, paramArrayOfDouble, paramInt3, paramInt4, paramArrayOfInt, paramInt5, newint);
        paramintW[0] = newint.val;
    }

    @Override
    public void dorgqr(int paramInt1, int paramInt2, int paramInt3, double[] paramArrayOfDouble1, int paramInt4, int paramInt5, double[] paramArrayOfDouble2, int paramInt6, double[] paramArrayOfDouble3, int paramInt7, int paramInt8, int[] paramintW) {
        intW newint = new intW(paramintW[0]);
        Dorgqr.dorgqr(paramInt1, paramInt2, paramInt3, paramArrayOfDouble1, paramInt4, paramInt5, paramArrayOfDouble2, paramInt6, paramArrayOfDouble3, paramInt7, paramInt8, newint);
        paramintW[0] = newint.val;
    }

    @Override
    public void dgeqrf(int paramInt1, int paramInt2, double[] paramArrayOfDouble1, int paramInt3, int paramInt4, double[] paramArrayOfDouble2, int paramInt5, double[] paramArrayOfDouble3, int paramInt6, int paramInt7, int[] paramintW) {
        intW newint = new intW(paramintW[0]);
        Dgeqrf.dgeqrf(paramInt1, paramInt2, paramArrayOfDouble1, paramInt3, paramInt4, paramArrayOfDouble2, paramInt5, paramArrayOfDouble3, paramInt6, paramInt7, newint);
        paramintW[0] = newint.val;
    }

    @Override
    public void dgesdd(String jobz, int m, int n, double[] data, int lda, double[] s, double[] u, int ldu, double[] vt, int ldvt, double[] work, int length, int[] iwork, int[] info) {
        intW newint = new intW(info[0]);
        Dgesdd.dgesdd(jobz, m, n, data, 0, lda, s, 0, u, 0, ldu, vt, 0, ldvt, work, 0, length, iwork, 0, newint);
        info[0] = newint.val;

    }

    @Override
    public void connect() {
    }

    @Override
    public void disconnect() {

    }

    private static final String TRANSPOSE_TYPE_CONJUCATE = "c";

    private static final String TRANSPOSE_TYPE_NOTRANSPOSE = "n";

    private static final String TRANSPOSE_TYPE_TRANSPOSE = "t";

    private static String transTypeToChar(TransposeType type) {
        if (type == TransposeType.NOTRANSPOSE) {
            return TRANSPOSE_TYPE_NOTRANSPOSE;
        } else if (type == TransposeType.TRANSPOSE) {
            return TRANSPOSE_TYPE_TRANSPOSE;
        }
        return null;
    }
}
