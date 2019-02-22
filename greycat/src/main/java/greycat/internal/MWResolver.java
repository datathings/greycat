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
package greycat.internal;

import greycat.*;
import greycat.chunk.*;
import greycat.plugin.*;
import greycat.struct.*;
import greycat.utility.*;
import greycat.base.BaseNode;

@SuppressWarnings("Duplicates")
final class MWResolver implements Resolver {

    private final Storage _storage;

    private final ChunkSpace _space;

    private final Graph _graph;

    private StateChunk dictionary;

    private WorldOrderChunk globalWorldOrderChunk;

    private static int KEY_SIZE = 3;

    private static double max_load_keys = 3000.0;

    public MWResolver(final Storage p_storage, final ChunkSpace p_space, final Graph p_graph) {
        _space = p_space;
        _storage = p_storage;
        _graph = p_graph;
    }

    @Override
    public final void init() {
        dictionary = (StateChunk) this._space.getAndMark(ChunkType.STATE_CHUNK, CoreConstants.GLOBAL_DICTIONARY_KEY[0], CoreConstants.GLOBAL_DICTIONARY_KEY[1], CoreConstants.GLOBAL_DICTIONARY_KEY[2]);
        globalWorldOrderChunk = (WorldOrderChunk) this._space.getAndMark(ChunkType.WORLD_ORDER_CHUNK, 0, 0, Constants.NULL_LONG);
    }

    @Override
    public final void free() {
        if (dictionary != null) {
            _space.free(dictionary);
        }
        if (globalWorldOrderChunk != null) {
            _space.free(globalWorldOrderChunk);
        }
    }

    @Override
    public final int typeCode(Node node) {
        final BaseNode casted = (BaseNode) node;
        final WorldOrderChunk worldOrderChunk = (WorldOrderChunk) this._space.get(casted._index_worldOrder);
        if (worldOrderChunk == null) {
            return -1;
        }
        return (int) worldOrderChunk.type();
    }

    private static long NodeValueType = -1834056593;

    @Override
    public final void initNode(final Node node, final long codeType) {
        final BaseNode casted = (BaseNode) node;
        //initiate universe management
        final WorldOrderChunk objectWorldOrder = (WorldOrderChunk) this._space.createAndMark(ChunkType.WORLD_ORDER_CHUNK, 0, 0, node.id());
        objectWorldOrder.put(node.world(), node.time());
        //initialize type
        if (codeType != Constants.NULL_LONG) {
            objectWorldOrder.setType(codeType);
        }
        final long time = node.time();
        //initiate superTime management
        final SuperTimeTreeChunk superTimeTree = (SuperTimeTreeChunk) this._space.createAndMark(ChunkType.SUPER_TIME_TREE_CHUNK, node.world(), 0, node.id());
        //initiate time management
        if (codeType == NodeValueType) { // HashHelper.hash(CoreNodeValue.NAME)
            final TimeTreeDValueChunk timeTree = (TimeTreeDValueChunk) this._space.createAndMark(ChunkType.TIME_TREE_DVALUE_CHUNK, node.world(), time, node.id());
            timeTree.insert(time);
            final long subTreeCapacity = superTimeTree.subTreeCapacity();
            superTimeTree.insert(time, subTreeCapacity);
            timeTree.setCapacity(subTreeCapacity);
            casted._index_timeTree = timeTree.index();
            casted._index_stateChunk = -1;
            casted._index_timeTree_offset = 0;
        } else {
            final TimeTreeChunk timeTree = (TimeTreeChunk) this._space.createAndMark(ChunkType.TIME_TREE_CHUNK, node.world(), time, node.id());
            timeTree.insert(time);
            final long subTreeCapacity = superTimeTree.subTreeCapacity();
            superTimeTree.insert(time, subTreeCapacity);
            timeTree.setCapacity(subTreeCapacity);
            final StateChunk cacheEntry = (StateChunk) this._space.createAndMark(ChunkType.STATE_CHUNK, node.world(), time, node.id());
            //declare dirty now because potentially no insert will occur
            this._space.notifyUpdate(cacheEntry.index());
            //initialize local pointer
            casted._index_stateChunk = cacheEntry.index();
            casted._index_timeTree = timeTree.index();
        }

        casted._index_superTimeTree = superTimeTree.index();
        casted._index_worldOrder = objectWorldOrder.index();
        casted._world_magic = -1;
        casted._super_time_magic = -1;
        casted._time_magic = -1;
        casted.init();
    }

    @Override
    public final void initWorld(final long parentWorld, final long childWorld) {
        globalWorldOrderChunk.put(childWorld, parentWorld);
    }

    @Override
    public final void freeNode(final Node node) {
        final BaseNode casted = (BaseNode) node;
        casted.cacheLock();
        if (!casted._dead) {
            if (casted._index_stateChunk != -1) {
                this._space.unmark(casted._index_stateChunk);
            }
            this._space.unmark(casted._index_timeTree);
            this._space.unmark(casted._index_superTimeTree);
            this._space.unmark(casted._index_worldOrder);
            casted._dead = true;
        }
        casted.cacheUnlock();
    }

    @Override
    public final void externalLock(Node node) {
        final BaseNode casted = (BaseNode) node;
        final WorldOrderChunk worldOrderChunk = (WorldOrderChunk) this._space.get(casted._index_worldOrder);
        worldOrderChunk.externalLock();
    }

    @Override
    public final void externalUnlock(final Node node) {
        final BaseNode casted = (BaseNode) node;
        final WorldOrderChunk worldOrderChunk = (WorldOrderChunk) this._space.get(casted._index_worldOrder);
        worldOrderChunk.externalUnlock();
    }

    @Override
    public final void setTimeSensitivity(final Node node, final long deltaTime, final long offset) {
        final BaseNode casted = (BaseNode) node;
        final SuperTimeTreeChunk superTimeTree = (SuperTimeTreeChunk) this._space.get(casted._index_superTimeTree);
        superTimeTree.setTimeSensitivity(deltaTime);
        superTimeTree.setTimeSensitivityOffset(offset);
    }

    @Override
    public final Tuple<Long, Long> getTimeSensitivity(final Node node) {
        final BaseNode casted = (BaseNode) node;
        final SuperTimeTreeChunk superTimeTree = (SuperTimeTreeChunk) this._space.get(casted._index_superTimeTree);
        return new Tuple<Long, Long>(superTimeTree.timeSensitivity(), superTimeTree.timeSensitivityOffset());
    }

