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
package greycat.ml;

import greycat.ml.common.NDimentionalArray;
import greycat.ml.common.matrix.VolatileDMatrix;
import org.junit.Assert;
import org.junit.Test;

/**
 * @ignore ts
 */
public class NDArrayTest {

    private static int tot = 0;

    private void reccursiveCalc(double[] min, double[] max, double[] steps, double[] seed) {
        int level;
        do {
            tot++;
            level = 0;
//            for (int i = 0; i < seed.length; i++) {
//                System.out.print(seed[i] + ",");
//            }
//            System.out.println();
            while (level < min.length && seed[level] >= max[level]) {
                level++;
            }
            if (level != min.length) {
                seed[level] += steps[level];
                System.arraycopy(min, 0, seed, 0, level);
            }
        }
        while (level != min.length);
    }

    @Test
    public void testND() {
        double[] min = {1.1, 5.5, 10};
        double[] max = {2.1, 10, 50};
        double[] precisions = {0.1, 0.5, 10};
        int[] dims = {11, 10, 5};
        // it should create a 11x10x5= 550 items
        NDimentionalArray array = new NDimentionalArray(min, max, precisions);
        Assert.assertTrue(array.getTotalDimension() == 550);
        int[] dimension = array.getDimensions();
        for (int i = 0; i < 3; i++) {
            Assert.assertTrue(dimension[i] == dims[i]);
        }


        int k = 0;


        for (int z = 0; z < dims[2]; z++) {
            for (int y = 0; y < dims[1]; y++) {
                for (int x = 0; x < dims[0]; x++) {
                    double[] vals = {x * 0.1 + min[0], y * 0.5 + min[1], z * 10.0 + min[2]};
                    int posF = array.convertFlat(vals);
                    //  System.out.println(k+ ": " + pos[0] + "," + pos[1] + "," + pos[2] + ", ->" + posF);
                    Assert.assertTrue(k == posF);

                    double[] temps = array.revertFlatIndex(k);
                    Assert.assertTrue(VolatileDMatrix.compare(vals, temps, 1e-6));

                    k++;

                }
            }
        }


        double[] seed = new double[min.length];
        System.arraycopy(min, 0, seed, 0, min.length);
        reccursiveCalc(min, max, precisions, seed);

        Assert.assertTrue(tot == array.getTotalDimension());
    }
}
