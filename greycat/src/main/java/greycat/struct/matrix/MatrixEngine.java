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

public interface MatrixEngine {

    DMatrix multiplyTransposeAlphaBeta(TransposeType transA, double alpha, DMatrix matA, TransposeType transB, DMatrix matB, double beta, DMatrix matC);

    DMatrix invert(DMatrix mat, boolean invertInPlace);

    DMatrix pinv(DMatrix mat, boolean invertInPlace);

    //Solve AX=B -> return X as a result
    DMatrix solveLU(DMatrix matA, DMatrix matB, boolean workInPlace, TransposeType transB);

    DMatrix solveQR(DMatrix matA, DMatrix matB, boolean workInPlace, TransposeType transB);

    SVDDecompose decomposeSVD(DMatrix matA, boolean workInPlace);

    EVDDecompose decomposeEVD(DMatrix matA, boolean workInPlace);

    DMatrix solve(DMatrix A, DMatrix B);

}
