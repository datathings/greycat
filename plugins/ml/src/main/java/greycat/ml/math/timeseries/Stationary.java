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
package greycat.ml.math.timeseries;

public class Stationary {


    public static double autoCorrelation(double[] series, int stepBack) {

        if (stepBack > series.length) {
            throw new RuntimeException("Step back can be maximum equal to array length!");
        }

        double xy = 0, x = 0, x2 = 0, y = 0, y2 = 0;

        for (int i = stepBack; i < series.length; i++) {
            xy += series[i] * series[i - stepBack];
            x += series[i];
            y += series[i - stepBack];
            x2 += series[i] * series[i];
            y2 += series[i - stepBack] * series[i - stepBack];
        }
        int n = series.length - stepBack;
        return (xy - (x * y) / n) / Math.sqrt((x2 - (x * x) / n) * (y2 - (y * y) / n));
    }


    public static double[] autoCorrelationFunction(double[] series, int elements) {
        double[] res = new double[elements];

        for (int i = 0; i < elements; i++) {
            res[i] = autoCorrelation(series, i);
        }
        return res;
    }


    public static double[] difference(double[] series) {
        if (series.length > 1) {
            double[] temp = new double[series.length - 1];

            for (int i = 1; i < series.length; i++) {
                temp[i - 1] = series[i] - series[i - 1];
            }

            return temp;
        }
        throw new RuntimeException("Series should be at least 2 elements!");
    }

    public static double[] differenceNOrder(double[] series, int n) {
        if (series.length > n) {
            double[] temp = series;
            for (int i = 0; i < n; i++) {
                temp = difference(temp);
            }
            return temp;
        }
        throw new RuntimeException("Series should be at least N elements!");
    }


}
