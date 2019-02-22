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

public class GaussianDistance implements Distance {

    double[] err;
    public GaussianDistance(double[] covariance){
        this.err=covariance;
    }

    @Override
    public double measure(double[] x, double[] y) {
        double max = 0;
        double temp;
        for (int i = 0; i < x.length; i++) {
            temp = (x[i] - y[i]) * (x[i] - y[i]) / err[i];
            if (temp > max) {
                max = temp;
            }
        }
        return Math.sqrt(max);
    }

    @Override
    public boolean compare(double x, double y) {
        return x < y;
    }


    @Override
    public double getMinValue() {
        return 0;
    }

    @Override
    public double getMaxValue() {
        return Double.MAX_VALUE;
    }
}
