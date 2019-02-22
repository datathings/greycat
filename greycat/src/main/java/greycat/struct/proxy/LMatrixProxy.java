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
import greycat.struct.LMatrix;

public final class LMatrixProxy implements LMatrix {

    private final int _index;
    private Container _target;
    private LMatrix _elem;

    public LMatrixProxy(final int _relationIndex, final Container _target, final LMatrix _relation) {
        this._index = _relationIndex;
        this._target = _target;
        this._elem = _relation;
    }

    private void check() {
        if (_target != null) {
            _elem = (LMatrix) _target.rephase().getRawAt(_index);
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
    public final long[] column(final int i) {
        return _elem.column(i);
    }

    @Override
    public final long get(final int rowIndex, final int columnIndex) {
        return _elem.get(rowIndex, columnIndex);
    }

    @Override
    public final long[] data() {
        return _elem.data();
    }

    @Override
    public final int leadingDimension() {
        return _elem.leadingDimension();
    }

    @Override
    public final long unsafeGet(int index) {
        return _elem.unsafeGet(index);
    }

    @Override
    public final LMatrix init(final int rows, final int columns) {
        check();
        return _elem.init(rows, columns);
    }

    @Override
    public final LMatrix fill(final long value) {
        check();
        return _elem.fill(value);
    }

    @Override
    public final LMatrix fillWith(final long[] values) {
        check();
        return _elem.fillWith(values);
    }

    @Override
    public final LMatrix fillWithRandom(final long min, final long max, final long seed) {
        check();
        return _elem.fillWithRandom(min, max, seed);
    }

    @Override
    public final LMatrix set(final int rowIndex, final int columnIndex, final long value) {
        check();
        return _elem.set(rowIndex, columnIndex, value);
    }

    @Override
    public final LMatrix add(final int rowIndex, final int columnIndex, final long value) {
        check();
        return _elem.add(rowIndex, columnIndex, value);
    }

    @Override
    public final LMatrix appendColumn(final long[] newColumn) {
        check();
        return _elem.appendColumn(newColumn);
    }

    @Override
    public final LMatrix unsafeSet(final int index, final long value) {
        check();
        return _elem.unsafeSet(index, value);
    }

    @Override
    public final String toString() {
        return _elem.toString();
    }


}
