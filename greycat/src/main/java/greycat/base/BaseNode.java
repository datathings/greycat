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

import greycat.*;
import greycat.chunk.StateChunk;
import greycat.struct.*;
import greycat.plugin.NodeDeclaration;
import greycat.plugin.NodeState;
import greycat.plugin.NodeStateCallback;
import greycat.plugin.Resolver;
import greycat.struct.proxy.*;
import greycat.utility.Tuple;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Base implementation to develop NodeFactory plugins without overriding every methods
 */
public class BaseNode implements Node {

    /**
     * @ignore ts
     */
    private static sun.misc.Unsafe unsafe;

    private final long _world;
    private final long _time;
    private final long _id;
    private final Graph _graph;
    protected final Resolver _resolver;

    //cache to enhance the resolving process
    public volatile long _index_worldOrder = -1;
    public volatile long _index_superTimeTree = -1;
    public volatile long _index_timeTree = -1;
    public volatile long _index_stateChunk = -1;
    public volatile long _world_magic = -1;
    public volatile long _super_time_magic = -1;
    public volatile long _time_magic = -1;
    public volatile boolean _dead = false;
    private volatile int _lock;

    public BaseNode(long p_world, long p_time, long p_id, Graph p_graph) {
        this._world = p_world;
        this._time = p_time;
        this._id = p_id;
        this._graph = p_graph;
        this._resolver = p_graph.resolver();
    }

    /**
     * @ignore ts
     */
    private static final long _lockOffset;

