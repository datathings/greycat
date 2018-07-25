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
package greycat.ml.preprocessing;

import greycat.Type;
import greycat.ml.profiling.Gaussian;
import greycat.struct.DMatrix;
import greycat.struct.DoubleArray;
import greycat.struct.EStruct;
import greycat.struct.matrix.EVDDecompose;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.TransposeType;
import greycat.struct.matrix.VolatileDMatrix;

public class PCAWrapper {

    public static String SPACE_ORIGIN = "spaceOrigin";
    public static String SPACE_CROPPED = "spaceCropped";

    public static String SINGULAR_VALUES = "singularValues";
    public static String EXPLAINED_VARIANCE = "explainedVariance";

    public static String SPACE_DENSITY = "spaceDensity";

    public static String ORIGINAL_DIM = "originalDim";
    public static String BEST_DIM = "bestDim";
    public static String SELECTED_DIM = "selectedDim";
    public static String DIM_INFORMATION = "dimInformation";
    public static String TOTAL = "total";
    public static String AVG = "avg";
    public static String WHITEN = "whiten";


    public static String PERCENT_AT_BEST_DIM = "percentAtBestDim";

    public static String THRESHOLD = "threshold";
    public static double THRESHOLD_DEF = 92.0;

    public static double EPS = 1e-30;

    private EStruct _backend;

    public PCAWrapper(EStruct backend) {
        this._backend = backend;
    }


    public PCAWrapper setCovariance(DMatrix covariance, double[] avg, long total, boolean whiten) {
        if (covariance == null || covariance.rows() != covariance.columns() || covariance.rows() == 0) {
            throw new RuntimeException("Correlation Matrix can't be empty");
        }

        DMatrix cov_internal = (DMatrix) _backend.getOrCreate(Gaussian.COV, Type.DMATRIX);
        MatrixOps.copy(covariance, cov_internal);

        EVDDecompose evd = MatrixOps.defaultEngine().decomposeEVD(covariance, false);

        DMatrix _spaceOrigin = (DMatrix) _backend.getOrCreate(SPACE_ORIGIN, Type.DMATRIX);
        MatrixOps.copy(evd.getEigenVectors(), _spaceOrigin);

        double[] evdValues = evd.getEigenValues();
        ((DoubleArray) _backend.getOrCreate(EXPLAINED_VARIANCE, Type.DOUBLE_ARRAY)).initWith(evdValues);

        DoubleArray singularValues = (DoubleArray) _backend.getOrCreate(SINGULAR_VALUES, Type.DOUBLE_ARRAY);
        int dim = covariance.rows();

        singularValues.init(dim);
        ((DoubleArray) _backend.getOrCreate(AVG, Type.DOUBLE_ARRAY)).initWith(avg);
        _backend.set(TOTAL, Type.LONG, total);
        _backend.set(WHITEN, Type.BOOL, whiten);
        _backend.set(ORIGINAL_DIM, Type.INT, dim);


        DoubleArray spaceInfo = (DoubleArray) _backend.getOrCreate(SPACE_DENSITY, Type.DOUBLE_ARRAY);

        spaceInfo.init(dim + 1);
        double power = total;
        spaceInfo.set(0, power);

        for (int i = 1; i <= dim; i++) {
            power = power / 2;
            spaceInfo.set(i, power);
        }


        DoubleArray explainedVariance = (DoubleArray) _backend.getOrCreate(EXPLAINED_VARIANCE, Type.DOUBLE_ARRAY);

        for (int i = 0; i < evdValues.length; i++) {
            int revI = evdValues.length - i - 1;
            explainedVariance.set(revI, evdValues[i]);
            singularValues.set(revI, Math.sqrt(explainedVariance.get(revI) * (total - 1)));

            for (int j = 0; j < evdValues.length; j++) {
                _spaceOrigin.set(j, revI, evd.getEigenVectors().get(j, i));
            }
        }

        retainDynamic();

        return this;
    }


    public void print(String pcaName, boolean fullinfo) {
        DoubleArray _information = (DoubleArray) _backend.getOrCreate(DIM_INFORMATION, Type.DOUBLE_ARRAY);
        System.out.println("");
        System.out.println("PCA " + pcaName);
        if (fullinfo) {
            for (int i = 0; i < _information.size(); i++) {
                System.out.println("Dim " + i + ": " + _information.get(i));
            }

            System.out.println("");
            System.out.println("Space density");
            DoubleArray density = (DoubleArray) _backend.get(SPACE_DENSITY);
            for (int i = 0; i < density.size(); i++) {
                System.out.println("Dim " + i + ": " + density.get(i));
            }
        }
        System.out.println("Best dim: " + getBestDim() + " percent retained: " + getPercentRetained());
    }

