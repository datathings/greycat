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
import greycat.struct.LongLongArrayMap;
import greycat.struct.LongLongArrayMapCallBack;

public final class LongLongArrayMapProxy implements LongLongArrayMap {

    private final int _index;
    private Container _target;
    private LongLongArrayMap _elem;

    public LongLongArrayMapProxy(final int _relationIndex, final Container _target, final LongLongArrayMap _relation) {
        this._index = _relationIndex;
        this._target = _target;
        this._elem = _relation;
    }

    private void check() {
        if (_target != null) {
            _elem = (LongLongArrayMap) _target.getRawAt(_index);
            _target = null;
        }
    }

    @Override
    public final int size() {
        return _elem.size();
    }

    @Override
    public final long[] get(final long key) {
        return _elem.get(key);
    }

    @Override
    public final void each(final LongLongArrayMapCallBack callback) {
        _elem.each(callback);
    }

    @Override
    public final boolean contains(final long key, final long value) {
        return _elem.contains(key, value);
    }


    @Override
    public final LongLongArrayMap put(final long key, final long value) {
        check();
        return _elem.put(key, value);
    }

    @Override
    public final void delete(long key, long value) {
        check();
        _elem.delete(key, value);
    }

}
