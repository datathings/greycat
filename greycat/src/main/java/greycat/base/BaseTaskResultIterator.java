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
package greycat.base;

import greycat.TaskResultIterator;
import greycat.utility.Tuple;

import java.util.concurrent.atomic.AtomicInteger;

class BaseTaskResultIterator<A> implements TaskResultIterator<A> {

    private final Object[] _backend;
    private final int _size;
    private final AtomicInteger _current;

    BaseTaskResultIterator(Object[] p_backend, int p_size) {
        _current = new AtomicInteger(0);
        if (p_backend != null) {
            this._backend = p_backend;
            _size = p_size;
        } else {
            _backend = new Object[0];
            _size = 0;
        }
    }

    @Override
    public final boolean hasNext() {
        return _current.intValue() < _size;
    }

    @Override
    public final A next() {
        final int cursor = _current.getAndIncrement();
        if (cursor < _size) {
            return (A) _backend[cursor];
        } else {
            return null;
        }
    }

    @Override
    public final Tuple<Integer, A> nextWithIndex() {
        final int cursor = _current.getAndIncrement();
        if (cursor < _size) {
            if (_backend[cursor] != null) {
                return new Tuple<Integer, A>(cursor, (A) _backend[cursor]);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}