    private int retainDynamic() {

        double[] _explainedVariance = _backend.getDoubleArray(EXPLAINED_VARIANCE).extract();
        double totalenergy = 0;
        for (double aSvector : _explainedVariance) {
            totalenergy += aSvector;
        }

        double[] _explainedVarianceRatio = new double[_explainedVariance.length + 1];
        double threshold = _backend.getWithDefault(THRESHOLD, THRESHOLD_DEF);


        double previoust = 1;
        double t = 1;
        int xi = 0;

        double integrator = _explainedVariance[0];

        for (int i = 1; i < _explainedVariance.length; i++) {
            previoust = t;
            t = _explainedVariance[i] * _explainedVariance[i] / (_explainedVariance[i - 1] * _explainedVariance[i - 1]);
            _explainedVarianceRatio[i] = ((integrator * 100) / totalenergy);
            if (t / previoust < 0.85 && xi == 0 && i != 1 && _explainedVarianceRatio[i] >= threshold) {
                xi = i;
            }
            integrator += _explainedVariance[i];
        }
        _explainedVarianceRatio[_explainedVariance.length] = 100;

        if (xi == 0) {
            for (int i = 0; i < _explainedVarianceRatio.length; i++) {
                if (_explainedVarianceRatio[i] >= threshold) {
                    xi = i;
                    break;
                }
            }
        }

        _backend.set(BEST_DIM, Type.INT, xi);
        _backend.set(PERCENT_AT_BEST_DIM, Type.DOUBLE, _explainedVarianceRatio[xi]);
        ((DoubleArray) _backend.getOrCreate(DIM_INFORMATION, Type.DOUBLE_ARRAY)).initWith(_explainedVarianceRatio);
        return xi;
    }


    public void setDimension(int dim) {
        DMatrix _spaceOrigin = _backend.getDMatrix(SPACE_ORIGIN);
        if (_spaceOrigin == null) {
            throw new RuntimeException("You should fit data first!");
        }
        if (dim <= 0 || dim > _spaceOrigin.rows()) {
            throw new RuntimeException("Dim should be >0 and less than original dimension");
        }


        DMatrix res = MatrixOps.cropMatrix(_spaceOrigin, _spaceOrigin.rows(), dim);
        DMatrix _spaceCropped = (DMatrix) _backend.getOrCreate(SPACE_CROPPED, Type.DMATRIX);
        MatrixOps.copy(res, _spaceCropped);

        _backend.set(SELECTED_DIM, Type.INT, dim);
    }


    public double[] convertVector(double[] data, boolean workInPlace) {
        DMatrix _spaceCropped = _backend.getDMatrix(SPACE_CROPPED);

        if (_spaceCropped == null) {
            throw new RuntimeException("Please set dimension first before calling PCA");
        }

        DMatrix v;
        double[] dataclone;

        if (workInPlace) {
            dataclone = data;
        } else {
            dataclone = new double[data.length];
            System.arraycopy(data, 0, dataclone, 0, data.length);
        }

        double[] avg = _backend.getDoubleArray(AVG).extract();
        long total = (long) _backend.get(TOTAL);
        boolean whiten = (boolean) _backend.get(WHITEN);
        double[] singularValues = ((DoubleArray) _backend.get(SINGULAR_VALUES)).extract();

        for (int i = 0; i < dataclone.length; i++) {
            dataclone[i] = dataclone[i] - avg[i];
        }

        v = VolatileDMatrix.wrap(dataclone, dataclone.length, 1);

        DMatrix res = MatrixOps.multiplyTranspose(TransposeType.TRANSPOSE, _spaceCropped, TransposeType.NOTRANSPOSE, v);
        double[] resV = res.column(0);


        if (whiten) {
            double sqrt = Math.sqrt(total);
            for (int i = 0; i < resV.length; i++) {
                resV[i] = resV[i] * sqrt / singularValues[i];
            }
        }

        return resV;
    }

    public double[] inverseConvertVector(double[] data, boolean workInPlace) {
        DMatrix _spaceCropped = _backend.getDMatrix(SPACE_CROPPED);

        if (_spaceCropped == null) {
            throw new RuntimeException("Please set dimension first before calling PCA");
        }

        double[] dataclone;

        if (workInPlace) {
            dataclone = data;
        } else {
            dataclone = new double[data.length];
            System.arraycopy(data, 0, dataclone, 0, data.length);
        }

        double[] avg = _backend.getDoubleArray(AVG).extract();
        long total = (long) _backend.get(TOTAL);
        boolean whiten = (boolean) _backend.get(WHITEN);
        double[] singularValues = ((DoubleArray) _backend.get(SINGULAR_VALUES)).extract();

        if (whiten) {
            double sqrt = Math.sqrt(total);
            for (int i = 0; i < dataclone.length; i++) {
                dataclone[i] = dataclone[i] * singularValues[i] / sqrt;
            }
        }

        DMatrix v = VolatileDMatrix.wrap(dataclone, dataclone.length, 1);
        DMatrix res = MatrixOps.multiplyTranspose(TransposeType.NOTRANSPOSE, _spaceCropped, TransposeType.NOTRANSPOSE, v);
        double[] result = res.column(0);
        for (int i = 0; i < result.length; i++) {
            result[i] += avg[i];
        }

        return result;
    }


