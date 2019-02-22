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
package greycatMLTest;

import greycat.ml.CRandom;
import org.junit.Test;

public class CRandomTest {

    /**
     * @native ts
     */
    @Test
    public void testrandom() {

        CRandom randomGenerator = new CRandom();
        randomGenerator.setSeed(123456789);

        long max = 1000;
        int result = 0;
        double result_d = 0;
        double result_g = 0;
        for (long t = 0; t < max; t++) {
            result = randomGenerator.nextInt();
        }
        for (long t = 0; t < max; t++) {
            result_d = randomGenerator.nextDouble();
        }
        for (long t = 0; t < max; t++) {
            result_g = randomGenerator.nextGaussian();
        }
//        System.out.println(result + " " + result_d + " " + result_g);

        //for 1000
        if (max == 1000) {
            assert (result == 70281542);
            assert (Math.abs(result_d - 0.069621) < 1e-5);
            assert (Math.abs(result_g + 1.139341) < 1e-5);
        } else if (max == 10000000) {
            assert (result == 28487886);
            assert (Math.abs(result_d - 0.034087) < 1e-5);
            assert (Math.abs(result_g - 0.582132) < 1e-5);
        }


//        assert (result == 70281542);

    }
}
