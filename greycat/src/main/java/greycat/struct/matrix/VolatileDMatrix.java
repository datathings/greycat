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
package greycat.struct.matrix;

import greycat.Constants;
import greycat.struct.DMatrix;

//Most of the time we will be using column based matrix due to blas.
public class VolatileDMatrix implements DMatrix {

    private double[] _data;
    private int _nbRows;
    private int _nbColumns;
    private int _nbMaxColumn;

    private VolatileDMatrix(double[] backend, int p_nbRows, int p_nbColumns) {
        this._nbRows = p_nbRows;
        this._nbColumns = p_nbColumns;
        this._nbMaxColumn = p_nbColumns;
        if (backend != null) {
            this._data = backend;
        } else {
            if (_nbRows != 0 && _nbColumns != 0) {
                this._data = new double[_nbRows * _nbColumns];
            }
        }
    }

    @Override
    public DMatrix init(int rows, int columns) {
        if (_data != null && rows != _nbRows && columns != _nbMaxColumn) {
            throw new RuntimeException("Bad API usage !");
        }
        _nbMaxColumn = columns;
        _nbColumns = columns;
        _nbRows = rows;
        this._data = new double[_nbRows * _nbMaxColumn];
        return this;
    }

    @Override
    public double[] data() {
        //TODO copy array
        return _data;
    }

    @Override
    public int rows() {
        return _nbRows;
    }

    @Override
    public int columns() {
        return _nbMaxColumn;
    }

    @Override
    public int length() {
        return _nbRows * _nbMaxColumn;
    }

    @Override
    public double[] column(int index) {
        double[] result = new double[_nbRows];
        System.arraycopy(_data, (index * _nbRows), result, 0, _nbRows);
        return result;
    }

    @Override
    public double get(int rowIndex, int columnIndex) {
        return _data[rowIndex + columnIndex * _nbRows];
    }

    @Override
    public DMatrix set(int rowIndex, int columnIndex, double value) {
        _data[rowIndex + columnIndex * _nbRows] = value;
        return this;
    }

    @Override
    public DMatrix add(int rowIndex, int columnIndex, double value) {
        int raw_index = rowIndex + columnIndex * _nbRows;
        _data[raw_index] = value + _data[raw_index];
        return this;
    }

    @Override
    public DMatrix appendColumn(double[] newColumn) {
        if (_data == null || _data.length == 0) {
            _nbRows = newColumn.length;
            _nbColumns = Constants.MAP_INITIAL_CAPACITY;
            _nbMaxColumn = 0;
            _data = new double[_nbRows * _nbColumns];
        }
        if (_nbMaxColumn == _nbColumns) {
            _nbColumns = _nbColumns * 2;
            final int newLength = _nbColumns * _nbRows;
            double[] next_backend = new double[newLength];
            System.arraycopy(_data, 0, next_backend, 0, _data.length);
            _data = next_backend;
        }
        //just insert
        if (newColumn.length != _nbRows) {
            throw new RuntimeException("Vector has different row size than Matrix");
        }
        System.arraycopy(newColumn, 0, _data, _nbMaxColumn * _nbRows, newColumn.length);
        _nbMaxColumn = _nbMaxColumn + 1;
        return this;
    }

    @Override
    public DMatrix fill(double value) {
        for (int i = 0; i < _nbMaxColumn * _nbRows; i++) {
            this._data[i] = value;
        }
        return this;
    }

    @Override
    public DMatrix fillWith(double[] values) {
        _data = values;
        return this;
    }


    @Override
    public int leadingDimension() {
        return Math.max(_nbMaxColumn, _nbRows);
    }

    @Override
    public double unsafeGet(int index) {
        return this._data[index];
    }

    @Override
    public DMatrix unsafeSet(int index, double value) {
        this._data[index] = value;
        return this;
    }

    @Override
    public DMatrix unsafeAdd(int index, double value) {
        this._data[index] += value;
        return this;
    }

    public static boolean compare(double[] a, double[] b, double eps) {
        if (a == null || b == null) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i] - b[i]) > eps) {
                return false;
            }
        }
        return true;
    }

    public static boolean compareArray(double[][] a, double[][] b, double eps) {
        if (a == null || b == null) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!compare(a[i], b[i], eps)) {
                return false;
            }
        }
        return true;
    }

    //   @Override
    public double[] exportRowMatrix() {
        double[] res = new double[_data.length];
        int k = 0;
        for (int i = 0; i < _nbRows; i++) {
            for (int j = 0; j < _nbMaxColumn; j++) {
                res[k] = get(i, j);
                k++;
            }
        }
        return res;
    }

    //   @Override
    public VolatileDMatrix importRowMatrix(double[] rowdata, int rows, int columns) {
        VolatileDMatrix res = new VolatileDMatrix(null, rows, columns);

        int k = 0;
        for (int i = 0; i < _nbRows; i++) {
            for (int j = 0; j < _nbMaxColumn; j++) {
                res.set(i, j, rowdata[k]);
                k++;
            }
        }
        return res;
    }

    //   @Override
    // public void setData(double[] data) {
    // System.arraycopy(data, 0, this._data, 0, data.length);
    //  }

    //  @Override
    public double addAtIndex(int index, double value) {
        this._data[index] += value;
        return this._data[index];
    }

    //  @Override
    /*
    public VolatileDMatrix clone() {
        double[] newback = new double[_data.length];
        System.arraycopy(_data, 0, newback, 0, _data.length);
        VolatileDMatrix res = new VolatileDMatrix(newback, this._nbRows, this._nbColumns);
        return res;

    }*/


    public static DMatrix random(int rows, int columns, RandomInterface rand, double min, double max) {
        VolatileDMatrix res = new VolatileDMatrix(null, rows, columns);
        for (int i = 0; i < rows * columns; i++) {
            res.unsafeSet(i, rand.nextDouble() * (max - min) + min);
        }
        return res;
    }

    public static DMatrix randomGaussian(int rows, int columns, RandomInterface rand) {
        VolatileDMatrix res = new VolatileDMatrix(null, rows, columns);
        for (int i = 0; i < rows * columns; i++) {
            res.unsafeSet(i, rand.nextGaussian());
        }
        return res;
    }

    public static double compareMatrix(VolatileDMatrix matA, VolatileDMatrix matB) {
        double err = 0;

        for (int i = 0; i < matA.rows(); i++) {
            for (int j = 0; j < matA.columns(); j++) {
                if (err < Math.abs(matA.get(i, j) - matB.get(i, j))) {
                    err = Math.abs(matA.get(i, j) - matB.get(i, j));
                    // System.out.println(i+" , "+ j+" , "+ err);
                }

            }
        }
        return err;
    }


    public static DMatrix identity(int rows, int columns) {
        DMatrix res = new VolatileDMatrix(null, rows, columns);
        for (int i = 0; i < Math.min(rows, columns); i++) {
            res.set(i, i, 1.0);
        }
        return res;
    }

    public static VolatileDMatrix empty(int rows, int columns) {
        return new VolatileDMatrix(null, rows, columns);
    }

    public static DMatrix wrap(double[] data, int rows, int columns) {
        return new VolatileDMatrix(data, rows, columns);
    }

    public static DMatrix cloneFrom(DMatrix origin) {
        //TODO check according to .data() clone
        double[] prev = origin.data();
        double[] copy = new double[prev.length];
        System.arraycopy(prev, 0, copy, 0, copy.length);
        return new VolatileDMatrix(copy, origin.rows(), origin.columns());
    }

}
