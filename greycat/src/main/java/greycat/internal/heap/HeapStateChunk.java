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
package greycat.internal.heap;

import greycat.*;
import greycat.base.BaseCustomType;
import greycat.chunk.Chunk;
import greycat.chunk.ChunkType;
import greycat.chunk.StateChunk;
import greycat.internal.CoreConstants;
import greycat.plugin.NodeStateCallback;
import greycat.plugin.TypeDeclaration;
import greycat.struct.*;
import greycat.utility.Base64;
import greycat.utility.HashHelper;

import java.util.Arrays;

class HeapStateChunk implements StateChunk, HeapContainer {

    private final long _index;
    private final HeapChunkSpace _space;

    private int _capacity;
    private volatile int _size;
    private int[] _k;
    private Object[] _v;
    private int[] _type;
    private int[] next_and_hash;
    private long _hash;
    private boolean _inSync;
    private int _group;

    @Override
    public final Graph graph() {
        return _space.graph();
    }

    HeapStateChunk(final HeapChunkSpace p_space, final long p_index) {
        _space = p_space;
        _index = p_index;
        //null hash function
        next_and_hash = null;
        _type = null;
        //init to empty size
        _size = 0;
        _capacity = 0;
        _hash = 0;
        _inSync = true;
        _group = 0;
    }

    @Override
    public synchronized final boolean inSync() {
        return _inSync;
    }