    @Override
    public final void drop(final Node target, final Callback callback) {
        final BaseNode castedNode = (BaseNode) target;
        castedNode.cacheLock();
        if (castedNode._dead) {
            castedNode.cacheUnlock();
            throw new RuntimeException(CoreConstants.DEAD_NODE_ERROR + " node id: " + castedNode.id());
        }
        final WorldOrderChunk worldOrderChunk = (WorldOrderChunk) this._space.get(castedNode._index_worldOrder);
        final SuperTimeTreeChunk superTimeTreeChunk = (SuperTimeTreeChunk) this._space.get(castedNode._index_superTimeTree);
        final TimeTreeChunk timeTreeChunk = (TimeTreeChunk) this._space.get(castedNode._index_timeTree);
        final StateChunk stateChunk = (StateChunk) this._space.get(castedNode._index_stateChunk);
        final long[] superTimeTreeKeys = new long[worldOrderChunk.size() * Constants.KEY_SIZE];
        final int[] cursor = {0};
        worldOrderChunk.each(new LongLongMapCallBack() {
            @Override
            public void on(final long worldKey, final long value) {
                superTimeTreeKeys[cursor[0] * Constants.KEY_SIZE] = ChunkType.SUPER_TIME_TREE_CHUNK;
                superTimeTreeKeys[(cursor[0] * Constants.KEY_SIZE) + 1] = worldKey;
                superTimeTreeKeys[(cursor[0] * Constants.KEY_SIZE) + 2] = 0;
                superTimeTreeKeys[(cursor[0] * Constants.KEY_SIZE) + 3] = target.id();
                cursor[0]++;
            }
        });
        _space.getOrLoadAndMarkAll(superTimeTreeKeys, new Callback<Chunk[]>() {
            @Override
            public void on(final Chunk[] superTimeTrees) {
                int sum = 0;
                for (int i = 0; i < superTimeTrees.length; i++) {
                    final SuperTimeTreeChunk stt = (SuperTimeTreeChunk) superTimeTrees[i];
                    sum = sum + stt.size();
                }
                final long[] timeTreeKeys = new long[sum * Constants.KEY_SIZE];
                for (int i = 0; i < superTimeTrees.length; i++) {
                    final SuperTimeTreeChunk stt = (SuperTimeTreeChunk) superTimeTrees[i];
                    cursor[0] = 0;
                    stt.range(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, stt.size(), new SuperTreeWalker() {
                        @Override
                        public void elem(long time, long capacity) {
                            timeTreeKeys[cursor[0] * Constants.KEY_SIZE] = ChunkType.TIME_TREE_CHUNK;
                            timeTreeKeys[(cursor[0] * Constants.KEY_SIZE) + 1] = stt.world();
                            timeTreeKeys[(cursor[0] * Constants.KEY_SIZE) + 2] = time;
                            timeTreeKeys[(cursor[0] * Constants.KEY_SIZE) + 3] = target.id();
                            cursor[0]++;
                        }
                    });
                }
                _space.getOrLoadAndMarkAll(timeTreeKeys, new Callback<Chunk[]>() {
                    @Override
                    public void on(final Chunk[] timeTrees) {
                        _space.unmark(worldOrderChunk.index());
                        _space.unmark(superTimeTreeChunk.index());
                        _space.unmark(timeTreeChunk.index());
                        _space.unmark(stateChunk.index());

                        final Buffer toDelete = _graph.newBuffer();
                        KeyHelper.keyToBuffer(toDelete, ChunkType.WORLD_ORDER_CHUNK, 0, 0, castedNode.id());
                        _space.delete(ChunkType.WORLD_ORDER_CHUNK, 0, 0, castedNode.id());
                        for (int i = 0; i < superTimeTrees.length; i++) {
                            toDelete.write(Constants.BUFFER_SEP);
                            final SuperTimeTreeChunk stt = (SuperTimeTreeChunk) superTimeTrees[i];
                            KeyHelper.keyToBuffer(toDelete, ChunkType.SUPER_TIME_TREE_CHUNK, stt.world(), 0, castedNode.id());
                            _space.unmark(stt.index());
                            _space.delete(ChunkType.SUPER_TIME_TREE_CHUNK, stt.world(), 0, castedNode.id());
                        }
                        for (int i = 0; i < timeTrees.length; i++) {
                            final TimeTreeChunk tt = (TimeTreeChunk) timeTrees[i];
                            tt.range(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, tt.size(), new TreeWalker() {
                                @Override
                                public void elem(long time) {
                                    toDelete.write(Constants.BUFFER_SEP);
                                    KeyHelper.keyToBuffer(toDelete, ChunkType.STATE_CHUNK, tt.world(), time, castedNode.id());
                                    _space.delete(ChunkType.STATE_CHUNK, tt.world(), time, castedNode.id());
                                }
                            });
                            toDelete.write(Constants.BUFFER_SEP);
                            KeyHelper.keyToBuffer(toDelete, ChunkType.TIME_TREE_CHUNK, tt.world(), tt.time(), castedNode.id());
                            _space.unmark(tt.index());
                            _space.delete(ChunkType.TIME_TREE_CHUNK, tt.world(), tt.time(), castedNode.id());
                        }
                        _storage.remove(toDelete, new Callback<Boolean>() {
                            @Override
                            public void on(Boolean result) {
                                toDelete.free();
                                castedNode.cacheUnlock();
                                if (callback != null) {
                                    callback.on(result);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void batchInsert(final Node target, final long[] times, final double[] values) {
        if (times.length != values.length) {
            throw new RuntimeException("Bad API usage, times and values array should have the same size");
        }
        final BaseNode castedNode = (BaseNode) target;
        castedNode.cacheLock();
        if (castedNode._dead) {
            castedNode.cacheUnlock();
            throw new RuntimeException(CoreConstants.DEAD_NODE_ERROR + " node id: " + castedNode.id());
        }
        final WorldOrderChunk worldOrderChunk = (WorldOrderChunk) this._space.get(castedNode._index_worldOrder);
        if (worldOrderChunk.type() != NodeValueType) {
            castedNode.cacheUnlock();
            throw new RuntimeException("Bad API usage, batch insert only valid for NodeValue");
        }
        worldOrderChunk.lock();
        //compute time sensitivity
        final SuperTimeTreeChunk superTimeTree = (SuperTimeTreeChunk) this._space.get(castedNode._index_superTimeTree);


        final long timeSensitivity = superTimeTree.timeSensitivity();


        /*
        if (timeSensitivity != 0 && timeSensitivity != Constants.NULL_LONG) {
            if (timeSensitivity < 0) {
                nodeTime = previousTime;
            } else {
                long timeSensitivityOffset = superTimeTree.timeSensitivityOffset();
                if (timeSensitivityOffset == Constants.NULL_LONG) {
                    timeSensitivityOffset = 0;
                }
                nodeTime = nodeTime - (nodeTime % timeSensitivity) + timeSensitivityOffset;
            }
        }*/

        /*
        StateChunk clonedState = null;
        if (castedNode._index_stateChunk != -1) {
            if (nodeTime != previousTime || nodeWorld != previousWorld) {
                try {
                    clonedState = (StateChunk) this._space.createAndMark(ChunkType.STATE_CHUNK, nodeWorld, nodeTime, nodeId);
                    clonedState.loadFrom(previouStateChunk);
                } catch (Exception e) {
                    nodeWorldOrder.unlock();
                    castedNode.cacheUnlock();
                    throw e;
                }
                castedNode._index_stateChunk = clonedState.index();
                _space.unmark(previouStateChunk.index());
            } else {
                clonedState = previouStateChunk;
            }
        }
        castedNode._world_magic = -1;
        castedNode._super_time_magic = -1;
        castedNode._time_magic = -1;

        byte subTreeType = ChunkType.TIME_TREE_CHUNK;
        if (castedNode._index_stateChunk == -1) {
            subTreeType = ChunkType.TIME_TREE_DVALUE_CHUNK;
        }
        if (previousWorld == nodeWorld || nodeWorldOrder.get(nodeWorld) != CoreConstants.NULL_LONG) {
            //final TimeTreeChunk superTimeTree = (TimeTreeChunk) this._space.get(castedNode._index_superTimeTree);
            final TimeTreeChunk timeTree = (TimeTreeChunk) this._space.get(castedNode._index_timeTree);
            final long subTreeCapacity = timeTree.capacity();
            if (timeTree.size() < subTreeCapacity) {
                //easy, just insert the new timeslot
                castedNode._index_timeTree_offset = timeTree.insert(nodeTime);
                if (superTimeTree.lastKey() == timeTree.time()) {
                    superTimeTree.setLastValue(timeTree.size());
                } else {
                    castedNode.cacheUnlock();
                    throw new RuntimeException("GreyCat internal error");
                }
            } else {
                //are we the last last one?
                if (superTimeTree.lastKey() == timeTree.time()) {
                    TimeTreeChunk newTimeTree = (TimeTreeChunk) this._space.createAndMark(subTreeType, nodeWorld, nodeTime, nodeId);
                    long allowedSubTreeCapacity = superTimeTree.subTreeCapacity();
                    castedNode._index_timeTree_offset = newTimeTree.insert(nodeTime);
                    newTimeTree.setCapacity(allowedSubTreeCapacity);
                    superTimeTree.insert(nodeTime, allowedSubTreeCapacity);
                    _space.unmark(castedNode._index_timeTree);
                    castedNode._index_timeTree = newTimeTree.index();
                } else {
                    //insertion in past, oversize tree
                    castedNode._index_timeTree_offset = timeTree.insert(nodeTime);
                    timeTree.setCapacity(subTreeCapacity + 1);
                    superTimeTree.insert(timeTree.time(), subTreeCapacity + 1);
                }
            }
        } else {
            //create a new node superTimeTree
            SuperTimeTreeChunk newSuperTimeTree = (SuperTimeTreeChunk) this._space.createAndMark(ChunkType.SUPER_TIME_TREE_CHUNK, nodeWorld, 0, nodeId);
            long subTreeCapacity = superTimeTree.subTreeCapacity();
            newSuperTimeTree.insert(nodeTime, subTreeCapacity);
            //create a new node timeTree
            TimeTreeChunk newTimeTree = (TimeTreeChunk) this._space.createAndMark(subTreeType, nodeWorld, nodeTime, nodeId);
            castedNode._index_timeTree_offset = newTimeTree.insert(nodeTime);
            newTimeTree.setCapacity(subTreeCapacity);
            //insert into node world order
            nodeWorldOrder.put(nodeWorld, nodeTime);
            //let's store the new state if necessary
            _space.unmark(castedNode._index_timeTree);
            _space.unmark(castedNode._index_superTimeTree);
            castedNode._index_timeTree = newTimeTree.index();
            castedNode._index_superTimeTree = newSuperTimeTree.index();
        }
        if (castedNode._index_stateChunk == -1) {
            clonedState = ((TimeTreeEmbeddedChunk) _space.get(castedNode._index_timeTree)).state(castedNode._index_timeTree_offset);
        }
        nodeWorldOrder.unlock();
        castedNode.cacheUnlock();
        return clonedState;
        */
    }

    @Override
    public final <A extends Node> void lookup(final long world, final long time, final long id, final Callback<A> callback) {
        final MWResolver selfPointer = this;
        selfPointer._space.getOrLoadAndMark(ChunkType.WORLD_ORDER_CHUNK, 0, 0, id, new Callback<Chunk>() {
            @Override
            public void on(final Chunk theNodeWorldOrder) {
                if (theNodeWorldOrder == null) {
                    callback.on(null);
                } else {
                    final WorldOrderChunk castedWC = (WorldOrderChunk) theNodeWorldOrder;
                    final long closestWorld = selfPointer.resolve_world(globalWorldOrderChunk, castedWC, time, world);
                    selfPointer._space.getOrLoadAndMark(ChunkType.SUPER_TIME_TREE_CHUNK, closestWorld, 0, id, new Callback<Chunk>() {
                        @Override
                        public void on(final Chunk theNodeSuperTimeTree) {
                            if (theNodeSuperTimeTree == null) {
                                selfPointer._space.unmark(theNodeWorldOrder.index());
                                callback.on(null);
                            } else {
                                final SuperTimeTreeChunk castedSTT = (SuperTimeTreeChunk) theNodeSuperTimeTree;
                                if (castedSTT.end() != 0 && time > castedSTT.end()) {
                                    selfPointer._space.unmark(theNodeSuperTimeTree.index());
                                    selfPointer._space.unmark(theNodeWorldOrder.index());
                                    callback.on(null);
                                    return;
                                }
                                final long closestSuperTime = castedSTT.previousOrEqual(time);
                                if (closestSuperTime == Constants.NULL_LONG) {
                                    selfPointer._space.unmark(theNodeSuperTimeTree.index());
                                    selfPointer._space.unmark(theNodeWorldOrder.index());
                                    callback.on(null);
                                    return;
                                }
                                byte treeType = ChunkType.TIME_TREE_CHUNK;
                                //TODO extends
                                if (castedWC.type() == NodeValueType) {
                                    treeType = ChunkType.TIME_TREE_DVALUE_CHUNK;
                                }
                                selfPointer._space.getOrLoadAndMark(treeType, closestWorld, closestSuperTime, id, new Callback<Chunk>() {
                                    @Override
                                    public void on(final Chunk theNodeTimeTree) {
                                        if (theNodeTimeTree == null) {
                                            selfPointer._space.unmark(theNodeSuperTimeTree.index());
                                            selfPointer._space.unmark(theNodeWorldOrder.index());
                                            callback.on(null);
                                        } else {
                                            final int closestTimeOffset = ((TimeTreeChunk) theNodeTimeTree).previousOrEqualOffset(time);
                                            final long closestTime = ((TimeTreeChunk) theNodeTimeTree).getKey(closestTimeOffset);
                                            if (closestTime == Constants.NULL_LONG) {
                                                selfPointer._space.unmark(theNodeTimeTree.index());
                                                selfPointer._space.unmark(theNodeSuperTimeTree.index());
                                                selfPointer._space.unmark(theNodeWorldOrder.index());
                                                callback.on(null);
                                                return;
                                            }
                                            Callback<Chunk> cc = new Callback<Chunk>() {
                                                @Override
                                                public void on(Chunk theObjectChunk) {
                                                    if (theObjectChunk == null) {
                                                        selfPointer._space.unmark(theNodeTimeTree.index());
                                                        selfPointer._space.unmark(theNodeSuperTimeTree.index());
                                                        selfPointer._space.unmark(theNodeWorldOrder.index());
                                                        callback.on(null);
                                                    } else {
                                                        WorldOrderChunk castedNodeWorldOrder = (WorldOrderChunk) theNodeWorldOrder;
                                                        int extraCode = (int) castedNodeWorldOrder.type();
                                                        NodeFactory resolvedFactory = null;
                                                        if (extraCode != -1) {
                                                            resolvedFactory = ((CoreGraph) selfPointer._graph).factoryByCode(extraCode);
                                                        }
                                                        BaseNode resolvedNode;
                                                        if (resolvedFactory == null) {
                                                            resolvedNode = new BaseNode(world, time, id, selfPointer._graph);
                                                        } else {
                                                            resolvedNode = (BaseNode) resolvedFactory.create(world, time, id, selfPointer._graph);
                                                        }
                                                        resolvedNode._dead = false;
                                                        resolvedNode._index_stateChunk = theObjectChunk.index();
                                                        resolvedNode._index_superTimeTree = theNodeSuperTimeTree.index();
                                                        resolvedNode._index_timeTree = theNodeTimeTree.index();
                                                        resolvedNode._index_timeTree_offset = closestTimeOffset;
                                                        resolvedNode._index_worldOrder = theNodeWorldOrder.index();

                                                        if (closestWorld == world && closestTime == time) {
                                                            resolvedNode._world_magic = -1;
                                                            resolvedNode._super_time_magic = -1;
                                                            resolvedNode._time_magic = -1;
                                                        } else {
                                                            resolvedNode._world_magic = ((WorldOrderChunk) theNodeWorldOrder).magic();
                                                            resolvedNode._super_time_magic = ((SuperTimeTreeChunk) theNodeSuperTimeTree).magic();
                                                            resolvedNode._time_magic = ((TimeTreeChunk) theNodeTimeTree).magic();
                                                        }
                                                        //selfPointer._tracker.monitor(resolvedNode);
                                                        if (callback != null) {
                                                            final Node casted = resolvedNode;
                                                            callback.on((A) casted);
                                                        }
                                                    }
                                                }
                                            };
                                            if (castedWC.type() == NodeValueType) {
                                                cc.on(((TimeTreeEmbeddedChunk) theNodeTimeTree).state(closestTimeOffset));
                                            } else {
                                                selfPointer._space.getOrLoadAndMark(ChunkType.STATE_CHUNK, closestWorld, closestTime, id, cc);
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void lookupBatch(final long[] worlds, final long[] times, final long[] ids, final Callback<Node[]> callback) {
        final int idsSize = ids.length;
        if (!(worlds.length == times.length && times.length == idsSize)) {
            throw new RuntimeException("Bad API usage");
        }
        final MWResolver selfPointer = this;
        final Node[] finalResult = new Node[idsSize];
        //init as null for JS compatibility
        for (int i = 0; i < idsSize; i++) {
            finalResult[i] = null;
        }
        final boolean[] isEmpty = {true};
        final long[] keys = new long[idsSize * Constants.KEY_SIZE];
        for (int i = 0; i < idsSize; i++) {
            isEmpty[0] = false;
            keys[i * Constants.KEY_SIZE] = ChunkType.WORLD_ORDER_CHUNK;
            keys[(i * Constants.KEY_SIZE) + 1] = 0;
            keys[(i * Constants.KEY_SIZE) + 2] = 0;
            keys[(i * Constants.KEY_SIZE) + 3] = ids[i];
        }
        if (isEmpty[0]) {
            lookupAll_end(finalResult, callback, idsSize, null, null, null, null);
        } else {
            selfPointer._space.getOrLoadAndMarkAll(keys, new Callback<Chunk[]>() {
                @Override
                public void on(final Chunk[] theNodeWorldOrders) {
                    if (theNodeWorldOrders == null) {
                        lookupAll_end(finalResult, callback, idsSize, null, null, null, null);
                    } else {
                        isEmpty[0] = true;
                        for (int i = 0; i < idsSize; i++) {
                            if (theNodeWorldOrders[i] != null) {
                                isEmpty[0] = false;
                                keys[i * Constants.KEY_SIZE] = ChunkType.SUPER_TIME_TREE_CHUNK;
                                keys[(i * Constants.KEY_SIZE) + 1] = selfPointer.resolve_world(globalWorldOrderChunk, (WorldOrderChunk) theNodeWorldOrders[i], times[i], worlds[i]);
                                keys[(i * Constants.KEY_SIZE) + 2] = 0;
                            } else {
                                keys[i * Constants.KEY_SIZE] = -1;
                            }
                        }
                        if (isEmpty[0]) {
                            lookupAll_end(finalResult, callback, idsSize, theNodeWorldOrders, null, null, null);
                        } else {
                            selfPointer._space.getOrLoadAndMarkAll(keys, new Callback<Chunk[]>() {
                                @Override
                                public void on(final Chunk[] theNodeSuperTimeTrees) {
                                    if (theNodeSuperTimeTrees == null) {
                                        lookupAll_end(finalResult, callback, idsSize, theNodeWorldOrders, null, null, null);
                                    } else {
                                        isEmpty[0] = true;
                                        for (int i = 0; i < idsSize; i++) {
                                            if (theNodeSuperTimeTrees[i] != null) {
                                                final long closestSuperTime = ((SuperTimeTreeChunk) theNodeSuperTimeTrees[i]).previousOrEqual(times[i]);
                                                if (closestSuperTime == Constants.NULL_LONG) {
                                                    keys[i * Constants.KEY_SIZE] = -1; //skip
                                                } else {
                                                    isEmpty[0] = false;
                                                    keys[(i * Constants.KEY_SIZE) + 2] = closestSuperTime;
                                                    keys[i * Constants.KEY_SIZE] = ChunkType.TIME_TREE_CHUNK;
                                                    if (((WorldOrderChunk) theNodeWorldOrders[i]).type() == NodeValueType) {
                                                        keys[i * Constants.KEY_SIZE] = ChunkType.TIME_TREE_DVALUE_CHUNK;
                                                    }
                                                }
                                            } else {
                                                keys[i * Constants.KEY_SIZE] = -1; //skip
                                            }
                                        }
                                        if (isEmpty[0]) {
                                            lookupAll_end(finalResult, callback, idsSize, theNodeWorldOrders, theNodeSuperTimeTrees, null, null);
                                        } else {
                                            selfPointer._space.getOrLoadAndMarkAll(keys, new Callback<Chunk[]>() {
                                                @Override
                                                public void on(final Chunk[] theNodeTimeTrees) {
                                                    if (theNodeTimeTrees == null) {
                                                        lookupAll_end(finalResult, callback, idsSize, theNodeWorldOrders, theNodeSuperTimeTrees, null, null);
                                                    } else {
                                                        isEmpty[0] = true;
                                                        for (int i = 0; i < idsSize; i++) {
                                                            if (theNodeTimeTrees[i] != null) {
                                                                final int closestTimeOffset = ((TimeTreeChunk) theNodeTimeTrees[i]).previousOrEqualOffset(times[i]);
                                                                final long closestTime = ((TimeTreeChunk) theNodeTimeTrees[i]).getKey(closestTimeOffset);
                                                                if (closestTime == Constants.NULL_LONG) {
                                                                    keys[i * Constants.KEY_SIZE] = -1; //skip
                                                                } else {
                                                                    isEmpty[0] = false;
                                                                    if (((WorldOrderChunk) theNodeWorldOrders[i]).type() == NodeValueType) {
                                                                        keys[i * Constants.KEY_SIZE] = -1;
                                                                        keys[(i * Constants.KEY_SIZE) + 2] = closestTimeOffset;
                                                                    } else {
                                                                        keys[(i * Constants.KEY_SIZE)] = ChunkType.STATE_CHUNK;
                                                                        keys[(i * Constants.KEY_SIZE) + 2] = closestTime;
                                                                    }
                                                                }
                                                            } else {
                                                                keys[i * Constants.KEY_SIZE] = -1; //skip
                                                            }
                                                        }
                                                        if (isEmpty[0]) {
                                                            lookupAll_end(finalResult, callback, idsSize, theNodeWorldOrders, theNodeSuperTimeTrees, theNodeTimeTrees, null);
                                                        } else {
                                                            selfPointer._space.getOrLoadAndMarkAll(keys, new Callback<Chunk[]>() {
                                                                @Override
                                                                public void on(Chunk[] theObjectChunks) {
                                                                    if (theObjectChunks == null) {
                                                                        lookupAll_end(finalResult, callback, idsSize, theNodeWorldOrders, theNodeSuperTimeTrees, theNodeTimeTrees, null);
                                                                    } else {
                                                                        for (int i = 0; i < idsSize; i++) {
                                                                            if (theNodeWorldOrders[i] != null && theNodeSuperTimeTrees[i] != null && theNodeTimeTrees[i] != null) {
                                                                                int timeOffset = -1;
                                                                                //only for value node
                                                                                if (theObjectChunks[i] == null && ((WorldOrderChunk) theNodeWorldOrders[i]).type() == NodeValueType) {
                                                                                    timeOffset = (int) keys[(i * Constants.KEY_SIZE) + 2];
                                                                                    theObjectChunks[i] = ((TimeTreeEmbeddedChunk) theNodeTimeTrees[i]).state(timeOffset);
                                                                                }
                                                                                if (theObjectChunks[i] != null) {
                                                                                    WorldOrderChunk castedNodeWorldOrder = (WorldOrderChunk) theNodeWorldOrders[i];
                                                                                    int extraCode = (int) castedNodeWorldOrder.type();
                                                                                    NodeFactory resolvedFactory = null;
                                                                                    if (extraCode != -1) {
                                                                                        resolvedFactory = ((CoreGraph) selfPointer._graph).factoryByCode(extraCode);
                                                                                    }
                                                                                    BaseNode resolvedNode;
                                                                                    if (resolvedFactory == null) {
                                                                                        resolvedNode = new BaseNode(worlds[i], times[i], ids[i], selfPointer._graph);
                                                                                    } else {
                                                                                        resolvedNode = (BaseNode) resolvedFactory.create(worlds[i], times[i], ids[i], selfPointer._graph);
                                                                                    }
                                                                                    resolvedNode._dead = false;
                                                                                    resolvedNode._index_stateChunk = theObjectChunks[i].index();
                                                                                    resolvedNode._index_superTimeTree = theNodeSuperTimeTrees[i].index();
                                                                                    resolvedNode._index_timeTree = theNodeTimeTrees[i].index();
                                                                                    resolvedNode._index_worldOrder = theNodeWorldOrders[i].index();
                                                                                    resolvedNode._index_timeTree_offset = timeOffset;
                                                                                    if (theObjectChunks[i].world() == worlds[i] && theObjectChunks[i].time() == times[i]) {
                                                                                        resolvedNode._world_magic = -1;
                                                                                        resolvedNode._super_time_magic = -1;
                                                                                        resolvedNode._time_magic = -1;
                                                                                    } else {
                                                                                        resolvedNode._world_magic = ((WorldOrderChunk) theNodeWorldOrders[i]).magic();
                                                                                        resolvedNode._super_time_magic = ((SuperTimeTreeChunk) theNodeSuperTimeTrees[i]).magic();
                                                                                        resolvedNode._time_magic = ((TimeTreeChunk) theNodeTimeTrees[i]).magic();
                                                                                    }
                                                                                    finalResult[i] = resolvedNode;
                                                                                } else {
                                                                                    finalResult[i] = null;
                                                                                }
                                                                            } else {
                                                                                finalResult[i] = null;
                                                                            }
                                                                        }
                                                                        lookupAll_end(finalResult, callback, idsSize, theNodeWorldOrders, theNodeSuperTimeTrees, theNodeTimeTrees, theObjectChunks);
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    }
                                                }
                                            });
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    private void lookupAll_end(final Node[] finalResult, final Callback<Node[]> callback, final int sizeIds, final Chunk[] worldOrders, final Chunk[] superTimes, final Chunk[] times, final Chunk[] chunks) {
        if (worldOrders != null || superTimes != null || times != null || chunks != null) {
            for (int i = 0; i < sizeIds; i++) {
                if (finalResult[i] == null) {
                    if (worldOrders != null && worldOrders[i] != null) {
                        _space.unmark(worldOrders[i].index());
                    }
                    if (superTimes != null && superTimes[i] != null) {
                        _space.unmark(superTimes[i].index());
                    }
                    if (times != null && times[i] != null) {
                        _space.unmark(times[i].index());
                    }
                    if (chunks != null && chunks[i] != null) {
                        _space.unmark(chunks[i].index());
                    }
                }
            }
        }
        callback.on(finalResult);
    }

    @Override
    public final void lookupAll(final long world, final long reqTime, final long ids[], final Callback<Node[]> callback) {
        double nbRounds = ((double) ids.length) / max_load_keys;
        if (nbRounds < 0) {
            internal_lookupAll(world, reqTime, ids, callback);
        } else {
            final Node[] finalResult = new Node[ids.length];
            int nbCalls = (int) Math.ceil(nbRounds);
            DeferCounter counter = new CoreDeferCounter(nbCalls);
            counter.then(new Job() {
                @Override
                public void run() {
                    callback.on(finalResult);
                }
            });
            int loaded = 0;
            for (int i = 0; i < nbCalls; i++) {
                int truncatedSize = (int) max_load_keys;
                if ((loaded + truncatedSize) > ids.length) {
                    truncatedSize = ids.length - loaded;
                }
                final long[] t_ids = new long[truncatedSize];
                System.arraycopy(ids, loaded, t_ids, 0, truncatedSize);
                final int f_loaded = loaded;
                loaded = loaded + truncatedSize;
                internal_lookupAll(world, reqTime, t_ids, new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        System.arraycopy(result, 0, finalResult, f_loaded, result.length);
                        counter.count();
                    }
                });
            }
        }
    }

    private final void internal_lookupAll(final long world, final long reqTime, final long ids[], final Callback<Node[]> callback) {
        final MWResolver selfPointer = this;
        final int idsSize = ids.length;
        final Node[] finalResult = new Node[idsSize];
        //init as null for JS compatibility
        for (int i = 0; i < idsSize; i++) {
            finalResult[i] = null;
        }
        final boolean[] isEmpty = {true};
        final long[] keys = new long[idsSize * Constants.KEY_SIZE];
        for (int i = 0; i < idsSize; i++) {
            isEmpty[0] = false;
            keys[i * Constants.KEY_SIZE] = ChunkType.WORLD_ORDER_CHUNK;
            keys[(i * Constants.KEY_SIZE) + 1] = 0;
            keys[(i * Constants.KEY_SIZE) + 2] = 0;
            keys[(i * Constants.KEY_SIZE) + 3] = ids[i];
        }
        if (isEmpty[0]) {
            lookupAll_end(finalResult, callback, idsSize, null, null, null, null);
        } else {
            selfPointer._space.getOrLoadAndMarkAll(keys, new Callback<Chunk[]>() {
                @Override
                public void on(final Chunk[] theNodeWorldOrders) {
                    if (theNodeWorldOrders == null) {
                        lookupAll_end(finalResult, callback, idsSize, null, null, null, null);
                    } else {
                        isEmpty[0] = true;
                        for (int i = 0; i < idsSize; i++) {
                            if (theNodeWorldOrders[i] != null) {
                                isEmpty[0] = false;
                                keys[i * Constants.KEY_SIZE] = ChunkType.SUPER_TIME_TREE_CHUNK;
                                keys[(i * Constants.KEY_SIZE) + 1] = selfPointer.resolve_world(globalWorldOrderChunk, (WorldOrderChunk) theNodeWorldOrders[i], reqTime, world);
                                keys[(i * Constants.KEY_SIZE) + 2] = 0;
                            } else {
                                keys[i * Constants.KEY_SIZE] = -1;
                            }
                        }
                        if (isEmpty[0]) {
                            lookupAll_end(finalResult, callback, idsSize, theNodeWorldOrders, null, null, null);
                        } else {
                            selfPointer._space.getOrLoadAndMarkAll(keys, new Callback<Chunk[]>() {
                                @Override
                                public void on(final Chunk[] theNodeSuperTimeTrees) {
                                    if (theNodeSuperTimeTrees == null) {
                                        lookupAll_end(finalResult, callback, idsSize, theNodeWorldOrders, null, null, null);
                                    } else {
                                        isEmpty[0] = true;
                                        for (int i = 0; i < idsSize; i++) {
                                            if (theNodeSuperTimeTrees[i] != null) {
                                                final long closestSuperTime = ((SuperTimeTreeChunk) theNodeSuperTimeTrees[i]).previousOrEqual(reqTime);
                                                if (closestSuperTime == Constants.NULL_LONG) {
                                                    keys[i * Constants.KEY_SIZE] = -1; //skip
                                                } else {
                                                    isEmpty[0] = false;
                                                    keys[i * Constants.KEY_SIZE] = ChunkType.TIME_TREE_CHUNK;
                                                    if (((WorldOrderChunk) theNodeWorldOrders[i]).type() == NodeValueType) {
                                                        keys[i * Constants.KEY_SIZE] = ChunkType.TIME_TREE_DVALUE_CHUNK;
                                                    }
                                                    keys[(i * Constants.KEY_SIZE) + 2] = closestSuperTime;
                                                }
                                            } else {
                                                keys[i * Constants.KEY_SIZE] = -1; //skip
                                            }
                                        }
                                        if (isEmpty[0]) {
                                            lookupAll_end(finalResult, callback, idsSize, theNodeWorldOrders, theNodeSuperTimeTrees, null, null);
                                        } else {
                                            selfPointer._space.getOrLoadAndMarkAll(keys, new Callback<Chunk[]>() {
                                                @Override
                                                public void on(final Chunk[] theNodeTimeTrees) {
                                                    if (theNodeTimeTrees == null) {
                                                        lookupAll_end(finalResult, callback, idsSize, theNodeWorldOrders, theNodeSuperTimeTrees, null, null);
                                                    } else {
                                                        isEmpty[0] = true;
                                                        for (int i = 0; i < idsSize; i++) {
                                                            if (theNodeTimeTrees[i] != null) {
                                                                final int closestTimeOffset = ((TimeTreeChunk) theNodeTimeTrees[i]).previousOrEqualOffset(reqTime);
                                                                final long closestTime = ((TimeTreeChunk) theNodeTimeTrees[i]).getKey(closestTimeOffset);
                                                                if (closestTime == Constants.NULL_LONG) {
                                                                    keys[i * Constants.KEY_SIZE] = -1; //skip
                                                                } else {
                                                                    isEmpty[0] = false;
                                                                    if (((WorldOrderChunk) theNodeWorldOrders[i]).type() == NodeValueType) {
                                                                        keys[i * Constants.KEY_SIZE] = -1;
                                                                        keys[(i * Constants.KEY_SIZE) + 2] = closestTimeOffset;
                                                                    } else {
                                                                        keys[(i * Constants.KEY_SIZE)] = ChunkType.STATE_CHUNK;
                                                                        keys[(i * Constants.KEY_SIZE) + 2] = closestTime;
                                                                    }
                                                                }
                                                            } else {
                                                                keys[i * Constants.KEY_SIZE] = -1; //skip
                                                            }
                                                        }
                                                        if (isEmpty[0]) {
                                                            lookupAll_end(finalResult, callback, idsSize, theNodeWorldOrders, theNodeSuperTimeTrees, theNodeTimeTrees, null);
                                                        } else {
                                                            final Callback<Chunk[]> process_chunks = new Callback<Chunk[]>() {
                                                                @Override
                                                                public void on(Chunk[] theObjectChunks) {
                                                                    if (theObjectChunks == null) {
                                                                        lookupAll_end(finalResult, callback, idsSize, theNodeWorldOrders, theNodeSuperTimeTrees, theNodeTimeTrees, null);
                                                                    } else {
                                                                        for (int i = 0; i < idsSize; i++) {
                                                                            int timeOffset = -1;
                                                                            if (theNodeWorldOrders[i] != null) {
                                                                                if (theObjectChunks[i] == null && ((WorldOrderChunk) theNodeWorldOrders[i]).type() == NodeValueType) {
                                                                                    timeOffset = (int) keys[(i * Constants.KEY_SIZE) + 2];
                                                                                    theObjectChunks[i] = ((TimeTreeEmbeddedChunk) theNodeTimeTrees[i]).state(timeOffset);
                                                                                }
                                                                            }
                                                                            if (theObjectChunks[i] != null) {
                                                                                WorldOrderChunk castedNodeWorldOrder = (WorldOrderChunk) theNodeWorldOrders[i];
                                                                                int extraCode = (int) castedNodeWorldOrder.type();
                                                                                NodeFactory resolvedFactory = null;
                                                                                if (extraCode != -1) {
                                                                                    resolvedFactory = ((CoreGraph) selfPointer._graph).factoryByCode(extraCode);
                                                                                }
                                                                                BaseNode resolvedNode;
                                                                                if (resolvedFactory == null) {
                                                                                    resolvedNode = new BaseNode(world, reqTime, ids[i], selfPointer._graph);
                                                                                } else {
                                                                                    resolvedNode = (BaseNode) resolvedFactory.create(world, reqTime, ids[i], selfPointer._graph);
                                                                                }
                                                                                resolvedNode._dead = false;
                                                                                resolvedNode._index_stateChunk = theObjectChunks[i].index();
                                                                                resolvedNode._index_superTimeTree = theNodeSuperTimeTrees[i].index();
                                                                                resolvedNode._index_timeTree = theNodeTimeTrees[i].index();
                                                                                resolvedNode._index_worldOrder = theNodeWorldOrders[i].index();
                                                                                resolvedNode._index_timeTree_offset = timeOffset;
                                                                                if (theObjectChunks[i].world() == world && theObjectChunks[i].time() == reqTime) {
                                                                                    resolvedNode._world_magic = -1;
                                                                                    resolvedNode._super_time_magic = -1;
                                                                                    resolvedNode._time_magic = -1;
                                                                                } else {
                                                                                    resolvedNode._world_magic = ((WorldOrderChunk) theNodeWorldOrders[i]).magic();
                                                                                    resolvedNode._super_time_magic = ((SuperTimeTreeChunk) theNodeSuperTimeTrees[i]).magic();
                                                                                    resolvedNode._time_magic = ((TimeTreeChunk) theNodeTimeTrees[i]).magic();
                                                                                }
                                                                                finalResult[i] = resolvedNode;
                                                                            }
                                                                        }
                                                                        lookupAll_end(finalResult, callback, idsSize, theNodeWorldOrders, theNodeSuperTimeTrees, theNodeTimeTrees, theObjectChunks);
                                                                    }
                                                                }
                                                            };
                                                            selfPointer._space.getOrLoadAndMarkAll(keys, process_chunks);
                                                        }
                                                    }
                                                }
                                            });
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    @Override
    public void lookupPTimes(long world, long[] times, long id, Callback<Node[]> callback) {
        throw new RuntimeException("Not implemented yet!");
    }

    @Override
    public final void lookupTimes(long world, long rfrom, long rto, long id, int limit, Callback<Node[]> callback) {
        final MWResolver selfPointer = this;
        _space.getOrLoadAndMark(ChunkType.WORLD_ORDER_CHUNK, 0, 0, id, new Callback<Chunk>() {
            @Override
            public void on(final Chunk resolved) {
                if (resolved == null) {
                    callback.on(new Node[0]);
                    return;
                }
                final WorldOrderChunk objectWorldOrder = (WorldOrderChunk) resolved;
                final long from;
                final long to;
                final boolean reversed;
                if (rfrom > rto) {
                    reversed = true;
                    from = rto;
                    to = rfrom;
                } else {
                    reversed = false;
                    from = rfrom;
                    to = rto;
                }
                //worlds collector
                final LArray worldCollector = new LArray();
                long currentWorld = world;
                while (currentWorld != CoreConstants.NULL_LONG) {
                    long divergenceTimepoint = objectWorldOrder.get(currentWorld);
                    if (divergenceTimepoint != CoreConstants.NULL_LONG) {
                        if (divergenceTimepoint <= from) {
                            //take the first one before leaving
                            worldCollector.add(currentWorld);
                            break;
                        } else if (divergenceTimepoint > to) {
                            //next round, go to parent world
                            currentWorld = selfPointer.globalWorldOrderChunk.get(currentWorld);
                        } else {
                            //that's fit, add to search
                            worldCollector.add(currentWorld);
                            //go to parent
                            currentWorld = selfPointer.globalWorldOrderChunk.get(currentWorld);
                        }
                    } else {
                        //go to parent
                        currentWorld = selfPointer.globalWorldOrderChunk.get(currentWorld);
                    }
                }
                if (worldCollector.size() == 0) {
                    callback.on(new Node[0]);
                    return;
                }
                final long[] call_keys = new long[worldCollector.size() * 3];
                final byte[] call_types = new byte[worldCollector.size()];
                for (int i = 0; i < worldCollector.size(); i++) {
                    call_types[i] = ChunkType.SUPER_TIME_TREE_CHUNK;
                    call_keys[i * Constants.KEY_SIZE] = worldCollector.get(i);
                    call_keys[(i * Constants.KEY_SIZE) + 1] = 0;
                    call_keys[(i * Constants.KEY_SIZE) + 2] = id;
                }
                getOrLoadAndMarkAll(call_types, call_keys, new Callback<Chunk[]>() {
                    @Override
                    public void on(final Chunk[] superTimeTrees) {
                        if (superTimeTrees == null) {
                            selfPointer._space.unmark(objectWorldOrder.index());
                            callback.on(new Node[0]);
                        } else {
                            //time collector
                            long[] call_keys2;
                            byte[] call_types2;
                            final LMap tempSuperTimeCollector = new LMap(true);
                            if (reversed) {
                                final int[] sumCapacity = {0};
                                long previousDivergenceTime = to;
                                for (int i = 0; i < worldCollector.size() && (limit == -1 || sumCapacity[0] <= limit); i++) {
                                    final SuperTimeTreeChunk timeTree = (SuperTimeTreeChunk) superTimeTrees[i];
                                    if (timeTree != null) {
                                        long currentDivergenceTime = objectWorldOrder.get(worldCollector.get(i));
                                        int finalI = i;
                                        timeTree.range(currentDivergenceTime, previousDivergenceTime, CoreConstants.END_OF_TIME, new SuperTreeWalker() {
                                            @Override
                                            public void elem(final long superTime, final long superCapacity) {
                                                if (!tempSuperTimeCollector.contains(superTime)) {
                                                    tempSuperTimeCollector.put(superTime, finalI);
                                                    sumCapacity[0] = sumCapacity[0] + ((int) superCapacity);
                                                }
                                            }
                                        });
                                        previousDivergenceTime = currentDivergenceTime;
                                    }
                                }
                                call_keys2 = new long[tempSuperTimeCollector.size() * 3];
                                call_types2 = new byte[tempSuperTimeCollector.size()];
                                for (int i = 0; i < tempSuperTimeCollector.size(); i++) {
                                    call_keys2[i * 3] = worldCollector.get((int) tempSuperTimeCollector.getValue(i));
                                    call_keys2[i * 3 + 1] = tempSuperTimeCollector.getKey(i);
                                    call_keys2[i * 3 + 2] = id;
                                    if (objectWorldOrder.type() == NodeValueType) {
                                        call_types2[i] = ChunkType.TIME_TREE_DVALUE_CHUNK;
                                    } else {
                                        call_types2[i] = ChunkType.TIME_TREE_CHUNK;
                                    }
                                }
                            } else {
                                final LMap tempSuperTimeCollectorCapacity = new LMap(true);
                                long previousDivergenceTime = to;
                                for (int i = 0; i < worldCollector.size(); i++) {
                                    final SuperTimeTreeChunk timeTree = (SuperTimeTreeChunk) superTimeTrees[i];
                                    if (timeTree != null) {
                                        long currentDivergenceTime = objectWorldOrder.get(worldCollector.get(i));
                                        int finalI = i;
                                        timeTree.range(currentDivergenceTime, previousDivergenceTime, CoreConstants.END_OF_TIME, new SuperTreeWalker() {
                                            @Override
                                            public void elem(final long superTime, final long superCapacity) {
                                                if (!tempSuperTimeCollector.contains(superTime)) {
                                                    tempSuperTimeCollector.put(superTime, finalI);
                                                    tempSuperTimeCollectorCapacity.put(superTime, superCapacity);
                                                }
                                            }
                                        });
                                        previousDivergenceTime = currentDivergenceTime;
                                    }
                                }
                                int neededSubTrees = 0;
                                int sumCapacity = 0;
                                for (int i = tempSuperTimeCollectorCapacity.size() - 1; i >= 0; i--) {
                                    sumCapacity = sumCapacity + ((int) tempSuperTimeCollectorCapacity.getValue(i));
                                    neededSubTrees++;
                                    if (limit != -1 && sumCapacity >= limit) {
                                        break;
                                    }
                                }
                                call_keys2 = new long[neededSubTrees * 3];
                                call_types2 = new byte[neededSubTrees];
                                int write_cursor = 0;
                                for (int i = tempSuperTimeCollector.size() - neededSubTrees; i < tempSuperTimeCollector.size(); i++) {
                                    call_keys2[write_cursor * 3] = worldCollector.get((int) tempSuperTimeCollector.getValue(i));
                                    call_keys2[write_cursor * 3 + 1] = tempSuperTimeCollector.getKey(i);
                                    call_keys2[write_cursor * 3 + 2] = id;
                                    if (objectWorldOrder.type() == NodeValueType) {
                                        call_types2[write_cursor] = ChunkType.TIME_TREE_DVALUE_CHUNK;
                                    } else {
                                        call_types2[write_cursor] = ChunkType.TIME_TREE_CHUNK;
                                    }
                                    write_cursor++;
                                }
                            }
                            getOrLoadAndMarkAll(call_types2, call_keys2, new Callback<Chunk[]>() {
                                @Override
                                public void on(final Chunk[] timeTrees) {
                                    if (timeTrees == null) {
                                        final ChunkSpace space = selfPointer._space;
                                        space.unmark(objectWorldOrder.index());
                                        for (int i = 0; i < worldCollector.size(); i++) {
                                            space.unmark(superTimeTrees[i].index());
                                        }
                                        callback.on(new Node[0]);
                                    } else {
                                        //time collector
                                        final LMap timeCollector = new LMap(true);
                                        long previousDivergenceTime = to;
                                        for (int i = 0; i < call_types2.length; i++) {
                                            final TimeTreeChunk timeTree = (TimeTreeChunk) timeTrees[i];
                                            if (timeTree != null) {
                                                long currentDivergenceTime = objectWorldOrder.get(call_keys2[i * 3]);
                                                if (currentDivergenceTime < from) {
                                                    currentDivergenceTime = from;
                                                }
                                                int finalI = i;
                                                timeTree.range(currentDivergenceTime, previousDivergenceTime, CoreConstants.END_OF_TIME, new TreeWalker() {
                                                    @Override
                                                    public void elem(long t) {
                                                        if (!timeCollector.contains(t)) {
                                                            timeCollector.put(t, finalI);
                                                        }
                                                    }
                                                });
                                                if (i < call_types2.length - 1) {
                                                    if (call_keys2[(i + 1) * 3] != call_keys2[i * 3]) {
                                                        //world overriding semantic
                                                        previousDivergenceTime = currentDivergenceTime;
                                                    }
                                                }
                                            }
                                        }
                                        //filter with max now
                                        int firmLimit;
                                        if (limit == -1 || limit >= timeCollector.size()) {
                                            firmLimit = timeCollector.size();
                                        } else {
                                            firmLimit = limit;
                                        }
                                        final int extraCode = (int) objectWorldOrder.type();
                                        NodeFactory resolvedFactory = null;
                                        if (extraCode != -1) {
                                            resolvedFactory = ((CoreGraph) selfPointer._graph).factoryByCode(extraCode);
                                        }
                                        final BaseNode[] result = new BaseNode[firmLimit];
                                        if (reversed) {
                                            for (int i = 0; i < firmLimit; i++) {
                                                final int reversedIndex = (int) timeCollector.getValue(i);
                                                if (resolvedFactory == null) {
                                                    result[i] = new BaseNode(call_keys2[reversedIndex * 3], timeCollector.getKey(i), id, selfPointer._graph);
                                                } else {
                                                    result[i] = (BaseNode) resolvedFactory.create(call_keys2[reversedIndex * 3], timeCollector.getKey(i), id, selfPointer._graph);
                                                }
                                                result[i]._dead = false;
                                                final SuperTimeTreeChunk stc = (SuperTimeTreeChunk) superTimeTrees[(int) tempSuperTimeCollector.get(timeTrees[reversedIndex].time())];
                                                final TimeTreeChunk ttc = (TimeTreeChunk) timeTrees[reversedIndex];
                                                _space.mark(stc.index());
                                                _space.mark(ttc.index());
                                                _space.mark(objectWorldOrder.index());
                                                result[i]._index_superTimeTree = stc.index();
                                                result[i]._index_timeTree = ttc.index();
                                                result[i]._index_worldOrder = objectWorldOrder.index();
                                                if (call_keys2[reversedIndex * 3] == world) { //time is always precise here
                                                    result[i]._world_magic = -1;
                                                    result[i]._super_time_magic = -1;
                                                    result[i]._time_magic = -1;
                                                } else {
                                                    result[i]._world_magic = objectWorldOrder.magic();
                                                    result[i]._super_time_magic = stc.magic();
                                                    result[i]._time_magic = ttc.magic();
                                                }
                                                //we need to marks
                                            }
                                        } else {
                                            int nodeIndex = 0;
                                            for (int i = timeCollector.size() - 1; i >= timeCollector.size() - firmLimit; i--) {
                                                final int reversedIndex = (int) timeCollector.getValue(i);
                                                if (resolvedFactory == null) {
                                                    result[nodeIndex] = new BaseNode(call_keys2[reversedIndex * 3], timeCollector.getKey(i), id, selfPointer._graph);
                                                } else {
                                                    result[nodeIndex] = (BaseNode) resolvedFactory.create(call_keys2[reversedIndex * 3], timeCollector.getKey(i), id, selfPointer._graph);
                                                }
                                                result[nodeIndex]._dead = false;
                                                final SuperTimeTreeChunk stc = (SuperTimeTreeChunk) superTimeTrees[(int) tempSuperTimeCollector.get(timeTrees[reversedIndex].time())];
                                                final TimeTreeChunk ttc = (TimeTreeChunk) timeTrees[reversedIndex];
                                                _space.mark(stc.index());
                                                _space.mark(ttc.index());
                                                _space.mark(objectWorldOrder.index());
                                                result[nodeIndex]._index_superTimeTree = stc.index();
                                                result[nodeIndex]._index_timeTree = ttc.index();
                                                result[nodeIndex]._index_worldOrder = objectWorldOrder.index();
                                                if (call_keys2[reversedIndex * 3] == world) { //time is always precise here
                                                    result[nodeIndex]._world_magic = -1;
                                                    result[nodeIndex]._super_time_magic = -1;
                                                    result[nodeIndex]._time_magic = -1;
                                                } else {
                                                    result[nodeIndex]._world_magic = objectWorldOrder.magic();
                                                    result[nodeIndex]._super_time_magic = stc.magic();
                                                    result[nodeIndex]._time_magic = ttc.magic();
                                                }
                                                nodeIndex++;
                                            }
                                        }
                                        long[] call_keys3 = new long[firmLimit * 3];
                                        byte[] call_types3 = new byte[firmLimit];
                                        for (int i = 0; i < firmLimit; i++) {
                                            call_keys3[i * 3] = result[i].world();
                                            call_keys3[i * 3 + 1] = result[i].time();
                                            call_keys3[i * 3 + 2] = id;
                                            call_types3[i] = ChunkType.STATE_CHUNK;
                                            if (objectWorldOrder.type() == NodeValueType) {
                                                call_types3[i] = -1;
                                                result[i]._index_timeTree_offset = ((TimeTreeDValueChunk) timeTrees[i]).previousOrEqualOffset(result[i].time());
                                            }
                                        }
                                        _space.unmark(objectWorldOrder.index());
                                        for (int i = 0; i < timeTrees.length; i++) {
                                            if (timeTrees[i] != null) {
                                                _space.unmark(timeTrees[i].index());
                                            }
                                        }
                                        for (int i = 0; i < superTimeTrees.length; i++) {
                                            if (superTimeTrees[i] != null) {
                                                _space.unmark(superTimeTrees[i].index());
                                            }
                                        }
                                        getOrLoadAndMarkAll(call_types3, call_keys3, new Callback<Chunk[]>() {
                                            @Override
                                            public void on(Chunk[] stateChunks) {
                                                for (int i = 0; i < firmLimit; i++) {
                                                    if (stateChunks[i] != null) {
                                                        result[i]._index_stateChunk = stateChunks[i].index();
                                                    } else {
                                                        result[i]._index_stateChunk = -1;
                                                    }
                                                }
                                                callback.on(result);
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    @Override
    public final void lookupTimesSparse(long world, long rfrom, long rto, long id, int limit, Callback<Node[]> callback) {
        final MWResolver selfPointer = this;
        _space.getOrLoadAndMark(ChunkType.WORLD_ORDER_CHUNK, 0, 0, id, new Callback<Chunk>() {
            @Override
            public void on(final Chunk resolved) {
                if (resolved == null) {
                    callback.on(new Node[0]);
                    return;
                }
                final WorldOrderChunk objectWorldOrder = (WorldOrderChunk) resolved;
                final long from;
                final long to;
                final boolean reversed;
                if (rfrom > rto) {
                    reversed = true;
                    from = rto;
                    to = rfrom;
                } else {
                    reversed = false;
                    from = rfrom;
                    to = rto;
                }
                //worlds collector
                final LArray worldCollector = new LArray();
                long currentWorld = world;
                while (currentWorld != CoreConstants.NULL_LONG) {
                    long divergenceTimepoint = objectWorldOrder.get(currentWorld);
                    if (divergenceTimepoint != CoreConstants.NULL_LONG) {
                        if (divergenceTimepoint <= from) {
                            //take the first one before leaving
                            worldCollector.add(currentWorld);
                            break;
                        } else if (divergenceTimepoint > to) {
                            //next round, go to parent world
                            currentWorld = selfPointer.globalWorldOrderChunk.get(currentWorld);
                        } else {
                            //that's fit, add to search
                            worldCollector.add(currentWorld);
                            //go to parent
                            currentWorld = selfPointer.globalWorldOrderChunk.get(currentWorld);
                        }
                    } else {
                        //go to parent
                        currentWorld = selfPointer.globalWorldOrderChunk.get(currentWorld);
                    }
                }
                if (worldCollector.size() == 0) {
                    callback.on(new Node[0]);
                    return;
                }
                final long[] call_keys = new long[worldCollector.size() * 3];
                final byte[] call_types = new byte[worldCollector.size()];
                for (int i = 0; i < worldCollector.size(); i++) {
                    call_types[i] = ChunkType.SUPER_TIME_TREE_CHUNK;
                    call_keys[i * Constants.KEY_SIZE] = worldCollector.get(i);
                    call_keys[(i * Constants.KEY_SIZE) + 1] = 0;
                    call_keys[(i * Constants.KEY_SIZE) + 2] = id;
                }
                getOrLoadAndMarkAll(call_types, call_keys, new Callback<Chunk[]>() {
                    @Override
                    public void on(final Chunk[] superTimeTrees) {
                        if (superTimeTrees == null) {
                            selfPointer._space.unmark(objectWorldOrder.index());
                            callback.on(new Node[0]);
                        } else {
                            //time collector
                            long[] call_keys2;
                            byte[] call_types2;
                            final LMap tempSuperTimeCollector = new LMap(true);

                            if (reversed) {
                                final int[] over_all_capacity = {0};
                                long previousDivergenceTime = to;
                                for (int i = 0; i < worldCollector.size(); i++) {
                                    final SuperTimeTreeChunk timeTree = (SuperTimeTreeChunk) superTimeTrees[i];
                                    if (timeTree != null) {
                                        long currentDivergenceTime = objectWorldOrder.get(worldCollector.get(i));
                                        int finalI = i;
                                        timeTree.range(currentDivergenceTime, previousDivergenceTime, CoreConstants.END_OF_TIME, new SuperTreeWalker() {
                                            @Override
                                            public void elem(final long superTime, final long superCapacity) {
                                                if (!tempSuperTimeCollector.contains(superTime)) {
                                                    tempSuperTimeCollector.put(superTime, finalI);
                                                    over_all_capacity[0] = over_all_capacity[0] + ((int) superCapacity);
                                                }
                                            }
                                        });
                                        previousDivergenceTime = currentDivergenceTime;
                                    }
                                }
                                call_keys2 = new long[tempSuperTimeCollector.size() * 3];
                                call_types2 = new byte[tempSuperTimeCollector.size()];
                                for (int i = 0; i < tempSuperTimeCollector.size(); i++) {
                                    call_keys2[i * 3] = worldCollector.get((int) tempSuperTimeCollector.getValue(i));
                                    call_keys2[i * 3 + 1] = tempSuperTimeCollector.getKey(i);
                                    call_keys2[i * 3 + 2] = id;
                                    if (objectWorldOrder.type() == NodeValueType) {
                                        call_types2[i] = ChunkType.TIME_TREE_DVALUE_CHUNK;
                                    } else {
                                        call_types2[i] = ChunkType.TIME_TREE_CHUNK;
                                    }
                                }
                            } else {
                                final LMap tempSuperTimeCollectorCapacity = new LMap(true);
                                long previousDivergenceTime = to;
                                for (int i = 0; i < worldCollector.size(); i++) {
                                    final SuperTimeTreeChunk timeTree = (SuperTimeTreeChunk) superTimeTrees[i];
                                    if (timeTree != null) {
                                        long currentDivergenceTime = objectWorldOrder.get(worldCollector.get(i));
                                        int finalI = i;
                                        timeTree.range(currentDivergenceTime, previousDivergenceTime, CoreConstants.END_OF_TIME, new SuperTreeWalker() {
                                            @Override
                                            public void elem(final long superTime, final long superCapacity) {
                                                if (!tempSuperTimeCollector.contains(superTime)) {
                                                    tempSuperTimeCollector.put(superTime, finalI);
                                                    tempSuperTimeCollectorCapacity.put(superTime, superCapacity);
                                                }
                                            }
                                        });
                                        previousDivergenceTime = currentDivergenceTime;
                                    }
                                }
                                int neededSubTrees = 0;
                                int over_all_capacity = 0;
                                for (int i = tempSuperTimeCollectorCapacity.size() - 1; i >= 0; i--) {
                                    over_all_capacity = over_all_capacity + ((int) tempSuperTimeCollectorCapacity.getValue(i));
                                    neededSubTrees++;
                                }
                                call_keys2 = new long[neededSubTrees * 3];
                                call_types2 = new byte[neededSubTrees];
                                int write_cursor = 0;
                                for (int i = tempSuperTimeCollector.size() - neededSubTrees; i < tempSuperTimeCollector.size(); i++) {
                                    call_keys2[write_cursor * 3] = worldCollector.get((int) tempSuperTimeCollector.getValue(i));
                                    call_keys2[write_cursor * 3 + 1] = tempSuperTimeCollector.getKey(i);
                                    call_keys2[write_cursor * 3 + 2] = id;
                                    if (objectWorldOrder.type() == NodeValueType) {
                                        call_types2[write_cursor] = ChunkType.TIME_TREE_DVALUE_CHUNK;
                                    } else {
                                        call_types2[write_cursor] = ChunkType.TIME_TREE_CHUNK;
                                    }
                                    write_cursor++;
                                }
                            }
                            getOrLoadAndMarkAll(call_types2, call_keys2, new Callback<Chunk[]>() {
                                @Override
                                public void on(final Chunk[] timeTrees) {
                                    if (timeTrees == null) {
                                        final ChunkSpace space = selfPointer._space;
                                        space.unmark(objectWorldOrder.index());
                                        for (int i = 0; i < worldCollector.size(); i++) {
                                            space.unmark(superTimeTrees[i].index());
                                        }
                                        callback.on(new Node[0]);
                                    } else {
                                        //time collector
                                        final LMap timeCollector_pre = new LMap(true);
                                        long previousDivergenceTime = to;
                                        final int[] skip = {0};

                                        for (int i = 0; i < call_types2.length; i++) {
                                            final TimeTreeChunk timeTree = (TimeTreeChunk) timeTrees[i];
                                            if (timeTree != null) {
                                                long currentDivergenceTime = objectWorldOrder.get(call_keys2[i * 3]);
                                                if (currentDivergenceTime < from) {
                                                    currentDivergenceTime = from;
                                                }
                                                int finalI = i;
                                                timeTree.range(currentDivergenceTime, previousDivergenceTime, CoreConstants.END_OF_TIME, new TreeWalker() {
                                                    @Override
                                                    public void elem(long t) {
                                                        if (!timeCollector_pre.contains(t)) {
                                                            //if (skip[0] == 0) {
                                                            timeCollector_pre.put(t, finalI);
                                                            //skip[0] = skip_times;
                                                            //}
                                                            //skip[0]--;
                                                        }
                                                    }
                                                });
                                                if (i < call_types2.length - 1) {
                                                    if (call_keys2[(i + 1) * 3] != call_keys2[i * 3]) {
                                                        //world overriding semantic
                                                        previousDivergenceTime = currentDivergenceTime;
                                                    }
                                                }
                                            }
                                        }

                                        final LMap timeCollector = new LMap(true);
                                        int skip_times = (timeCollector_pre.size() - limit) / limit;
                                        if(skip_times < 0){
                                            skip_times = 0;
                                        }
                                        skip_times = Math.round(skip_times);
                                        for (int i = 0; i < timeCollector_pre.size(); i = i + (skip_times + 1)) {
                                            timeCollector.put(timeCollector_pre.getKey(i), timeCollector_pre.getValue(i));
                                        }

                                        //filter with max now
                                        int firmLimit;
                                        if (limit == -1 || limit >= timeCollector.size()) {
                                            firmLimit = timeCollector.size();
                                        } else {
                                            firmLimit = limit;
                                        }
                                        final int extraCode = (int) objectWorldOrder.type();
                                        NodeFactory resolvedFactory = null;
                                        if (extraCode != -1) {
                                            resolvedFactory = ((CoreGraph) selfPointer._graph).factoryByCode(extraCode);
                                        }
                                        final BaseNode[] result = new BaseNode[firmLimit];
                                        if (reversed) {
                                            for (int i = 0; i < firmLimit; i++) {
                                                final int reversedIndex = (int) timeCollector.getValue(i);
                                                if (resolvedFactory == null) {
                                                    result[i] = new BaseNode(call_keys2[reversedIndex * 3], timeCollector.getKey(i), id, selfPointer._graph);
                                                } else {
                                                    result[i] = (BaseNode) resolvedFactory.create(call_keys2[reversedIndex * 3], timeCollector.getKey(i), id, selfPointer._graph);
                                                }
                                                result[i]._dead = false;
                                                final SuperTimeTreeChunk stc = (SuperTimeTreeChunk) superTimeTrees[(int) tempSuperTimeCollector.get(timeTrees[reversedIndex].time())];
                                                final TimeTreeChunk ttc = (TimeTreeChunk) timeTrees[reversedIndex];
                                                _space.mark(stc.index());
                                                _space.mark(ttc.index());
                                                _space.mark(objectWorldOrder.index());
                                                result[i]._index_superTimeTree = stc.index();
                                                result[i]._index_timeTree = ttc.index();
                                                result[i]._index_worldOrder = objectWorldOrder.index();
                                                if (call_keys2[reversedIndex * 3] == world) { //time is always precise here
                                                    result[i]._world_magic = -1;
                                                    result[i]._super_time_magic = -1;
                                                    result[i]._time_magic = -1;
                                                } else {
                                                    result[i]._world_magic = objectWorldOrder.magic();
                                                    result[i]._super_time_magic = stc.magic();
                                                    result[i]._time_magic = ttc.magic();
                                                }
                                                //we need to marks
                                            }
                                        } else {
                                            int nodeIndex = 0;
                                            for (int i = timeCollector.size() - 1; i >= timeCollector.size() - firmLimit; i--) {
                                                final int reversedIndex = (int) timeCollector.getValue(i);
                                                if (resolvedFactory == null) {
                                                    result[nodeIndex] = new BaseNode(call_keys2[reversedIndex * 3], timeCollector.getKey(i), id, selfPointer._graph);
                                                } else {
                                                    result[nodeIndex] = (BaseNode) resolvedFactory.create(call_keys2[reversedIndex * 3], timeCollector.getKey(i), id, selfPointer._graph);
                                                }
                                                result[nodeIndex]._dead = false;
                                                final SuperTimeTreeChunk stc = (SuperTimeTreeChunk) superTimeTrees[(int) tempSuperTimeCollector.get(timeTrees[reversedIndex].time())];
                                                final TimeTreeChunk ttc = (TimeTreeChunk) timeTrees[reversedIndex];
                                                _space.mark(stc.index());
                                                _space.mark(ttc.index());
                                                _space.mark(objectWorldOrder.index());
                                                result[nodeIndex]._index_superTimeTree = stc.index();
                                                result[nodeIndex]._index_timeTree = ttc.index();
                                                result[nodeIndex]._index_worldOrder = objectWorldOrder.index();
                                                if (call_keys2[reversedIndex * 3] == world) { //time is always precise here
                                                    result[nodeIndex]._world_magic = -1;
                                                    result[nodeIndex]._super_time_magic = -1;
                                                    result[nodeIndex]._time_magic = -1;
                                                } else {
                                                    result[nodeIndex]._world_magic = objectWorldOrder.magic();
                                                    result[nodeIndex]._super_time_magic = stc.magic();
                                                    result[nodeIndex]._time_magic = ttc.magic();
                                                }
                                                nodeIndex++;
                                            }
                                        }
                                        long[] call_keys3 = new long[firmLimit * 3];
                                        byte[] call_types3 = new byte[firmLimit];
                                        for (int i = 0; i < firmLimit; i++) {
                                            call_keys3[i * 3] = result[i].world();
                                            call_keys3[i * 3 + 1] = result[i].time();
                                            call_keys3[i * 3 + 2] = id;
                                            call_types3[i] = ChunkType.STATE_CHUNK;
                                            if (objectWorldOrder.type() == NodeValueType) {
                                                call_types3[i] = -1;
                                                result[i]._index_timeTree_offset = ((TimeTreeDValueChunk) timeTrees[i]).previousOrEqualOffset(result[i].time());
                                            }
                                        }
                                        _space.unmark(objectWorldOrder.index());
                                        for (int i = 0; i < timeTrees.length; i++) {
                                            if (timeTrees[i] != null) {
                                                _space.unmark(timeTrees[i].index());
                                            }
                                        }
                                        for (int i = 0; i < superTimeTrees.length; i++) {
                                            if (superTimeTrees[i] != null) {
                                                _space.unmark(superTimeTrees[i].index());
                                            }
                                        }
                                        getOrLoadAndMarkAll(call_types3, call_keys3, new Callback<Chunk[]>() {
                                            @Override
                                            public void on(Chunk[] stateChunks) {
                                                for (int i = 0; i < firmLimit; i++) {
                                                    if (stateChunks[i] != null) {
                                                        result[i]._index_stateChunk = stateChunks[i].index();
                                                    } else {
                                                        result[i]._index_stateChunk = -1;
                                                    }
                                                }
                                                callback.on(result);
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    private long resolve_world(final LongLongMap globalWorldOrder, final LongLongMap nodeWorldOrder, final long timeToResolve, long originWorld) {
        if (globalWorldOrder == null || nodeWorldOrder == null) {
            return originWorld;
        }
        long currentUniverse = originWorld;
        long previousUniverse = Constants.NULL_LONG;
        long divergenceTime = nodeWorldOrder.get(currentUniverse);
        while (currentUniverse != previousUniverse) {
            //check range
            if (divergenceTime != Constants.NULL_LONG && divergenceTime <= timeToResolve) {
                return currentUniverse;
            }
            //next round
            previousUniverse = currentUniverse;
            currentUniverse = globalWorldOrder.get(currentUniverse);
            divergenceTime = nodeWorldOrder.get(currentUniverse);
        }
        return originWorld;
    }

    /*
    private void getOrLoadAndMark(final byte type, final long world, final long time, final long id, final Callback<Chunk> callback) {
        if (world == CoreConstants.NULL_KEY[0] && time == CoreConstants.NULL_KEY[1] && id == CoreConstants.NULL_KEY[2]) {
            callback.on(null);
            return;
        }
        final MWResolver selfPointer = this;
        final Chunk cached = this._space.getAndMark(type, world, time, id);
        if (cached != null) {
            callback.on(cached);
        } else {
            final Buffer buffer = selfPointer._graph.newBuffer();
            KeyHelper.keyToBuffer(buffer, type, world, time, id);
            this._storage.get(buffer, new Callback<Buffer>() {
                @Override
                public void on(Buffer payloads) {
                    buffer.free();
                    Chunk result = null;
                    final BufferIterator it = payloads.iterator();
                    if (it.hasNext()) {
                        final Buffer view = it.next();
                        if (view.length() > 0) {
                            result = selfPointer._space.create(type, world, time, id, view, null);
                            selfPointer._space.putAndMark(type, world, time, id, result);
                        }
                    }
                    payloads.free();
                    callback.on(result);
                }
            });

        }
    }*/


    private void getOrLoadAndMarkAll(final byte[] types, final long[] keys, final Callback<Chunk[]> callback) {
        int nbKeys = keys.length / KEY_SIZE;
        double nbRounds = ((double) nbKeys) / max_load_keys;
        if (nbRounds < 0) {
            internal_getOrLoadAndMarkAll(nbKeys, types, keys, callback);
        } else {
            final Chunk[] finalResult = new Chunk[nbKeys];
            int nbCalls = (int) Math.ceil(nbRounds);
            DeferCounter counter = new CoreDeferCounter(nbCalls);
            counter.then(new Job() {
                @Override
                public void run() {
                    callback.on(finalResult);
                }
            });
            int loaded = 0;
            for (int i = 0; i < nbCalls; i++) {
                int truncatedSize = (int) max_load_keys;
                if ((loaded + truncatedSize) > nbKeys) {
                    truncatedSize = nbKeys - loaded;
                }
                final byte[] t_types = new byte[truncatedSize];
                final long[] t_keys = new long[truncatedSize * KEY_SIZE];
                System.arraycopy(types, loaded, t_types, 0, truncatedSize);
                System.arraycopy(keys, (loaded * KEY_SIZE), t_keys, 0, truncatedSize * KEY_SIZE);
                final int f_loaded = loaded;
                loaded = loaded + truncatedSize;
                internal_getOrLoadAndMarkAll(truncatedSize, t_types, t_keys, new Callback<Chunk[]>() {
                    @Override
                    public void on(Chunk[] result) {
                        System.arraycopy(result, 0, finalResult, f_loaded, result.length);
                        counter.count();
                    }
                });
            }
        }
    }

    private void internal_getOrLoadAndMarkAll(final int nbKeys, final byte[] types, final long[] keys, final Callback<Chunk[]> callback) {
        //int nbKeys = keys.length / KEY_SIZE;
        final boolean[] toLoadIndexes = new boolean[nbKeys];
        int nbElem = 0;
        final Chunk[] result = new Chunk[nbKeys];
        for (int i = 0; i < nbKeys; i++) {
            if (keys[i * KEY_SIZE] == CoreConstants.NULL_KEY[0] && keys[i * KEY_SIZE + 1] == CoreConstants.NULL_KEY[1] && keys[i * KEY_SIZE + 2] == CoreConstants.NULL_KEY[2]) {
                toLoadIndexes[i] = false;
                result[i] = null;
            } else {
                result[i] = this._space.getAndMark(types[i], keys[i * KEY_SIZE], keys[i * KEY_SIZE + 1], keys[i * KEY_SIZE + 2]);
                if (result[i] == null) {
                    toLoadIndexes[i] = true;
                    nbElem++;
                } else {
                    toLoadIndexes[i] = false;
                }
            }
        }
        if (nbElem == 0) {
            callback.on(result);
        } else {
            final Buffer keysToLoad = _graph.newBuffer();
            final int[] reverseIndex = new int[nbElem];
            int lastInsertedIndex = 0;
            for (int i = 0; i < nbKeys; i++) {
                if (toLoadIndexes[i]) {
                    reverseIndex[lastInsertedIndex] = i;
                    if (lastInsertedIndex != 0) {
                        keysToLoad.write(CoreConstants.BUFFER_SEP);
                    }
                    KeyHelper.keyToBuffer(keysToLoad, types[i], keys[i * KEY_SIZE], keys[i * KEY_SIZE + 1], keys[i * KEY_SIZE + 2]);
                    lastInsertedIndex = lastInsertedIndex + 1;
                }
            }
            final MWResolver selfPointer = this;
            this._storage.get(keysToLoad, new Callback<Buffer>() {
                @Override
                public void on(Buffer fromDbBuffers) {
                    keysToLoad.free();
                    BufferIterator it = fromDbBuffers.iterator();
                    int i = 0;
                    while (it.hasNext()) {
                        int reversedIndex = reverseIndex[i];
                        final Buffer view = it.next();
                        if (view.length() > 0) {
                            result[reversedIndex] = selfPointer._space.createAndMark(types[reversedIndex], keys[reversedIndex * KEY_SIZE], keys[reversedIndex * KEY_SIZE + 1], keys[reversedIndex * KEY_SIZE + 2]);
                            result[reversedIndex].load(view);
                        } else {
                            result[reversedIndex] = null;
                        }
                        i++;
                    }
                    fromDbBuffers.free();
                    callback.on(result);
                }
            });
        }
    }

    /*
    @Override
    public NodeState newState(Node node, long world, long time) {
        //Retrieve Node needed chunks
        final WorldOrderChunk nodeWorldOrder = (WorldOrderChunk) this._space.getAndMark(ChunkType.WORLD_ORDER_CHUNK, CoreConstants.NULL_LONG, CoreConstants.NULL_LONG, node.id());
        if (nodeWorldOrder == null) {
            return null;
        }
        //SOMETHING WILL MOVE HERE ANYWAY SO WE SYNC THE OBJECT, even for dePhasing read only objects because they can be unaligned after
        nodeWorldOrder.lock();
        //OK NOW WE HAVE THE TOKEN globally FOR the node ID
        StateChunk resultState = null;
        try {
            BaseNode castedNode = (BaseNode) node;
            //protection against deleted Node
            long[] previousResolveds = castedNode._previousResolveds.get();
            if (previousResolveds == null) {
                throw new RuntimeException(CoreConstants.DEAD_NODE_ERROR);
            }

            if (time < previousResolveds[CoreConstants.PREVIOUS_RESOLVED_TIME_MAGIC]) {
                throw new RuntimeException("New state cannot be used to create state before the previously resolved state");
            }

            long nodeId = node.id();

            //check if anything as moved
            if (previousResolveds[CoreConstants.PREVIOUS_RESOLVED_WORLD_INDEX] == world && previousResolveds[CoreConstants.PREVIOUS_RESOLVED_TIME_INDEX] == time) {
                //no new state to create
                resultState = (StateChunk) this._space.getAndMark(ChunkType.STATE_CHUNK, world, time, nodeId);
                this._space.unmarkChunk(resultState);
                this._space.unmarkChunk(nodeWorldOrder);
                return resultState;
            }

            //first we create and insert the empty state
            Chunk resultState_0 = this._space.create(ChunkType.STATE_CHUNK, world, time, nodeId, null, null);
            resultState = (StateChunk) _space.putAndMark(resultState_0);
            if (resultState_0 != resultState) {
                _space.free(resultState_0);
            }

            if (previousResolveds[CoreConstants.PREVIOUS_RESOLVED_WORLD_INDEX] == world || nodeWorldOrder.get(world) != CoreConstants.NULL_LONG) {

                //let's go for the resolution now
                TimeTreeChunk nodeSuperTimeTree = (TimeTreeChunk) this._space.getAndMark(ChunkType.TIME_TREE_CHUNK, previousResolveds[CoreConstants.PREVIOUS_RESOLVED_WORLD_INDEX], CoreConstants.NULL_LONG, nodeId);
                if (nodeSuperTimeTree == null) {
                    this._space.unmarkChunk(nodeWorldOrder);
                    return null;
                }
                TimeTreeChunk nodeTimeTree = (TimeTreeChunk) this._space.getAndMark(ChunkType.TIME_TREE_CHUNK, previousResolveds[CoreConstants.PREVIOUS_RESOLVED_WORLD_INDEX], previousResolveds[CoreConstants.PREVIOUS_RESOLVED_SUPER_TIME_INDEX], nodeId);
                if (nodeTimeTree == null) {
                    this._space.unmarkChunk(nodeSuperTimeTree);
                    this._space.unmarkChunk(nodeWorldOrder);
                    return null;
                }

                //manage super tree here
                long superTreeSize = nodeSuperTimeTree.size();
                long threshold = CoreConstants.SCALE_1 * 2;
                if (superTreeSize > threshold) {
                    threshold = CoreConstants.SCALE_2 * 2;
                }
                if (superTreeSize > threshold) {
                    threshold = CoreConstants.SCALE_3 * 2;
                }
                if (superTreeSize > threshold) {
                    threshold = CoreConstants.SCALE_4 * 2;
                }
                nodeTimeTree.insert(time);
                if (nodeTimeTree.size() == threshold) {
                    final long[] medianPoint = {-1};
                    //we iterate over the tree without boundaries for values, but with boundaries for number of collected times
                    nodeTimeTree.range(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, nodeTimeTree.size() / 2, new TreeWalker() {
                        @Override
                        public void elem(long t) {
                            medianPoint[0] = t;
                        }
                    });

                    TimeTreeChunk rightTree_0 = (TimeTreeChunk) this._space.create(ChunkType.TIME_TREE_CHUNK, world, medianPoint[0], nodeId, null, null);
                    TimeTreeChunk rightTree = (TimeTreeChunk) this._space.putAndMark(rightTree_0);
                    if (rightTree_0 != rightTree) {
                        this._space.free(rightTree_0);
                    }

                    //TODO second iterate that can be avoided, however we need the median point to create the right tree
                    //we iterate over the tree without boundaries for values, but with boundaries for number of collected times
                    final TimeTreeChunk finalRightTree = rightTree;
                    //rang iterate from the end of the tree
                    nodeTimeTree.range(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, nodeTimeTree.size() / 2, new TreeWalker() {
                        @Override
                        public void elem(long t) {
                            finalRightTree.insert(t);
                        }
                    });
                    nodeSuperTimeTree.insert(medianPoint[0]);
                    //remove times insert in the right tree
                    nodeTimeTree.clearAt(medianPoint[0]);

                    //ok ,now manage marks
                    if (time < medianPoint[0]) {

                        this._space.unmarkChunk(rightTree);
                        this._space.unmarkChunk(nodeSuperTimeTree);
                        this._space.unmarkChunk(nodeTimeTree);

                        long[] newResolveds = new long[6];
                        newResolveds[CoreConstants.PREVIOUS_RESOLVED_WORLD_INDEX] = world;

                        newResolveds[CoreConstants.PREVIOUS_RESOLVED_SUPER_TIME_INDEX] = previousResolveds[CoreConstants.PREVIOUS_RESOLVED_SUPER_TIME_INDEX];
                        newResolveds[CoreConstants.PREVIOUS_RESOLVED_TIME_INDEX] = time;
                        newResolveds[CoreConstants.PREVIOUS_RESOLVED_WORLD_MAGIC] = nodeWorldOrder.magic();
                        newResolveds[CoreConstants.PREVIOUS_RESOLVED_SUPER_TIME_MAGIC] = nodeSuperTimeTree.magic();
                        newResolveds[CoreConstants.PREVIOUS_RESOLVED_TIME_MAGIC] = nodeTimeTree.magic();
                        castedNode._previousResolveds.set(newResolveds);
                    } else {

                        //double unMark current nodeTimeTree
                        this._space.unmarkChunk(nodeTimeTree);
                        this._space.unmarkChunk(nodeTimeTree);
                        //unmark node superTimeTree
                        this._space.unmarkChunk(nodeSuperTimeTree);

                        //let's store the new state if necessary
                        long[] newResolveds = new long[6];
                        newResolveds[CoreConstants.PREVIOUS_RESOLVED_WORLD_INDEX] = world;
                        newResolveds[CoreConstants.PREVIOUS_RESOLVED_SUPER_TIME_INDEX] = medianPoint[0];
                        newResolveds[CoreConstants.PREVIOUS_RESOLVED_TIME_INDEX] = time;
                        newResolveds[CoreConstants.PREVIOUS_RESOLVED_WORLD_MAGIC] = nodeWorldOrder.magic();
                        newResolveds[CoreConstants.PREVIOUS_RESOLVED_SUPER_TIME_MAGIC] = rightTree.magic();
                        newResolveds[CoreConstants.PREVIOUS_RESOLVED_TIME_MAGIC] = nodeTimeTree.magic();
                        castedNode._previousResolveds.set(newResolveds);
                    }
                } else {
                    //update the state cache without superTree modification
                    long[] newResolveds = new long[6];
                    //previously resolved
                    newResolveds[CoreConstants.PREVIOUS_RESOLVED_WORLD_INDEX] = world;
                    newResolveds[CoreConstants.PREVIOUS_RESOLVED_SUPER_TIME_INDEX] = previousResolveds[CoreConstants.PREVIOUS_RESOLVED_SUPER_TIME_INDEX];
                    newResolveds[CoreConstants.PREVIOUS_RESOLVED_TIME_INDEX] = time;
                    //previously magics
                    newResolveds[CoreConstants.PREVIOUS_RESOLVED_WORLD_MAGIC] = nodeWorldOrder.magic();
                    newResolveds[CoreConstants.PREVIOUS_RESOLVED_SUPER_TIME_MAGIC] = nodeSuperTimeTree.magic();
                    newResolveds[CoreConstants.PREVIOUS_RESOLVED_TIME_MAGIC] = nodeTimeTree.magic();
                    castedNode._previousResolveds.set(newResolveds);

                    this._space.unmarkChunk(nodeSuperTimeTree);
                    this._space.unmarkChunk(nodeTimeTree);
                }
            } else {

                //create a new node superTimeTree
                TimeTreeChunk newSuperTimeTree_0 = (TimeTreeChunk) this._space.create(ChunkType.TIME_TREE_CHUNK, world, CoreConstants.NULL_LONG, nodeId, null, null);
                TimeTreeChunk newSuperTimeTree = (TimeTreeChunk) this._space.putAndMark(newSuperTimeTree_0);
                if (newSuperTimeTree != newSuperTimeTree_0) {
                    this._space.free(newSuperTimeTree_0);
                }
                newSuperTimeTree.insert(time);

                //create a new node timeTree
                TimeTreeChunk newTimeTree_0 = (TimeTreeChunk) this._space.create(ChunkType.TIME_TREE_CHUNK, world, time, nodeId, null, null);
                TimeTreeChunk newTimeTree = (TimeTreeChunk) this._space.putAndMark(newTimeTree_0);
                if (newTimeTree != newTimeTree_0) {
                    this._space.free(newTimeTree_0);
                }
                newTimeTree.insert(time);

                //insert into node world order
                nodeWorldOrder.put(world, time);

                //let's store the new state if necessary
                long[] newResolveds = new long[6];
                //previously resolved
                newResolveds[CoreConstants.PREVIOUS_RESOLVED_WORLD_INDEX] = world;
                newResolveds[CoreConstants.PREVIOUS_RESOLVED_SUPER_TIME_INDEX] = time;
                newResolveds[CoreConstants.PREVIOUS_RESOLVED_TIME_INDEX] = time;
                //previously magics
                newResolveds[CoreConstants.PREVIOUS_RESOLVED_WORLD_MAGIC] = nodeWorldOrder.magic();
                newResolveds[CoreConstants.PREVIOUS_RESOLVED_SUPER_TIME_MAGIC] = newSuperTimeTree.magic();
                newResolveds[CoreConstants.PREVIOUS_RESOLVED_TIME_MAGIC] = newTimeTree.magic();
                castedNode._previousResolveds.set(newResolveds);

                //unMark previous super Tree
                _space.unmark(ChunkType.TIME_TREE_CHUNK, previousResolveds[CoreConstants.PREVIOUS_RESOLVED_WORLD_INDEX], Constants.NULL_LONG, nodeId);
                //unMark previous time Tree
                _space.unmark(ChunkType.TIME_TREE_CHUNK, previousResolveds[CoreConstants.PREVIOUS_RESOLVED_WORLD_INDEX], previousResolveds[CoreConstants.PREVIOUS_RESOLVED_SUPER_TIME_INDEX], nodeId);

            }

            //unMark previous state, for the newly created one
            _space.unmark(ChunkType.STATE_CHUNK, previousResolveds[CoreConstants.PREVIOUS_RESOLVED_WORLD_INDEX], previousResolveds[CoreConstants.PREVIOUS_RESOLVED_TIME_INDEX], nodeId);
            _space.unmarkChunk(nodeWorldOrder);

        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            nodeWorldOrder.unlock();
        }
        return (NodeState) resultState;
    }*/

    @Override
    public final NodeState resolveState(final Node node) {
        return internal_resolveState(node, true);
    }

    private StateChunk internal_resolveState(final Node node, final boolean safe) {
        final BaseNode castedNode = (BaseNode) node;
        StateChunk stateResult = null;
        if (safe) {
            castedNode.cacheLock();
        }
        if (castedNode._dead) {
            if (safe) {
                castedNode.cacheUnlock();
            }
            throw new RuntimeException(CoreConstants.DEAD_NODE_ERROR + " node id: " + node.id());
        }
        /* OPTIMIZATION #1: NO DEPHASING */
        if (castedNode._world_magic == -1 && castedNode._time_magic == -1 && castedNode._super_time_magic == -1) {
            if (castedNode._index_stateChunk == -1) {
                final TimeTreeEmbeddedChunk ttec = (TimeTreeEmbeddedChunk) this._space.get(castedNode._index_timeTree);
                stateResult = ttec.state(castedNode._index_timeTree_offset);
            } else {
                stateResult = (StateChunk) this._space.get(castedNode._index_stateChunk);
            }
        } else {
            /* OPTIMIZATION #2: SAME DEPHASING */
            final WorldOrderChunk nodeWorldOrder = (WorldOrderChunk) this._space.get(castedNode._index_worldOrder);
            SuperTimeTreeChunk nodeSuperTimeTree = (SuperTimeTreeChunk) this._space.get(castedNode._index_superTimeTree);
            TimeTreeChunk nodeTimeTree = (TimeTreeChunk) this._space.get(castedNode._index_timeTree);
            if (nodeWorldOrder != null && nodeSuperTimeTree != null && nodeTimeTree != null) {
                if (castedNode._world_magic == nodeWorldOrder.magic() && castedNode._super_time_magic == nodeSuperTimeTree.magic() && castedNode._time_magic == nodeTimeTree.magic()) {
                    if (castedNode._index_stateChunk == -1) {
                        stateResult = ((TimeTreeEmbeddedChunk) nodeTimeTree).state(castedNode._index_timeTree_offset);
                    } else {
                        stateResult = (StateChunk) this._space.get(castedNode._index_stateChunk);
                    }
                } else {
                    /* NOMINAL CASE, LET'S RESOLVE AGAIN */
                    if (safe) {
                        nodeWorldOrder.lock();
                    }
                    final long nodeTime = castedNode.time();
                    final long nodeId = castedNode.id();
                    final long nodeWorld = castedNode.world();
                    //Common case, we have to traverseIndex World Order and Time chunks
                    final long resolvedWorld = resolve_world(globalWorldOrderChunk, nodeWorldOrder, nodeTime, nodeWorld);
                    if (resolvedWorld != nodeSuperTimeTree.world()) {
                        //we have to update the superTree
                        final SuperTimeTreeChunk tempNodeSuperTimeTree = (SuperTimeTreeChunk) this._space.getAndMark(ChunkType.SUPER_TIME_TREE_CHUNK, resolvedWorld, 0, nodeId);
                        if (tempNodeSuperTimeTree != null) {
                            _space.unmark(nodeSuperTimeTree.index());
                            nodeSuperTimeTree = tempNodeSuperTimeTree;
                        }
                    }
                    long resolvedSuperTime = nodeSuperTimeTree.previousOrEqual(nodeTime);
                    if (resolvedSuperTime != nodeTimeTree.time()) {
                        //we have to update the timeTree
                        final TimeTreeChunk tempNodeTimeTree = (TimeTreeChunk) this._space.getAndMark(ChunkType.TIME_TREE_CHUNK, resolvedWorld, resolvedSuperTime, nodeId);
                        if (tempNodeTimeTree != null) {
                            _space.unmark(nodeTimeTree.index());
                            nodeTimeTree = tempNodeTimeTree;
                        }
                    }
                    final int resolvedTimeOffset = nodeTimeTree.previousOrEqualOffset(nodeTime);
                    final long resolvedTime = nodeTimeTree.getKey(resolvedTimeOffset);
                    //are we still unphased
                    if (resolvedWorld == nodeWorld && resolvedTime == nodeTime) {
                        castedNode._world_magic = -1;
                        castedNode._time_magic = -1;
                        castedNode._super_time_magic = -1;
                        //save updated index
                        castedNode._index_superTimeTree = nodeSuperTimeTree.index();
                        castedNode._index_timeTree = nodeTimeTree.index();
                    } else {
                        //save magic numbers
                        castedNode._world_magic = nodeWorldOrder.magic();
                        castedNode._time_magic = nodeTimeTree.magic();
                        castedNode._super_time_magic = nodeSuperTimeTree.magic();
                        //save updated index
                        castedNode._index_superTimeTree = nodeSuperTimeTree.index();
                        castedNode._index_timeTree = nodeTimeTree.index();
                    }
                    if (castedNode._index_stateChunk == -1) {
                        stateResult = ((TimeTreeEmbeddedChunk) nodeTimeTree).state(resolvedTimeOffset);
                        castedNode._index_timeTree_offset = resolvedTimeOffset;
                    } else {
                        stateResult = (StateChunk) this._space.get(castedNode._index_stateChunk);
                        if (resolvedWorld != stateResult.world() || resolvedTime != stateResult.time()) {
                            final StateChunk tempNodeState = (StateChunk) this._space.getAndMark(ChunkType.STATE_CHUNK, resolvedWorld, resolvedTime, nodeId);
                            if (tempNodeState != null) {
                                this._space.unmark(stateResult.index());
                                stateResult = tempNodeState;
                                castedNode._index_stateChunk = stateResult.index();
                            } else {
                                throw new RuntimeException("GreyCat Internal Exception");
                            }
                        }
                    }
                    if (safe) {
                        nodeWorldOrder.unlock();
                    }
                }
            }
        }
        if (safe) {
            castedNode.cacheUnlock();
        }
        return stateResult;
    }


    @Override
    public final void end(Node node) {
        final BaseNode castedNode = (BaseNode) node;
        castedNode.cacheLock();
        if (castedNode._dead) {
            castedNode.cacheUnlock();
            throw new RuntimeException(CoreConstants.DEAD_NODE_ERROR + " node id: " + node.id());
        }
        final WorldOrderChunk nodeWorldOrder = (WorldOrderChunk) this._space.get(castedNode._index_worldOrder);
        if (nodeWorldOrder == null) {
            castedNode.cacheUnlock();
            return;
        }
        nodeWorldOrder.lock();
        final long alignedTime = node.time();
        final long nodeWorld = castedNode.world();
        final SuperTimeTreeChunk superTimeTree = (SuperTimeTreeChunk) this._space.get(castedNode._index_superTimeTree);
        if (superTimeTree.world() == nodeWorld) {
            superTimeTree.setEnd(alignedTime);
        } else {
            SuperTimeTreeChunk newSuperTimeTree = (SuperTimeTreeChunk) this._space.createAndMark(ChunkType.SUPER_TIME_TREE_CHUNK, nodeWorld, 0, castedNode.id());
            newSuperTimeTree.setGroup(nodeWorldOrder.group());

            newSuperTimeTree.setEnd(alignedTime);
            //insert into node world order
            nodeWorldOrder.put(nodeWorld, alignedTime);
            _space.unmark(newSuperTimeTree.index());
        }
        nodeWorldOrder.unlock();
        castedNode.cacheUnlock();
    }

    @Override
    public final NodeState alignState(final Node node) {
        final BaseNode castedNode = (BaseNode) node;
        castedNode.cacheLock();
        if (castedNode._dead) {
            castedNode.cacheUnlock();
            throw new RuntimeException(CoreConstants.DEAD_NODE_ERROR + " node id: " + node.id());
        }
        //OPTIMIZATION #1: NO DEPHASING
        if (castedNode._world_magic == -1 && castedNode._time_magic == -1 && castedNode._super_time_magic == -1) {
            if (castedNode._index_stateChunk == -1) {
                final TimeTreeEmbeddedChunk ttec = (TimeTreeEmbeddedChunk) this._space.get(castedNode._index_timeTree);
                final StateChunk result = ttec.state(castedNode._index_timeTree_offset);
                castedNode.cacheUnlock();
                return result;
            } else {
                final StateChunk currentEntry = (StateChunk) this._space.get(castedNode._index_stateChunk);
                if (currentEntry != null) {
                    castedNode.cacheUnlock();
                    return currentEntry;
                }
            }
        }
        //NOMINAL CASE
        final WorldOrderChunk nodeWorldOrder = (WorldOrderChunk) this._space.get(castedNode._index_worldOrder);
        if (nodeWorldOrder == null) {
            castedNode.cacheUnlock();
            return null;
        }
        nodeWorldOrder.lock();
        //Get the previous StateChunk
        final StateChunk previouStateChunk = internal_resolveState(node, false);
        final long previousTime = previouStateChunk.time();
        final long previousWorld = previouStateChunk.world();
        if (castedNode._world_magic == -1 && castedNode._time_magic == -1 && castedNode._super_time_magic == -1) {
            //it has been already rePhased, just return
            nodeWorldOrder.unlock();
            castedNode.cacheUnlock();
            return previouStateChunk;
        }
        final long nodeWorld = node.world();
        long nodeTime = node.time();
        final long nodeId = node.id();
        //compute time sensitivity
        final SuperTimeTreeChunk superTimeTree = (SuperTimeTreeChunk) this._space.get(castedNode._index_superTimeTree);
        final long timeSensitivity = superTimeTree.timeSensitivity();
        if (timeSensitivity != 0 && timeSensitivity != Constants.NULL_LONG) {
            if (timeSensitivity < 0) {
                nodeTime = previousTime;
            } else {
                long timeSensitivityOffset = superTimeTree.timeSensitivityOffset();
                if (timeSensitivityOffset == Constants.NULL_LONG) {
                    timeSensitivityOffset = 0;
                }
                nodeTime = nodeTime - (nodeTime % timeSensitivity) + timeSensitivityOffset;
            }
        }
        StateChunk clonedState = null;
        if (castedNode._index_stateChunk != -1) {
            if (nodeTime != previousTime || nodeWorld != previousWorld) {
                try {
                    clonedState = (StateChunk) this._space.createAndMark(ChunkType.STATE_CHUNK, nodeWorld, nodeTime, nodeId);
                    clonedState.loadFrom(previouStateChunk);
                } catch (Exception e) {
                    nodeWorldOrder.unlock();
                    castedNode.cacheUnlock();
                    throw e;
                }
                castedNode._index_stateChunk = clonedState.index();
                _space.unmark(previouStateChunk.index());
            } else {
                clonedState = previouStateChunk;
            }
        }
        castedNode._world_magic = -1;
        castedNode._super_time_magic = -1;
        castedNode._time_magic = -1;

        byte subTreeType = ChunkType.TIME_TREE_CHUNK;
        if (castedNode._index_stateChunk == -1) {
            subTreeType = ChunkType.TIME_TREE_DVALUE_CHUNK;
        }
        if (previousWorld == nodeWorld || nodeWorldOrder.get(nodeWorld) != CoreConstants.NULL_LONG) {
            //final TimeTreeChunk superTimeTree = (TimeTreeChunk) this._space.get(castedNode._index_superTimeTree);
            final TimeTreeChunk timeTree = (TimeTreeChunk) this._space.get(castedNode._index_timeTree);
            final long subTreeCapacity = timeTree.capacity();
            //if (true) {
            if (timeTree.size() < subTreeCapacity) {
                //easy, just insert the new timeslot
                castedNode._index_timeTree_offset = timeTree.insert(nodeTime);
                if (superTimeTree.lastKey() == timeTree.time()) {
                    superTimeTree.setLastValue(timeTree.size());
                } else {
                    superTimeTree.insert(timeTree.time(), timeTree.size());
                }
            } else {
                //are we the last last one?
                //if (false) {
                if (superTimeTree.lastKey() == timeTree.time()) {
                    long p_found = timeTree.max();
                    if (p_found == nodeTime) {
                        throw new RuntimeException("GreyCat Internal Error");
                        //noop
                    } else if (nodeTime < p_found) {
                        //we insert in the past
                        castedNode._index_timeTree_offset = timeTree.insert(nodeTime);
                        timeTree.setCapacity(subTreeCapacity + 1);
                        superTimeTree.insert(timeTree.time(), subTreeCapacity + 1);
                    } else {
                        final TimeTreeChunk newTimeTree = (TimeTreeChunk) this._space.createAndMark(subTreeType, nodeWorld, nodeTime, nodeId);
                        newTimeTree.setGroup(nodeWorldOrder.group());

                        final long allowedSubTreeCapacity = superTimeTree.subTreeCapacity();
                        castedNode._index_timeTree_offset = newTimeTree.insert(nodeTime);
                        newTimeTree.setCapacity(allowedSubTreeCapacity);
                        superTimeTree.insert(nodeTime, allowedSubTreeCapacity);
                        _space.unmark(castedNode._index_timeTree);
                        castedNode._index_timeTree = newTimeTree.index();
                    }
                } else {
                    //insertion in past, oversize tree
                    castedNode._index_timeTree_offset = timeTree.insert(nodeTime);
                    timeTree.setCapacity(subTreeCapacity + 1);
                    superTimeTree.insert(timeTree.time(), subTreeCapacity + 1);
                }
            }
        } else {
            //create a new node superTimeTree
            SuperTimeTreeChunk newSuperTimeTree = (SuperTimeTreeChunk) this._space.createAndMark(ChunkType.SUPER_TIME_TREE_CHUNK, nodeWorld, 0, nodeId);
            newSuperTimeTree.setGroup(nodeWorldOrder.group());

            long subTreeCapacity = superTimeTree.subTreeCapacity();
            newSuperTimeTree.insert(nodeTime, subTreeCapacity);
            //create a new node timeTree
            TimeTreeChunk newTimeTree = (TimeTreeChunk) this._space.createAndMark(subTreeType, nodeWorld, nodeTime, nodeId);
            newTimeTree.setGroup(nodeWorldOrder.group());

            castedNode._index_timeTree_offset = newTimeTree.insert(nodeTime);
            newTimeTree.setCapacity(subTreeCapacity);
            //insert into node world order
            nodeWorldOrder.put(nodeWorld, nodeTime);
            //let's store the new state if necessary
            _space.unmark(castedNode._index_timeTree);
            _space.unmark(castedNode._index_superTimeTree);
            castedNode._index_timeTree = newTimeTree.index();
            castedNode._index_superTimeTree = newSuperTimeTree.index();
        }
        if (castedNode._index_stateChunk == -1) {
            clonedState = ((TimeTreeEmbeddedChunk) _space.get(castedNode._index_timeTree)).state(castedNode._index_timeTree_offset);
        }
        nodeWorldOrder.unlock();
        castedNode.cacheUnlock();
        return clonedState;
    }

    @Override
    public NodeState newState(Node node, long world, long time) {
        final BaseNode castedNode = (BaseNode) node;

        NodeState resolved;
        castedNode.cacheLock();

        BaseNode fakeNode = new BaseNode(world, time, node.id(), node.graph());
        fakeNode._index_worldOrder = castedNode._index_worldOrder;
        fakeNode._index_superTimeTree = castedNode._index_superTimeTree;
        fakeNode._index_timeTree = castedNode._index_timeTree;
        fakeNode._index_stateChunk = castedNode._index_stateChunk;

        fakeNode._time_magic = castedNode._time_magic;
        fakeNode._super_time_magic = castedNode._super_time_magic;
        fakeNode._world_magic = castedNode._world_magic;

        resolved = alignState(fakeNode);

        castedNode._index_worldOrder = fakeNode._index_worldOrder;
        castedNode._index_superTimeTree = fakeNode._index_superTimeTree;
        castedNode._index_timeTree = fakeNode._index_timeTree;
        castedNode._index_stateChunk = fakeNode._index_stateChunk;

        castedNode._time_magic = fakeNode._time_magic;
        castedNode._super_time_magic = fakeNode._super_time_magic;
        castedNode._world_magic = fakeNode._world_magic;

        castedNode.cacheUnlock();

        return resolved;
    }

    @Override
    public void resolveTimepoints(final Node node, final long beginningOfSearch, final long endOfSearch, final Callback<long[]> callback) {
        internal_resolveTimepoints(node, beginningOfSearch, endOfSearch, callback);
    }

    @Override
    public void countTimepoints(final Node node, final long beginningOfSearch, final long endOfSearch, final Callback<Long> callback) {
        internal_countTimepoints(node, beginningOfSearch, endOfSearch, callback);
    }

    private void internal_countTimepoints(final Node node, final long beginningOfSearch, final long endOfSearch, final Callback<Long> callback) {
        final MWResolver selfPointer = this;
        _space.getOrLoadAndMark(ChunkType.WORLD_ORDER_CHUNK, 0, 0, node.id(), new Callback<Chunk>() {
            @Override
            public void on(Chunk resolved) {
                if (resolved == null) {
                    callback.on(0L);
                    return;
                }
                final WorldOrderChunk objectWorldOrder = (WorldOrderChunk) resolved;
                //worlds collector
                final int[] collectionSize = {CoreConstants.MAP_INITIAL_CAPACITY};
                final long[][] collectedWorlds = {new long[collectionSize[0]]};
                int collectedIndex = 0;
                long currentWorld = node.world();
                while (currentWorld != CoreConstants.NULL_LONG) {
                    long divergenceTimepoint = objectWorldOrder.get(currentWorld);
                    if (divergenceTimepoint != CoreConstants.NULL_LONG) {
                        if (divergenceTimepoint <= beginningOfSearch) {
                            //take the first one before leaving
                            collectedWorlds[0][collectedIndex] = currentWorld;
                            collectedIndex++;
                            break;
                        } else if (divergenceTimepoint > endOfSearch) {
                            //next round, go to parent world
                            currentWorld = selfPointer.globalWorldOrderChunk.get(currentWorld);
                        } else {
                            //that's fit, add to search
                            collectedWorlds[0][collectedIndex] = currentWorld;
                            collectedIndex++;
                            if (collectedIndex == collectionSize[0]) {
                                //reallocate
                                long[] temp_collectedWorlds = new long[collectionSize[0] * 2];
                                System.arraycopy(collectedWorlds[0], 0, temp_collectedWorlds, 0, collectionSize[0]);
                                collectedWorlds[0] = temp_collectedWorlds;
                                collectionSize[0] = collectionSize[0] * 2;
                            }
                            //go to parent
                            currentWorld = selfPointer.globalWorldOrderChunk.get(currentWorld);
                        }
                    } else {
                        //go to parent
                        currentWorld = selfPointer.globalWorldOrderChunk.get(currentWorld);
                    }
                }
                //create request concat keys
                selfPointer.countTimepointsFromWorlds(objectWorldOrder, node, beginningOfSearch, endOfSearch, collectedWorlds[0], collectedIndex, callback);
            }
        });
    }

    private void countTimepointsFromWorlds(final WorldOrderChunk objectWorldOrder, final Node node, final long beginningOfSearch, final long endOfSearch, final long[] collectedWorlds, final int collectedWorldsSize, final Callback<Long> callback) {
        final MWResolver selfPointer = this;
        final long[] timeTreeKeys = new long[collectedWorldsSize * 3];
        final byte[] types = new byte[collectedWorldsSize];
        for (int i = 0; i < collectedWorldsSize; i++) {
            timeTreeKeys[i * 3] = collectedWorlds[i];
            timeTreeKeys[i * 3 + 1] = 0;
            timeTreeKeys[i * 3 + 2] = node.id();
            types[i] = ChunkType.SUPER_TIME_TREE_CHUNK;
        }
        getOrLoadAndMarkAll(types, timeTreeKeys, new Callback<Chunk[]>() {
            @Override
            public void on(final Chunk[] superTimeTrees) {
                if (superTimeTrees == null) {
                    selfPointer._space.unmark(objectWorldOrder.index());
                    callback.on(0L);
                } else {
                    //time collector

                    final long[] timesCounters = new long[1];
                    timesCounters[0] = 0;

                    long previousDivergenceTime = endOfSearch;
                    for (int i = 0; i < collectedWorldsSize; i++) {
                        final SuperTimeTreeChunk timeTree = (SuperTimeTreeChunk) superTimeTrees[i];
                        if (timeTree != null) {
                            long currentDivergenceTime = objectWorldOrder.get(collectedWorlds[i]);
                            final long finalPreviousDivergenceTime = previousDivergenceTime;
                            timeTree.range(currentDivergenceTime, previousDivergenceTime, CoreConstants.END_OF_TIME, new SuperTreeWalker() {
                                @Override
                                public void elem(long t, long capacity) {
                                    if (t != finalPreviousDivergenceTime) {
                                        timesCounters[0] = timesCounters[0] + capacity;
                                    }
                                }
                            });
                            previousDivergenceTime = currentDivergenceTime;
                            selfPointer._space.unmark(timeTree.index());
                        }
                    }
                    selfPointer._space.unmark(objectWorldOrder.index());
                    callback.on(timesCounters[0]);
                }
            }
        });
    }

    private void internal_resolveTimepoints(final Node node, final long beginningOfSearch, final long endOfSearch, final Callback<long[]> callback) {
        final MWResolver selfPointer = this;
        _space.getOrLoadAndMark(ChunkType.WORLD_ORDER_CHUNK, 0, 0, node.id(), new Callback<Chunk>() {
            @Override
            public void on(Chunk resolved) {
                if (resolved == null) {
                    callback.on(new long[0]);
                    return;
                }
                final WorldOrderChunk objectWorldOrder = (WorldOrderChunk) resolved;
                //worlds collector
                final int[] collectionSize = {CoreConstants.MAP_INITIAL_CAPACITY};
                final long[][] collectedWorlds = {new long[collectionSize[0]]};
                int collectedIndex = 0;
                long currentWorld = node.world();
                while (currentWorld != CoreConstants.NULL_LONG) {
                    long divergenceTimepoint = objectWorldOrder.get(currentWorld);
                    if (divergenceTimepoint != CoreConstants.NULL_LONG) {
                        if (divergenceTimepoint <= beginningOfSearch) {
                            //take the first one before leaving
                            collectedWorlds[0][collectedIndex] = currentWorld;
                            collectedIndex++;
                            break;
                        } else if (divergenceTimepoint > endOfSearch) {
                            //next round, go to parent world
                            currentWorld = selfPointer.globalWorldOrderChunk.get(currentWorld);
                        } else {
                            //that's fit, add to search
                            collectedWorlds[0][collectedIndex] = currentWorld;
                            collectedIndex++;
                            if (collectedIndex == collectionSize[0]) {
                                //reallocate
                                long[] temp_collectedWorlds = new long[collectionSize[0] * 2];
                                System.arraycopy(collectedWorlds[0], 0, temp_collectedWorlds, 0, collectionSize[0]);
                                collectedWorlds[0] = temp_collectedWorlds;
                                collectionSize[0] = collectionSize[0] * 2;
                            }
                            //go to parent
                            currentWorld = selfPointer.globalWorldOrderChunk.get(currentWorld);
                        }
                    } else {
                        //go to parent
                        currentWorld = selfPointer.globalWorldOrderChunk.get(currentWorld);
                    }
                }
                //create request concat keys
                selfPointer.resolveTimepointsFromWorlds(objectWorldOrder, node, beginningOfSearch, endOfSearch, collectedWorlds[0], collectedIndex, callback);
            }
        });
    }


    private void resolveTimepointsFromWorlds(final WorldOrderChunk objectWorldOrder, final Node node, final long beginningOfSearch, final long endOfSearch, final long[] collectedWorlds, final int collectedWorldsSize, final Callback<long[]> callback) {
        final MWResolver selfPointer = this;
        final long[] timeTreeKeys = new long[collectedWorldsSize * 3];
        final byte[] types = new byte[collectedWorldsSize];
        for (int i = 0; i < collectedWorldsSize; i++) {
            timeTreeKeys[i * 3] = collectedWorlds[i];
            timeTreeKeys[i * 3 + 1] = 0;
            timeTreeKeys[i * 3 + 2] = node.id();
            types[i] = ChunkType.SUPER_TIME_TREE_CHUNK;
        }
        getOrLoadAndMarkAll(types, timeTreeKeys, new Callback<Chunk[]>() {
            @Override
            public void on(final Chunk[] superTimeTrees) {
                if (superTimeTrees == null) {
                    selfPointer._space.unmark(objectWorldOrder.index());
                    callback.on(new long[0]);
                } else {
                    //time collector
                    final int[] collectedSize = {CoreConstants.MAP_INITIAL_CAPACITY};
                    final long[][] collectedSuperTimes = {new long[collectedSize[0]]};
                    final long[][] collectedSuperTimesAssociatedWorlds = {new long[collectedSize[0]]};
                    final int[] insert_index = {0};

                    long previousDivergenceTime = endOfSearch;
                    for (int i = 0; i < collectedWorldsSize; i++) {
                        final SuperTimeTreeChunk timeTree = (SuperTimeTreeChunk) superTimeTrees[i];
                        if (timeTree != null) {
                            long currentDivergenceTime = objectWorldOrder.get(collectedWorlds[i]);
                            //if (currentDivergenceTime < beginningOfSearch) {
                            //    currentDivergenceTime = beginningOfSearch;
                            //}
                            final long finalPreviousDivergenceTime = previousDivergenceTime;
                            timeTree.range(currentDivergenceTime, previousDivergenceTime, CoreConstants.END_OF_TIME, new SuperTreeWalker() {
                                @Override
                                public void elem(long t, long capacity) {
                                    if (t != finalPreviousDivergenceTime) {
                                        collectedSuperTimes[0][insert_index[0]] = t;
                                        collectedSuperTimesAssociatedWorlds[0][insert_index[0]] = timeTree.world();
                                        insert_index[0]++;
                                        if (collectedSize[0] == insert_index[0]) {
                                            //reallocate
                                            long[] temp_collectedSuperTimes = new long[collectedSize[0] * 2];
                                            long[] temp_collectedSuperTimesAssociatedWorlds = new long[collectedSize[0] * 2];
                                            System.arraycopy(collectedSuperTimes[0], 0, temp_collectedSuperTimes, 0, collectedSize[0]);
                                            System.arraycopy(collectedSuperTimesAssociatedWorlds[0], 0, temp_collectedSuperTimesAssociatedWorlds, 0, collectedSize[0]);

                                            collectedSuperTimes[0] = temp_collectedSuperTimes;
                                            collectedSuperTimesAssociatedWorlds[0] = temp_collectedSuperTimesAssociatedWorlds;

                                            collectedSize[0] = collectedSize[0] * 2;
                                        }
                                    }
                                }
                            });
                            previousDivergenceTime = currentDivergenceTime;
                        }
                        selfPointer._space.unmark(timeTree.index());
                    }
                    //now we have superTimes, lets convert them to all times
                    selfPointer.resolveTimepointsFromSuperTimes(objectWorldOrder, node, beginningOfSearch, endOfSearch, collectedSuperTimesAssociatedWorlds[0], collectedSuperTimes[0], insert_index[0], callback);
                }
            }
        });
    }

    private void resolveTimepointsFromSuperTimes(final WorldOrderChunk objectWorldOrder, final Node node, final long beginningOfSearch, final long endOfSearch, final long[] collectedWorlds, final long[] collectedSuperTimes, final int collectedSize, final Callback<long[]> callback) {
        final MWResolver selfPointer = this;
        final long[] timeTreeKeys = new long[collectedSize * 3];
        final byte[] types = new byte[collectedSize];
        for (int i = 0; i < collectedSize; i++) {
            timeTreeKeys[i * 3] = collectedWorlds[i];
            timeTreeKeys[i * 3 + 1] = collectedSuperTimes[i];
            timeTreeKeys[i * 3 + 2] = node.id();
            if (objectWorldOrder.type() == NodeValueType) {
                types[i] = ChunkType.TIME_TREE_DVALUE_CHUNK;
            } else {
                types[i] = ChunkType.TIME_TREE_CHUNK;
            }

        }
        getOrLoadAndMarkAll(types, timeTreeKeys, new Callback<Chunk[]>() {
            @Override
            public void on(Chunk[] timeTrees) {
                if (timeTrees == null) {
                    selfPointer._space.unmark(objectWorldOrder.index());
                    callback.on(new long[0]);
                } else {
                    //time collector
                    final int[] collectedTimesSize = {CoreConstants.MAP_INITIAL_CAPACITY};
                    final long[][] collectedTimes = {new long[collectedTimesSize[0]]};
                    final int[] insert_index = {0};
                    long previousDivergenceTime = endOfSearch;
                    for (int i = 0; i < collectedSize; i++) {
                        final TimeTreeChunk timeTree = (TimeTreeChunk) timeTrees[i];
                        if (timeTree != null) {
                            long currentDivergenceTime = objectWorldOrder.get(collectedWorlds[i]);
                            if (currentDivergenceTime < beginningOfSearch) {
                                currentDivergenceTime = beginningOfSearch;
                            }
                            final long finalPreviousDivergenceTime = previousDivergenceTime;
                            timeTree.range(currentDivergenceTime, previousDivergenceTime, CoreConstants.END_OF_TIME, new TreeWalker() {
                                @Override
                                public void elem(long t) {
                                    if (t != finalPreviousDivergenceTime) {
                                        collectedTimes[0][insert_index[0]] = t;
                                        insert_index[0]++;
                                        if (collectedTimesSize[0] == insert_index[0]) {
                                            //reallocate
                                            long[] temp_collectedTimes = new long[collectedTimesSize[0] * 2];
                                            System.arraycopy(collectedTimes[0], 0, temp_collectedTimes, 0, collectedTimesSize[0]);
                                            collectedTimes[0] = temp_collectedTimes;
                                            collectedTimesSize[0] = collectedTimesSize[0] * 2;
                                        }
                                    }
                                }
                            });
                            if (i < collectedSize - 1) {
                                if (collectedWorlds[i + 1] != collectedWorlds[i]) {
                                    //world overriding semantic
                                    previousDivergenceTime = currentDivergenceTime;
                                }
                            }
                            selfPointer._space.unmark(timeTree.index());
                        }
                    }
                    //now we have times
                    if (insert_index[0] != collectedTimesSize[0]) {
                        long[] tempTimeline = new long[insert_index[0]];
                        System.arraycopy(collectedTimes[0], 0, tempTimeline, 0, insert_index[0]);
                        collectedTimes[0] = tempTimeline;
                    }
                    selfPointer._space.unmark(objectWorldOrder.index());
                    callback.on(collectedTimes[0]);
                }
            }
        });
    }

    @Override
    public final int stringToHash(String name, boolean insertIfNotExists) {
        int hash = HashHelper.hash(name);
        if (insertIfNotExists) {
            StringIntMap dictionaryIndex = (StringIntMap) this.dictionary.getAt(0);
            if (dictionaryIndex == null) {
                dictionaryIndex = (StringIntMap) this.dictionary.getOrCreateAt(0, Type.STRING_TO_INT_MAP);
            }
            if (!dictionaryIndex.containsHash(hash)) {
                dictionaryIndex.put(name, hash);
            }
        }
        return hash;
    }

    @Override
    public final String hashToString(int key) {
        final StringIntMap dictionaryIndex = (StringIntMap) this.dictionary.getAt(0);
        if (dictionaryIndex != null) {
            return dictionaryIndex.getByHash(key);
        }
        return null;
    }

}
