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
package greycat.ml.math;

import greycat.struct.DMatrix;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.PInvSVD;
import greycat.struct.matrix.VolatileDMatrix;

public class MultivariateNormalDistribution {

    double[] min;
    double[] max;
    double[] means;
    double[] covDiag;
    DMatrix inv;
    DMatrix covariance;
    PInvSVD pinvsvd;
    int rank;
    double det;

    public MultivariateNormalDistribution(double[] means, DMatrix cov, boolean allowSingular) {
        this.means = means;
        if (cov != null) {
            this.covariance = cov;
            covDiag = new double[cov.rows()];
            for (int i = 0; i < covDiag.length; i++) {
                covDiag[i] = cov.get(i, i);
            }
            this.pinvsvd = new PInvSVD();
            this.pinvsvd.factor(covariance, false);
            this.inv = pinvsvd.getPInv();
            this.det = pinvsvd.getDeterminant();
            this.rank = pinvsvd.getRank();

            if (!allowSingular && this.rank < cov.rows()) {
                this.covariance = VolatileDMatrix.cloneFrom(cov);
                double[] temp = new double[covDiag.length];
                for (int i = 0; i < covDiag.length; i++) {
                    temp[i] = Math.sqrt(covDiag[i]);
                }

                for (int i = 0; i < covDiag.length; i++) {
                    for (int j = i + 1; j < covDiag.length; j++) {
                        double d = this.covariance.get(i, j) - 0.0001 * temp[i] * temp[j];
                        this.covariance.set(i, j, d);
                        this.covariance.set(j, i, d);
                    }
                }
                pinvsvd = new PInvSVD();
                pinvsvd.factor(this.covariance, false);
                inv = pinvsvd.getPInv();
                det = pinvsvd.getDeterminant();
                rank = pinvsvd.getRank();
            }

            int x = 0;

            //Solve complete covariance dependence
         /*   if(this.rank<means.length){
                this.covariance=cov.clone();
                double[] temp=new double[covDiag.length];
                for(int i=0;i<covDiag.length;i++){
                    temp[i]=Math.sqrt(covDiag[i]);
                }
                for(int i=0;i<covDiag.length;i++){
                    for(int j=i+1;j<covDiag.length;j++){
                        double d=this.covariance.get(i,j)-0.001*temp[i]*temp[j];
                        this.covariance.set(i,j,d);
                        this.covariance.set(j,i,d);
                    }
                }
                pinvsvd = new PInvSVD();
                pinvsvd.factor(this.covariance, false);
                inv = pinvsvd.getPInv();
                det = pinvsvd.getDeterminant();
                rank = pinvsvd.getRank();
            }*/


        }
    }

    public static DMatrix getCovariance(double[] sum, double[] sumsquares, int total) {
        if (total < 2) {
            return null;
        }
        int features = sum.length;
        double[] avg = new double[features];
        for (int i = 0; i < features; i++) {
            avg[i] = sum[i] / total;
        }
        double[] covariances = new double[features * features];
        double correction = total;
        correction = correction / (total - 1);
        int count = 0;
        for (int i = 0; i < features; i++) {
            for (int j = i; j < features; j++) {
                covariances[i * features + j] = (sumsquares[count] / total - avg[i] * avg[j]) * correction;
                covariances[j * features + i] = covariances[i * features + j];
                count++;
            }
        }
        return VolatileDMatrix.wrap(covariances, features, features);
    }

    //Sum is a n-vector sum of features
    //Sum squares is a n(n+1)/2 vector of sumsquares of features, in upper-triangle row shapes
    //Example:   for (int i = 0; i < features; i++) {    for (int j = i; j < features; j++) {  sumsquares[count] + = x[i] * x[j];  count++; } }
    //Total is the number of observations
    public static MultivariateNormalDistribution getDistribution(double[] sum, double[] sumsquares, int total, boolean allowSingular) {
        if (total < 2) {
            return null;
        }
        int features = sum.length;
        double[] avg = new double[features];
        for (int i = 0; i < features; i++) {
            avg[i] = sum[i] / total;
        }
        double[] covariances = new double[features * features];
        double correction = total;
        correction = correction / (total - 1);
        int count = 0;
        for (int i = 0; i < features; i++) {
            for (int j = i; j < features; j++) {
                covariances[i * features + j] = (sumsquares[count] / total - avg[i] * avg[j]) * correction;
                covariances[j * features + i] = covariances[i * features + j];
                count++;
            }
        }
        DMatrix cov = VolatileDMatrix.wrap(covariances, features, features);
        return new MultivariateNormalDistribution(avg, cov, allowSingular);
    }

    public double[] getMin() {
        return min;
    }

    public void setMin(double[] min) {
        this.min = min;
    }

    public double[] getMax() {
        return max;
    }

    public void setMax(double[] max) {
        this.max = max;
    }

    public double[] getAvg() {
        return means;
    }

    public double[] getCovDiag() {
        return covDiag;
    }

    public double density(double[] features, boolean normalizeOnAvg) {
        if (normalizeOnAvg) {
            return getExponentTerm(features);
        } else {
            return Math.pow(2 * Math.PI, -0.5 * rank) *
                    Math.pow(det, -0.5) * getExponentTerm(features);
        }
    }

    public double getExponentTerm(double[] features) {
        double[] f = new double[features.length];
        System.arraycopy(features, 0, f, 0, features.length);
        //double[] f = features.clone();
        for (int i = 0; i < features.length; i++) {
            f[i] = f[i] - means[i];
        }

        DMatrix ft = VolatileDMatrix.wrap(f, 1, f.length);
        DMatrix ftt = VolatileDMatrix.wrap(f, f.length, 1);

        DMatrix res = MatrixOps.multiply(ft, inv);
        DMatrix res2 = MatrixOps.multiply(res, ftt);

        double d = Math.exp(-0.5 * res2.get(0, 0));
        return d;
    }

    public MultivariateNormalDistribution clone(double[] avg) {
        MultivariateNormalDistribution res = new MultivariateNormalDistribution(avg, null, false);
        res.pinvsvd = this.pinvsvd;
        res.inv = this.inv;
        res.det = this.det;
        res.rank = this.rank;
        res.covDiag = this.covDiag;
        return res;
    }

    public double densityExponent(double[] features) {
        double[] f = new double[features.length];
        System.arraycopy(features, 0, f, 0, features.length);
        //double[] f = features.clone();
        for (int i = 0; i < features.length; i++) {
            f[i] = f[i] - means[i];
        }
        DMatrix ft = VolatileDMatrix.wrap(f, 1, f.length);
        DMatrix ftt = VolatileDMatrix.wrap(f, f.length, 1);
        DMatrix res = MatrixOps.multiply(ft, inv);
        DMatrix res2 = MatrixOps.multiply(res, ftt);
        return -0.5 * res2.get(0, 0);
    }
}
