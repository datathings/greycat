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
package greycat.struct.matrix;

import greycat.struct.DMatrix;

public class PInvSVD {

    private SVDDecompose _svd;
    private DMatrix pinv;
    private DMatrix S;
    private int rank;
    private double det;

    public int getRank() {
        return rank;
    }

    public double getDeterminant() {
        return det;
    }
    
    public PInvSVD factor(DMatrix A, boolean invertInPlace) {
        _svd = MatrixOps.defaultEngine().decomposeSVD(A, invertInPlace);
        //We get UxSxVt

        //  debug purpose
        //  KMatrix t1= DMatrix.multiply(svd[0],svd[1]);
        //  KMatrix t2= DMatrix.multiply(t1,svd[2]);
        DMatrix V = _svd.getVt();
        S = VolatileDMatrix.cloneFrom(_svd.getSMatrix());

        double maxSingular = 0;
        int dim = Math.min(S.columns(), S.rows());
        for (int i = 0; i < dim; i++) {
            if (S.get(i, i) > maxSingular)
                maxSingular = S.get(i, i);
        }
        double tau = Math.pow(2, -46) * Math.max(A.columns(), A.rows()) * maxSingular;

        rank = 0;
        det = 1;
        // computer the pseudo inverse of A
        if (maxSingular != 0.0) {
            for (int i = 0; i < dim; i++) {
                double s = S.get(i, i);
                if (s < tau)
                    S.set(i, i, 0);
                else {
                    S.set(i, i, 1 / s);
                    det = det * s;
                    rank++;
                }
            }
        }

        // V*W
        DMatrix temp = MatrixOps.multiplyTranspose(TransposeType.TRANSPOSE, V, TransposeType.TRANSPOSE, S);
        //V*W*Ut
        pinv = MatrixOps.multiplyTranspose(TransposeType.NOTRANSPOSE, temp, TransposeType.TRANSPOSE, _svd.getU());
        return this;
    }

    public SVDDecompose getSvd() {
        return _svd;
    }

    public DMatrix getInvDeterminant() {
        return S;
    }

    public DMatrix getPInv() {
        return pinv;
    }
}
