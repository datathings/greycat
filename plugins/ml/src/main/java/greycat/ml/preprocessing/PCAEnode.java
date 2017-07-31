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
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.SVDDecompose;
import greycat.struct.matrix.TransposeType;
import greycat.struct.matrix.VolatileDMatrix;

public class PCAEnode {
    public static String MATRIX_V_ORIGIN = "matrixVOrigin";
    public static String MATRIX_V_TRANS = "matrixVTrans";
    public static String SINGULAR_VALUES = "singularValues";

    public static String ORIGINAL_DIM = "originalDim";
    public static String BEST_DIM = "bestDim";
    public static String SELECTED_DIM = "selectedDim";
    public static String DIM_INFORMATION = "dimInformation";


    public static String PERCENT_AT_BEST_DIM = "percentAtBestDim";

    public static String THRESHOLD = "threshold";
    public static double THRESHOLD_DEF = 92.0;

    public static double EPS = 1e-30;

    EStruct _backend;

    public PCAEnode(EStruct backend) {
        this._backend = backend;
    }


    public PCAEnode setCorrelation(DMatrix correlation) {
        if (correlation == null || correlation.rows() != correlation.columns() || correlation.rows() == 0) {
            throw new RuntimeException("Correlation Matrix can't be empty");
        }

        DMatrix correlation_internal = (DMatrix) _backend.getOrCreate(Gaussian.COR, Type.DMATRIX);
        MatrixOps.copy(correlation, correlation_internal);

        SVDDecompose _svdDecompose = MatrixOps.defaultEngine().decomposeSVD(correlation_internal, false);
        DoubleArray sv = (DoubleArray) _backend.getOrCreate(SINGULAR_VALUES, Type.DOUBLE_ARRAY);
        sv.initWith(_svdDecompose.getS());

        DMatrix v_origin = (DMatrix) _backend.getOrCreate(MATRIX_V_ORIGIN, Type.DMATRIX);
        MatrixOps.copy(_svdDecompose.getVt(), v_origin);

        _backend.set(ORIGINAL_DIM, Type.INT, correlation.rows());

        retainDynamic(_svdDecompose.getS());

        return this;
    }


    private int retainDynamic(double[] svector) {

        double totalenergy = 0;

        for (double aSvector : svector) {
            totalenergy += aSvector;
        }



        DoubleArray _information = (DoubleArray) _backend.getOrCreate(DIM_INFORMATION, Type.DOUBLE_ARRAY);
        _information.init(svector.length + 1);

        double threshold = _backend.getWithDefault(THRESHOLD, THRESHOLD_DEF);


        double previoust = 1;
        double t = 1;
        int xi = 0;

        double integrator = svector[0];

        for (int i = 1; i < svector.length; i++) {

            previoust = t;
            t = svector[i] * svector[i] / (svector[i - 1] * svector[i - 1]);

            _information.set(i, (integrator * 100) / totalenergy);

            if (t / previoust < 0.85 && xi == 0 && i != 1 && _information.get(i) >= threshold) {
                xi = i;
            }

            integrator += svector[i];
        }
        _information.set(svector.length, 100);

        if (xi == 0) {
            for(int i=0;i<_information.size();i++){
                if(_information.get(i)>=threshold){
                    xi=i;
                    break;
                }
            }
        }


        _backend.set(BEST_DIM, Type.INT, xi);
        _backend.set(PERCENT_AT_BEST_DIM, Type.DOUBLE, _information.get(xi));


        return xi;
    }


    public double[] getDimInformation() {
        if (_backend.getDoubleArray(DIM_INFORMATION) != null) {
            return _backend.getDoubleArray(DIM_INFORMATION).extract();
        } else {
            return null;
        }
    }

    public int getBestDim() {
        return (int) _backend.get(BEST_DIM);
    }

    public double getPercentRetained() {
        return (double) _backend.get(PERCENT_AT_BEST_DIM);
    }


    public void print(String pcaName, boolean fullinfo){
        DoubleArray _information = (DoubleArray) _backend.getOrCreate(DIM_INFORMATION, Type.DOUBLE_ARRAY);
        System.out.println("");
        System.out.println("PCA "+pcaName);
        if(fullinfo) {
            for (int i = 0; i < _information.size(); i++) {
                System.out.println("Dim\t" + i + ": " + _information.get(i));
            }
        }
        System.out.println("Best dim: "+getBestDim()+" percent retained: "+getPercentRetained());
    }


    public void setDimension(int dim) {

        DMatrix v_origin = _backend.getDMatrix(MATRIX_V_ORIGIN);
        if (v_origin == null) {
            throw new RuntimeException("You should set Correlation Matrix first!");
        }
        int origin_dim = (int) _backend.get(ORIGINAL_DIM);
        if (dim <= 0 || dim > origin_dim) {
            throw new RuntimeException("Dim should be >0 and less than original dimension");
        }

        DMatrix v_trans = (DMatrix) _backend.getOrCreate(MATRIX_V_TRANS, Type.DMATRIX);
        v_trans.init(dim, origin_dim);

        _backend.set(SELECTED_DIM, Type.INT, dim);

        DoubleArray eigenvalues= _backend.getDoubleArray(SINGULAR_VALUES);

        for (int i = 0; i < dim; i++) {
            //double v=Math.sqrt(eigenvalues.get(i));
            for (int j = 0; j < origin_dim; j++) {
                v_trans.set(i, j, v_origin.get(i, j));
            }
        }
    }


    public double[] convertVector(double[] data) {
        DMatrix _matrixV = (DMatrix) _backend.get(MATRIX_V_TRANS);
        if (_matrixV == null) {
            throw new RuntimeException("Please set dimension first before calling PCA");
        }

        DMatrix v = VolatileDMatrix.wrap(data, data.length,1);

        DMatrix res = MatrixOps.multiply( _matrixV,v);
        return res.column(0);
    }


    //Column vectors based
    public DMatrix convertSpace(DMatrix initial) {
        DMatrix _matrixV = (DMatrix) _backend.get(MATRIX_V_TRANS);
        if (_matrixV == null) {
            throw new RuntimeException("Please set dimension first before calling PCA");
        }

        return MatrixOps.multiply(_matrixV,initial);
    }

    public double[] inverseConvertVector(double[] data) {
        DMatrix _matrixV = (DMatrix) _backend.get(MATRIX_V_TRANS);
        if (_matrixV == null) {
            throw new RuntimeException("Please set dimension first before calling PCA");
        }


        DMatrix v = VolatileDMatrix.wrap(data, data.length,1);
        DMatrix res = MatrixOps.multiplyTranspose(TransposeType.TRANSPOSE, _matrixV, TransposeType.NOTRANSPOSE, v);
        return res.column(0);
    }

    public DMatrix inverseConvertSpace(DMatrix initial) {
        DMatrix _matrixV = (DMatrix) _backend.get(MATRIX_V_TRANS);
        if (_matrixV == null) {
            throw new RuntimeException("Please set dimension first before calling PCA");
        }

        DMatrix res = MatrixOps.multiplyTranspose(TransposeType.TRANSPOSE, _matrixV, TransposeType.NOTRANSPOSE, initial);
        return res;
    }


    public DMatrix getVT() {
        return _backend.getDMatrix(MATRIX_V_TRANS);
    }
}
