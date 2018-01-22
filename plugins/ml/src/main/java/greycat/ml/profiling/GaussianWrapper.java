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

import greycat.Type;
import greycat.struct.DMatrix;
import greycat.struct.DoubleArray;
import greycat.struct.EStruct;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.VolatileDMatrix;

public class GaussianWrapper {
    //Getters and setters
    public final static String NAME = "GaussianENode";


    private EStruct backend;
    //can be used for normalization
    private double[] avg = null;
    private double[] std = null;
    private DMatrix cov = null;


    public GaussianWrapper(EStruct backend) {
        if (backend == null) {
            throw new RuntimeException("backend can't be null for Gaussian node!");
        }
        this.backend = backend;
    }

    public void setPrecisions(double[] precisions) {
        ((DoubleArray) backend.getOrCreate(Gaussian.PRECISIONS, Type.DOUBLE_ARRAY)).initWith(precisions);
    }


    public void learnWithOccurence(double[] values, int occ) {
        int features = values.length;
        long total = backend.getWithDefault(Gaussian.TOTAL, 0l);
        //Create dirac only save total and sum
        if (total == 0) {
            double[] sum = new double[features];
            if (occ == 1) {
                System.arraycopy(values, 0, sum, 0, features);
                total = 1;
                backend.set(Gaussian.TOTAL, Type.LONG, total);
                ((DoubleArray) backend.getOrCreate(Gaussian.SUM, Type.DOUBLE_ARRAY)).initWith(sum);
            } else {
                double[] min = new double[features];
                double[] max = new double[features];
                double[] sumsq = new double[features * (features + 1) / 2];
                System.arraycopy(values, 0, min, 0, features);
                System.arraycopy(values, 0, max, 0, features);
                int count = 0;
                for (int i = 0; i < features; i++) {
                    sum[i] = values[i] * occ;
                    for (int j = i; j < features; j++) {
                        sumsq[count] = values[i] * values[j] * occ;
                        count++;
                    }
                }
                total = occ;
                backend.set(Gaussian.TOTAL, Type.LONG, total);
                ((DoubleArray) backend.getOrCreate(Gaussian.SUM, Type.DOUBLE_ARRAY)).initWith(sum);
                ((DoubleArray) backend.getOrCreate(Gaussian.MIN, Type.DOUBLE_ARRAY)).initWith(min);
                ((DoubleArray) backend.getOrCreate(Gaussian.MAX, Type.DOUBLE_ARRAY)).initWith(max);
                ((DoubleArray) backend.getOrCreate(Gaussian.SUMSQ, Type.DOUBLE_ARRAY)).initWith(sumsq);
            }

            //set total, weight, sum, return
        } else {
            DoubleArray sum = (DoubleArray) backend.getOrCreate(Gaussian.SUM, Type.DOUBLE_ARRAY);
            DoubleArray min = (DoubleArray) backend.getOrCreate(Gaussian.MIN, Type.DOUBLE_ARRAY);
            DoubleArray max = (DoubleArray) backend.getOrCreate(Gaussian.MAX, Type.DOUBLE_ARRAY);
            DoubleArray sumsquares = (DoubleArray) backend.getOrCreate(Gaussian.SUMSQ, Type.DOUBLE_ARRAY);

            if (sum == null) {
                throw new RuntimeException("Total is " + total + " but sum is null");
            }
            if (features != sum.size()) {
                throw new RuntimeException("Input dimensions have changed!");
            }
            //Upgrade dirac to gaussian
            if (total == 1) {
                //Create getMin, getMax, sumsquares
                double[] sumex = sum.extract();
                min.initWith(sumex);
                max.initWith(sumex);
                sumsquares.init(features * (features + 1) / 2);
                int count = 0;
                for (int i = 0; i < features; i++) {
                    for (int j = i; j < features; j++) {
                        sumsquares.set(count, sumex[i] * sumex[j]);
                        count++;
                    }
                }
            }

            //Update the values
            for (int i = 0; i < features; i++) {
                if (values[i] < min.get(i)) {
                    min.set(i, values[i]);
                }

                if (values[i] > max.get(i)) {
                    max.set(i, values[i]);
                }
                sum.set(i, sum.get(i) + values[i] * occ);
            }

            int count = 0;
            for (int i = 0; i < features; i++) {
                for (int j = i; j < features; j++) {
                    sumsquares.set(count, sumsquares.get(count) + values[i] * values[j] * occ);
                    count++;
                }
            }
            total += occ;
            //Store everything
            backend.set(Gaussian.TOTAL, Type.LONG, total);
        }
        // set all cached avg, std, and cov arrays to null
        invalidate();

    }

    public void learn(double[] values) {
        learnWithOccurence(values, 1);
    }

    private void invalidate() {
        avg = null;
        std = null;
        cov = null;
    }


    private boolean initAvg() {
        if (avg != null) {
            return true;
        }

        long total = backend.getWithDefault(Gaussian.TOTAL, 0l);
        if (total != 0) {
            double[] sum = ((DoubleArray) backend.get(Gaussian.SUM)).extract();
            avg = new double[sum.length];
            for (int i = 0; i < sum.length; i++) {
                avg[i] = sum[i] / total;
            }
            return true;
        } else {
            return false;
        }
    }


