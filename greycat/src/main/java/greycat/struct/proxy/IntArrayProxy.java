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
package greycat.struct.proxy;

import greycat.Container;
import greycat.struct.IntArray;

public final class IntArrayProxy implements IntArray {

    private final int _index;
    private Container _target;
    private IntArray _elem;

    public IntArrayProxy(final int _relationIndex, final Container _target, final IntArray _relation) {
        this._index = _relationIndex;
        this._target = _target;
        this._elem = _relation;
    }

    private void check() {
        if (_target != null) {
            _elem = (IntArray) _target.rephase().getRawAt(_index);
            _target = null;
        }
    }

    @Override
    public final int get(final int index) {
        return _elem.get(index);
    }

    @Override
    public final int size() {
        return _elem.size();
    }


    @Override
    public final void set(final int index, final int value) {
        check();
        _elem.set(index, value);
    }

    @Override
    public final void init(final int size) {
        check();
        _elem.init(size);
    }

    @Override
    public final void clear() {
        check();
        _elem.clear();
    }

    @Override
    public final void initWith(final int[] values) {
        check();
        _elem.initWith(values);
    }

    @Override
    public final int[] extract() {
        return _elem.extract();
    }

    @Override
    public boolean removeElement(int value) {
        check();
        return _elem.removeElement(value);
    }

    @Override
    public boolean removeElementbyIndex(int index) {
        check();
        return _elem.removeElementbyIndex(index);
    }

    @Override
    public IntArray addElement(int value) {
        check();
        return _elem.addElement(value);
    }

    @Override
    public boolean insertElementAt(int position, int value) {
        check();
        return _elem.insertElementAt(position, value);
    }

    @Override
    public boolean replaceElementby(int element, int value) {
        check();
        return _elem.replaceElementby(element, value);
    }

    @Override
    public void addAll(int[] values) {
        check();
        _elem.addAll(values);
    }

}
