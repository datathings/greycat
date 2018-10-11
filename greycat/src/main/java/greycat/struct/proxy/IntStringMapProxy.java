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
import greycat.struct.IntStringMap;
import greycat.struct.IntStringMapCallBack;

public class IntStringMapProxy implements IntStringMap {
    private final int _index;
    private Container _target;
    private IntStringMap _elem;

    public IntStringMapProxy(final int _relationIndex, final Container _target, final IntStringMap _relation) {
        this._index = _relationIndex;
        this._target = _target;
        this._elem = _relation;
    }

    private void check() {
        if (_target != null) {
            _elem = (IntStringMap) _target.getRawAt(_index);
            _target = null;
        }
    }

    @Override
    public final int size() {
        return _elem.size();
    }

    @Override
    public final String get(final int key) {
        return _elem.get(key);
    }

    @Override
    public final void each(final IntStringMapCallBack callback) {
        _elem.each(callback);
    }

    @Override
    public final IntStringMap put(final int key, final String value) {
        check();
        return _elem.put(key, value);
    }

    @Override
    public final void remove(final int key) {
        check();
        _elem.remove(key);
    }
}
