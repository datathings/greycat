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

import greycat.ml.math.PolynomialFit;
import org.junit.Assert;
import org.junit.Test;

public class PolynomialSolveTest {

    /**
     * @native ts
     */
    @Test
    public void polytest() {
        double eps = 1e-7;
        double[] coef = {5, -4, 1, 7};
        double[] t = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        double[] res = new double[t.length];

        for (int i = 0; i < t.length; i++) {
            res[i] = PolynomialFit.extrapolate(t[i], coef);
        }

        PolynomialFit pf = new PolynomialFit(coef.length - 1);

        pf.fit(t, res);
        double[] blasCoef = pf.getCoef();

        for (int i = 0; i < coef.length; i++) {
            Assert.assertTrue(Math.abs(blasCoef[i] - coef[i]) < eps);
        }
    }
}
