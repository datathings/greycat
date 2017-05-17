/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
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
package greycat.ml.profiling;

import greycat.Node;
import greycat.Type;
import greycat.ml.math.Gaussian1D;
import greycat.struct.DoubleArray;
import greycat.struct.Relation;

/**
 * Created by assaad on 20/02/2017.
 */
public class Gaussian {
    public static final String NULL = "profile_null";
    public static final String REJECT = "profile_reject";
    public static final String MIN = "profile_min";
    public static final String MAX = "profile_max";
    public static final String AVG = "profile_avg";
    public static final String COV = "profile_cov";
    public static final String STD = "profile_std";
    public static final String SUM = "profile_sum";
    public static final String SUMSQ = "profile_sumsq";
    public static final String TOTAL = "profile_total";
    public static final String PRECISIONS = "profile_precisions"; //Default covariance matrix for a dirac function
    public static final String VALUES = "profile_values";

    public static final String HISTOGRAM_CENTER = "histogram_center";
    public static final String HISTOGRAM_VALUES = "histogram_values";

    public static final int STATUS_NULL=0;
    public static final int STATUS_ACCEPTED=1;
    public static final int STATUS_REJECTED=2;


    public static int profile(Node host, Double value, Double boundMin, Double boundMax) {
        if (value == null) {
            host.set(NULL, Type.LONG, host.getWithDefault(NULL, 0l) + 1);
            return STATUS_NULL;
        }

        if (boundMin != null && value < boundMin || boundMax != null && value > boundMax) {
            host.set(REJECT, Type.LONG, host.getWithDefault(REJECT, 0l) + 1);
            return STATUS_REJECTED;
        }

        Double min = host.getWithDefault(MIN, null);
        Double max = host.getWithDefault(MAX, null);
        if (min == null || value < min) {
            host.set(MIN, Type.DOUBLE, value);
        }
        if (max == null || value > max) {
            host.set(MAX, Type.DOUBLE, value);
        }


        long total = host.getWithDefault(TOTAL, 0L) + 1;
        double sum = host.getWithDefault(SUM, 0.0) + value;
        double sumsq = host.getWithDefault(SUMSQ, 0.0) + value * value;


        host.set(TOTAL, Type.LONG, total);
        host.set(SUM, Type.DOUBLE, sum);
        host.set(SUMSQ, Type.DOUBLE, sumsq);
        host.set(AVG, Type.DOUBLE, sum / total);

        if (total > 1) {
            double cov = Gaussian1D.getCovariance(sum, sumsq, total);
            host.set(COV, Type.DOUBLE, cov);
            host.set(STD, Type.DOUBLE, Math.sqrt(cov));
        }
        else{
            host.set(COV, Type.DOUBLE, 0);
            host.set(STD, Type.DOUBLE, 0);
        }
        return STATUS_ACCEPTED;
    }

    public static void clearProfile(Node host) {
        host.set(NULL, Type.LONG, null);
        host.set(REJECT, Type.LONG, null);
        host.set(TOTAL, Type.LONG, null);
        host.set(SUM, Type.DOUBLE, null);
        host.set(SUMSQ, Type.DOUBLE, null);
        host.set(AVG, Type.DOUBLE, null);
        host.set(COV, Type.DOUBLE, null);
        host.set(STD, Type.DOUBLE, null);
        host.set(MIN, Type.DOUBLE, null);
        host.set(MAX, Type.DOUBLE, null);
        DoubleArray center =host.getDoubleArray(HISTOGRAM_CENTER);
        DoubleArray values= host.getDoubleArray(HISTOGRAM_VALUES);
        if(center!=null){
            center.clear();
        }
        if(values!=null){
            values.clear();
        }
    }

    public static void histogram(Node host, double min, double max, Double value, int histogramBins) {
        if (value==null ||max <= min || value < min || value > max) {
            return;
        }
        if(histogramBins<=0){
            throw new RuntimeException("Histogram bins should be at least 1");
        }

        double stepsize = (max - min) / histogramBins;
        DoubleArray hist_center = (DoubleArray) host.getOrCreate(HISTOGRAM_CENTER, Type.DOUBLE_ARRAY);
        DoubleArray hist_values = (DoubleArray) host.getOrCreate(HISTOGRAM_VALUES, Type.DOUBLE_ARRAY);

        if (hist_center.size() == 0) {
            hist_values.init(histogramBins);
            hist_center.init(histogramBins);
            for (int i = 0; i < histogramBins; i++) {
                hist_center.set(i, min + stepsize * (i + 0.5));
            }
        }

        int index = (int) ((value - min) / stepsize);
        if (index == histogramBins) {
            index--;
        }
        hist_values.set(index, hist_values.get(index) + 1);
    }





    public static double normalizeValue(final double input, final double avg, final double std) {
        double res;

        if (std != 0) {
            res = (input - avg) / std;
        } else {
            res = 0;
        }

        return res;
    }

    public static double inverseNormaliseValue(final double input, final double avg, final double std) {
        return input * std + avg;
    }

    public static double normalizeMinMaxValue(final double input, final double min, final double max) {

        double res = 0;

        if ((max - min) != 0) {
            res = (input - min) / (max - min);
        } else {
            res = 0;
        }

        return res;
    }

    public double inverseNormaliseMinMaxValue(final double input, final double min, final double max) {
        return input * (max - min) + min;
    }




    public static double[] normalize(final double[] input, final double[] avg, final double[] std) {
        double[] res = new double[input.length];

        for (int i = 0; i < input.length; i++) {
            if (std[i] != 0) {
                res[i] = (input[i] - avg[i]) / std[i];
            } else {
                res[i] = 0;
            }
        }

        return res;
    }

    public static double[] inverseNormalise(final double[] input, final double[] avg, final double[] std) {

        double[] res = new double[input.length];

        for (int i = 0; i < input.length; i++) {
            res[i] = input[i] * std[i] + avg[i];
        }
        return res;
    }

    public static double[] normalizeMinMax(final double[] input, final double [] min, final double[] max) {

        double[] res = new double[input.length];

        for (int i = 0; i < input.length; i++) {
            if ((max[i] - min[i]) != 0) {
                res[i] = (input[i] - min[i]) / (max[i] - min[i]);
            } else {
                res[i] = 0;
            }
        }

        return res;
    }

    public static double[] inverseNormaliseMinMax(final double[] input, final double [] min, final double[] max) {

        double[] res = new double[input.length];

        for (int i = 0; i < input.length; i++) {
            res[i] = input[i] * (max[i] - min[i]) + min[i];
        }
        return res;
    }

}
