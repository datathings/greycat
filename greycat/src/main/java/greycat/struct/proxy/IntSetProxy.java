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
import greycat.struct.IntSet;

public class IntSetProxy implements IntSet {

    private final int _index;
    private Container _target;
    private IntSet _elem;

    public IntSetProxy(final int _relationIndex, final Container _target, final IntSet _relation) {
        this._index = _relationIndex;
        this._target = _target;
        this._elem = _relation;
    }

    private void check() {
        if (_target != null) {
            _elem = (IntSet) _target.rephase().getRawAt(_index);
            _target = null;
        }
    }

    @Override
    public boolean put(int element) {
        check();
        return _elem.put(element);
    }

    @Override
    public boolean contains(int element) {
        return _elem.contains(element);
    }

    @Override
    public boolean remove(int element) {
        check();
        return _elem.remove(element);
    }

    @Override
    public int[] extract() {
        return _elem.extract();
    }

    @Override
    public int size() {
        return _elem.size();
    }
}
