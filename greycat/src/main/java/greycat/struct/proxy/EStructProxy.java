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
import greycat.Index;
import greycat.Type;
import greycat.base.BaseNode;
import greycat.plugin.NodeStateCallback;
import greycat.struct.*;
import greycat.utility.HashHelper;

public final class EStructProxy implements EStruct {

    EStructArrayProxy _parent;
    EStruct _node;
    final int _index;

    public EStructProxy(EStructArrayProxy _parent, EStruct _node, int _index) {
        this._parent = _parent;
        this._node = _node;
        this._index = _index;
    }

    private void check() {
        if (_parent != null) {
            if (_index == -1) {
                _node = _parent.rephase().root();
            } else {
                _node = _parent.rephase().estruct(_index);
            }
            _parent = null;
        }
    }

    @Override
    public final Object get(final String name) {
        return this.getAt(_node.egraph().graph().resolver().stringToHash(name, false));
    }

    @Override
    public final Relation getRelation(String name) {
        return (Relation) get(name);
    }

    @Override
    public final Index getIndex(String name) {
        return (Index) get(name);
    }

    @Override
    public final DMatrix getDMatrix(String name) {
        return (DMatrix) get(name);
    }

    @Override
    public final LMatrix getLMatrix(String name) {
        return (LMatrix) get(name);
    }

    @Override
    public final EStructArray getEGraph(String name) {
        return (EStructArray) get(name);
    }

    @Override
    public final LongArray getLongArray(String name) {
        return (LongArray) get(name);
    }

    @Override
    public IntArray getIntArray(String name) {
        return (IntArray) get(name);
    }

    @Override
    public final DoubleArray getDoubleArray(String name) {
        return (DoubleArray) get(name);
    }

    @Override
    public final StringArray getStringArray(String name) {
        return (StringArray) get(name);
    }

    @Override
    public final StringIntMap getStringIntMap(String name) {
        return (StringIntMap) get(name);
    }

    @Override
    public final LongLongMap getLongLongMap(String name) {
        return (LongLongMap) get(name);
    }

    @Override
    public IntIntMap getIntIntMap(String name) {
        return (IntIntMap) get(name);
    }

    @Override
    public IntStringMap getIntStringMap(String name) {
        return (IntStringMap) get(name);
    }

    @Override
    public final LongLongArrayMap getLongLongArrayMap(String name) {
        return (LongLongArrayMap) get(name);
    }

    @Override
    public final <A> A getWithDefault(final String key, final A defaultValue) {
        return getAtWithDefault(_node.egraph().graph().resolver().stringToHash(key, false), defaultValue);
    }

    @Override
    public final <A> A getAtWithDefault(final int key, final A defaultValue) {
        Object elem = getAt(key);
        if (elem != null) {
            return (A) elem;
        } else {
            return defaultValue;
        }
    }

    @Override
    public final Object getOrCreate(final String name, final int type) {
        return this.getOrCreateAt(_node.egraph().graph().resolver().stringToHash(name, false), type);
    }

    @Override
    public final Object getOrCreateAt(final int index, final int type) {
        Object elem = getAt(index);
        if (elem != null) {
            return proxifyIfNeeded(elem, index);
        } else {
            check();
            return _node.getOrCreateAt(index, type);
        }
    }

    @Override
    public Object getOrCreateCustom(String name, String typeName) {
        return getOrCreateAt(HashHelper.hash(name), HashHelper.hash(typeName));
    }

    @Override
    public Object getOrCreateCustomAt(int index, String typeName) {
        return getOrCreateAt(index, HashHelper.hash(typeName));
    }

    private Object proxifyIfNeeded(Object elem, int index) {
        if (elem == null || _parent == null || !(elem instanceof BaseNode)) { //implement time sensitivity
            return elem;
        } else {
            int type = typeAt(index);
            switch (type) {
                case Type.LMATRIX:
                    return new LMatrixProxy(index, this, (LMatrix) elem);
                case Type.DMATRIX:
                    return new DMatrixProxy(index, this, (DMatrix) elem);
                case Type.ERELATION:
                    return new ERelationProxy(index, this, (ERelation) elem);
                case Type.RELATION:
                    return new RelationProxy(index, this, (Relation) elem);
                /*case Type.RELATION_INDEXED:
                    return new RelationIndexedProxy(index, this, (RelationIndexed) elem);*/
                /*case Type.KDTREE:
                    return new TreeProxy(index, this, (Tree) elem);
                case Type.NDTREE:
                    return new ProfileProxy(index, this, (Profile) elem);
                    */
                case Type.LONG_TO_LONG_MAP:
                    return new LongLongMapProxy(index, this, (LongLongMap) elem);
                case Type.LONG_TO_LONG_ARRAY_MAP:
                    return new LongLongArrayMapProxy(index, this, (LongLongArrayMap) elem);
                case Type.STRING_TO_INT_MAP:
                    return new StringIntMapProxy(index, this, (StringIntMap) elem);
                case Type.LONG_ARRAY:
                    return new LongArrayProxy(index, this, (LongArray) elem);
                case Type.INT_ARRAY:
                    return new IntArrayProxy(index, this, (IntArray) elem);
                case Type.DOUBLE_ARRAY:
                    return new DoubleArrayProxy(index, this, (DoubleArray) elem);
                case Type.STRING_ARRAY:
                    return new StringArrayProxy(index, this, (StringArray) elem);
                default:
                    return elem;
            }
        }
    }

    @Override
    public final Object getAt(final int index) {
        return proxifyIfNeeded(_node.getAt(index), index);
    }

    @Override
    public final Object getRawAt(final int index) {
        return _node.getAt(index);
    }

    @Override
    public final Object getTypedRawAt(final int index, final int type) {
        return _node.getTypedRawAt(index, type);
    }

    @Override
    public final int type(final String name) {
        return _node.type(name);
    }

    @Override
    public final int typeAt(final int index) {
        return _node.typeAt(index);
    }

    @Override
    public final EStructArray egraph() {
        if (_parent != null) {
            return _parent;
        } else {
            return _node.egraph();
        }
    }

    @Override
    public final void each(final NodeStateCallback callBack) {
        _node.each(callBack);
    }

    @Override
    public final Container set(final String name, final int type, final Object value) {
        check();
        return _node.set(name, type, value);
    }

    @Override
    public final Container setAt(final int index, final int type, final Object value) {
        check();
        return _node.setAt(index, type, value);
    }

    @Override
    public final Container remove(final String name) {
        check();
        return _node.remove(name);
    }

    @Override
    public final Container removeAt(final int index) {
        check();
        return _node.removeAt(index);
    }

    @Override
    public final Container rephase() {
        if (_index == -1) {
            return _parent.rephase().root();
        } else {
            return _parent.rephase().estruct(_index);
        }
    }

    @Override
    public final int[] attributeIndexes() {
        return _node.attributeIndexes();
    }

    @Override
    public final void drop() {
        check();
        _node.drop();
    }

    @Override
    public final EStruct clear() {
        check();
        return _node.clear();
    }

    @Override
    public int id() {
        return _node.id();
    }

    @Override
    public final String toString() {
        return _node.toString();
    }

}
