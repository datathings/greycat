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

public class EuclideanDistance implements Distance {

    private static EuclideanDistance static_instance = null;

    public static EuclideanDistance instance(){
        if(static_instance == null){
            static_instance = new EuclideanDistance();
        }
        return static_instance;
    }

    private EuclideanDistance() {
    }

    @Override
    public final double measure(double[] x, double[] y) {
        double value = 0;
        for (int i = 0; i < x.length; i++) {
            value = value + (x[i] - y[i]) * (x[i] - y[i]);
        }
        return Math.sqrt(value);
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
