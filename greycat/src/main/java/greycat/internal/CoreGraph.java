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
package greycat.internal;

import greycat.*;
import greycat.chunk.*;
import greycat.internal.custom.*;
import greycat.internal.heap.HeapMemoryFactory;
import greycat.plugin.*;
import greycat.struct.*;
import greycat.utility.HashHelper;
import greycat.base.BaseNode;
import greycat.internal.task.CoreActionRegistry;
import greycat.internal.task.CoreTask;
import greycat.TaskHook;
import greycat.utility.Base64;
import greycat.utility.KeyHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CoreGraph implements Graph {

    private final Storage _storage;
    private final ChunkSpace _space;
    private final Scheduler _scheduler;
    private final Resolver _resolver;

    private final AtomicBoolean _isConnected;
    private final AtomicBoolean _lock;
    private final Plugin[] _plugins;

    private Short _prefix = null;
    private GenChunk _nodeKeyCalculator = null;
    private GenChunk _worldKeyCalculator = null;

    private final ActionRegistry _actionRegistry;
    private final NodeRegistry _nodeRegistry;
    private final TypeRegistry _typeRegistry;
    private MemoryFactory _memoryFactory;
    private TaskHook[] _taskHooks;
    private List<Callback<Callback<Boolean>>> _connectHooks;

    public CoreGraph(final Storage p_storage, final long memorySize, final long batchSize, final Scheduler p_scheduler, final Plugin[] p_plugins, final boolean deepPriority) {
        //initiate the two registry
        _actionRegistry = new CoreActionRegistry();
        _nodeRegistry = new CoreNodeRegistry();
        _typeRegistry = new CoreTypeRegistry();
        _memoryFactory = new HeapMemoryFactory();
        this._isConnected = new AtomicBoolean(false);
        this._lock = new AtomicBoolean(false);
        this._plugins = p_plugins;
        final Graph selfPointer = this;
        //First round, find relevant
        _connectHooks = new ArrayList<Callback<Callback<Boolean>>>();
        TaskHook[] temp_hooks = new TaskHook[0];
        if (p_plugins != null) {
            for (int i = 0; i < p_plugins.length; i++) {
                final Plugin loopPlugin = p_plugins[i];
                loopPlugin.start(this);
            }
        }
        //Third round, initialize all mandatory elements
        _taskHooks = temp_hooks;
        _storage = p_storage;
        _space = _memoryFactory.newSpace(memorySize, batchSize, selfPointer, deepPriority);
        _resolver = new MWResolver(_storage, _space, selfPointer);
        _scheduler = p_scheduler;
        //Fourth round, initialize all taskActions and nodeTypes
        CoreTask.fillDefault(this._actionRegistry);

        //Register default Custom Types
        this._typeRegistry.getOrCreateDeclaration(CoreIndexAttribute.NAME).setFactory(new TypeFactory() {
            @Override
            public Object wrap(final EStructArray backend) {
                return new CoreIndexAttribute(backend);
            }
        });
        this._typeRegistry.getOrCreateDeclaration(KDTree.NAME).setFactory(new TypeFactory() {
            @Override
            public Object wrap(final EStructArray backend) {
                return new KDTree(backend);
            }
        });
        this._typeRegistry.getOrCreateDeclaration(NDTree.NAME).setFactory(new TypeFactory() {
            @Override
            public Object wrap(final EStructArray backend) {
                return new NDTree(backend, new IndexManager());
            }
        });

        this._nodeRegistry.getOrCreateDeclaration(CoreNodeIndex.NAME).setFactory(new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new CoreNodeIndex(world, time, id, graph);
            }
        });
        this._nodeRegistry.getOrCreateDeclaration(CoreNodeValue.NAME).setFactory(new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new CoreNodeValue(world, time, id, graph);
            }
        });
        this._nodeRegistry.getOrCreateDeclaration(KDTreeNode.NAME).setFactory(new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new KDTreeNode(world, time, id, graph);
            }
        });
        this._nodeRegistry.getOrCreateDeclaration(NDTreeNode.NAME).setFactory(new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new NDTreeNode(world, time, id, graph);
            }
        });
        //variables init
    }

    @Override
    public final long fork(long world) {
        long childWorld = this._worldKeyCalculator.newKey();
        this._resolver.initWorld(world, childWorld);
        return childWorld;
    }

    @Override
    public final Node newNode(long world, long time) {
        if (!_isConnected.get()) {
            throw new RuntimeException(CoreConstants.DISCONNECTED_ERROR);
        }
        final Node newNode = new BaseNode(world, time, this._nodeKeyCalculator.newKey(), this);
        this._resolver.initNode(newNode, Constants.NULL_LONG);
        return newNode;
    }

    @Override
    public final Node newTypedNode(long world, long time, String nodeType) {
        if (nodeType == null) {
            throw new RuntimeException("nodeType should not be null");
        }
        if (!_isConnected.get()) {
            throw new RuntimeException(CoreConstants.DISCONNECTED_ERROR);
        }
        final int extraCode = _resolver.stringToHash(nodeType, false);
        final NodeFactory resolvedFactory = factoryByCode(extraCode);
        BaseNode newNode;
        if (resolvedFactory == null) {
            System.out.println("WARNING: UnKnow NodeType " + nodeType + ", missing plugin configuration in the builder ? Using generic node as a fallback");
            newNode = new BaseNode(world, time, this._nodeKeyCalculator.newKey(), this);
        } else {
            newNode = (BaseNode) resolvedFactory.create(world, time, this._nodeKeyCalculator.newKey(), this);
        }
        this._resolver.initNode(newNode, extraCode);
        return newNode;
    }

    /**
     * @ignore ts
     */
    @Override
    public final <A extends Node> A newTypedNode(long world, long time, String nodeType, Class<A> type) {
        return (A) newTypedNode(world, time, nodeType);
    }

    @Override
    public final Node cloneNode(Node origin) {
        if (origin == null) {
            throw new RuntimeException("origin node should not be null");
        }
        if (!_isConnected.get()) {
            throw new RuntimeException(CoreConstants.DISCONNECTED_ERROR);
        }
        final BaseNode casted = (BaseNode) origin;
        casted.cacheLock();
        if (casted._dead) {
            casted.cacheUnlock();
            throw new RuntimeException(CoreConstants.DEAD_NODE_ERROR + " node id: " + casted.id());
        } else {
            //Duplicate marks on all chunks
            this._space.mark(casted._index_stateChunk);
            this._space.mark(casted._index_superTimeTree);
            this._space.mark(casted._index_timeTree);
            this._space.mark(casted._index_worldOrder);
            //Create the cloned node
            final WorldOrderChunk worldOrderChunk = (WorldOrderChunk) this._space.get(casted._index_worldOrder);
            final NodeFactory resolvedFactory = factoryByCode((int) worldOrderChunk.type());
            BaseNode newNode;
            if (resolvedFactory == null) {
                newNode = new BaseNode(origin.world(), origin.time(), origin.id(), this);
            } else {
                newNode = (BaseNode) resolvedFactory.create(origin.world(), origin.time(), origin.id(), this);
            }
            //Init the cloned node with clonee resolver cache
            newNode._index_stateChunk = casted._index_stateChunk;
            newNode._index_timeTree = casted._index_timeTree;
            newNode._index_superTimeTree = casted._index_superTimeTree;
            newNode._index_worldOrder = casted._index_worldOrder;
            newNode._world_magic = casted._world_magic;
            newNode._super_time_magic = casted._super_time_magic;
            newNode._time_magic = casted._time_magic;
            casted.cacheUnlock();
            return newNode;
        }
    }

    final NodeFactory factoryByCode(int code) {
        NodeDeclaration declaration = _nodeRegistry.declarationByHash(code);
        if (declaration != null) {
            return declaration.factory();
        }
        return null;
    }

    @Override
    public final TaskHook[] taskHooks() {
        return _taskHooks;
    }

    @Override
    public final ActionRegistry actionRegistry() {
        return _actionRegistry;
    }

    @Override
    public final NodeRegistry nodeRegistry() {
        return _nodeRegistry;
    }

    @Override
    public final TypeRegistry typeRegistry() {
        return _typeRegistry;
    }

    @Override
    public final Graph setMemoryFactory(MemoryFactory factory) {
        if (_isConnected.get()) {
            throw new RuntimeException("Memory factory cannot be changed after connection !");
        }
        _memoryFactory = factory;
        return this;
    }

    @Override
    public final synchronized Graph addGlobalTaskHook(TaskHook newTaskHook) {
        if (_taskHooks == null) {
            _taskHooks = new TaskHook[1];
            _taskHooks[0] = newTaskHook;
        } else {
            TaskHook[] temp_temp_hooks = new TaskHook[_taskHooks.length + 1];
            System.arraycopy(_taskHooks, 0, temp_temp_hooks, 0, _taskHooks.length);
            temp_temp_hooks[_taskHooks.length] = newTaskHook;
            _taskHooks = temp_temp_hooks;
        }
        return this;
    }

    @Override
    public final Graph addConnectHook(Callback<Callback<Boolean>> onConnect) {
        _connectHooks.add(onConnect);
        return this;
    }

    @Override
    public final <A extends Node> void lookup(long world, long time, long id, Callback<A> callback) {
        if (!_isConnected.get()) {
            throw new RuntimeException(CoreConstants.DISCONNECTED_ERROR);
        }
        this._resolver.lookup(world, time, id, callback);
    }

    @Override
    public final void lookupBatch(long[] worlds, long[] times, long[] ids, Callback<Node[]> callback) {
        if (!_isConnected.get()) {
            throw new RuntimeException(CoreConstants.DISCONNECTED_ERROR);
        }
        this._resolver.lookupBatch(worlds, times, ids, callback);
    }

    @Override
    public final void lookupAll(long world, long time, long[] ids, Callback<Node[]> callback) {
        if (!_isConnected.get()) {
            throw new RuntimeException(CoreConstants.DISCONNECTED_ERROR);
        }
        this._resolver.lookupAll(world, time, ids, callback);
    }

    @Override
    public final void lookupTimes(long world, long from, long to, long id, int limit, Callback<Node[]> callback) {
        if (!_isConnected.get()) {
            throw new RuntimeException(CoreConstants.DISCONNECTED_ERROR);
        }
        this._resolver.lookupTimes(world, from, to, id, limit, callback);
    }

    @Override
    public final void save(Callback<Boolean> callback) {
        if (callback == null) {
            _space.save(false, false, null, null);
        } else {
            _space.save(false, false, null, new Callback<Buffer>() {
                @Override
                public void on(Buffer result) {
                    callback.on(true);
                }
            });
        }
    }

    @Override
    public void savePartial(Callback<Boolean> callback) {
        if (callback == null) {
            _space.save(false, true, null, null);
        } else {
            _space.save(false, true, null, new Callback<Buffer>() {
                @Override
                public void on(Buffer result) {
                    callback.on(true);
                }
            });
        }
    }

    @Override
    public final void saveSilent(Callback<Buffer> callback) {
        _space.save(true, false, null, callback);
    }

    @Override
    public final void savePartialSilent(Callback<Buffer> callback) {
        _space.save(true, true, null, callback);
    }

    @Override
    public final void connect(final Callback<Boolean> callback) {
        final CoreGraph selfPointer = this;
        //negociate a lock
        while (selfPointer._lock.compareAndSet(false, true)) ;
        //ok we have it, let's go
        if (_isConnected.compareAndSet(false, true)) {
            //first connect the scheduler
            selfPointer._scheduler.start();
            selfPointer._storage.connect(selfPointer, new Callback<Boolean>() {
                @Override
                public void on(Boolean connection) {
                    if (connection) {
                        selfPointer._storage.lock(new Callback<Buffer>() {
                            @Override
                            public void on(Buffer prefixBuf) {
                                _prefix = (short) Base64.decodeToIntWithBounds(prefixBuf, 0, prefixBuf.length());
                                prefixBuf.free();
                                final Buffer connectionKeys = selfPointer.newBuffer();
                                //preload ObjKeyGenerator
                                KeyHelper.keyToBuffer(connectionKeys, ChunkType.GEN_CHUNK, Constants.BEGINNING_OF_TIME, Constants.NULL_LONG, _prefix);
                                connectionKeys.write(CoreConstants.BUFFER_SEP);
                                //preload WorldKeyGenerator
                                KeyHelper.keyToBuffer(connectionKeys, ChunkType.GEN_CHUNK, Constants.END_OF_TIME, Constants.NULL_LONG, _prefix);
                                connectionKeys.write(CoreConstants.BUFFER_SEP);
                                //preload GlobalWorldOrder
                                KeyHelper.keyToBuffer(connectionKeys, ChunkType.WORLD_ORDER_CHUNK, 0, 0, Constants.NULL_LONG);
                                connectionKeys.write(CoreConstants.BUFFER_SEP);
                                //preload GlobalDictionary
                                KeyHelper.keyToBuffer(connectionKeys, ChunkType.STATE_CHUNK, CoreConstants.GLOBAL_DICTIONARY_KEY[0], CoreConstants.GLOBAL_DICTIONARY_KEY[1], CoreConstants.GLOBAL_DICTIONARY_KEY[2]);
                                connectionKeys.write(CoreConstants.BUFFER_SEP);
                                selfPointer._storage.get(connectionKeys, new Callback<Buffer>() {
                                    @Override
                                    public void on(Buffer payloads) {
                                        connectionKeys.free();
                                        if (payloads != null) {
                                            BufferIterator it = payloads.iterator();
                                            Buffer view1 = it.next();
                                            Buffer view2 = it.next();
                                            Buffer view3 = it.next();
                                            Buffer view4 = it.next();
                                            Boolean noError = true;
                                            try {
                                                //init the global universe tree (mandatory for synchronious create)
                                                WorldOrderChunk globalWorldOrder = (WorldOrderChunk) selfPointer._space.createAndMark(ChunkType.WORLD_ORDER_CHUNK, 0, 0, Constants.NULL_LONG);
                                                if (view3.length() > 0) {
                                                    globalWorldOrder.load(view3);
                                                }
                                                //init the global dictionary chunk
                                                StateChunk globalDictionaryChunk = (StateChunk) selfPointer._space.createAndMark(ChunkType.STATE_CHUNK, CoreConstants.GLOBAL_DICTIONARY_KEY[0], CoreConstants.GLOBAL_DICTIONARY_KEY[1], CoreConstants.GLOBAL_DICTIONARY_KEY[2]);
                                                if (view4.length() > 0) {
                                                    globalDictionaryChunk.load(view4);
                                                }
                                                selfPointer._worldKeyCalculator = (GenChunk) selfPointer._space.createAndMark(ChunkType.GEN_CHUNK, Constants.END_OF_TIME, Constants.NULL_LONG, _prefix);
                                                if (view2.length() > 0) {
                                                    selfPointer._worldKeyCalculator.load(view2);
                                                }
                                                selfPointer._nodeKeyCalculator = (GenChunk) selfPointer._space.createAndMark(ChunkType.GEN_CHUNK, Constants.BEGINNING_OF_TIME, Constants.NULL_LONG, _prefix);
                                                if (view1.length() > 0) {
                                                    selfPointer._nodeKeyCalculator.load(view1);
                                                }
                                                //init the resolver
                                                selfPointer._resolver.init();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                noError = false;
                                            }
                                            payloads.free();
                                            selfPointer._lock.set(true);
                                            if (HashHelper.isDefined(callback)) {
                                                if (_connectHooks == null || _connectHooks.size() == 0) {
                                                    callback.on(noError);
                                                } else {
                                                    final Boolean finalNoError = noError;
                                                    final int cbs_size = _connectHooks.size();
                                                    final int[] cursor = new int[1];
                                                    cursor[0] = 0;
                                                    final Callback[] cbs = new Callback[1];
                                                    cbs[0] = new Callback<Boolean>() {
                                                        @Override
                                                        public void on(final Boolean result) {
                                                            cursor[0]++;
                                                            if (cursor[0] < cbs_size) {
                                                                final Callback<Callback<Boolean>> cb = _connectHooks.get(cursor[0]);
                                                                cb.on(cbs[0]);
                                                            } else {
                                                                callback.on(finalNoError);
                                                            }
                                                        }
                                                    };
                                                    final Callback<Callback<Boolean>> cb = _connectHooks.get(cursor[0]);
                                                    cb.on(cbs[0]);
                                                }
                                            }
                                        } else {
                                            selfPointer._lock.set(true);
                                            if (HashHelper.isDefined(callback)) {
                                                callback.on(false);
                                            }
                                        }
                                    }
                                });
                            }
                        });
                    } else {
                        callback.on(false);
                    }
                }
            });

        } else {
            //already connected
            selfPointer._lock.set(true);
            if (HashHelper.isDefined(callback)) {
                callback.on(null);
            }
        }
    }

    @Override
    public final void disconnect(final Callback callback) {
        while (this._lock.compareAndSet(false, true)) ;
        //ok we have the lock
        if (_isConnected.compareAndSet(true, false)) {
            //JS workaround for closure encapsulation and this variable
            final CoreGraph selfPointer = this;
            //first we stop scheduler, no tasks will be executed anymore
            selfPointer._scheduler.stop();
            if (this._plugins != null) {
                for (int i = 0; i < _plugins.length; i++) {
                    this._plugins[i].stop();
                }
            }
            save(new Callback<Boolean>() {
                @Override
                public void on(Boolean result) {
                    final ChunkSpace space = selfPointer._space;
                    space.free(_nodeKeyCalculator);
                    space.free(_worldKeyCalculator);
                    selfPointer._resolver.free();
                    space.freeAll();
                    if (selfPointer._storage != null) {
                        final Buffer prefixBuf = selfPointer.newBuffer();
                        Base64.encodeIntToBuffer(selfPointer._prefix, prefixBuf);
                        selfPointer._storage.unlock(prefixBuf, new Callback<Boolean>() {
                            @Override
                            public void on(Boolean result) {
                                prefixBuf.free();
                                selfPointer._storage.disconnect(new Callback<Boolean>() {
                                    @Override
                                    public void on(Boolean result) {
                                        selfPointer._lock.set(true);
                                        if (HashHelper.isDefined(callback)) {
                                            callback.on(result);
                                        }
                                    }
                                });
                            }
                        });
                    } else {
                        selfPointer._lock.set(true);
                        if (HashHelper.isDefined(callback)) {
                            callback.on(result);
                        }
                    }
                }
            });
        } else {
            //not previously connected
            this._lock.set(true);
            if (HashHelper.isDefined(callback)) {
                callback.on(null);
            }
        }
    }

    @Override
    public final Buffer newBuffer() {
        return _memoryFactory.newBuffer();
    }

    @Override
    public final Query newQuery() {
        return new CoreQuery(this, _resolver);
    }

    @Override
    public final void declareIndex(long world, String name, Callback<NodeIndex> callback, String... indexedAttributes) {
        internal_index(world, Constants.BEGINNING_OF_TIME, name, false, new Callback<NodeIndex>() {
            @Override
            public void on(final NodeIndex nodeIndex) {
                nodeIndex.setTimeSensitivity(-1, 0);
                nodeIndex.declareAttributes(new Callback() {
                    @Override
                    public void on(Object result) {
                        if (callback != null) {
                            callback.on(nodeIndex);
                        }
                    }
                }, indexedAttributes);
            }
        });
    }

    @Override
    public final void declareTimedIndex(long world, long originTime, String name, Callback<NodeIndex> callback, String... indexedAttributes) {
        internal_index(world, originTime, name, false, new Callback<NodeIndex>() {
            @Override
            public void on(final NodeIndex nodeIndex) {
                nodeIndex.declareAttributes(new Callback() {
                    @Override
                    public void on(Object ignore) {
                        if (callback != null) {
                            callback.on(nodeIndex);
                        }
                    }
                }, indexedAttributes);
            }
        });
    }

    @Override
    public final synchronized void index(long world, long time, String name, Callback<NodeIndex> callback) {
        internal_index(world, time, name, true, callback);
    }

    private void internal_index(long world, long time, String name, boolean ifExists, Callback<NodeIndex> callback) {
        final CoreGraph selfPointer = this;
        final long indexNameCoded = this._resolver.stringToHash(name, true);
        this._resolver.lookup(world, CoreConstants.BEGINNING_OF_TIME, CoreConstants.END_OF_TIME, new Callback<Node>() {
            @Override
            public void on(Node globalIndexNodeUnsafe) {
                if (ifExists && globalIndexNodeUnsafe == null) {
                    callback.on(null);
                } else {
                    LongLongMap globalIndexContent;
                    if (globalIndexNodeUnsafe == null) {
                        globalIndexNodeUnsafe = new BaseNode(world, CoreConstants.BEGINNING_OF_TIME, CoreConstants.END_OF_TIME, selfPointer);
                        selfPointer._resolver.initNode(globalIndexNodeUnsafe, CoreConstants.NULL_LONG);
                        globalIndexContent = (LongLongMap) globalIndexNodeUnsafe.getOrCreateAt(0, Type.LONG_TO_LONG_MAP);
                    } else {
                        globalIndexContent = (LongLongMap) globalIndexNodeUnsafe.getAt(0);
                    }
                    long indexId = globalIndexContent.get(indexNameCoded);
                    if (indexId == CoreConstants.NULL_LONG) {
                        //insert null
                        if (ifExists) {
                            globalIndexNodeUnsafe.free();
                            callback.on(null);
                        } else {
                            final NodeIndex newIndexNode = (NodeIndex) selfPointer.newTypedNode(world, time, CoreNodeIndex.NAME);
                            //newIndexNode.getOrCreate(CoreConstants.INDEX_ATTRIBUTE, Type.RELATION_INDEXED);
                            indexId = newIndexNode.id();
                            globalIndexContent.put(indexNameCoded, indexId);
                            globalIndexNodeUnsafe.free();
                            callback.on(newIndexNode);
                        }
                    } else {
                        globalIndexNodeUnsafe.free();
                        selfPointer._resolver.lookup(world, time, indexId, callback);
                    }
                }
            }
        });
    }

    @Override
    public final void indexNames(final long world, final long time, final Callback<String[]> callback) {
        final CoreGraph selfPointer = this;
        this._resolver.lookup(world, time, CoreConstants.END_OF_TIME, new Callback<Node>() {
            @Override
            public void on(Node globalIndexNodeUnsafe) {
                if (globalIndexNodeUnsafe == null) {
                    callback.on(new String[0]);
                } else {
                    LongLongMap globalIndexContent = (LongLongMap) globalIndexNodeUnsafe.getAt(0);
                    if (globalIndexContent == null) {
                        globalIndexNodeUnsafe.free();
                        callback.on(new String[0]);
                    } else {
                        final String[] result = new String[(int) globalIndexContent.size()];
                        final int[] resultIndex = {0};
                        globalIndexContent.each(new LongLongMapCallBack() {
                            @Override
                            public void on(long key, long value) {
                                result[resultIndex[0]] = selfPointer._resolver.hashToString((int) key);
                                resultIndex[0]++;
                            }
                        });
                        globalIndexNodeUnsafe.free();
                        callback.on(result);
                    }
                }
            }
        });
    }

    @Override
    public final DeferCounter newCounter(int expectedCountCalls) {
        return new CoreDeferCounter(expectedCountCalls);
    }

    @Override
    public final DeferCounterSync newSyncCounter(int expectedCountCalls) {
        return new CoreDeferCounterSync(expectedCountCalls);
    }

    @Override
    public final Resolver resolver() {
        return _resolver;
    }

    @Override
    public final Scheduler scheduler() {
        return _scheduler;
    }

    @Override
    public final ChunkSpace space() {
        return _space;
    }

    @Override
    public final Storage storage() {
        return _storage;
    }

    @Override
    public final void freeNodes(Node[] nodes) {
        if (nodes != null) {
            for (int i = 0; i < nodes.length; i++) {
                if (nodes[i] != null) {
                    nodes[i].free();
                }
            }
        }
    }
}
