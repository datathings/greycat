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
package greycatTest;

import greycat.struct.DMatrix;
import greycat.struct.matrix.*;
import org.junit.Test;

public class MatrixTest {
    //@Test
    public void testLU() {

        int totalrows = 4;
        int totalcols = 4;

        DMatrix matA = VolatileDMatrix.empty(totalrows, totalcols);
        DMatrix matB = VolatileDMatrix.empty(totalcols, 1);

        for (int col = 0; col < totalcols; col++) {
            for (int row = 0; row < totalrows; row++) {
                if ((row + col) % 2 == 0) {
                    matA.set(row, col, (row * 3 + col * 7));
                } else {
                    matA.set(row, col, (row * 5 - col * 3));
                }
            }
        }

        for (int row = 0; row < totalcols; row++) {
            matB.set(row, 0, row);
        }


        MatrixOps.print(matA, "Matrix A");
        MatrixOps.print(matB, "Matrix B");

        MatrixEngine me = new PlainMatrixEngine();

        DMatrix matC = MatrixOps.multiply(matA, matB);
        MatrixOps.print(matC, "Matrix C=AxB");

        DMatrix matX = me.solveLU(matA, matB, false, TransposeType.NOTRANSPOSE);
        MatrixOps.print(matX, "Matrix X");


        DMatrix matY=me.solveQR(matA,matB,false,TransposeType.NOTRANSPOSE);
        MatrixOps.print(matY, "Matrix Y");

        DMatrix matAinv=me.invert(matA,false);
        MatrixOps.print(matAinv, "Matrix Ainv");

        MatrixOps.print(MatrixOps.multiply(matAinv,matA), "Matrix I");

        SVDDecompose svd = me.decomposeSVD(matA,false);

        MatrixOps.print(svd.getU(),"svd U");
        MatrixOps.print(svd.getVt(),"svd V");
        MatrixOps.printArray(svd.getS(),"svd S");

    }
}
