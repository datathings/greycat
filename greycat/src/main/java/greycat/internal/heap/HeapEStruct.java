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
package greycat.internal.heap;

import greycat.*;
import greycat.base.BaseCustomType;
import greycat.internal.CoreConstants;
import greycat.plugin.NodeStateCallback;
import greycat.plugin.Resolver;
import greycat.plugin.TypeDeclaration;
import greycat.struct.*;
import greycat.utility.Base64;
import greycat.utility.HashHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class HeapEStruct implements EStruct, HeapContainer {

    private final HeapEStructArray _parent;
    int _id;

    HeapEStruct(final HeapEStructArray p_egraph, final int p_id, final HeapEStruct origin) {
        _parent = p_egraph;
        _id = p_id;
        if (origin != null) {
            _capacity = origin._capacity;
            _size = origin._size;
            //copy keys
            if (origin._k != null) {
                int[] cloned_k = new int[_capacity];
                System.arraycopy(origin._k, 0, cloned_k, 0, _capacity);
                _k = cloned_k;
            }
            //copy types
            if (origin._type != null) {
                int[] cloned_type = new int[_capacity];
                System.arraycopy(origin._type, 0, cloned_type, 0, _capacity);
                _type = cloned_type;
            }
            //copy next_hash if not empty
            if (origin._next_hash != null) {
                int[] cloned_hash = new int[_capacity * 3];
                System.arraycopy(origin._next_hash, 0, cloned_hash, 0, _capacity * 3);
                _next_hash = cloned_hash;
            }
            if (origin._v != null) {
                _v = new Object[_capacity];
                for (int i = 0; i < _size; i++) {
                    switch (origin._type[i]) {
                        case Type.LONG_TO_LONG_MAP:
                            if (origin._v[i] != null) {
                                _v[i] = ((HeapLongLongMap) origin._v[i]).cloneFor(this);
                            }
                            break;
                        case Type.INT_TO_INT_MAP:
                            if (origin._v[i] != null) {
                                _v[i] = ((HeapIntIntMap) origin._v[i]).cloneFor(this);
                            }
                            break;
                        case Type.INT_TO_STRING_MAP:
                            if (origin._v[i] != null) {
                                _v[i] = ((HeapIntStringMap) origin._v[i]).cloneFor(this);
                            }
                            break;
                        case Type.LONG_TO_LONG_ARRAY_MAP:
                            if (origin._v[i] != null) {
                                _v[i] = ((HeapLongLongArrayMap) origin._v[i]).cloneFor(this);
                            }
                            break;
                        case Type.STRING_TO_INT_MAP:
                            if (origin._v[i] != null) {
                                _v[i] = ((HeapStringIntMap) origin._v[i]).cloneFor(this);
                            }
                            break;
                        case Type.RELATION:
                            if (origin._v[i] != null) {
                                _v[i] = new HeapRelation(this, (HeapRelation) origin._v[i]);
                            }
                            break;
                        case Type.DMATRIX:
                            if (origin._v[i] != null) {
                                _v[i] = new HeapDMatrix(this, (HeapDMatrix) origin._v[i]);
                            }
                            break;
                        case Type.LMATRIX:
                            if (origin._v[i] != null) {
                                _v[i] = new HeapLMatrix(this, (HeapLMatrix) origin._v[i]);
                            }
                            break;
                        case Type.IMATRIX:
                            if (origin._v[i] != null) {
                                _v[i] = new HeapIMatrix(this, (HeapIMatrix) origin._v[i]);
                            }
                            break;
                        case Type.LONG_ARRAY:
                            if (origin._v[i] != null) {
                                _v[i] = ((HeapLongArray) origin._v[i]).cloneFor(this);
                            }
                            break;
                        case Type.DOUBLE_ARRAY:
                            if (origin._v[i] != null) {
                                _v[i] = ((HeapDoubleArray) origin._v[i]).cloneFor(this);
                            }
                            break;
                        case Type.INT_ARRAY:
                            if (origin._v[i] != null) {
                                _v[i] = ((HeapIntArray) origin._v[i]).cloneFor(this);
                            }
                            break;
                        case Type.STRING_ARRAY:
                            if (origin._v[i] != null) {
                                _v[i] = ((HeapStringArray) origin._v[i]).cloneFor(this);
                            }
                            break;
                        default:
                            _v[i] = origin._v[i];
                            break;
                    }
                }
            }
        } else {
            _capacity = 0;
            _size = 0;
        }
    }

    private int _capacity;
    private volatile int _size;
    private int[] _k;
    private Object[] _v;
    private int[] _next_hash;
    private int[] _type;

    @Override
    public final EStruct clear() {
        _capacity = 0;
        _size = 0;
        _k = null;
        _v = null;
        _next_hash = null;
        _type = null;
        return this;
    }

    @Override
    public int id() {
        return _id;
    }

    @Override
    public final void declareDirty() {
        if (_parent != null) {
            _parent.declareDirty();
        }
    }

    @Override
    public final Graph graph() {
        return _parent.graph();
    }

    final void rebase() {
        for (int i = 0; i < _size; i++) {
            switch (_type[i]) {
                case Type.ERELATION:
                    final HeapERelation previousERel = (HeapERelation) _v[i];
                    previousERel.rebase(_parent);
                    break;
                case Type.ESTRUCT:
                    final HeapEStruct previous = (HeapEStruct) _v[i];
                    _v[i] = _parent._nodes[previous._id];
                    break;
            }
        }
    }

    @SuppressWarnings("Duplicates")
    private void allocate(int newCapacity) {
        if (newCapacity <= _capacity) {
            return;
        }
        int[] ex_k = new int[newCapacity];
        if (_k != null) {
            System.arraycopy(_k, 0, ex_k, 0, _capacity);
        }
        _k = ex_k;
        Object[] ex_v = new Object[newCapacity];
        if (_v != null) {
            System.arraycopy(_v, 0, ex_v, 0, _capacity);
        }
        _v = ex_v;
        int[] ex_type = new int[newCapacity];
        if (_type != null) {
            System.arraycopy(_type, 0, ex_type, 0, _capacity);
        }
        _type = ex_type;
        _capacity = newCapacity;
        _next_hash = new int[_capacity * 3];
        Arrays.fill(_next_hash, 0, _capacity * 3, -1);
        final int double_capacity = _capacity * 2;
        for (int i = 0; i < _size; i++) {
            int keyHash = _k[i] % double_capacity;
            if (keyHash < 0) {
                keyHash = keyHash * -1;
            }
            _next_hash[i] = _next_hash[_capacity + keyHash];
            _next_hash[_capacity + keyHash] = i;
        }
    }

    private int internal_find(final int p_key) {
        if (_size == 0) {
            return -1;
        }
        int hashIndex = p_key % (_capacity * 2);
        if (hashIndex < 0) {
            hashIndex = hashIndex * -1;
        }
        int m = _next_hash[_capacity + hashIndex];
        while (m >= 0) {
            if (p_key == _k[m]) {
                return m;
            } else {
                m = _next_hash[m];
            }
        }
        return -1;
    }

    private Object internal_get(final int p_key) {
        //empty chunk, we return immediately
        if (_size == 0) {
            return null;
        }
        int found = internal_find(p_key);
        if (found != -1) {
            return _v[found];
        }
        return null;
    }

    private int internal_type(final int p_key) {
        //empty chunk, we return immediately
        if (_size == 0) {
            return -1;
        }
        int found = internal_find(p_key);
        if (found != -1) {
            return _type[found];
        }
        return -1;
    }

    private void internal_set(final int p_key, final int p_type, final Object p_unsafe_elem, boolean replaceIfPresent, boolean initial) {
        Object param_elem = null;
        //check the param type
        if (p_unsafe_elem != null) {
            try {
                switch (p_type) {
                    case Type.BOOL:
                        param_elem = (boolean) p_unsafe_elem;
                        break;
                    case Type.DOUBLE:
                        param_elem = (double) p_unsafe_elem;
                        break;
                    case Type.LONG:
                        if (p_unsafe_elem instanceof Integer) {
                            int preCasting = (Integer) p_unsafe_elem;
                            param_elem = (long) preCasting;
                        } else {
                            param_elem = (long) p_unsafe_elem;
                        }
                        break;
                    case Type.INT:
                        param_elem = (int) p_unsafe_elem;
                        break;
                    case Type.STRING:
                        param_elem = (String) p_unsafe_elem;
                        break;
                    case Type.DMATRIX:
                        param_elem = (DMatrix) p_unsafe_elem;
                        break;
                    case Type.LMATRIX:
                        param_elem = (LMatrix) p_unsafe_elem;
                        break;
                    case Type.IMATRIX:
                        param_elem = (IMatrix) p_unsafe_elem;
                        break;
                    case Type.RELATION:
                        param_elem = (Relation) p_unsafe_elem;
                        break;
                    case Type.ERELATION:
                        param_elem = (ERelation) p_unsafe_elem;
                        break;
                    case Type.ESTRUCT:
                        param_elem = (EStruct) p_unsafe_elem;
                        break;
                    case Type.DOUBLE_ARRAY:
                        param_elem = (DoubleArray) p_unsafe_elem;
                        break;
                    case Type.LONG_ARRAY:
                        param_elem = (LongArray) p_unsafe_elem;
                        break;
                    case Type.INT_ARRAY:
                        param_elem = (IntArray) p_unsafe_elem;
                        break;
                    case Type.STRING_ARRAY:
                        param_elem = (StringArray) p_unsafe_elem;
                        break;
                    case Type.STRING_TO_INT_MAP:
                        param_elem = (StringIntMap) p_unsafe_elem;
                        break;
                    case Type.LONG_TO_LONG_MAP:
                        param_elem = (LongLongMap) p_unsafe_elem;
                        break;
                    case Type.INT_TO_INT_MAP:
                        param_elem = (IntIntMap) p_unsafe_elem;
                        break;
                    case Type.INT_TO_STRING_MAP:
                        param_elem = (IntStringMap) p_unsafe_elem;
                        break;
                    case Type.LONG_TO_LONG_ARRAY_MAP:
                        param_elem = (LongLongArrayMap) p_unsafe_elem;
                        break;
                    default:
                        param_elem = (EStructArray) p_unsafe_elem;
                }
            } catch (Exception e) {
                throw new RuntimeException("GreyCat usage error, set method called with type " + Type.typeName(p_type) + " while param object is " + p_unsafe_elem);
            }
        }
        //first value
        if (_k == null) {
            //we do not allocate for empty element
            /*
            if (param_elem == null) {//TODO
                return;
            }*/
            _capacity = Constants.MAP_INITIAL_CAPACITY;
            _k = new int[_capacity];
            _v = new Object[_capacity];
            _type = new int[_capacity];
            _next_hash = new int[_capacity * 3];
            Arrays.fill(_next_hash, 0, _capacity * 3, -1);
            _k[0] = p_key;
            _v[0] = param_elem;
            _type[0] = p_type;
            _size = 1;
            int hashIndex = p_key % (_capacity * 2);
            if (hashIndex < 0) {
                hashIndex = hashIndex * -1;
            }
            _next_hash[_capacity + hashIndex] = 0;
            if (!initial) {
                declareDirty();
            }
            return;
        }
        int entry = -1;
        // int p_entry = -1;
        int hashIndex = p_key % (_capacity * 2);
        if (hashIndex < 0) {
            hashIndex = hashIndex * -1;
        }
        int m = _next_hash[_capacity + hashIndex];
        while (m != -1) {
            if (_k[m] == p_key) {
                entry = m;
                break;
            }
            //p_entry = m;
            m = _next_hash[m];
        }
        //case already present
        if (entry != -1) {
            if (replaceIfPresent || (p_type != _type[entry])) {
                /*
                if (param_elem == null) {
                    //unHash previous
                    if (p_entry != -1) {
                        _next_hash[p_entry] = _next_hash[entry];
                    } else {
                        _next_hash[_capacity + hashIndex] = -1;
                    }
                    int indexVictim = _size - 1;
                    //just pop the last value
                    if (entry == indexVictim) {
                        _k[entry] = -1;
                        _v[entry] = null;
                        _type[entry] = -1;
                    } else {
                        //we need to reHash the new last element at our place
                        _k[entry] = _k[indexVictim];
                        _v[entry] = _v[indexVictim];
                        _type[entry] = _type[indexVictim];
                        _next_hash[entry] = _next_hash[indexVictim];
                        int victimHash = _k[entry] % (_capacity * 2);
                        if (victimHash < 0) {
                            victimHash = victimHash * -1;
                        }
                        m = _next_hash[_capacity + victimHash];
                        if (m == indexVictim) {
                            //the victim was the head of hashing list
                            _next_hash[_capacity + victimHash] = entry;
                        } else {
                            //the victim is in the next, reChain it
                            while (m != -1) {
                                if (_next_hash[m] == indexVictim) {
                                    _next_hash[m] = entry;
                                    break;
                                }
                                m = _next_hash[m];
                            }
                        }
                        _k[indexVictim] = -1;
                        _v[indexVictim] = null;
                        _type[indexVictim] = -1;
                    }
                    _size--;
                } else {*/
                _v[entry] = param_elem;
                if (_type[entry] != p_type) {
                    _type[entry] = p_type;
                }
                //}
            }
            if (!initial) {
                declareDirty();
            }
            return;
        }
        if (_size < _capacity) {
            _k[_size] = p_key;
            _v[_size] = param_elem;
            _type[_size] = p_type;
            _next_hash[_size] = _next_hash[_capacity + hashIndex];
            _next_hash[_capacity + hashIndex] = _size;
            _size++;
            if (!initial) {
                declareDirty();
            }
            return;
        }
        //extend capacity
        int newCapacity = _capacity * 2;
        int[] ex_k = new int[newCapacity];
        System.arraycopy(_k, 0, ex_k, 0, _capacity);
        _k = ex_k;
        Object[] ex_v = new Object[newCapacity];
        System.arraycopy(_v, 0, ex_v, 0, _capacity);
        _v = ex_v;
        int[] ex_type = new int[newCapacity];
        System.arraycopy(_type, 0, ex_type, 0, _capacity);
        _type = ex_type;
        _capacity = newCapacity;
        //insert the next
        _k[_size] = p_key;
        _v[_size] = param_elem;
        _type[_size] = p_type;
        _size++;
        //reHash
        _next_hash = new int[_capacity * 3];
        Arrays.fill(_next_hash, 0, _capacity * 3, -1);
        final int hashCapacity = _capacity * 2;
        for (int i = 0; i < _size; i++) {
            int keyHash = _k[i] % hashCapacity;
            if (keyHash < 0) {
                keyHash = keyHash * -1;
            }
            _next_hash[i] = _next_hash[_capacity + keyHash];
            _next_hash[_capacity + keyHash] = i;
        }
        if (!initial) {
            declareDirty();
        }
    }

    @Override
    public EStruct set(String name, int type, Object value) {
        internal_set(_parent.graph().resolver().stringToHash(name, true), type, value, true, false);
        return this;
    }

    @Override
    public EStruct setAt(int key, int type, Object value) {
        internal_set(key, type, value, true, false);
        return this;
    }

    @Override
    public Container remove(String name) {
        internal_set(_parent.graph().resolver().stringToHash(name, true), Type.INT, null, true, false);
        return this;
    }

    @Override
    public Container removeAt(int index) {
        internal_set(index, Type.INT, null, true, false);
        return this;
    }

    @Override
    public Object get(String name) {
        return internal_get(_parent.graph().resolver().stringToHash(name, false));
    }

    @Override
    public Object getAt(int key) {
        return internal_get(key);
    }

    @Override
    public Object getRawAt(int key) {
        return getAt(key);
    }

    @Override
    public final Object getTypedRawAt(final int index, final int type) {
        if (_size == 0) {
            return null;
        }
        int found = internal_find(index);
        if (found != -1 && _type[found] == type) {
            return _v[found];
        }
        return null;
    }

    @Override
    public int type(String name) {
        return internal_type(_parent.graph().resolver().stringToHash(name, false));
    }

    @Override
    public int typeAt(int key) {
        return internal_type(key);
    }

    @Override
    public <A> A getWithDefault(String key, A defaultValue) {
        final Object result = get(key);
        if (result == null) {
            return defaultValue;
        } else {
            return (A) result;
        }
    }

    @Override
    public <A> A getAtWithDefault(int key, A defaultValue) {
        final Object result = internal_get(key);
        if (result == null) {
            return defaultValue;
        } else {
            return (A) result;
        }
    }

    @Override
    public Container rephase() {
        //EStruct proxy will take care
        return this;
    }

    @Override
    public final int[] attributeIndexes() {
        final int[] result = new int[_size];
        System.arraycopy(_k, 0, result, 0, _size);
        return result;
    }

    @Override
    public void drop() {
        _parent.drop(this);
    }

    @Override
    public EStructArray egraph() {
        return _parent;
    }

    @Override
    public Object getOrCreate(String key, int type) {
        Object previous = get(key);
        if (previous != null) {
            return previous;
        } else {
            return getOrCreateAt(_parent.graph().resolver().stringToHash(key, true), type);
        }
    }

    @Override
    public final Object getOrCreateAt(final int key, final int p_type) {
        final int found = internal_find(key);
        if (found != -1) {
            if (_type[found] == p_type) {
                Object foundValue = _v[found];
                if (foundValue != null) {
                    if (Type.isCustom(p_type)) {
                        final TypeDeclaration typeDeclaration = graph().typeRegistry().declarationByHash(p_type);
                        if (typeDeclaration == null) {
                            return _v[found];
                        } else {
                            return typeDeclaration.factory().wrap((EStructArray) _v[found]);
                        }
                    } else {
                        return _v[found];
                    }
                }
            }
        }
        Object toSet = null;
        switch (p_type) {
            case Type.LONG_ARRAY:
                toSet = new HeapLongArray(this);
                break;
            case Type.DOUBLE_ARRAY:
                toSet = new HeapDoubleArray(this);
                break;
            case Type.INT_ARRAY:
                toSet = new HeapIntArray(this);
                break;
            case Type.STRING_ARRAY:
                toSet = new HeapStringArray(this);
                break;
            case Type.ERELATION:
                toSet = new HeapERelation(this, null);
                break;
            case Type.RELATION:
                toSet = new HeapRelation(this, null);
                break;
            case Type.DMATRIX:
                toSet = new HeapDMatrix(this, null);
                break;
            case Type.LMATRIX:
                toSet = new HeapLMatrix(this, null);
                break;
            case Type.IMATRIX:
                toSet = new HeapIMatrix(this, null);
                break;
            case Type.STRING_TO_INT_MAP:
                toSet = new HeapStringIntMap(this);
                break;
            case Type.INT_TO_INT_MAP:
                toSet = new HeapIntIntMap(this);
                break;
            case Type.INT_TO_STRING_MAP:
                toSet = new HeapIntStringMap(this);
                break;
            case Type.LONG_TO_LONG_MAP:
                toSet = new HeapLongLongMap(this);
                break;
            case Type.LONG_TO_LONG_ARRAY_MAP:
                toSet = new HeapLongLongArrayMap(this);
                break;
        }
        if (toSet == null) {
            final Object toGet;
            final Graph g = graph();
            final EStructArray tempND = new HeapEStructArray(this, null, g);
            toSet = tempND;
            final TypeDeclaration typeDeclaration = g.typeRegistry().declarationByHash(p_type);
            if (typeDeclaration == null) {
                toGet = toSet;
            } else {
                toGet = typeDeclaration.factory().wrap(tempND);
                ((BaseCustomType) toGet).init();
            }
            internal_set(key, p_type, toSet, true, false);
            return toGet;
        } else {
            internal_set(key, p_type, toSet, true, false);
            return toSet;
        }
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
    public final String toString() {
        final StringBuilder builder = new StringBuilder();
        final boolean[] isFirst = {true};
        boolean isFirstField = true;
        builder.append("{");
        for (int i = 0; i < _size; i++) {
            final Object elem = _v[i];
            final Resolver resolver = _parent.graph().resolver();
            final int attributeKey = _k[i];
            final int elemType = _type[i];
            if (elem != null) {
                if (isFirstField) {
                    isFirstField = false;
                } else {
                    builder.append(",");
                }
                String resolveName = resolver.hashToString(attributeKey);
                if (resolveName == null) {
                    resolveName = attributeKey + "";
                }
                switch (elemType) {
                    case Type.BOOL: {
                        builder.append("\"");
                        builder.append(resolveName);
                        builder.append("\":");
                        if ((Boolean) elem) {
                            builder.append("1");
                        } else {
                            builder.append("0");
                        }
                        break;
                    }
                    case Type.STRING: {
                        builder.append("\"");
                        builder.append(resolveName);
                        builder.append("\":");
                        builder.append("\"");
                        builder.append(elem);
                        builder.append("\"");
                        break;
                    }
                    case Type.LONG: {
                        builder.append("\"");
                        builder.append(resolveName);
                        builder.append("\":");
                        builder.append(elem);
                        break;
                    }
                    case Type.INT: {
                        builder.append("\"");
                        builder.append(resolveName);
                        builder.append("\":");
                        builder.append(elem);
                        break;
                    }
                    case Type.DOUBLE: {
                        if (!Constants.isNaN((double) elem)) {
                            builder.append("\"");
                            builder.append(resolveName);
                            builder.append("\":");
                            builder.append(elem);
                        }
                        break;
                    }
                    case Type.DOUBLE_ARRAY: {
                        builder.append("\"");
                        builder.append(resolveName);
                        builder.append("\":");
                        builder.append("[");
                        DoubleArray castedArr = (DoubleArray) elem;
                        for (int j = 0; j < castedArr.size(); j++) {
                            if (j != 0) {
                                builder.append(",");
                            }
                            builder.append(castedArr.get(j));
                        }
                        builder.append("]");
                        break;
                    }
                    case Type.RELATION:
                        builder.append("\"");
                        builder.append(resolveName);
                        builder.append("\":");
                        builder.append("[");
                        Relation castedRelArr = (Relation) elem;
                        for (int j = 0; j < castedRelArr.size(); j++) {
                            if (j != 0) {
                                builder.append(",");
                            }
                            builder.append(castedRelArr.get(j));
                        }
                        builder.append("]");
                        break;
                    case Type.ERELATION:
                        builder.append("\"");
                        builder.append(resolveName);
                        builder.append("\":");
                        builder.append("[");
                        ERelation castedERelArr = (ERelation) elem;
                        for (int j = 0; j < castedERelArr.size(); j++) {
                            if (j != 0) {
                                builder.append(",");
                            }
                            builder.append(((HeapEStruct) castedERelArr.node(j))._id);
                        }
                        builder.append("]");
                        break;
                    case Type.LONG_ARRAY: {
                        builder.append("\"");
                        builder.append(resolveName);
                        builder.append("\":");
                        builder.append("[");
                        LongArray castedArr2 = (LongArray) elem;
                        for (int j = 0; j < castedArr2.size(); j++) {
                            if (j != 0) {
                                builder.append(",");
                            }
                            builder.append(castedArr2.get(j));
                        }
                        builder.append("]");
                        break;
                    }
                    case Type.INT_ARRAY: {
                        builder.append("\"");
                        builder.append(resolveName);
                        builder.append("\":");
                        builder.append("[");
                        IntArray castedArr3 = (IntArray) elem;
                        for (int j = 0; j < castedArr3.size(); j++) {
                            if (j != 0) {
                                builder.append(",");
                            }
                            builder.append(castedArr3.get(j));
                        }
                        builder.append("]");
                        break;
                    }
                    case Type.LONG_TO_LONG_MAP: {
                        builder.append("\"");
                        builder.append(resolveName);
                        builder.append("\":");
                        builder.append("{");
                        LongLongMap castedMapL2L = (LongLongMap) elem;
                        isFirst[0] = true;
                        castedMapL2L.each(new LongLongMapCallBack() {
                            @Override
                            public void on(long key, long value) {
                                if (!isFirst[0]) {
                                    builder.append(",");
                                } else {
                                    isFirst[0] = false;
                                }
                                builder.append("\"");
                                builder.append(key);
                                builder.append("\":");
                                builder.append(value);
                            }
                        });
                        builder.append("}");
                        break;
                    }
                    case Type.INT_TO_INT_MAP: {
                        builder.append("\"");
                        builder.append(resolveName);
                        builder.append("\":");
                        builder.append("{");
                        IntIntMap castedMapI2I = (IntIntMap) elem;
                        isFirst[0] = true;
                        castedMapI2I.each(new IntIntMapCallBack() {
                            @Override
                            public void on(int key, int value) {
                                if (!isFirst[0]) {
                                    builder.append(",");
                                } else {
                                    isFirst[0] = false;
                                }
                                builder.append("\"");
                                builder.append(key);
                                builder.append("\":");
                                builder.append(value);
                            }
                        });
                        builder.append("}");
                        break;
                    }
                    case Type.INT_TO_STRING_MAP: {
                        builder.append("\"");
                        builder.append(resolveName);
                        builder.append("\":");
                        builder.append("{");
                        IntStringMap castedMapI2S = (IntStringMap) elem;
                        isFirst[0] = true;
                        castedMapI2S.each(new IntStringMapCallBack() {
                            @Override
                            public void on(int key, String value) {
                                if (!isFirst[0]) {
                                    builder.append(",");
                                } else {
                                    isFirst[0] = false;
                                }
                                builder.append("\"");
                                builder.append(key);
                                builder.append("\":");
                                builder.append(value);
                            }
                        });
                        builder.append("}");
                        break;
                    }
                    case Type.LONG_TO_LONG_ARRAY_MAP: {
                        builder.append("\"");
                        builder.append(resolveName);
                        builder.append("\":");
                        builder.append("{");
                        LongLongArrayMap castedMapL2LA = (LongLongArrayMap) elem;
                        isFirst[0] = true;
                        Set<Long> keys = new HashSet<Long>();
                        castedMapL2LA.each(new LongLongArrayMapCallBack() {
                            @Override
                            public void on(long key, long value) {
                                keys.add(key);
                            }
                        });
                        final Long[] flatKeys = keys.toArray(new Long[keys.size()]);
                        for (int k = 0; k < flatKeys.length; k++) {
                            long[] values = castedMapL2LA.get(flatKeys[k]);
                            if (!isFirst[0]) {
                                builder.append(",");
                            } else {
                                isFirst[0] = false;
                            }
                            builder.append("\"");
                            builder.append(flatKeys[k]);
                            builder.append("\":[");
                            for (int j = 0; j < values.length; j++) {
                                if (j != 0) {
                                    builder.append(",");
                                }
                                builder.append(values[j]);
                            }
                            builder.append("]");
                        }
                        builder.append("}");
                        break;
                    }
                    case Type.STRING_TO_INT_MAP: {
                        builder.append("\"");
                        builder.append(resolveName);
                        builder.append("\":");
                        builder.append("{");
                        StringIntMap castedMapS2L = (StringIntMap) elem;
                        isFirst[0] = true;
                        castedMapS2L.each(new StringLongMapCallBack() {
                            @Override
                            public void on(String key, long value) {
                                if (!isFirst[0]) {
                                    builder.append(",");
                                } else {
                                    isFirst[0] = false;
                                }
                                builder.append("\"");
                                builder.append(key);
                                builder.append("\":");
                                builder.append(value);
                            }
                        });
                        builder.append("}");
                        break;
                    }

                }
            }
        }
        builder.append("}");
        return builder.toString();
    }

    @SuppressWarnings("Duplicates")
    final void save(final Buffer buffer) {
        Base64.encodeIntToBuffer(_size, buffer);
        for (int i = 0; i < _size; i++) {
            if (_v[i] != null) { //there is a real value
                final Object loopValue = _v[i];
                if (loopValue != null) {
                    buffer.write(CoreConstants.CHUNK_SEP);
                    Base64.encodeIntToBuffer(_type[i], buffer);
                    buffer.write(CoreConstants.CHUNK_SEP);
                    Base64.encodeIntToBuffer(_k[i], buffer);
                    buffer.write(CoreConstants.CHUNK_SEP);
                    switch (_type[i]) {
                        //additional types for embedded
                        case Type.ESTRUCT:
                            Base64.encodeIntToBuffer(((HeapEStruct) loopValue)._id, buffer);
                            break;
                        case Type.ERELATION:
                            ((HeapERelation) loopValue).save(buffer);
                            break;
                        //common types
                        case Type.STRING:
                            Base64.encodeStringToBuffer((String) loopValue, buffer);
                            break;
                        case Type.BOOL:
                            if ((Boolean) _v[i]) {
                                Base64.encodeIntToBuffer(CoreConstants.BOOL_TRUE, buffer);
                            } else {
                                Base64.encodeIntToBuffer(CoreConstants.BOOL_FALSE, buffer);
                            }
                            break;
                        case Type.LONG:
                            Base64.encodeLongToBuffer((Long) loopValue, buffer);
                            break;
                        case Type.DOUBLE:
                            Base64.encodeDoubleToBuffer((Double) loopValue, buffer);
                            break;
                        case Type.INT:
                            Base64.encodeIntToBuffer((Integer) loopValue, buffer);
                            break;
                        case Type.RELATION:
                            ((HeapRelation) loopValue).save(buffer);
                            break;
                        case Type.DOUBLE_ARRAY:
                            ((HeapDoubleArray) loopValue).save(buffer);
                            break;
                        case Type.LONG_ARRAY:
                            ((HeapLongArray) loopValue).save(buffer);
                            break;
                        case Type.INT_ARRAY:
                            ((HeapIntArray) loopValue).save(buffer);
                            break;
                        case Type.STRING_ARRAY:
                            ((HeapStringArray) loopValue).save(buffer);
                            break;
                        case Type.DMATRIX:
                            ((HeapDMatrix) loopValue).save(buffer);
                            break;
                        case Type.LMATRIX:
                            ((HeapLMatrix) loopValue).save(buffer);
                            break;
                        case Type.IMATRIX:
                            ((HeapIMatrix) loopValue).save(buffer);
                            break;
                        case Type.STRING_TO_INT_MAP:
                            ((HeapStringIntMap) loopValue).save(buffer);
                            break;
                        case Type.LONG_TO_LONG_MAP:
                            ((HeapLongLongMap) loopValue).save(buffer);
                            break;
                        case Type.INT_TO_INT_MAP:
                            ((HeapIntIntMap) loopValue).save(buffer);
                            break;
                        case Type.INT_TO_STRING_MAP:
                            ((HeapIntStringMap) loopValue).save(buffer);
                            break;
                        case Type.LONG_TO_LONG_ARRAY_MAP:
                            ((HeapLongLongArrayMap) loopValue).save(buffer);
                            break;
                        default:
                            ((HeapEStructArray) loopValue).save(buffer);
                            break;
                    }
                }
            }
        }
    }

    private static final byte LOAD_WAITING_ALLOC = 0;
    private static final byte LOAD_WAITING_TYPE = 1;
    private static final byte LOAD_WAITING_KEY = 2;
    private static final byte LOAD_WAITING_VALUE = 3;


    @SuppressWarnings("Duplicates")
    public final long load(final Buffer buffer, final long currentCursor, final Graph graph) {
        final boolean initial = _k == null;
        final long payloadSize = buffer.length();
        long cursor = currentCursor;
        long previous = cursor;
        byte state = LOAD_WAITING_ALLOC;
        int read_type = -1;
        int read_key = -1;
        while (cursor < payloadSize) {
            byte current = buffer.read(cursor);
            if (current == Constants.BLOCK_CLOSE) {
                break;
            } else if (current == Constants.CHUNK_SEP) {
                switch (state) {
                    case LOAD_WAITING_ALLOC:
                        final int closePowerOfTwo = (int) Math.pow(2, Math.ceil(Math.log(Base64.decodeToIntWithBounds(buffer, previous, cursor)) / Math.log(2)));
                        allocate(closePowerOfTwo);
                        state = LOAD_WAITING_TYPE;
                        cursor++;
                        previous = cursor;
                        break;
                    case LOAD_WAITING_TYPE:
                        read_type = Base64.decodeToIntWithBounds(buffer, previous, cursor);
                        state = LOAD_WAITING_KEY;
                        cursor++;
                        previous = cursor;
                        break;
                    case LOAD_WAITING_KEY:
                        read_key = Base64.decodeToIntWithBounds(buffer, previous, cursor);
                        //primitive default loader
                        switch (read_type) {
                            //primitive types
                            case Type.BOOL:
                            case Type.INT:
                            case Type.DOUBLE:
                            case Type.LONG:
                            case Type.STRING:
                            case Type.ESTRUCT:
                                state = LOAD_WAITING_VALUE;
                                cursor++;
                                previous = cursor;
                                break;
                            case Type.LONG_ARRAY:
                                HeapLongArray larray = new HeapLongArray(this);
                                cursor++;
                                cursor = larray.load(buffer, cursor, payloadSize);
                                internal_set(read_key, read_type, larray, true, initial);
                                if (cursor < payloadSize) {
                                    current = buffer.read(cursor);
                                    if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                        state = LOAD_WAITING_TYPE;
                                        cursor++;
                                        previous = cursor;
                                    }
                                }
                                break;
                            case Type.DOUBLE_ARRAY:
                                HeapDoubleArray darray = new HeapDoubleArray(this);
                                cursor++;
                                cursor = darray.load(buffer, cursor, payloadSize);
                                internal_set(read_key, read_type, darray, true, initial);
                                if (cursor < payloadSize) {
                                    current = buffer.read(cursor);
                                    if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                        state = LOAD_WAITING_TYPE;
                                        cursor++;
                                        previous = cursor;
                                    }
                                }
                                break;
                            case Type.INT_ARRAY:
                                HeapIntArray iarray = new HeapIntArray(this);
                                cursor++;
                                cursor = iarray.load(buffer, cursor, payloadSize);
                                internal_set(read_key, read_type, iarray, true, initial);
                                if (cursor < payloadSize) {
                                    current = buffer.read(cursor);
                                    if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                        state = LOAD_WAITING_TYPE;
                                        cursor++;
                                        previous = cursor;
                                    }
                                }
                                break;
                            case Type.STRING_ARRAY:
                                HeapStringArray sarray = new HeapStringArray(this);
                                cursor++;
                                cursor = sarray.load(buffer, cursor, payloadSize);
                                internal_set(read_key, read_type, sarray, true, initial);
                                if (cursor < payloadSize) {
                                    current = buffer.read(cursor);
                                    if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                        state = LOAD_WAITING_TYPE;
                                        cursor++;
                                        previous = cursor;
                                    }
                                }
                                break;
                            case Type.RELATION:
                                HeapRelation relation = new HeapRelation(this, null);
                                cursor++;
                                cursor = relation.load(buffer, cursor, payloadSize);
                                internal_set(read_key, read_type, relation, true, initial);
                                if (cursor < payloadSize) {
                                    current = buffer.read(cursor);
                                    if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                        state = LOAD_WAITING_TYPE;
                                        cursor++;
                                        previous = cursor;
                                    }
                                }
                                break;
                            case Type.DMATRIX:
                                HeapDMatrix matrix = new HeapDMatrix(this, null);
                                cursor++;
                                cursor = matrix.load(buffer, cursor, payloadSize);
                                internal_set(read_key, read_type, matrix, true, initial);
                                if (cursor < payloadSize) {
                                    current = buffer.read(cursor);
                                    if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                        state = LOAD_WAITING_TYPE;
                                        cursor++;
                                        previous = cursor;
                                    }
                                }
                                break;
                            case Type.LMATRIX:
                                HeapLMatrix lmatrix = new HeapLMatrix(this, null);
                                cursor++;
                                cursor = lmatrix.load(buffer, cursor, payloadSize);
                                internal_set(read_key, read_type, lmatrix, true, initial);
                                if (cursor < payloadSize) {
                                    current = buffer.read(cursor);
                                    if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                        state = LOAD_WAITING_TYPE;
                                        cursor++;
                                        previous = cursor;
                                    }
                                }
                                break;
                            case Type.IMATRIX:
                                HeapIMatrix imatrix = new HeapIMatrix(this, null);
                                cursor++;
                                cursor = imatrix.load(buffer, cursor, payloadSize);
                                internal_set(read_key, read_type, imatrix, true, initial);
                                if (cursor < payloadSize) {
                                    current = buffer.read(cursor);
                                    if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                        state = LOAD_WAITING_TYPE;
                                        cursor++;
                                        previous = cursor;
                                    }
                                }
                                break;
                            case Type.LONG_TO_LONG_MAP:
                                HeapLongLongMap l2lmap = new HeapLongLongMap(this);
                                cursor++;
                                cursor = l2lmap.load(buffer, cursor, payloadSize);
                                internal_set(read_key, read_type, l2lmap, true, initial);
                                if (cursor < payloadSize) {
                                    current = buffer.read(cursor);
                                    if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                        state = LOAD_WAITING_TYPE;
                                        cursor++;
                                        previous = cursor;
                                    }
                                }
                                break;
                            case Type.INT_TO_INT_MAP:
                                HeapIntIntMap i2imap = new HeapIntIntMap(this);
                                cursor++;
                                cursor = i2imap.load(buffer, cursor, payloadSize);
                                internal_set(read_key, read_type, i2imap, true, initial);
                                if (cursor < payloadSize) {
                                    current = buffer.read(cursor);
                                    if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                        state = LOAD_WAITING_TYPE;
                                        cursor++;
                                        previous = cursor;
                                    }
                                }
                                break;
                            case Type.INT_TO_STRING_MAP:
                                HeapIntStringMap i2smap = new HeapIntStringMap(this);
                                cursor++;
                                cursor = i2smap.load(buffer, cursor, payloadSize);
                                internal_set(read_key, read_type, i2smap, true, initial);
                                if (cursor < payloadSize) {
                                    current = buffer.read(cursor);
                                    if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                        state = LOAD_WAITING_TYPE;
                                        cursor++;
                                        previous = cursor;
                                    }
                                }
                                break;
                            case Type.LONG_TO_LONG_ARRAY_MAP:
                                HeapLongLongArrayMap l2lrmap = new HeapLongLongArrayMap(this);
                                cursor++;
                                cursor = l2lrmap.load(buffer, cursor, payloadSize);
                                internal_set(read_key, read_type, l2lrmap, true, initial);
                                if (cursor < payloadSize) {
                                    current = buffer.read(cursor);
                                    if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                        state = LOAD_WAITING_TYPE;
                                        cursor++;
                                        previous = cursor;
                                    }
                                }
                                break;
                            case Type.STRING_TO_INT_MAP:
                                HeapStringIntMap s2lmap = new HeapStringIntMap(this);
                                cursor++;
                                cursor = s2lmap.load(buffer, cursor, payloadSize);
                                internal_set(read_key, read_type, s2lmap, true, initial);
                                if (cursor < payloadSize) {
                                    current = buffer.read(cursor);
                                    if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                        state = LOAD_WAITING_TYPE;
                                        cursor++;
                                        previous = cursor;
                                    }
                                }
                                break;
                            case Type.ERELATION:
                                HeapERelation eRelation = new HeapERelation(this, null);
                                cursor++;
                                cursor = eRelation.load(buffer, cursor, payloadSize);
                                internal_set(read_key, read_type, eRelation, true, initial);
                                if (cursor < payloadSize) {
                                    current = buffer.read(cursor);
                                    if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                        state = LOAD_WAITING_TYPE;
                                        cursor++;
                                        previous = cursor;
                                    }
                                }
                                break;
                            default:
                                HeapEStructArray eGraphDef = new HeapEStructArray(this, null, this.graph());
                                cursor++;
                                cursor = eGraphDef.load(buffer, cursor, payloadSize);
                                internal_set(read_key, read_type, eGraphDef, true, initial);
                                if (cursor < payloadSize) {
                                    current = buffer.read(cursor);
                                    if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                        state = LOAD_WAITING_TYPE;
                                        cursor++;
                                        previous = cursor;
                                    }
                                }
                        }
                        break;
                    case LOAD_WAITING_VALUE:
                        load_primitive(read_key, read_type, buffer, previous, cursor, initial);
                        state = LOAD_WAITING_TYPE;
                        cursor++;
                        previous = cursor;
                        break;
                }
            } else {
                cursor++;
            }
        }
        if (state == LOAD_WAITING_VALUE) {
            load_primitive(read_key, read_type, buffer, previous, cursor, initial);
        }
        return cursor;
    }

    private void load_primitive(final int read_key, final int read_type, final Buffer buffer, final long previous, final long cursor, final boolean initial) {
        switch (read_type) {
            case Type.BOOL:
                internal_set(read_key, read_type, (((byte) Base64.decodeToIntWithBounds(buffer, previous, cursor)) == CoreConstants.BOOL_TRUE), true, initial);
                break;
            case Type.INT:
                internal_set(read_key, read_type, Base64.decodeToIntWithBounds(buffer, previous, cursor), true, initial);
                break;
            case Type.DOUBLE:
                internal_set(read_key, read_type, Base64.decodeToDoubleWithBounds(buffer, previous, cursor), true, initial);
                break;
            case Type.LONG:
                internal_set(read_key, read_type, Base64.decodeToLongWithBounds(buffer, previous, cursor), true, initial);
                break;
            case Type.STRING:
                internal_set(read_key, read_type, Base64.decodeToStringWithBounds(buffer, previous, cursor), true, initial);
                break;
            case Type.ESTRUCT:
                internal_set(read_key, read_type, _parent.nodeByIndex(Base64.decodeToIntWithBounds(buffer, previous, cursor), true), true, initial);
                break;
        }
    }

    @Override
    public final void each(final NodeStateCallback callBack) {
        for (int i = 0; i < _size; i++) {
            if (_v[i] != null) {
                callBack.on(_k[i], _type[i], _v[i]);
            }
        }
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
    public final IMatrix getIMatrix(String name) {
        return (IMatrix) get(name);
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
}
