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

import greycat.struct.DMatrix;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.SVDDecompose;
import greycat.struct.matrix.TransposeType;
import greycat.struct.matrix.VolatileDMatrix;

public class PCA {
    private DMatrix _data;
    private int _olddim;
    private double[] _min;
    private double[] _max;
    private double[] _sigma;
    private double[] _avg;
    private double[] _information;
    private int _processType;
    private double _percentToRetain;
    private int _bestDim;
    SVDDecompose _svdDecompose;
    public static double EPS = 1e-30;
    private static DMatrix _matrixV;

    public int get_bestDim() {
        return _bestDim;
    }

    public DMatrix get_data() {
        return _data;
    }

    private static double[] clone(double[] data) {
        double[] c = new double[data.length];
        System.arraycopy(data, 0, c, 0, data.length);
        return c;
    }

    public double[] getDimensionInfo() {
        return clone(_information);
    }

    public double[] get_min() {

        return clone(_min);
    }

    public double[] get_max() {
        return clone(_max);
    }

    public double[] get_avg() {
        return clone(_avg);
    }

    public double[] get_sigma() {
        return clone(_sigma);
    }



    public static int NOPROCESS = 0;
    public static int CENTER_ON_AVG = 1;
    public static int NORMALIZE = 2;


    public void normalizeData(DMatrix data) {
        double d = 1;
        for (int j = 0; j < data.columns(); j++) {
            if (_sigma[j] < EPS) {
                for (int i = 0; i < data.rows(); i++) {
                    data.set(i, j, 0);
                }
            } else {
                d = 1 / _sigma[j];
                for (int i = 0; i < data.rows(); i++) {
                    data.set(i, j, (data.get(i, j) - _avg[j]) * d);
                }
            }
        }
    }


    public double[] convertVector(double[] data) {
        DMatrix v = VolatileDMatrix.wrap(clone(data), 1, data.length);
        if (_processType == NORMALIZE) {
            normalizeData(v);
        }
        DMatrix res = MatrixOps.multiply(v, _matrixV);
        double[] result = new double[res.columns()];
        for (int i = 0; i < res.columns(); i++) {
            result[i] = res.get(0, i);
        }
        return result;
    }

    public DMatrix convertSpace(DMatrix initial) {
        if (_processType == NORMALIZE) {
            normalizeData(initial);
        }
        return MatrixOps.multiply(initial, _matrixV);
    }


    public void setDimension(int dim) {
        _matrixV = VolatileDMatrix.empty(_olddim, dim);
        DMatrix tempV = _svdDecompose.getVt();
        for (int i = 0; i < _olddim; i++) {
            for (int j = 0; j < dim; j++) {
                _matrixV.set(i, j, tempV.get(j, i));
            }
        }
    }

    public void inverseNormalizeData(DMatrix data) {
        for (int j = 0; j < data.columns(); j++) {
            if ((_sigma[j]) < EPS) {
                for (int i = 0; i < data.rows(); i++) {
                    data.set(i, j, _avg[j]);
                }
            } else {
                for (int i = 0; i < data.rows(); i++) {
                    data.set(i, j, data.get(i, j) * _sigma[j] + _avg[j]);
                }
            }
        }
    }

    public double[] inverseConvertVector(double[] data) {
        DMatrix v = VolatileDMatrix.wrap(clone(data), 1, data.length);
        DMatrix res = MatrixOps.multiplyTranspose(TransposeType.NOTRANSPOSE, v, TransposeType.TRANSPOSE, _matrixV);
        if (_processType == NORMALIZE) {
            inverseNormalizeData(res);
        }

        double[] result = new double[res.columns()];
        for (int i = 0; i < res.columns(); i++) {
            result[i] = res.get(0, i);
        }
        return result;
    }

    public DMatrix inverseConvertSpace(DMatrix initial) {
        DMatrix res = MatrixOps.multiplyTranspose(TransposeType.NOTRANSPOSE, initial, TransposeType.TRANSPOSE, _matrixV);
        if (_processType == NORMALIZE) {
            inverseNormalizeData(res);
        }
        return res;
    }


    public DMatrix getTransformationVector() {
        return _matrixV;
    }

