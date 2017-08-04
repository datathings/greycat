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
package greycat.base;

import greycat.Container;
import greycat.Index;
import greycat.struct.*;
import greycat.utility.HashHelper;

public class BaseCustomType implements Container {

    public EStructArray _backend;

    public void init() {
    }

    public BaseCustomType(final EStructArray p_backend) {
        this._backend = p_backend;
    }

    public final EStructArray backend() {
        return this._backend;
    }

    @Override
    public final Object get(String name) {
        return getAt(HashHelper.hash(name));
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
    public final IntArray getIntArray(String name) {
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
    public final IntIntMap getIntIntMap(String name) {
        return (IntIntMap) get(name);
    }

    @Override
    public final IntStringMap getIntStringMap(String name) {
        return (IntStringMap) get(name);
    }

    @Override
    public final LongLongArrayMap getLongLongArrayMap(String name) {
        return (LongLongArrayMap) get(name);
    }

    @Override
    public Object getAt(int index) {
        return null;
    }

    @Override
    public Object getRawAt(int index) {
        return null;
    }

    @Override
    public Object getTypedRawAt(int index, int type) {
        return null;
    }

    @Override
    public final int type(String name) {
        return typeAt(HashHelper.hash(name));
    }

    @Override
    public int typeAt(int index) {
        return 0;
    }

    @Override
    public final Container set(String name, int type, Object value) {
        setAt(HashHelper.hash(name), type, value);
        return this;
    }

    @Override
    public Container setAt(int index, int type, Object value) {
        return null;
    }

    @Override
    public final Container remove(String name) {
        removeAt(HashHelper.hash(name));
        return this;
    }

    @Override
    public Container removeAt(int index) {
        return null;
    }

    @Override
    public final Object getOrCreate(String name, int type) {
        return getOrCreateAt(HashHelper.hash(name), type);
    }

    @Override
    public Object getOrCreateAt(int index, int type) {
        return null;
    }

    @Override
    public final Object getOrCreateCustom(String name, String typeName) {
        return getOrCreateAt(HashHelper.hash(name), HashHelper.hash(typeName));
    }

    @Override
    public final Object getOrCreateCustomAt(int index, String typeName) {
        return getOrCreateAt(index, HashHelper.hash(typeName));
    }

    @Override
    public final <A> A getWithDefault(String key, A defaultValue) {
        return getAtWithDefault(HashHelper.hash(key), defaultValue);
    }

    @Override
    public <A> A getAtWithDefault(int key, A defaultValue) {
        return null;
    }

    @Override
    public Container rephase() {
        //TODO
        return this;
    }

    @Override
    public int[] attributeIndexes() {
        return null;
    }

}
