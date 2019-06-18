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
package greycat.ml.profiling;

import greycat.Type;
import greycat.ml.math.Gaussian1D;
import greycat.struct.EStruct;

public class Gaussian1DWrapper {
    //Getters and setters
    public final static String NAME = "Gaussian1DENode";
    private EStruct backend;


    public Gaussian1DWrapper(EStruct backend) {
        if (backend == null) {
            throw new RuntimeException("backend can't be null for Gaussian node!");
        }
        this.backend = backend;
    }

    public double getMin() {
        return (double) backend.get(Gaussian.MIN);
    }

    public double getMax() {
        return (double) backend.get(Gaussian.MAX);
    }

    public double getSum() {
        return (double) backend.get(Gaussian.SUM);
    }
    public double getSumSq() {
        return (double) backend.get(Gaussian.SUMSQ);
    }

    public long getTotal() {
        return (long) backend.get(Gaussian.TOTAL);
    }

    public double getAvg() {
        long total = getTotal();
        if (total == 0) {
            return 0;
        } else {
            double sum = (double) backend.get(Gaussian.SUM);
            return sum / total;
        }
    }

    public double getStd() {
        return Math.sqrt(getCov());
    }

    private double getCov() {
        long total = getTotal();
        if (total < 2) {
            return 0;
        } else {
            double sum = (double) backend.get(Gaussian.SUM);
            double sumsq = (double) backend.get(Gaussian.SUMSQ);
            return Gaussian1D.getCovariance(sum, sumsq, total);
        }
    }


    public double predict(double value) {
        long total = getTotal();
        if (total < 2) {
            return 0;
        }
        double avg = getAvg();
        double cov = getCov();
        return 1 / Math.sqrt(2 * Math.PI * cov) * Math.exp(-(value - avg) * (value - avg) / (2 * cov));
    }

    public void learn(double value) {
        //Create dirac
        long total = getTotal();
        double sum = (double) backend.get(Gaussian.SUM);
        double sumsq = (double) backend.get(Gaussian.SUMSQ);
        double min = (double) backend.get(Gaussian.MIN);
        double max = (double) backend.get(Gaussian.MAX);
        if (total == 0) {
            sum = value;
            min = value;
            max = value;
            sumsq = value * value;
        } else {
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
            sum += value;
            sumsq += value * value;
        }
        total++;
        backend.set(Gaussian.SUM, Type.DOUBLE, sum);
        backend.set(Gaussian.SUMSQ, Type.DOUBLE, sumsq);
        backend.set(Gaussian.MIN, Type.DOUBLE, min);
        backend.set(Gaussian.MAX, Type.DOUBLE, max);
        backend.set(Gaussian.TOTAL, Type.LONG, total);

    }


    public void print() {
        System.out.println("Gaussian1D Enode: " + backend.toString());
    }
}
