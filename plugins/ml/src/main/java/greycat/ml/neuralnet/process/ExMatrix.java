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
package greycat.ml.neuralnet.process;

import greycat.Type;
import greycat.struct.DMatrix;
import greycat.struct.EStruct;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.VolatileDMatrix;

public class ExMatrix implements DMatrix {

    private static String DW_KEY = "-dw";
    private static String STEPCACHE_KEY = "-sc";

    private DMatrix w;
    private DMatrix dw;
    private DMatrix stepCache;
    private EStruct backend;
    private String att;

    public ExMatrix(EStruct node, String attribute) {
        if (node != null) {
            w = (DMatrix) node.getOrCreate(attribute, Type.DMATRIX);
            dw = (DMatrix) node.getOrCreate(attribute + DW_KEY, Type.DMATRIX);
            this.backend = node;
            this.att = attribute;
        }
    }

    public static ExMatrix deepCopy(ExMatrix src){
        ExMatrix dm = new ExMatrix(null, null);
        if(src.rows()!=0){
            dm.w= VolatileDMatrix.empty(src.rows(), src.columns());
            dm.dw= VolatileDMatrix.empty(src.rows(), src.columns());
            MatrixOps.copy(src.w,dm.w);
            MatrixOps.copy(src.dw, dm.dw);
        }
        return dm;
    }

    public static ExMatrix empty(int rows, int column) {
        ExMatrix dm = new ExMatrix(null, null);
        dm.init(rows, column);
        return dm;
    }

    public static ExMatrix createFromW(DMatrix w) {
        ExMatrix res = new ExMatrix(null, null);
        res.w = w;
        res.dw = VolatileDMatrix.empty(w.rows(), w.columns());
        return res;
    }

    @Override
    public DMatrix init(int rows, int columns) {
        if (w == null) {
            w = VolatileDMatrix.empty(rows, columns);
            dw = VolatileDMatrix.empty(rows, columns);
        } else {
            w.init(rows, columns);
            dw.init(rows, columns);
        }
        return this;
    }

    @Override
    public DMatrix fill(double value) {
        w.fill(value);
        return this;
    }

    @Override
    public DMatrix fillWith(double[] values) {
        w.fillWith(values);
        return this;
    }

    @Override
    public int rows() {
        return w.rows();
    }

    @Override
    public int columns() {
        return w.columns();
    }

    @Override
    public int length() {
        return w.length();
    }

    @Override
    public double[] column(int i) {
        return w.column(i);
    }

    @Override
    public double get(int rowIndex, int columnIndex) {
        return w.get(rowIndex, columnIndex);
    }

    @Override
    public DMatrix set(int rowIndex, int columnIndex, double value) {
        w.set(rowIndex, columnIndex, value);
        return this;
    }

    @Override
    public DMatrix add(int rowIndex, int columnIndex, double value) {
        w.add(rowIndex, columnIndex, value);
        return this;
    }

    @Override
    public DMatrix appendColumn(double[] newColumn) {
        w.appendColumn(newColumn);
        dw.appendColumn(new double[newColumn.length]);
        return null;
    }

    @Override
    public double[] data() {
        return w.data();
    }

    @Override
    public int leadingDimension() {
        return w.leadingDimension();
    }

    @Override
    public double unsafeGet(int index) {
        return w.unsafeGet(index);
    }

    @Override
    public DMatrix unsafeSet(int index, double value) {
        w.unsafeSet(index, value);
        return this;
    }

    @Override
    public DMatrix unsafeAdd(int index, double value) {
        w.unsafeAdd(index, value);
        return this;
    }

    public DMatrix getW() {
        return w;
    }

    public DMatrix getDw() {
        return dw;
    }

    public boolean hasStepCache() {
        return stepCache != null;
    }

    public DMatrix getStepCache() {
        if (stepCache == null) {
            if (backend == null) {
                stepCache = VolatileDMatrix.empty(w.rows(), w.columns());
            } else {
                stepCache = (DMatrix) backend.getOrCreate(att + STEPCACHE_KEY, Type.DMATRIX);
            }
        }
        return stepCache;
    }


}
