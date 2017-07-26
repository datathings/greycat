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
import greycat.struct.*;

public class Gaussian {
    public static final String NULL = "nullValues";
    public static final String REJECTED = "rejectedValues";
    public static final String ACCEPTED = "acceptedValues";

    public static final String MIN = "min";
    public static final String MAX = "max";
    public static final String AVG = "avg";
    public static final String COV = "cov";
    public static final String STD = "std";
    public static final String SUM = "sum";
    public static final String SUMSQ = "sumsq";
    public static final String TOTAL = "total";

    public static final String PRECISIONS = "precisions"; //Default covariance matrix for a dirac function
    public static final String VALUES = "values";

    public static final String HISTOGRAM_CENTERS = "centers";
    public static final String HISTOGRAM_VALUES = "values";

    public static final int STATUS_NULL = 0;
    public static final int STATUS_ACCEPTED = 1;
    public static final int STATUS_REJECTED = 2;


    private static EStruct getRoot(EStructArray hostnode) {
        EStruct host = hostnode.root();
        if (host == null) {
            host = hostnode.newEStruct();
            hostnode.setRoot(host);
        }
        return host;
    }

    public static int profile(EStructArray hostnode, Double value, Double boundMin, Double boundMax) {
        EStruct host = getRoot(hostnode);

        if (value == null) {
            host.set(NULL, Type.LONG, host.getWithDefault(NULL, 0l) + 1);
            return STATUS_NULL;
        }

        if (boundMin != null && value < boundMin || boundMax != null && value > boundMax) {
            host.set(REJECTED, Type.LONG, host.getWithDefault(REJECTED, 0l) + 1);
            return STATUS_REJECTED;
        }
        host.set(ACCEPTED, Type.LONG, host.getWithDefault(ACCEPTED, 0l) + 1);

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
        } else {
            host.set(COV, Type.DOUBLE, 0.0);
            host.set(STD, Type.DOUBLE, 0.0);
        }
        return STATUS_ACCEPTED;
    }

    public static void clearProfile(EStructArray hostnode) {
        EStruct host = getRoot(hostnode);

        host.remove(NULL);
        host.remove(REJECTED);
        host.remove(ACCEPTED);
        host.remove(TOTAL);
        host.remove(SUM);
        host.remove(SUMSQ);
        host.remove(AVG);
        host.remove(COV);
        host.remove(STD);
        host.remove(MIN);
        host.remove(MAX);
        host.remove(HISTOGRAM_CENTERS);
        host.remove(HISTOGRAM_VALUES);
    }

    public static void histogram(EStructArray hostnode, double min, double max, Double value, int histogramBins) {
        EStruct host = getRoot(hostnode);

        if (value == null || max <= min || value < min || value > max) {
            return;
        }
        if (histogramBins <= 0) {
            throw new RuntimeException("Histogram bins should be at least 1");
        }

        double stepsize = (max - min) / histogramBins;
        DoubleArray hist_center = (DoubleArray) host.getOrCreate(HISTOGRAM_CENTERS, Type.DOUBLE_ARRAY);
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


    public static void normaliseMatrix(DMatrix input, double[] avg, double[] std) {
        for (int i = 0; i < input.columns(); i++) {
            for (int j = 0; j < input.rows(); j++) {
                input.set(j, i, normaliseValue(input.get(j, i), avg[j], std[j]));
            }
        }
    }


    public static void inversenormaliseMatrix(DMatrix input, double[] avg, double[] std) {
        for (int i = 0; i < input.columns(); i++) {
            for (int j = 0; j < input.rows(); j++) {
                input.set(j, i, inverseNormaliseValue(input.get(j, i), avg[j], std[j]));
            }
        }
    }


    public static void normaliseMinMaxMatrix(DMatrix input, double[] avg, double[] std) {
        for (int i = 0; i < input.columns(); i++) {
            for (int j = 0; j < input.rows(); j++) {
                input.set(j, i, normaliseMinMaxValue(input.get(j, i), avg[j], std[j]));
            }
        }
    }


    public static void inversenormaliseMinMaxMatrix(DMatrix input, double[] avg, double[] std) {
        for (int i = 0; i < input.columns(); i++) {
            for (int j = 0; j < input.rows(); j++) {
                input.set(j, i, inverseNormaliseMinMaxValue(input.get(j, i), avg[j], std[j]));
            }
        }
    }


    public static double normaliseValue(final double input, final double avg, final double std) {
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

    public static double normaliseMinMaxValue(final double input, final double min, final double max) {

        double res = 0;

        if ((max - min) != 0) {
            res = (input - min) / (max - min);
        } else {
            res = 0;
        }

        return res;
    }

    public static double inverseNormaliseMinMaxValue(final double input, final double min, final double max) {
        return input * (max - min) + min;
    }


    public static double[] normalise(final double[] input, final double[] avg, final double[] std) {
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

    public static double[] normaliseMinMax(final double[] input, final double[] min, final double[] max) {

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

    public static double[] inverseNormaliseMinMax(final double[] input, final double[] min, final double[] max) {

        double[] res = new double[input.length];

        for (int i = 0; i < input.length; i++) {
            res[i] = input[i] * (max[i] - min[i]) + min[i];
        }
        return res;
    }

}
