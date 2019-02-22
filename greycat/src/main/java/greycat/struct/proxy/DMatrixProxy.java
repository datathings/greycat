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
package greycat.struct.proxy;

import greycat.Container;
import greycat.struct.DMatrix;

public final class DMatrixProxy implements DMatrix {

    private final int _index;
    private Container _target;
    private DMatrix _elem;

    public DMatrixProxy(final int _relationIndex, final Container _target, final DMatrix _relation) {
        this._index = _relationIndex;
        this._target = _target;
        this._elem = _relation;
    }

    private void check() {
        if (_target != null) {
            _elem = (DMatrix) _target.rephase().getRawAt(_index);
            _target = null;
        }
    }

    @Override
    public final int rows() {
        return _elem.rows();
    }

    @Override
    public final int columns() {
        return _elem.columns();
    }

    @Override
    public int length() {
        return _elem.length();
    }

    @Override
    public final double[] column(final int i) {
        return _elem.column(i);
    }

    @Override
    public final double get(final int rowIndex, final int columnIndex) {
        return _elem.get(rowIndex, columnIndex);
    }

    @Override
    public final double[] data() {
        return _elem.data();
    }

    @Override
    public final int leadingDimension() {
        return _elem.leadingDimension();
    }

    @Override
    public final double unsafeGet(int index) {
        return _elem.unsafeGet(index);
    }

    @Override
    public final DMatrix init(final int rows, final int columns) {
        check();
        return _elem.init(rows, columns);
    }

    @Override
    public final DMatrix fill(final double value) {
        check();
        return _elem.fill(value);
    }

    @Override
    public final DMatrix fillWith(final double[] values) {
        check();
        return _elem.fillWith(values);
    }

    @Override
    public final DMatrix set(final int rowIndex, final int columnIndex, final double value) {
        check();
        return _elem.set(rowIndex, columnIndex, value);
    }

    @Override
    public final DMatrix add(final int rowIndex, final int columnIndex, final double value) {
        check();
        return _elem.add(rowIndex, columnIndex, value);
    }

    @Override
    public final DMatrix appendColumn(final double[] newColumn) {
        check();
        return _elem.appendColumn(newColumn);
    }

    @Override
    public final DMatrix unsafeSet(final int index, final double value) {
        check();
        return _elem.unsafeSet(index, value);
    }

    @Override
    public DMatrix unsafeAdd(int index, double value) {
        check();
        return _elem.unsafeAdd(index, value);
    }

    @Override
    public final String toString() {
        return _elem.toString();
    }

}
