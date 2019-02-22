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
package greycatMLTest.common;

import greycat.struct.DMatrix;
import greycat.struct.matrix.*;
import org.junit.Assert;
import org.junit.Test;

public class MultiplyTest {

    @Test
    public void MatrixMultJama() {
        InternalManualMult(new PlainMatrixEngine());
    }

    public DMatrix manualMultpily(DMatrix matA, DMatrix matB) {
        DMatrix matC = VolatileDMatrix.empty(matA.rows(), matB.columns());
        for (int i = 0; i < matA.rows(); i++) {
            for (int j = 0; j < matB.columns(); j++) {
                for (int k = 0; k < matA.columns(); k++) {
                    matC.add(i, j, matA.get(i, k) * matB.get(k, j));
                }
            }
        }

        return matC;
    }

    public void InternalManualMult(MatrixEngine engine) {

        //  long current = System.currentTimeMillis();

        //Test matrix mult
        int r = 30;
        int o = 30;
        int p = 30;
        JavaRandom javaRandom = new JavaRandom();
        javaRandom.setSeed(0);

        DMatrix matA = VolatileDMatrix.random(r, o, javaRandom, 0, 100);
        DMatrix matB = VolatileDMatrix.random(o, p, javaRandom, 0, 100);

        DMatrix result = engine.multiplyTransposeAlphaBeta(TransposeType.NOTRANSPOSE, 1, matA, TransposeType.NOTRANSPOSE, matB, 0, null);
        DMatrix matD = manualMultpily(matA, matB);

        double eps = 1e-7;

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < p; j++) {
                Assert.assertTrue(Math.abs(result.get(i, j) - matD.get(i, j)) < eps);
            }
        }

        //  System.out.println(System.currentTimeMillis() - current);

    }
}
