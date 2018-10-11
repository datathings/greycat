/**
 * Copyright 2017-2018 The GreyCat Authors.  All rights reserved.
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
package greycat.ml.algorithm.profiling;

import greycat.ml.ProgressReporter;
import greycat.ml.common.NDimentionalArray;
import greycat.ml.common.matrix.operation.MultivariateNormalDistribution;


/**
 * @ignore ts
 */
public class ProbaDistribution2 {

    public MultivariateNormalDistribution[] distributions;
    public int total[];
    public int global;

    public ProbaDistribution2(int total[], MultivariateNormalDistribution[] distributions, int global) {
        this.total = total;
        this.distributions = distributions;
        this.global = global;
    }

    public NDimentionalArray calculate(double[] min, double[] max, double[] resolution, double[] err, ProgressReporter reporter) {
        if (reporter != null) {
            reporter.updateInformation("Number of distributions: " + distributions.length + " , values: " + global);
        }
        NDimentionalArray result = new NDimentionalArray(min, max, resolution);
        int percent;
        double weight;

        double[] sqrerr = new double[err.length];
        for (int i = 0; i < err.length; i++) {
            sqrerr[i] = Math.sqrt(err[i]);
        }

        for (int i = 0; i < distributions.length; i++) {
            weight = total[i] * 1.0 / global;
            calculateOneDist(distributions[i], weight, min, max, sqrerr, result);
            if (reporter != null) {
                percent = (i + 1) * 100 / distributions.length;
                reporter.updateProgress(percent);
                if (reporter.isCancelled()) {
                    return null;
                }
            }
        }
        result.normalize();
        return result;
    }

    private void calculateOneDist(MultivariateNormalDistribution distribution, double weight, double[] min, double[] max, double[] sqrerr, NDimentionalArray result) {
        double[] seed = new double[min.length];
        System.arraycopy(min, 0, seed, 0, min.length);
        reccursiveCalc(distribution, weight, min, max, sqrerr, seed, result);
    }

    private void reccursiveCalc(MultivariateNormalDistribution distribution, double weight, double[] min, double[] max, double[] steps, double[] seed, NDimentionalArray result) {
        int level;
        do {
            result.add(seed, distribution.density(seed, false) * weight);
            level = 0;
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


}
