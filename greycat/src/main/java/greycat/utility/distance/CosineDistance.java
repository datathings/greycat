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
package greycat.utility.distance;

public class CosineDistance implements Distance {

    private static CosineDistance static_instance = null;

    public static CosineDistance instance(){
        if(static_instance == null){
            static_instance = new CosineDistance();
        }
        return static_instance;
    }

    private CosineDistance() {
    }

    @Override
    public final double measure(double[] x, double[] y) {
        double sumTop = 0;
        double sumOne = 0;
        double sumTwo = 0;
        for (int i = 0; i < x.length; i++) {
            sumTop += x[i] * y[i];
            sumOne += x[i] * x[i];
            sumTwo += y[i] * y[i];
        }
        double cosSim = sumTop / (Math.sqrt(sumOne) * Math.sqrt(sumTwo));
        if (cosSim < 0) {
            cosSim = 0;//This should not happen, but does because of rounding errorsl
        }
        return 1 - cosSim;
    }

    @Override
    public final boolean compare(double x, double y) {
        return x < y;
    }

    @Override
    public final double getMinValue() {
        return 0;
    }

    @Override
    public final double getMaxValue() {
        return Double.MAX_VALUE;
    }
}