    private boolean initStd() {
        if (std != null) {
            return true;
        }
        long total = backend.getWithDefault(Gaussian.TOTAL, 0l);
        if (total >= 2) {
            initAvg();
            int dim = avg.length;
            DoubleArray errArray = ((DoubleArray) backend.get(Gaussian.PRECISIONS));
            double[] err;
            if (errArray != null) {
                err = errArray.extract();
            } else {
                err = new double[avg.length];
            }

            double[] sumsq = getSumSq();
            std = new double[dim];

            double correction = total;
            correction = correction / (total - 1);

            int count = 0;
            for (int i = 0; i < dim; i++) {
                std[i] = Math.sqrt((sumsq[count] / total - avg[i] * avg[i]) * correction);
                count += (dim - i);
                if (std[i] < err[i]) {
                    std[i] = err[i];
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean initCov() {
        if (cov != null) {
            return true;
        }
        long total = backend.getWithDefault(Gaussian.TOTAL, 0l);
        if (total >= 2) {
            initAvg();
            int dim = avg.length;

            DoubleArray gp = (DoubleArray) backend.get(Gaussian.PRECISIONS);

            double[] err;
            if (gp != null) {
                err = gp.extract();
            } else {
                err = new double[avg.length];
            }


            for (int i = 0; i < err.length; i++) {
                err[i] = err[i] * err[i];
            }

            double[] sumsq = getSumSq();
            double[] covariances = new double[dim * dim];

            double correction = total;
            correction = correction / (total - 1);

            int count = 0;
            for (int i = 0; i < dim; i++) {
                for (int j = i; j < dim; j++) {
                    covariances[i * dim + j] = (sumsq[count] / total - avg[i] * avg[j]) * correction;
                    covariances[j * dim + i] = covariances[i * dim + j];
                    count++;
                }
                if (covariances[i * dim + i] < err[i]) {
                    covariances[i * dim + i] = err[i];
                }
            }
            cov = VolatileDMatrix.wrap(covariances, dim, dim);
            return true;
        } else {
            return false;
        }

    }


    public double[] getAvg() {
        if (!initAvg()) {
            return null;
        }
        double[] tempAvg = new double[avg.length];
        System.arraycopy(avg, 0, tempAvg, 0, avg.length);
        return tempAvg;
    }

    public double[] getSTD() {
        if (!initStd()) {
            return null;
        }
        double[] tempStd = new double[std.length];
        System.arraycopy(std, 0, tempStd, 0, std.length);
        return tempStd;
    }

    public DMatrix getCovariance() {
        if (!initCov()) {
            return null;
        }
        VolatileDMatrix covtemp = VolatileDMatrix.empty(cov.rows(), cov.columns());
        MatrixOps.copy(cov, covtemp);
        return covtemp;
    }

    public DMatrix getCorrelation() {
        if (!initCov()) {
            return null;
        }
        VolatileDMatrix covtemp = VolatileDMatrix.empty(cov.rows(), cov.columns());

        for (int i = 0; i < covtemp.rows(); i++) {
            for (int j = 0; j < covtemp.columns(); j++) {
                if (cov.get(i, i) != 0 && cov.get(j, j) != 0) {
                    covtemp.set(i, j, (cov.get(i, j) / Math.sqrt(cov.get(i, i) * cov.get(j, j))));
                }
            }
        }
        return covtemp;
    }

    public double[] getSum() {
        long total = backend.getWithDefault(Gaussian.TOTAL, 0l);
        if (total != 0) {
            return ((DoubleArray) backend.get(Gaussian.SUM)).extract();
        } else {
            return null;
        }
    }

    public double[] getSumSq() {
        long total = backend.getWithDefault(Gaussian.TOTAL, 0l);
        if (total == 0) {
            return null;
        }
        if (total == 1) {
            double[] sum = ((DoubleArray) backend.get(Gaussian.SUM)).extract();

            int features = sum.length;
            double[] sumsquares = new double[features * (features + 1) / 2];
            int count = 0;
            for (int i = 0; i < features; i++) {
                for (int j = i; j < features; j++) {
                    sumsquares[count] = sum[i] * sum[j];
                    count++;
                }
            }
            return sumsquares;
        } else {
            return ((DoubleArray) backend.get(Gaussian.SUMSQ)).extract();
        }
    }


    public double[] getMin() {
        long total = backend.getWithDefault(Gaussian.TOTAL, 0l);
        if (total == 0) {
            return null;
        }
        if (total == 1) {
            return ((DoubleArray) backend.get(Gaussian.SUM)).extract();
        } else {
            return ((DoubleArray) backend.get(Gaussian.MIN)).extract();
        }
    }

    public double[] getMax() {
        long total = backend.getWithDefault(Gaussian.TOTAL, 0l);
        if (total == 0) {
            return null;
        }
        if (total == 1) {
            return ((DoubleArray) backend.get(Gaussian.SUM)).extract();
        } else {
            return ((DoubleArray) backend.get(Gaussian.MAX)).extract();
        }
    }


    public long getTotal() {
        return backend.getWithDefault(Gaussian.TOTAL, 0l);
    }

    public int getDimensions() {
        long total = backend.getWithDefault(Gaussian.TOTAL, 0l);
        if (total != 0) {
            return ((DoubleArray) backend.get(Gaussian.SUM)).size();
        } else {
            return 0;
        }
    }


    public void print() {
        System.out.println("Gaussian Enode: " + backend.toString());
    }
}
