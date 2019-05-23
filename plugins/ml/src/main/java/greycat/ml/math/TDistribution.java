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
package greycat.ml.math;

public class TDistribution {

    private int degreesOfFreedom;
    private double entropy;
    private double np;
    private double fac;

    public TDistribution(int degreesOfFreedom) {
        if (degreesOfFreedom < 1) {
            throw new RuntimeException("Invalid degreesOfFreedom = " + degreesOfFreedom);
        }

        this.degreesOfFreedom = degreesOfFreedom;

        entropy = 0.5 * (degreesOfFreedom + 1) * (Gamma.digamma((degreesOfFreedom + 1) / 2.0) - Gamma.digamma(degreesOfFreedom / 2.0)) + Math.log(Math.sqrt(degreesOfFreedom) * Beta.beta(degreesOfFreedom / 2.0, 0.5));
        np = 0.5 * (degreesOfFreedom + 1.0);
        fac = Gamma.lgamma(np) - Gamma.lgamma(0.5 * degreesOfFreedom);
    }

    public double p(double x) {
        return Math.exp(-np * Math.log(1.0 + x * x / degreesOfFreedom) + fac) / Math.sqrt(Math.PI * degreesOfFreedom);
    }


    public double cdf(double x) {
        double p = 0.5 * Beta.regularizedIncompleteBetaFunction(0.5 * degreesOfFreedom, 0.5, degreesOfFreedom / (degreesOfFreedom + x * x));

        if (x >= 0) {
            return 1.0 - p;
        } else {
            return p;
        }
    }

    public double twoTailsDist(double x) {
        return Beta.regularizedIncompleteBetaFunction(0.5 * degreesOfFreedom, 0.5, degreesOfFreedom / (degreesOfFreedom + x * x));
    }


    public double quantile(double p) {
        if (p < 0.0 || p > 1.0) {
            throw new RuntimeException("Invalid p: " + p);
        }

        double x = Beta.inverseRegularizedIncompleteBetaFunction(0.5 * degreesOfFreedom, 0.5, 2.0 * Math.min(p, 1.0 - p));
        x = Math.sqrt(degreesOfFreedom * (1.0 - x) / x);
        return p >= 0.5 ? x : -x;
    }

    public double cdf2tiled(double x) {
        if (x < 0) {
            throw new RuntimeException("Invalid x: " + x);
        }

        return 1.0 - Beta.regularizedIncompleteBetaFunction(0.5 * degreesOfFreedom, 0.5, degreesOfFreedom / (degreesOfFreedom + x * x));
    }

    public double quantile2tiled(double p) {
        if (p < 0.0 || p > 1.0) {
            throw new RuntimeException("Invalid p: " + p);
        }

        double x = Beta.inverseRegularizedIncompleteBetaFunction(0.5 * degreesOfFreedom, 0.5, 1.0 - p);
        return Math.sqrt(degreesOfFreedom * (1.0 - x) / x);
    }
}
