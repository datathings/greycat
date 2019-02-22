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
import greycat.Graph;
import greycat.struct.EStructArray;
import greycat.struct.EStruct;

public final class EStructArrayProxy implements EStructArray {

    private final int _index;
    private Container _target;
    private EStructArray _elem;

    public EStructArrayProxy(final int _relationIndex, final Container _target, final EStructArray _relation) {
        this._index = _relationIndex;
        this._target = _target;
        this._elem = _relation;
    }

    private void check() {
        if (_target != null) {
            _elem = (EStructArray) _target.rephase().getRawAt(_index);
            _target = null;
        }
    }

    @Override
    public final int size() {
        return _elem.size();
    }

    @Override
    public final void free() {
        _elem.free();
    }

    @Override
    public final Graph graph() {
        return _elem.graph();
    }

    @Override
    public final EStruct newEStruct() {
        check();
        return _elem.newEStruct();
    }

    @Override
    public final EStruct root() {
        final Container _host = _target;
        if (_host != null) {
            EStruct noProxy = _elem.root();
            if (noProxy == null) {
                return null;
            } else {
                return new EStructProxy(this, noProxy, noProxy.id());
            }
        } else {
            return _elem.root();
        }
    }

    @Override
    public final EStruct estruct(int index) {
        final Container _host = _target;
        if (_host != null) {
            return new EStructProxy(this, _elem.estruct(index), index);
        } else {
            return _elem.estruct(index);
        }
    }

    @Override
    public final EStructArray setRoot(EStruct eStruct) {
        check();
        return _elem.setRoot(eStruct);
    }

    @Override
    public final EStructArray drop(EStruct eStruct) {
        check();
        return _elem.drop(eStruct);
    }

    EStructArray rephase() {
        check();
        return _elem;
    }

    @Override
    public final String toString() {
        return _elem.toString();
    }


}