    private void calculateMinMaxAvg() {
        this._min = new double[_data.columns()];
        this._max = new double[_data.columns()];
        this._avg = new double[_data.columns()];
        this._sigma = new double[_data.columns()];
        this._olddim = _data.columns();

        for (int j = 0; j < _data.columns(); j++) {
            _min[j] = _data.get(0, j);
            _max[j] = _min[j];
            _avg[j] = _min[j];
            _sigma[j] = _min[j] * _min[j];
        }

        double d;

        for (int i = 1; i < _data.rows(); i++) {
            for (int j = 0; j < _data.columns(); j++) {
                d = _data.get(i, j);
                _avg[j] += d;
                _sigma[j] += d * d;
                if (d < _min[j]) {
                    _min[j] = d;
                } else if (d > _max[j]) {
                    _max[j] = d;
                }
            }
        }

        for (int j = 0; j < _data.columns(); j++) {
            _avg[j] = _avg[j] / _data.rows();
            _sigma[j] = Math.sqrt(_sigma[j]);
        }

//        System.out.println();
//        for(int i=0;i<_data.columns();i++){
//            System.out.println(_min[i]+" , "+_max[i]+" , "+_avg[i]);
//        }
//        System.out.println();

    }


    private static DMatrix shiftColumn(DMatrix data, double[] shift, boolean workInPlace) {
        DMatrix temp = data;
        if (!workInPlace) {
            temp = VolatileDMatrix.cloneFrom(data);
        }
        for (int i = 0; i < temp.rows(); i++) {
            for (int j = 0; j < temp.columns(); j++) {
                temp.set(i, j, temp.get(i, j) - shift[j]);
            }
        }
        return temp;
    }

    private static DMatrix inverseShift(DMatrix data, double[] shift, boolean workInPlace) {
        DMatrix temp = data;
        if (!workInPlace) {
            temp = VolatileDMatrix.cloneFrom(data);
        }

        for (int i = 0; i < temp.rows(); i++) {
            for (int j = 0; j < temp.columns(); j++) {
                temp.set(i, j, temp.get(i, j) + shift[j]);
            }
        }
        return temp;
    }


    private int retainDynamic(double[] svector) {
        double d = 0;
        for (int i = 0; i < svector.length; i++) {
            d += svector[i] * svector[i];
        }


        double integrator = 0;
        double previoust = 1;
        double t = 1;
        double p;
        boolean tag = true;
        integrator = svector[0] * svector[0];

        int xi = 0;

        _information = new double[svector.length + 1];
        for (int i = 1; i < svector.length; i++) {
            _information[i] = integrator * 100 / d;
            previoust = t;
            t = svector[i] * svector[i] / (svector[i - 1] * svector[i - 1]);
//            System.out.println(i + " , " + svector[i] + " , " + t / previoust + " , " + _information[i] + "%");
            p = integrator * 100 / d;
            if (t / previoust < 0.85 && xi == 0 && i != 1 && p >= 85 && tag) {
                tag = false;
                _percentToRetain = p;
                xi = i;
            }
            integrator += svector[i] * svector[i];
        }
        _information[svector.length] = 100;
        if (xi == 0) {
            _percentToRetain = integrator * 100 / d;
            xi = svector.length;
        }
//        System.out.println(svector.length + " , " + svector[svector.length - 1] + " , " + t / previoust + " , " + integrator * 100 / d + "%");
//        System.out.println("");
        _bestDim = xi;
        return xi;
    }

    public double getPercentRetained(){
        return _percentToRetain;
    }


    public static int retain(double[] svector, double percent) {
        double d = 0;
        for (int i = 0; i < svector.length; i++) {
            d += svector[i] * svector[i];
        }
        d = d * percent;
        double t = 0;
        for (int i = 0; i < svector.length; i++) {
            t += svector[i] * svector[i];
            if (t > d) {
                return i + 1;
            }
        }
        return svector.length;
    }


    public PCA(DMatrix data, int processType) {
        this._data = data;
        this._processType = processType;
        calculateMinMaxAvg();

        if (processType == CENTER_ON_AVG) {
            shiftColumn(_data, _avg, true);
        } else if (processType == NORMALIZE) {
            normalizeData(_data);
        }


        //shiftColumn(_data,_avg,true);

//        if (normalize) {
//            normalizeData(_data);
//        }



        _svdDecompose = MatrixOps.defaultEngine().decomposeSVD(_data, false);

        double[] singularValues = _svdDecompose.getS();
        retainDynamic(singularValues);


//        System.out.println("Singular values");
//        for (int i = 0; i < singularValues.length; i++) {
//            System.out.println(singularValues[i]);
//        }
//        System.out.println("");
//        System.out.println("Need to retain: " + retainDynamic(singularValues) + " / " + data.columns() + " dimensions");
//        System.out.println("Energy retained: " + _percentToRetain + " %");
    }


}