    @Override
    public synchronized final boolean sync(long remoteHash) {
        if (_inSync && remoteHash != _hash) {
            _inSync = false;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public final long world() {
        return _space.worldByIndex(_index);
    }

    @Override
    public final long time() {
        return _space.timeByIndex(_index);
    }

    @Override
    public final long id() {
        return _space.idByIndex(_index);
    }

    @Override
    public final byte chunkType() {
        return ChunkType.STATE_CHUNK;
    }

    @Override
    public final long index() {
        return _index;
    }

    @Override
    public synchronized final Object getAt(final int p_key) {
        return internal_get(p_key, false);
    }

    @Override
    public synchronized Object getRawAt(final int p_key) {
        return internal_get(p_key, true);
    }

    @Override
    public final int group() {
        return _group;
    }

    @Override
    public final Chunk setGroup(int g) {
        _group = g;
        return this;
    }

    @Override
    public Object getTypedRawAt(int index, int type) {
        //empty chunk, we return immediately
        if (_size == 0) {
            return null;
        }
        int found = internal_find(index);
        if (found != -1 && _type[found] == type) {
            return _v[found];
        }
        return null;
    }

    private int internal_find(final int p_key) {
        if (_size == 0) {
            return -1;
        } else if (next_and_hash == null) {
            for (int i = 0; i < _size; i++) {
                if (_k[i] == p_key) {
                    return i;
                }
            }
            return -1;
        } else {
            int hashIndex = p_key % (_capacity * 2);
            if (hashIndex < 0) {
                hashIndex = hashIndex * -1;
            }
            int m = next_and_hash[_capacity + hashIndex];
            while (m >= 0) {
                if (p_key == _k[m]) {
                    return m;
                } else {
                    m = next_and_hash[m];
                }
            }
            return -1;
        }
    }

    private Object internal_get(final int p_key, final boolean p_raw) {
        //empty chunk, we return immediately
        if (_size == 0) {
            return null;
        }
        int found = internal_find(p_key);
        Object result;
        if (found != -1) {
            result = _v[found];
            if (result != null) {
                switch (_type[found]) {
                    case Type.BOOL:
                    case Type.STRING:
                    case Type.LONG:
                    case Type.INT:
                    case Type.DOUBLE:
                    case Type.DOUBLE_ARRAY:
                    case Type.LONG_ARRAY:
                    case Type.INT_ARRAY:
                    case Type.STRING_ARRAY:
                    case Type.LONG_TO_LONG_MAP:
                    case Type.LONG_TO_LONG_ARRAY_MAP:
                    case Type.STRING_TO_INT_MAP:
                    case Type.RELATION:
                    case Type.DMATRIX:
                    case Type.LMATRIX:
                    case Type.ESTRUCT_ARRAY:
                    case Type.ESTRUCT:
                    case Type.ERELATION:
                    case Type.TASK:
                    case Type.TASK_ARRAY:
                    case Type.NODE:
                        //case Type.NODE_ARRAY:
                    case Type.INT_TO_INT_MAP:
                    case Type.INT_TO_STRING_MAP:
                        return result;
                    default:
                        if (p_raw) {
                            return result;
                        } else {
                            final TypeDeclaration declaration = graph().typeRegistry().declarationByHash(_type[found]);
                            if (declaration == null) {
                                return result;
                            } else {
                                return declaration.factory().wrap((EStructArray) result);
                            }
                        }
                }
            }
        }
        return null;
    }

    /**
     * {@native ts
     * if(p_unsafe_elem != null){
     * if(p_elemType == Type.STRING){ if(!(typeof p_unsafe_elem === 'string')){ throw new Error("GreyCat usage error, set method called with type " + Type.typeName(p_elemType) + " while param object is " + p_unsafe_elem); } }
     * if(p_elemType == Type.BOOL){ if(!(typeof p_unsafe_elem === 'boolean')){ throw new Error("GreyCat usage error, set method called with type " + Type.typeName(p_elemType) + " while param object is " + p_unsafe_elem); } }
     * if(p_elemType == Type.DOUBLE || p_elemType == Type.LONG || p_elemType == Type.INT){ if(!(typeof p_unsafe_elem === 'number')){ throw new Error("GreyCat usage error, set method called with type " + Type.typeName(p_elemType) + " while param object is " + p_unsafe_elem); } }
     * if(p_elemType == Type.DOUBLE_ARRAY){ if(!(p_unsafe_elem instanceof Float64Array)){ throw new Error("GreyCat usage error, set method called with type " + Type.typeName(p_elemType) + " while param object is " + p_unsafe_elem); } }
     * if(p_elemType == Type.LONG_ARRAY){ if(!(p_unsafe_elem instanceof Float64Array)){ throw new Error("GreyCat usage error, set method called with type " + Type.typeName(p_elemType) + " while param object is " + p_unsafe_elem); } }
     * if(p_elemType == Type.INT_ARRAY){ if(!(p_unsafe_elem instanceof Int32Array)){ throw new Error("GreyCat usage error, set method called with type " + Type.typeName(p_elemType) + " while param object is " + p_unsafe_elem); } }
     * if(p_elemType == Type.STRING_TO_INT_MAP){ if(!(typeof p_unsafe_elem === 'object')){ throw new Error("GreyCat usage error, set method called with type " + Type.typeName(p_elemType) + " while param object is " + p_unsafe_elem); } }
     * if(p_elemType == Type.LONG_TO_LONG_MAP){ if(!(typeof p_unsafe_elem === 'boolean')){ throw new Error("GreyCat usage error, set method called with type " + Type.typeName(p_elemType) + " while param object is " + p_unsafe_elem); } }
     * if(p_elemType == Type.LONG_TO_LONG_ARRAY_MAP){ if(!(typeof p_unsafe_elem === 'boolean')){ throw new Error("GreyCat usage error, set method called with type " + Type.typeName(p_elemType) + " while param object is " + p_unsafe_elem); } }
     * }
     * this.internal_set(p_elementIndex, p_elemType, p_unsafe_elem, true, false);
     * return this;
     * }
     */
    @Override
    public synchronized final Container setAt(final int p_elementIndex, final int p_elemType, final Object p_unsafe_elem) {
        internal_set(p_elementIndex, p_elemType, p_unsafe_elem, true, false);
        return this;
    }

    @Override
    public Container remove(String name) {
        return set(name, Type.INT, null);
    }

    @Override
    public Container removeAt(int key) {
        return setAt(key, Type.INT, null);
    }

    @Override
    public synchronized final Container set(final String key, final int p_elemType, final Object p_unsafe_elem) {
        internal_set(_space.graph().resolver().stringToHash(key, true), p_elemType, p_unsafe_elem, true, false);
        return this;
    }

    @Override
    public synchronized final Object get(final String key) {
        return internal_get(_space.graph().resolver().stringToHash(key, false), false);
    }

    @Override
    public final <A> A getWithDefault(final String key, final A defaultValue) {
        final Object result = get(key);
        if (result == null) {
            return defaultValue;
        } else {
            return (A) result;
        }
    }

    @Override
    public final <A> A getAtWithDefault(final int key, final A defaultValue) {
        final Object result = getAt(key);
        if (result == null) {
            return defaultValue;
        } else {
            return (A) result;
        }
    }

    @Override
    public Container rephase() {
        return this;
    }

    @Override
    public synchronized final int typeAt(final int p_key) {
        final int found_index = internal_find(p_key);
        if (found_index != -1) {
            return _type[found_index];
        } else {
            return -1;
        }
    }

    @Override
    public final int type(final String key) {
        return typeAt(_space.graph().resolver().stringToHash(key, false));
    }

    @Override
    public final Object getOrCreateCustom(final String name, final String typeName) {
        return getOrCreateAt(HashHelper.hash(name), HashHelper.hash(typeName));
    }

    @Override
    public final Object getOrCreateCustomAt(final int index, final String typeName) {
        return getOrCreateAt(index, HashHelper.hash(typeName));
    }

    @Override
    public synchronized final Object getOrCreateAt(final int p_key, final int p_type) {
        final int found = internal_find(p_key);
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
        Object toGet = null;
        switch (p_type) {
            case Type.LONG_ARRAY:
                toSet = new HeapLongArray(this);
                toGet = toSet;
                break;
            case Type.DOUBLE_ARRAY:
                toSet = new HeapDoubleArray(this);
                toGet = toSet;
                break;
            case Type.INT_ARRAY:
                toSet = new HeapIntArray(this);
                toGet = toSet;
                break;
            case Type.STRING_ARRAY:
                toSet = new HeapStringArray(this);
                toGet = toSet;
                break;
            case Type.RELATION:
                toSet = new HeapRelation(this, null);
                toGet = toSet;
                break;
            case Type.DMATRIX:
                toSet = new HeapDMatrix(this, null);
                toGet = toSet;
                break;
            case Type.LMATRIX:
                toSet = new HeapLMatrix(this, null);
                toGet = toSet;
                break;
            case Type.STRING_TO_INT_MAP:
                toSet = new HeapStringIntMap(this);
                toGet = toSet;
                break;
            case Type.LONG_TO_LONG_MAP:
                toSet = new HeapLongLongMap(this);
                toGet = toSet;
                break;
            case Type.INT_TO_INT_MAP:
                toSet = new HeapIntIntMap(this);
                toGet = toSet;
                break;
            case Type.INT_TO_STRING_MAP:
                toSet = new HeapIntStringMap(this);
                toGet = toSet;
                break;
            case Type.LONG_TO_LONG_ARRAY_MAP:
                toSet = new HeapLongLongArrayMap(this);
                toGet = toSet;
                break;
                /*
            case Type.ESTRUCT:
                toSet = new HeapEStruct(this, null, _space.graph());
                toGet = toSet;
                break;*/
            case Type.ESTRUCT_ARRAY:
                toSet = new HeapEStructArray(this, null, _space.graph());
                toGet = toSet;
                break;
                /*
            case Type.KDTREE:
                EStructArray tempKD = new HeapEStructArray(this, null, _space.graph());
                toSet = tempKD;
                toGet = new KDTree(tempKD);
                break;
            case Type.NDTREE:
                EStructArray tempND = new HeapEStructArray(this, null, _space.graph());
                toSet = tempND;
                toGet = new NDTree(tempND, new IndexManager());
                break;
                */
        }
        //Default, custom Type
        if (toSet == null) {
            final EStructArray tempND = new HeapEStructArray(this, null, _space.graph());
            toSet = tempND;
            final TypeDeclaration typeDeclaration = graph().typeRegistry().declarationByHash(p_type);
            if (typeDeclaration == null) {
                toGet = toSet;
            } else {
                toGet = typeDeclaration.factory().wrap(tempND);
                ((BaseCustomType) toGet).init();
            }
        }
        internal_set(p_key, p_type, toSet, true, false);
        return toGet;
    }

    @Override
    public final Object getOrCreate(final String key, final int elemType) {
        return getOrCreateAt(_space.graph().resolver().stringToHash(key, true), elemType);
    }

    @Override
    final public void declareDirty() {
        if (_space != null && _hash != Constants.EMPTY_HASH) {
            _hash = Constants.EMPTY_HASH;
            _space.notifyUpdate(_index);
        }
    }

    @SuppressWarnings("Duplicates")
    @Override
    public synchronized final void save(final Buffer buffer) {
        final long beginIndex = buffer.writeIndex();
        if (_group != 0) {
            Base64.encodeIntToBuffer(_group, buffer);
            buffer.write(CoreConstants.CHUNK_META_SEP);
        }
        Base64.encodeIntToBuffer(_size, buffer);
        for (int i = 0; i < _size; i++) {
            final Object loopValue = _v[i]; //there is a real value
            buffer.write(CoreConstants.CHUNK_SEP);
            Base64.encodeIntToBuffer(_type[i], buffer);
            buffer.write(CoreConstants.CHUNK_SEP);
            Base64.encodeIntToBuffer(_k[i], buffer);
            buffer.write(CoreConstants.CHUNK_SEP);
            if (loopValue != null) {
                switch (_type[i]) {
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
                    case Type.RELATION:
                        ((HeapRelation) loopValue).save(buffer);
                        break;
                    case Type.DMATRIX:
                        ((HeapDMatrix) loopValue).save(buffer);
                        break;
                    case Type.LMATRIX:
                        ((HeapLMatrix) loopValue).save(buffer);
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
        _hash = HashHelper.hashBuffer(buffer, beginIndex, buffer.writeIndex());
    }

    @Override
    public void saveDiff(Buffer buffer) {

    }

    @Override
    public synchronized final void each(final NodeStateCallback callBack) {
        for (int i = 0; i < _size; i++) {
            if (_v[i] != null) {
                callBack.on(_k[i], _type[i], _v[i]);
            }
        }
    }

    @Override
    public synchronized void loadFrom(final StateChunk origin) {
        if (origin == null) {
            return;
        }
        HeapStateChunk casted = (HeapStateChunk) origin;
        _capacity = casted._capacity;
        _size = casted._size;
        _group = casted._group;
        //copy keys
        if (casted._k != null) {
            int[] cloned_k = new int[_capacity];
            System.arraycopy(casted._k, 0, cloned_k, 0, _capacity);
            _k = cloned_k;
        }
        //copy values
        /*
        Object[] cloned_v = new Object[_capacity];
        System.arraycopy(casted._v, 0, cloned_v, 0, _capacity);
        _v = cloned_v;
        */
        //copy types
        if (casted._type != null) {
            int[] cloned_type = new int[_capacity];
            System.arraycopy(casted._type, 0, cloned_type, 0, _capacity);
            _type = cloned_type;
        }
        //copy next if not empty
        if (casted.next_and_hash != null) {
            int[] cloned_hash = new int[_capacity * 3];
            System.arraycopy(casted.next_and_hash, 0, cloned_hash, 0, _capacity * 3);
            next_and_hash = cloned_hash;
        }
        if (casted._v != null) {
            _v = new Object[_capacity];
            for (int i = 0; i < _size; i++) {
                if (casted._v[i] == null) {
                    _v[i] = null;
                } else {
                    switch (casted._type[i]) {
                        case Type.LONG_TO_LONG_MAP:
                            if (casted._v[i] != null) {
                                _v[i] = ((HeapLongLongMap) casted._v[i]).cloneFor(this);
                            }
                            break;
                        case Type.LONG_TO_LONG_ARRAY_MAP:
                            if (casted._v[i] != null) {
                                _v[i] = ((HeapLongLongArrayMap) casted._v[i]).cloneFor(this);
                            }
                            break;
                        case Type.STRING_TO_INT_MAP:
                            if (casted._v[i] != null) {
                                _v[i] = ((HeapStringIntMap) casted._v[i]).cloneFor(this);
                            }
                            break;
                        case Type.INT_TO_INT_MAP:
                            if (casted._v[i] != null) {
                                _v[i] = ((HeapIntIntMap) casted._v[i]).cloneFor(this);
                            }
                            break;
                        case Type.INT_TO_STRING_MAP:
                            if (casted._v[i] != null) {
                                _v[i] = ((HeapIntStringMap) casted._v[i]).cloneFor(this);
                            }
                            break;
                        case Type.RELATION:
                            if (casted._v[i] != null) {
                                _v[i] = new HeapRelation(this, (HeapRelation) casted._v[i]);
                            }
                            break;
                        case Type.DMATRIX:
                            if (casted._v[i] != null) {
                                _v[i] = new HeapDMatrix(this, (HeapDMatrix) casted._v[i]);
                            }
                            break;
                        case Type.LMATRIX:
                            if (casted._v[i] != null) {
                                _v[i] = new HeapLMatrix(this, (HeapLMatrix) casted._v[i]);
                            }
                            break;
                        case Type.ESTRUCT_ARRAY:
                            if (casted._v[i] != null) {
                                _v[i] = new HeapEStructArray(this, (HeapEStructArray) casted._v[i], _space.graph());
                            }
                            break;
                        case Type.LONG_ARRAY:
                            if (casted._v[i] != null) {
                                _v[i] = ((HeapLongArray) casted._v[i]).cloneFor(this);
                            }
                            break;
                        case Type.DOUBLE_ARRAY:
                            if (casted._v[i] != null) {
                                _v[i] = ((HeapDoubleArray) casted._v[i]).cloneFor(this);
                            }
                            break;
                        case Type.INT_ARRAY:
                            if (casted._v[i] != null) {
                                _v[i] = ((HeapIntArray) casted._v[i]).cloneFor(this);
                            }
                            break;
                        case Type.STRING_ARRAY:
                            if (casted._v[i] != null) {
                                _v[i] = ((HeapStringArray) casted._v[i]).cloneFor(this);
                            }
                            break;
                        default:
                            if (!Type.isCustom(casted._type[i])) {
                                _v[i] = casted._v[i];
                            } else {
                                if (casted._v[i] != null) {
                                    _v[i] = new HeapEStructArray(this, (HeapEStructArray) casted._v[i], _space.graph());
                                }
                            }
                            break;
                    }
                }
            }
        }
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
                    case Type.INT:
                        if (p_unsafe_elem instanceof Integer) {
                            param_elem = (int) p_unsafe_elem;
                        } else if (p_unsafe_elem instanceof Double) {
                            double preCasting = (Double) p_unsafe_elem;
                            param_elem = (int) preCasting;
                        } else if (p_unsafe_elem instanceof Long) {
                            long preCastingLong = (Long) p_unsafe_elem;
                            param_elem = (int) preCastingLong;
                        } else if (p_unsafe_elem instanceof Float) {
                            float preCastingLong = (Float) p_unsafe_elem;
                            param_elem = (int) preCastingLong;
                        } else if (p_unsafe_elem instanceof Byte) {
                            byte preCastingLong = (Byte) p_unsafe_elem;
                            param_elem = (int) preCastingLong;
                        } else {
                            param_elem = (int) p_unsafe_elem;
                        }
                        break;
                    case Type.DOUBLE:
                        if (p_unsafe_elem instanceof Double) {
                            param_elem = (double) p_unsafe_elem;
                        } else if (p_unsafe_elem instanceof Integer) {
                            int preCasting = (Integer) p_unsafe_elem;
                            param_elem = (double) preCasting;
                        } else if (p_unsafe_elem instanceof Long) {
                            long preCastingLong = (Long) p_unsafe_elem;
                            param_elem = (double) preCastingLong;
                        } else if (p_unsafe_elem instanceof Float) {
                            float preCastingLong = (Float) p_unsafe_elem;
                            param_elem = (double) preCastingLong;
                        } else if (p_unsafe_elem instanceof Byte) {
                            byte preCastingLong = (Byte) p_unsafe_elem;
                            param_elem = (double) preCastingLong;
                        } else {
                            param_elem = (double) p_unsafe_elem;
                        }
                        break;
                    case Type.LONG:
                        if (p_unsafe_elem instanceof Long) {
                            param_elem = (long) p_unsafe_elem;
                        } else if (p_unsafe_elem instanceof Integer) {
                            int preCasting = (Integer) p_unsafe_elem;
                            param_elem = (long) preCasting;
                        } else if (p_unsafe_elem instanceof Double) {
                            double preCastingLong = (Double) p_unsafe_elem;
                            param_elem = (long) preCastingLong;
                        } else if (p_unsafe_elem instanceof Float) {
                            float preCastingLong = (Float) p_unsafe_elem;
                            param_elem = (long) preCastingLong;
                        } else if (p_unsafe_elem instanceof Byte) {
                            byte preCastingLong = (Byte) p_unsafe_elem;
                            param_elem = (long) preCastingLong;
                        } else {
                            param_elem = (long) p_unsafe_elem;
                        }
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
                    case Type.RELATION:
                        param_elem = (Relation) p_unsafe_elem;
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
                    case Type.ESTRUCT_ARRAY:
                        param_elem = (EStructArray) p_unsafe_elem;
                        break;
                    default:
                        param_elem = (EStructArray) p_unsafe_elem;
                        // throw new RuntimeException("Internal Exception, unknown type");
                }
            } catch (Exception e) {
                throw new RuntimeException("GreyCat usage error, set method called with type " + Type.typeName(p_type) + " while param object is " + p_unsafe_elem);
            }
        }
        //first value
        if (_k == null) {
            //we do not allocate for empty element

            /*if (param_elem == null) {
                return;
            }*/
            _capacity = Constants.MAP_INITIAL_CAPACITY;
            _k = new int[_capacity];
            _v = new Object[_capacity];
            _type = new int[_capacity];
            _k[0] = p_key;
            _v[0] = param_elem;
            _type[0] = p_type;
            _size = 1;
            if (!initial) {
                declareDirty();
            }
            return;
        }
        int entry = -1;
        //int p_entry = -1;
        int hashIndex = -1;
        if (next_and_hash == null) {
            for (int i = 0; i < _size; i++) {
                if (_k[i] == p_key) {
                    entry = i;
                    break;
                }
            }
        } else {
            hashIndex = p_key % (_capacity * 2);
            if (hashIndex < 0) {
                hashIndex = hashIndex * -1;
            }
            int m = next_and_hash[_capacity + hashIndex];
            while (m != -1) {
                if (_k[m] == p_key) {
                    entry = m;
                    break;
                }
                //p_entry = m;
                m = next_and_hash[m];
            }
        }
        //case already present
        if (entry != -1) {
            if (replaceIfPresent || (p_type != _type[entry])) {
                /*if (param_elem == null) {
                    if (next_and_hash != null) {
                        //unHash previous
                        if (p_entry != -1) {
                            next_and_hash[p_entry] = next_and_hash[entry];
                        } else {
                            next_and_hash[_capacity + hashIndex] = -1;
                        }
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
                        if (next_and_hash != null) {
                            next_and_hash[entry] = next_and_hash[indexVictim];
                            int victimHash = _k[entry] % (_capacity * 2);
                            if (victimHash < 0) {
                                victimHash = victimHash * -1;
                            }
                            int m = next_and_hash[_capacity + victimHash];
                            if (m == indexVictim) {
                                //the victim was the head of hashing list
                                next_and_hash[_capacity + victimHash] = entry;
                            } else {
                                //the victim is in the next, reChain it
                                while (m != -1) {
                                    if (next_and_hash[m] == indexVictim) {
                                        next_and_hash[m] = entry;
                                        break;
                                    }
                                    m = next_and_hash[m];
                                }
                            }
                        }
                        _k[indexVictim] = -1;
                        _v[indexVictim] = null;
                        _type[indexVictim] = -1;
                    }
                    _size--;
                } else {*/
                _v[entry] = param_elem;
                //if (_type[entry] != p_type) {
                _type[entry] = p_type;
                //}
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
            if (next_and_hash != null) {
                next_and_hash[_size] = next_and_hash[_capacity + hashIndex];
                next_and_hash[_capacity + hashIndex] = _size;
            }
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
        next_and_hash = new int[_capacity * 3];
        Arrays.fill(next_and_hash, 0, _capacity * 3, -1);
        int double_capacity = _capacity * 2;
        for (int i = 0; i < _size; i++) {
            int keyHash = _k[i] % double_capacity;
            if (keyHash < 0) {
                keyHash = keyHash * -1;
            }
            next_and_hash[i] = next_and_hash[_capacity + keyHash];
            next_and_hash[_capacity + keyHash] = i;
        }
        if (!initial) {
            declareDirty();
        }
    }

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
        next_and_hash = new int[_capacity * 3];
        Arrays.fill(next_and_hash, 0, _capacity * 3, -1);
        for (int i = 0; i < _size; i++) {
            int keyHash = _k[i] % (_capacity * 2);
            if (keyHash < 0) {
                keyHash = keyHash * -1;
            }
            next_and_hash[i] = next_and_hash[_capacity + keyHash];
            next_and_hash[_capacity + keyHash] = i;
        }
    }

    private static final byte LOAD_WAITING_ALLOC = 0;
    private static final byte LOAD_WAITING_TYPE = 1;
    private static final byte LOAD_WAITING_KEY = 2;
    private static final byte LOAD_WAITING_VALUE = 3;

    private synchronized void internal_load(final Buffer buffer, final boolean initial) {
        if (buffer != null && buffer.length() > 0) {
            final long payloadSize = buffer.length();
            long previous = 0;
            long cursor = 0;
            byte state = LOAD_WAITING_ALLOC;
            int read_type = -1;
            int read_key = -1;
            while (cursor < payloadSize) {
                byte current = buffer.read(cursor);
                if (current == Constants.CHUNK_META_SEP) {
                    if (previous != cursor) {
                        _group = Base64.decodeToIntWithBounds(buffer, previous, cursor);
                    }
                    cursor++;
                    previous = cursor;
                } else if (current == Constants.CHUNK_SEP) {
                    switch (state) {
                        case LOAD_WAITING_ALLOC:
                            allocate(Base64.decodeToIntWithBounds(buffer, previous, cursor));
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
                                    final int previousFound = internal_find(read_key);
                                    HeapStringIntMap s2lmap;
                                    if (previousFound != -1 && _type[previousFound] == Type.STRING_TO_INT_MAP) {
                                        s2lmap = (HeapStringIntMap) _v[previousFound];
                                    } else {
                                        s2lmap = new HeapStringIntMap(this);
                                        internal_set(read_key, read_type, s2lmap, true, initial);
                                    }
                                    cursor++;
                                    cursor = s2lmap.load(buffer, cursor, payloadSize);
                                    if (cursor < payloadSize) {
                                        current = buffer.read(cursor);
                                        if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                            state = LOAD_WAITING_TYPE;
                                            cursor++;
                                            previous = cursor;
                                        }
                                    }
                                    break;
                                /*case Type.NDTREE:
                                case Type.KDTREE:
                                */
                                    /*
                                case Type.ESTRUCT_ARRAY:
                                    HeapEStructArray eGraph = new HeapEStructArray(this, null, this.graph());
                                    cursor++;
                                    cursor = eGraph.load(buffer, cursor, payloadSize);
                                    internal_set(read_key, read_type, eGraph, true, initial);
                                    if (cursor < payloadSize) {
                                        current = buffer.read(cursor);
                                        if (current == Constants.CHUNK_SEP && cursor < payloadSize) {
                                            state = LOAD_WAITING_TYPE;
                                            cursor++;
                                            previous = cursor;
                                        }
                                    }
                                    break;*/
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
            _hash = HashHelper.hashBuffer(buffer, 0, payloadSize);
        } else {
            _hash = 0;
        }
    }

    private void load_primitive(final int read_key, final int read_type, final Buffer buffer, final long previous, final long cursor, final boolean initial) {
        if (previous == cursor) {
            internal_set(read_key, read_type, null, true, initial);
        } else {
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
            }
        }
    }

    public final synchronized void load(final Buffer buffer) {
        internal_load(buffer, true);
    }

    @Override
    public final void loadDiff(Buffer buffer) {
        internal_load(buffer, false);
    }

    @Override
    public synchronized final long hash() {
        return _hash;
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
    public final int[] attributeIndexes() {
        final int[] result = new int[_size];
        System.arraycopy(_k, 0, result, 0, _size);
        return result;
    }

}