    //Column vectors based
    public DMatrix convertSpace(DMatrix initial, boolean workInPlace) {
        DMatrix _spaceCropped = _backend.getDMatrix(SPACE_CROPPED);

        if (_spaceCropped == null) {
            throw new RuntimeException("Please set dimension first before calling PCA");
        }


        DMatrix input;

        if (workInPlace) {
            input = initial;
        } else {
            input = MatrixOps.cloneMatrix(initial);
        }

        double[] avg = _backend.getDoubleArray(AVG).extract();
        long total = (long) _backend.get(TOTAL);
        boolean whiten = (boolean) _backend.get(WHITEN);
        double[] singularValues = ((DoubleArray) _backend.get(SINGULAR_VALUES)).extract();

        for (int i = 0; i < input.rows(); i++) {
            double av = avg[i];
            for (int j = 0; j < input.columns(); j++) {
                input.set(i, j, input.get(i, j) - av);
            }
        }


        DMatrix res = MatrixOps.multiplyTranspose(TransposeType.TRANSPOSE, _spaceCropped, TransposeType.NOTRANSPOSE, input);


        if (whiten) {
            double sqrt = Math.sqrt(total);
            for (int i = 0; i < res.columns(); i++) {
                for (int j = 0; j < res.rows(); j++) {
                    res.set(j, i, res.get(j, i) * sqrt / singularValues[j]);
                }
            }
        }
        return res;
    }


    public DMatrix inverseConvertSpace(DMatrix initial, boolean workInPlace) {

        DMatrix _spaceCropped = _backend.getDMatrix(SPACE_CROPPED);


        if (_spaceCropped == null) {
            throw new RuntimeException("Please set dimension first before calling PCA");
        }

        DMatrix input;

        if (workInPlace) {
            input = initial;
        } else {
            input = MatrixOps.cloneMatrix(initial);
        }

        double[] avg = _backend.getDoubleArray(AVG).extract();
        long total = (long) _backend.get(TOTAL);
        boolean whiten = (boolean) _backend.get(WHITEN);
        double[] singularValues = ((DoubleArray) _backend.get(SINGULAR_VALUES)).extract();

        if (whiten) {
            double sqrt = Math.sqrt(total);
            for (int i = 0; i < input.columns(); i++) {
                for (int j = 0; j < input.rows(); j++) {
                    input.set(j, i, input.get(j, i) * singularValues[j] / sqrt);
                }
            }
        }

        DMatrix res = MatrixOps.multiplyTranspose(TransposeType.NOTRANSPOSE, _spaceCropped, TransposeType.NOTRANSPOSE, input);

        for (int i = 0; i < res.rows(); i++) {
            double av = avg[i];
            for (int j = 0; j < res.columns(); j++) {
                res.set(i, j, res.get(i, j) + av);
            }
        }
        return res;
    }

    public double[] getComponent(int i) {
        return _backend.getDMatrix(SPACE_ORIGIN).column(i);
    }

    public double[] getDimInformation() {
        return ((DoubleArray) _backend.get(DIM_INFORMATION)).extract();
    }


    public void printInfo() {
        double[] singularValues = ((DoubleArray) _backend.get(SINGULAR_VALUES)).extract();
        double[] dimInfo = ((DoubleArray) _backend.get(DIM_INFORMATION)).extract();
        double[] explainedVariance = ((DoubleArray) _backend.get(EXPLAINED_VARIANCE)).extract();


        int originalDim = (int) _backend.get(ORIGINAL_DIM);
        int selectedDim = (int) _backend.get(SELECTED_DIM);
        int bestDim = (int) _backend.get(BEST_DIM);
        double percentAtBestDim = (double) _backend.get(PERCENT_AT_BEST_DIM);
        DMatrix spaceCropped = _backend.getDMatrix(SPACE_CROPPED);

        MatrixOps.printArray(singularValues, "PCA Singular values:");
        System.out.println("");

        MatrixOps.printArray(explainedVariance, "PCA variances:");
        System.out.println("");

        MatrixOps.printArray(dimInfo, "PCA Dimension info:");
        System.out.println("");

        DoubleArray density = (DoubleArray) _backend.get(SPACE_DENSITY);
        for (int i = 0; i < density.size(); i++) {
            System.out.println("Dim " + i + ": " + density.get(i));
        }
        System.out.println("");

        System.out.println("Original dimension:\t" + originalDim);
        System.out.println("Selected dimension:\t" + selectedDim);
        System.out.println("Best dimension:\t" + bestDim);
        System.out.println("Percentage at best dim:\t" + percentAtBestDim);
        System.out.println("");

        System.out.println("PCA main components:");
        for (int i = 0; i < selectedDim; i++) {
            System.out.print("vector " + i + ":\t");
            for (int j = 0; j < originalDim; j++) {
                System.out.print(spaceCropped.get(j, i) + "\t");
            }
            System.out.println("");
        }
        System.out.println("");
    }

    public int getBestDim() {
        return (int) _backend.get(BEST_DIM);
    }

    public double getPercentRetained() {
        return (double) _backend.get(PERCENT_AT_BEST_DIM);
    }
}
