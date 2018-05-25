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
package greycat.ml.math;

public class Gaussian1D {

    public static double getCovariance(double sum, double sumSq, long total) {
        return (sumSq - (sum * sum) / total) / (total - 1);
    }

    public static double getDensity(double sum, double sumSq, int total, double feature) {
        if (total < 2) {
            return 0;
        }
        double avg = sum / total;
        double cov = getCovariance(sum, sumSq, total);
        return 1 / Math.sqrt(2 * Math.PI * cov) * Math.exp(-(feature - avg) * (feature - avg) / (2 * cov));
    }


    public static boolean isAccepted(double value, double avg, double std, double epsSTD, double numberOfAcceptedSTD) {
        return Math.abs(value - avg) / Math.max(std, epsSTD) < numberOfAcceptedSTD;
    }


    public static double getProbability(double value, double avg, double std) {
        double cov = std * std;
        return 1 / Math.sqrt(2 * Math.PI * cov) * Math.exp(-(value - avg) * (value - avg) / (2 * cov));
    }

    public static double getConfidence(double value, double avg, double std) {
        double cov = std * std;
        return Math.exp(-(value - avg) * (value - avg) / (2 * cov));
    }

//    public static double draw(double sum, double sumSq, int total){
//        double avg=sum/total;
//        double cov=getCovariance(sum,sumSq,total);
//        Random random=new Random();
//        return random.nextGaussian()*Math.sqrt(cov)+avg;
//    }

    public static double[] getDensityArray(double sum, double sumSq, int total, double[] feature) {
        if (total < 2) {
            return null;
        }
        double avg = sum / total;
        double cov = getCovariance(sum, sumSq, total);
        double exp = 1 / Math.sqrt(2 * Math.PI * cov);
        double[] proba = new double[feature.length];
        for (int i = 0; i < feature.length; i++) {
            proba[i] = exp * Math.exp(-(feature[i] - avg) * (feature[i] - avg) / (2 * cov));
        }
        return proba;
    }
}
