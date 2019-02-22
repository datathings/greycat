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
import greycat.Node;
import greycat.struct.Relation;

public final class RelationProxy implements Relation {

    private final int _relationIndex;
    private Container _target;
    private Relation _relation;

    public RelationProxy(final int _relationIndex, final Container _target, final Relation _relation) {
        this._relationIndex = _relationIndex;
        this._target = _target;
        this._relation = _relation;
    }

    private void check() {
        if (_target != null) {
            _relation = (Relation) _target.rephase().getRawAt(_relationIndex);
            _target = null;
        }
    }

    @Override
    public final long[] all() {
        return _relation.all();
    }

    @Override
    public final int size() {
        return _relation.size();
    }

    @Override
    public final long get(int index) {
        return _relation.get(index);
    }

    @Override
    public final void set(int index, long value) {
        check();
        _relation.set(index, value);
    }

    @Override
    public final Relation add(long newValue) {
        check();
        return _relation.add(newValue);
    }

    @Override
    public final Relation addAll(long[] newValues) {
        check();
        return _relation.addAll(newValues);
    }

    @Override
    public final Relation addNode(Node node) {
        check();
        return _relation.addNode(node);
    }

    @Override
    public final Relation insert(int index, long newValue) {
        check();
        return _relation.insert(index, newValue);
    }

    @Override
    public final Relation remove(long oldValue) {
        check();
        return _relation.remove(oldValue);
    }

    @Override
    public final Relation delete(int oldValue) {
        check();
        return _relation.delete(oldValue);
    }

    @Override
    public final Relation clear() {
        check();
        return _relation.clear();
    }

    @Override
    public final String toString() {
        return _relation.toString();
    }

}
