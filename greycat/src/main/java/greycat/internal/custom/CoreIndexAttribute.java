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
package greycat.internal.custom;

import greycat.*;
import greycat.base.BaseCustomTypeSingle;
import greycat.base.BaseNode;
import greycat.plugin.NodeState;
import greycat.struct.*;
import greycat.utility.HashHelper;

public class CoreIndexAttribute extends BaseCustomTypeSingle implements Index {

    private static final int P_MAP = 0;
    private static final int R_MAP = 1;
    private static final int HASHES = 2;

    public static final String NAME = "INDEX";

    public CoreIndexAttribute(final EStructArray p_backend) {
        super(p_backend);
    }

    @Override
    public final void declareAttributes(Callback callback, String... attributeNames) {
        getOrCreateAt(P_MAP, Type.LONG_TO_LONG_ARRAY_MAP);
        getOrCreateAt(R_MAP, Type.LONG_TO_LONG_MAP);
        final String[] casted = attributeNames;
        final IntArray hashes = (IntArray) getOrCreateAt(HASHES, Type.INT_ARRAY);
        hashes.init(casted.length);
        for (int i = 0; i < casted.length; i++) {
            hashes.set(i, HashHelper.hash(casted[i]));
        }
        if (callback != null) {
            callback.on(this);
        }
    }

    @Override
    public final int size() {
        return ((LongLongArrayMap) getAt(P_MAP)).size();
    }

    @Override
    public final long[] all() {
        final LongLongArrayMap l_map = (LongLongArrayMap) getAt(P_MAP);
        if (l_map == null) {
            return new long[0];
        } else {
            long[] flat = new long[l_map.size()];
            final int[] i = {0};
            l_map.each(new LongLongArrayMapCallBack() {
                @Override
                public void on(long key, long value) {
                    flat[i[0]] = value;
                    i[0]++;
                }
            });
            return flat;
        }
    }

    @Override
    public final Index update(final Node node) {
        final LongLongArrayMap relationIndexed = (LongLongArrayMap) getAt(P_MAP);
        final LongLongMap reverseMap = (LongLongMap) getAt(R_MAP);
        final IntArray hashes = (IntArray) getAt(HASHES);
        final Query flatQuery = node.graph().newQuery();
        final NodeState toIndexNodeState = node.graph().resolver().resolveState(node);
        for (int i = 0; i < hashes.size(); i++) {
            final int hash = hashes.get(i);
            final Object attValue = toIndexNodeState.getAt(hash);
            if (attValue != null) {
                flatQuery.addRaw(hash, attValue.toString());
            } else {
                flatQuery.addRaw(hash, null);
            }
        }
        final long newHash = flatQuery.hash();
        final long prevHash = reverseMap.get(node.id());
        relationIndexed.delete(prevHash, node.id());
        relationIndexed.put(newHash, node.id());
        reverseMap.put(node.id(), newHash);
        return this;
    }

    @Override
    public final Index unindex(final Node node) {
        final LongLongArrayMap relationIndexed = (LongLongArrayMap) getAt(P_MAP);
        final LongLongMap reverseMap = (LongLongMap) getAt(R_MAP);
        final long prevHash = reverseMap.get(node.id());
        relationIndexed.delete(prevHash, node.id());
        reverseMap.remove(node.id());
        return this;
    }

    @Override
    public final Index clear() {
        setAt(P_MAP, Type.LONG_TO_LONG_ARRAY_MAP, null);
        setAt(R_MAP, Type.LONG_TO_LONG_MAP, null);
        return this;
    }

    @Override
    public final void find(Callback<Node[]> callback, long world, long time, String... params) {
        if (params == null || params.length == 0) {
            _backend.graph().lookupAll(world, time, all(), callback);
        } else {
            final IntArray hashes = (IntArray) getAt(HASHES);
            if (hashes == null) {
                if (callback != null) {
                    callback.on(new Node[0]);
                }
            } else {
                if (hashes.size() != params.length) {
                    throw new RuntimeException("Bad API usage: number of parameters in the query differs from index declaration. Expected " + hashes.size() + " parameters, received " + params.length);
                }
                Query queryObj = _backend.graph().newQuery();
                queryObj.setWorld(world);
                queryObj.setTime(time);
                for (int i = 0; i < hashes.size(); i++) {
                    queryObj.addRaw(hashes.get(i), params[i]);
                }
                findByQuery(queryObj, callback);
            }
        }
    }

    @Override
    public final void findByQuery(Query query, Callback<Node[]> callback) {
        final LongLongArrayMap relationIndexed = (LongLongArrayMap) getAt(P_MAP);
        final Graph g = _backend.graph();
        final long[] foundIds = relationIndexed.get(query.hash());
        if (foundIds == null) {
            callback.on(new BaseNode[0]);
        } else {
            g.resolver().lookupAll(query.world(), query.time(), foundIds, new Callback<Node[]>() {
                @Override
                public void on(Node[] resolved) {
                    //select
                    Node[] resultSet = new BaseNode[foundIds.length];
                    int resultSetIndex = 0;
                    for (int i = 0; i < resultSet.length; i++) {
                        final Node resolvedNode = resolved[i];
                        if (resolvedNode != null) {
                            final NodeState resolvedState = g.resolver().resolveState(resolvedNode);
                            boolean exact = true;
                            for (int j = 0; j < query.attributes().length; j++) {
                                Object obj = resolvedState.getAt(query.attributes()[j]);
                                if (query.values()[j] == null) {
                                    if (obj != null) {
                                        exact = false;
                                        break;
                                    }
                                } else {
                                    if (obj == null) {
                                        exact = false;
                                        break;
                                    } else {
                                        if (obj instanceof long[]) {
                                            if (query.values()[j] instanceof long[]) {
                                                if (!Constants.longArrayEquals((long[]) query.values()[j], (long[]) obj)) {
                                                    exact = false;
                                                    break;
                                                }
                                            } else {
                                                exact = false;
                                                break;
                                            }
                                        } else {
                                            if (!Constants.equals(query.values()[j].toString(), obj.toString())) {
                                                exact = false;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            if (exact) {
                                resultSet[resultSetIndex] = resolvedNode;
                                resultSetIndex++;
                            }
                        }
                    }
                    if (resultSet.length == resultSetIndex) {
                        callback.on(resultSet);
                    } else {
                        Node[] trimmedResultSet = new BaseNode[resultSetIndex];
                        System.arraycopy(resultSet, 0, trimmedResultSet, 0, resultSetIndex);
                        callback.on(trimmedResultSet);
                    }
                }
            });
        }
    }

    @Override
    public final long[] select(String... params) {
        final IntArray hashes = (IntArray) getAt(HASHES);
        final LongLongArrayMap relationIndexed = (LongLongArrayMap) getAt(P_MAP);
        if (params.length != hashes.size()) {
            throw new RuntimeException("Bad API usage: number of parameters in the select differs from index declaration. Expected " + hashes.size() + " parameters, received " + params.length);
        }
        final Query queryObj = _backend.graph().newQuery();
        for (int i = 0; i < params.length; i++) {
            queryObj.addRaw(hashes.get(i), params[i]);
        }
        long hash = queryObj.hash();
        return relationIndexed.get(hash);
    }

    @Override
    public final long[] selectByQuery(Query query) {
        final LongLongArrayMap relationIndexed = (LongLongArrayMap) getAt(P_MAP);
        return relationIndexed.get(query.hash());
    }
}