    /**
     * @ignore ts
     */
    static {
        if (unsafe == null) {
            try {
                Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
            } catch (Exception e) {
                throw new RuntimeException("ERROR: unsafe operations are not available");
            }
        }
        try {
            _lockOffset = unsafe.objectFieldOffset(BaseNode.class.getDeclaredField("_lock"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    /**
     * @native ts
     */
    public final void cacheLock() {
        while (!unsafe.compareAndSwapInt(this, _lockOffset, 0, 1)) ;
    }

    /**
     * @native ts
     */
    public final void cacheUnlock() {
        _lock = 0;
    }

    /**
     * This method should be overridden to init the object
     */
    public void init() {
        //noop
    }

    @Override
    public final String nodeTypeName() {
        final NodeDeclaration declaration = this.graph().nodeRegistry().declarationByHash(this._resolver.typeCode(this));
        if (declaration != null) {
            return declaration.name();
        }
        return null;
    }

    protected final NodeState unphasedState() {
        return this._resolver.resolveState(this);
    }

    protected final NodeState phasedState() {
        return this._resolver.alignState(this);
    }

    protected final NodeState newState(long relativeTime) {
        return this._resolver.newState(this, _world, relativeTime);
    }

    @Override
    public final Graph graph() {
        return _graph;
    }

    @Override
    public final long world() {
        return this._world;
    }

    @Override
    public final long time() {
        return this._time;
    }

    @Override
    public final long id() {
        return this._id;
    }

    @Override
    public Object get(String name) {
        return this.getAt(this._resolver.stringToHash(name, false));
    }

    @Override
    public Object getAt(int propIndex) {
        final NodeState resolved = this._resolver.resolveState(this);
        if (resolved != null) {
            return proxyIfNecessary(resolved, propIndex, resolved.getAt(propIndex));
        }
        return null;
    }

    @Override
    public final Object getRawAt(int propIndex) {
        final NodeState resolved = this._resolver.resolveState(this);
        if (resolved != null) {
            return resolved.getAt(propIndex);
        }
        return null;
    }

    @Override
    public final Object getTypedRawAt(final int propIndex, final byte type) {
        final NodeState resolved = this._resolver.resolveState(this);
        if (resolved != null) {
            return resolved.getTypedRawAt(propIndex, type);
        }
        return null;
    }

    private Object proxyIfNecessary(NodeState state, int index, Object elem) {
        long resolvedTime = state.time();
        long resolvedWorld = state.world();
        if (resolvedTime == _time && resolvedWorld == _world) { //implement time sensitivity
            return elem;
        } else {
            byte type = state.typeAt(index);
            //temporary proxy
            switch (type) {
                case Type.LMATRIX:
                    return new LMatrixProxy(index, this, (LMatrix) elem);
                case Type.DMATRIX:
                    return new DMatrixProxy(index, this, (DMatrix) elem);
                case Type.RELATION:
                    return new RelationProxy(index, this, (Relation) elem);
                case Type.RELATION_INDEXED:
                    return new RelationIndexedProxy(index, this, (RelationIndexed) elem);
                case Type.EGRAPH:
                    return new EGraphProxy(index, this, (EGraph) elem);
                case Type.KDTREE:
                    return new TreeProxy(index, this, (Tree) elem);
                case Type.NDTREE:
                    return new ProfileProxy(index, this, (Profile) elem);
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
                case Type.BOOL_ARRAY:
                    return new BoolArrayProxy(index,this,(BoolArray)elem);
                case Type.INT_TO_INT_MAP:
                    return new IntIntMapProxy(index, this, (IntIntMap) elem);
                case Type.INT_TO_STRING_MAP:
                    return new IntStringMapProxy(index, this, (IntStringMap) elem);
                default:
                    return elem;
            }
        }
    }

    @Override
    public final Object getOrCreate(String name, byte type) {
        return this.getOrCreateAt(this._resolver.stringToHash(name, true), type);
    }

    @Override
    public Object getOrCreateAt(int index, byte type) {
        final NodeState previousState = this._resolver.resolveState(this);
        final Object elem = previousState.getAt(index);
        if (elem != null) {
            return proxyIfNecessary(previousState, index, elem);
        } else {
            final NodeState preciseState = this._resolver.resolveState(this);
            if (preciseState != null) {
                return proxyIfNecessary(preciseState, index, preciseState.getOrCreateAt(index, type));
            } else {
                throw new RuntimeException(Constants.CACHE_MISS_ERROR);
            }
        }
    }

    @Override
    public <A> A getWithDefault(final String key, final A defaultValue) {
        return getAtWithDefault(this._resolver.stringToHash(key, false), defaultValue);
    }

    @Override
    public <A> A getAtWithDefault(final int key, final A defaultValue) {
        Object found = getAt(key);
        if (found != null) {
            return (A) found;
        } else {
            return defaultValue;
        }
    }

    @Override
    public Node forceSet(String name, byte type, Object value) {
        return forceSetAt(this._resolver.stringToHash(name, true), type, value);
    }

    @Override
    public Node forceSetAt(int index, byte type, Object value) {
        final NodeState preciseState = this._resolver.alignState(this);
        if (preciseState != null) {
            preciseState.setAt(index, type, value);
        } else {
            throw new RuntimeException(Constants.CACHE_MISS_ERROR);
        }
        return this;
    }

    @Override
    public Node setAt(int index, byte type, Object value) {
        final NodeState unPhasedState = this._resolver.resolveState(this);
        boolean isDiff = (type != unPhasedState.typeAt(index));
        if (!isDiff) {
            isDiff = !isEquals(unPhasedState.getAt(index), value, type);
        }
        if (isDiff) {
            final NodeState preciseState = this._resolver.alignState(this);
            if (preciseState != null) {
                preciseState.setAt(index, type, value);
            } else {
                throw new RuntimeException(Constants.CACHE_MISS_ERROR);
            }
        }
        return this;
    }

    @Override
    public Node set(String name, byte type, Object value) {
        //hash the property a single time
        final int hashed = this._resolver.stringToHash(name, true);
        return setAt(hashed, type, value);
    }

    private boolean isEquals(Object obj1, Object obj2, byte type) {
        if (obj1 == null && obj2 == null) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }
        switch (type) {
            case Type.BOOL:
                return (((boolean) obj1) == ((boolean) obj2));
            case Type.DOUBLE:
                return (((double) obj1) == ((double) obj2));
            case Type.INT:
                return (((int) obj1) == ((int) obj2));
            case Type.LONG:
                return (((long) obj1) == ((long) obj2));
            case Type.STRING:
                return (((String) obj1).equals((String) obj2));
            case Type.DOUBLE_ARRAY:
                DoubleArray obj1_ar_d = (DoubleArray) obj1;
                DoubleArray obj2_ar_d = (DoubleArray) obj2;
                if (obj1_ar_d.size() != obj2_ar_d.size()) {
                    return false;
                } else {
                    for (int i = 0; i < obj1_ar_d.size(); i++) {
                        if (obj1_ar_d.get(i) != obj2_ar_d.get(i)) {
                            return false;
                        }
                    }
                }
                return true;
            case Type.INT_ARRAY:
                IntArray obj1_ar_i = (IntArray) obj1;
                IntArray obj2_ar_i = (IntArray) obj2;
                if (obj1_ar_i.size() != obj2_ar_i.size()) {
                    return false;
                } else {
                    for (int i = 0; i < obj1_ar_i.size(); i++) {
                        if (obj1_ar_i.get(i) != obj2_ar_i.get(i)) {
                            return false;
                        }
                    }
                }
                return true;
            case Type.LONG_ARRAY:
                LongArray obj1_ar_l = (LongArray) obj1;
                LongArray obj2_ar_l = (LongArray) obj2;
                if (obj1_ar_l.size() != obj2_ar_l.size()) {
                    return false;
                } else {
                    for (int i = 0; i < obj1_ar_l.size(); i++) {
                        if (obj1_ar_l.get(i) != obj2_ar_l.get(i)) {
                            return false;
                        }
                    }
                }
                return true;
                /*
            case Type.STRING_ARRAY:
              /*  StringArray obj1_ar_s = (StringArray) obj1;
                StringArray obj2_ar_s = (StringArray) obj2;
                if (obj1_ar_s.size() != obj2_ar_s.size()) {
                    return false;
                } else {
                    for (int i = 0; i < obj1_ar_s.size(); i++) {
                        if (!obj1_ar_s.get(i).equals(obj2_ar_s.get(i))) {
                            return false;
                        }
                    }
                }
                return true;
                */
            case Type.BOOL_ARRAY:
                BoolArray bObj1 = (BoolArray) obj1;
                BoolArray bObj2 = (BoolArray) obj2;
                if(bObj1.size() != bObj2.size()) {
                    return false;
                }
                for (int i = 0; i < bObj1.size(); i++) {
                    if (bObj1.get(i) != (bObj2.get(i))) {
                        return false;
                    }
                }
                return true;
            case Type.RELATION:
            case Type.RELATION_INDEXED:
            case Type.STRING_TO_INT_MAP:
            case Type.LONG_TO_LONG_MAP:
            case Type.LONG_TO_LONG_ARRAY_MAP:
            case Type.INT_TO_INT_MAP:
            case Type.INT_TO_STRING_MAP:
                throw new RuntimeException("Bad API usage: set can't be used with complex type, please use getOrCreate instead.");
            default:
                throw new RuntimeException("Not managed type " + type);
        }
    }

    @Override
    public byte type(String name) {
        final NodeState resolved = this._resolver.resolveState(this);
        if (resolved != null) {
            return resolved.typeAt(this._resolver.stringToHash(name, false));
        }
        return -1;
    }

    @Override
    public byte typeAt(final int index) {
        final NodeState resolved = this._resolver.resolveState(this);
        if (resolved != null) {
            return resolved.typeAt(index);
        }
        return -1;
    }

    @Override
    public final Node remove(String name) {
        return set(name, Type.INT, null);
    }

    @Override
    public final Node removeAt(final int index) {
        return setAt(index, Type.INT, null);
    }

    @Override
    public final void relation(String relationName, final Callback<Node[]> callback) {
        relationAt(this._resolver.stringToHash(relationName, false), callback);
    }

    @Override
    public void relationAt(int relationIndex, Callback<Node[]> callback) {
        if (callback == null) {
            return;
        }
        final NodeState resolved = this._resolver.resolveState(this);
        if (resolved != null) {
            switch (resolved.typeAt(relationIndex)) {
                case Type.RELATION:
                    final Relation relation = (Relation) resolved.getAt(relationIndex);
                    if (relation == null || relation.size() == 0) {
                        callback.on(new Node[0]);
                    } else {
                        final int relSize = relation.size();
                        final long[] ids = new long[relSize];
                        for (int i = 0; i < relSize; i++) {
                            ids[i] = relation.get(i);
                        }
                        this._resolver.lookupAll(_world, _time, ids, new Callback<Node[]>() {
                            @Override
                            public void on(Node[] result) {
                                callback.on(result);
                            }
                        });
                    }
                    break;
                case Type.RELATION_INDEXED:
                    final RelationIndexed relation_indexed = (RelationIndexed) resolved.getAt(relationIndex);
                    if (relation_indexed == null || relation_indexed.size() == 0) {
                        callback.on(new Node[0]);
                    } else {
                        this._resolver.lookupAll(_world, _time, relation_indexed.all(), new Callback<Node[]>() {
                            @Override
                            public void on(Node[] result) {
                                callback.on(result);
                            }
                        });
                    }
                    break;
                default:
                    callback.on(new Node[0]);
                    break;
            }
        } else {
            callback.on(new Node[0]);
        }
    }

    @Override
    public final Node addToRelation(String relationName, Node relatedNode, String... attributes) {
        return addToRelationAt(this._resolver.stringToHash(relationName, true), relatedNode, attributes);
    }

    @Override
    public Node addToRelationAt(int relationIndex, Node relatedNode, String... attributes) {
        if (relatedNode != null) {
            NodeState preciseState = this._resolver.alignState(this);
            if (preciseState != null) {
                boolean attributesNotEmpty = (attributes != null && attributes.length > 0);
                if (attributesNotEmpty) {
                    RelationIndexed indexedRel = (RelationIndexed) preciseState.getOrCreateAt(relationIndex, Type.RELATION_INDEXED);
                    indexedRel.add(relatedNode, attributes);
                } else {
                    Relation relationArray = (Relation) preciseState.getOrCreateAt(relationIndex, Type.RELATION);
                    relationArray.add(relatedNode.id());
                }
            } else {
                throw new RuntimeException(Constants.CACHE_MISS_ERROR);
            }
        }
        return this;
    }

    @Override
    public final Node removeFromRelation(String relationName, Node relatedNode, String... attributes) {
        return removeFromRelationAt(this._resolver.stringToHash(relationName, false), relatedNode, attributes);
    }

    @Override
    public Node removeFromRelationAt(int relationIndex, Node relatedNode, String... attributes) {
        if (relatedNode != null) {
            final NodeState preciseState = this._resolver.alignState(this);
            if (preciseState != null) {
                boolean attributesNotEmpty = (attributes != null && attributes.length > 0);
                if (attributesNotEmpty) {
                    RelationIndexed indexedRel = (RelationIndexed) preciseState.getOrCreateAt(relationIndex, Type.RELATION_INDEXED);
                    indexedRel.remove(relatedNode, attributes);
                } else {
                    Relation relationArray = (Relation) preciseState.getOrCreateAt(relationIndex, Type.RELATION);
                    relationArray.remove(relatedNode.id());
                }
            } else {
                throw new RuntimeException(Constants.CACHE_MISS_ERROR);
            }
        }
        return this;
    }

    @Override
    public final void free() {
        this._resolver.freeNode(this);
    }

    @Override
    public final long timeDephasing() {
        final NodeState state = this._resolver.resolveState(this);
        if (state != null) {
            return this._time - state.time();
        } else {
            throw new RuntimeException(Constants.CACHE_MISS_ERROR);
        }
    }

    @Override
    public final long lastModification() {
        final NodeState state = this._resolver.resolveState(this);
        if (state != null) {
            return state.time();
        } else {
            throw new RuntimeException(Constants.CACHE_MISS_ERROR);
        }
    }

    @Override
    public final Node rephase() {
        this._resolver.alignState(this);
        return this;
    }

    @Override
    public final void timepoints(final long beginningOfSearch, final long endOfSearch, final Callback<long[]> callback) {
        this._resolver.resolveTimepoints(this, beginningOfSearch, endOfSearch, callback);
    }

    @Override
    public final <A extends Node> void travelInTime(final long targetTime, final Callback<A> callback) {
        _resolver.lookup(_world, targetTime, _id, callback);
    }

    @Override
    public <A extends Node> void travelInWorld(final long targetWorld, final Callback<A> callback) {
        _resolver.lookup(targetWorld, _time, _id, callback);
    }

    @Override
    public <A extends Node> void travel(long targetWorld, long targetTime, Callback<A> callback) {
        _resolver.lookup(targetWorld, targetTime, _id, callback);
    }

    @Override
    public final Node setTimeSensitivity(long deltaTime, long offset) {
        _resolver.setTimeSensitivity(this, deltaTime, offset);
        return this;
    }

    @Override
    public final Tuple<Long, Long> timeSensitivity() {
        return _resolver.getTimeSensitivity(this);
    }

    @Override
    public final void end() {
        _resolver.end(this);
    }

    @Override
    public String toString() {
        if (_lock == 1) {
            return "locked";
        }
        final StringBuilder builder = new StringBuilder();
        final boolean[] isFirst = {true};
        builder.append("{\"world\":");
        builder.append(world());
        builder.append(",\"time\":");
        builder.append(time());
        builder.append(",\"id\":");
        builder.append(id());
        final NodeState state = this._resolver.resolveState(this);
        if (state != null) {
            state.each(new NodeStateCallback() {
                @Override
                public void on(int attributeKey, byte elemType, Object elem) {
                    if (elem != null) {
                        String resolveName = _resolver.hashToString(attributeKey);
                        if (resolveName == null) {
                            resolveName = attributeKey + "";
                        }
                        switch (elemType) {
                            case Type.BOOL: {
                                builder.append(",\"");
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
                                builder.append(",\"");
                                builder.append(resolveName);
                                builder.append("\":");
                                builder.append("\"");
                                builder.append(elem);
                                builder.append("\"");
                                break;
                            }
                            case Type.LONG: {
                                builder.append(",\"");
                                builder.append(resolveName);
                                builder.append("\":");
                                builder.append(elem);
                                break;
                            }
                            case Type.INT: {
                                builder.append(",\"");
                                builder.append(resolveName);
                                builder.append("\":");
                                builder.append(elem);
                                break;
                            }
                            case Type.DOUBLE: {
                                if (!Constants.isNaN((double) elem)) {
                                    builder.append(",\"");
                                    builder.append(resolveName);
                                    builder.append("\":");
                                    builder.append(elem);
                                }
                                break;
                            }
                            case Type.DOUBLE_ARRAY: {
                                builder.append(",\"");
                                builder.append(resolveName);
                                builder.append("\":");
                                builder.append("[");
                                DoubleArray castedArr = ((DoubleArray) elem);
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
                                builder.append(",\"");
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
                            case Type.LONG_ARRAY: {
                                builder.append(",\"");
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
                                builder.append(",\"");
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
                            case Type.BOOL_ARRAY: {
                                builder.append(",\"");
                                builder.append(resolveName);
                                builder.append("\":");
                                builder.append("[");
                                BoolArray castedArr3 = (BoolArray) elem;
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
                                builder.append(",\"");
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
                                builder.append(",\"");
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
                                builder.append(",\"");
                                builder.append(resolveName);
                                builder.append("\":");
                                builder.append("{");
                                IntStringMap castedMapI2I = (IntStringMap) elem;
                                isFirst[0] = true;
                                castedMapI2I.each(new IntStringMapCallBack() {
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
                            case Type.RELATION_INDEXED:
                            case Type.LONG_TO_LONG_ARRAY_MAP: {
                                builder.append(",\"");
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
                                for (int i = 0; i < flatKeys.length; i++) {
                                    long[] values = castedMapL2LA.get(flatKeys[i]);
                                    if (!isFirst[0]) {
                                        builder.append(",");
                                    } else {
                                        isFirst[0] = false;
                                    }
                                    builder.append("\"");
                                    builder.append(flatKeys[i]);
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
                                builder.append(",\"");
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
            });
            builder.append("}");
        }
        return builder.toString();
    }

    @Override
    public final Relation getRelation(String name) {
        return (Relation) get(name);
    }

    @Override
    public final RelationIndexed getRelationIndexed(String name) {
        return (RelationIndexed) get(name);
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
    public final EGraph getEGraph(String name) {
        return (EGraph) get(name);
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
    public final IntIntMap getIntIntMap(String name) {
        return (IntIntMap) get(name);
    }

    @Override
    public final IntStringMap getIntStringMap(String name){ return (IntStringMap) get(name);}

    @Override
    public final LongLongArrayMap getLongLongArrayMap(String name) {
        return (LongLongArrayMap) get(name);
    }

    public final Node createClone() {
        final Node cloned = _graph.newNode(_world, _time);
        final StateChunk clonedStateChunk = (StateChunk) _resolver.resolveState(cloned);
        final StateChunk currentStateChunk = (StateChunk) _resolver.resolveState(this);
        clonedStateChunk.loadFrom(currentStateChunk);
        return cloned;
    }

}
