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
package greycat.struct.proxy;

import greycat.Container;
import greycat.struct.BoolArray;

public final class BoolArrayProxy implements BoolArray {
    private final int _index;
    private Container _target;
    private BoolArray _elem;

    public BoolArrayProxy(int relationIndex, final Container target, final BoolArray relation) {
        this._index = relationIndex;
        this._target = target;
        this._elem = relation;
    }

    @Override
    public final boolean get(int index) {
        return _elem.get(index);
    }

    @Override
    public final void set(int index, boolean value) {
        check();
    }

    private void check() {
        if(_target != null) {
            _elem = (BoolArray) _target.rephase().getRawAt(_index);
            _target = null;
        }
    }

    @Override
    public final void initWith(boolean[] values) {
        check();
        _elem.initWith(values);
    }

    @Override
    public final boolean[] extract() {
        return _elem.extract();
    }

    @Override
    public final boolean removeElement(boolean value) {
        check();
        return _elem.removeElement(value);
    }

    @Override
    public final boolean removeElementbyIndex(int index) {
        check();
        return _elem.removeElementbyIndex(index);
    }

    @Override
    public final void addElement(boolean value) {
        check();
        _elem.addElement(value);
    }

    @Override
    public final boolean insertElementAt(int position, boolean value) {
        check();
        return _elem.insertElementAt(position,value);
    }

    @Override
    public final boolean replaceElementby(boolean element, boolean value) {
        check();
        return _elem.replaceElementby(element,value);
    }

    @Override
    public final void addAll(boolean[] values) {
        check();
        _elem.addAll(values);
    }

    @Override
    public final int size() {
        return _elem.size();
    }

    @Override
    public final void clear() {
        check();
        _elem.clear();
    }

    @Override
    public final void init(int size) {
        check();
        _elem.init(size);
    }
}
