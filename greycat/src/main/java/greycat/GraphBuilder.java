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
package greycat;

import greycat.internal.BlackHoleStorage;
import greycat.internal.CoreGraph;
import greycat.plugin.StorageFactory;
import greycat.scheduler.TrampolineScheduler;
import greycat.internal.ReadOnlyStorage;
import greycat.plugin.Plugin;
import greycat.plugin.Scheduler;
import greycat.plugin.Storage;

/**
 * Creates an instance of a Graph, with several customizable features.
 */
public class GraphBuilder {

    public Storage storage = null;
    public StorageFactory storageFactory = null;
    private Scheduler _scheduler = null;
    private Plugin[] _plugins = null;
    private long _memorySize = -1;
    private long _batchSize = -1;
    private boolean _readOnly = false;
    private boolean _deepPriority = true;
    private boolean _useProxies = true;

    private Callback<Graph> postBuildHook;

    public static GraphBuilder newBuilder() {
        return new GraphBuilder();
    }

    public final GraphBuilder clone() {
        GraphBuilder copy = new GraphBuilder();
        copy.storage = this.storage;
        copy._scheduler = this._scheduler;
        copy._plugins = this._plugins;
        copy._memorySize = this._memorySize;
        copy._batchSize = this._batchSize;
        copy._readOnly = this._readOnly;
        copy._deepPriority = this._deepPriority;
        return copy;
    }

    /**
     * Sets the storage system to the given parameter.
     *
     * @param storage the storage system to be used by the graph
     * @return the {@link GraphBuilder}, for a fluent API
     */
    public final GraphBuilder withStorage(Storage storage) {
        this.storage = storage;
        return this;
    }

    /**
     * Sets the storage system to the given parameter, in read-only mode.
     *
     * @param storage the storage system to be used by the graph in read-only mode
     * @return the {@link GraphBuilder}, for a fluent API
     */
    public GraphBuilder withReadOnlyStorage(Storage storage) {
        this.storage = storage;
        _readOnly = true;
        return this;
    }

    /**
     * Sets the storage factory to the given parameter.
     *
     * @param storage the storage factory will be used to create the storage to be used by the graph
     * @return the {@link GraphBuilder}, for a fluent API
     */
    public final GraphBuilder withStorageFactory(StorageFactory storage) {
        this.storageFactory = storage;
        return this;
    }

    /**
     * Sets the storage system to the given parameter, in read-only mode.
     *
     * @param storage the storage system to be used by the graph in read-only mode
     * @return the {@link GraphBuilder}, for a fluent API
     */
    public GraphBuilder withReadOnlyStorageFactory(StorageFactory storage) {
        this.storageFactory = storage;
        _readOnly = true;
        return this;
    }

    public GraphBuilder withSaveBatchSize(long numberOfchunks) {
        this._batchSize = numberOfchunks;
        return this;
    }

    /**
     * Sets the maximum size of the memory that can be used before automated unload.
     *
     * @param numberOfElements the number of elements in memory before unloading
     * @return the {@link GraphBuilder}, for a fluent API
     */
    public GraphBuilder withMemorySize(long numberOfElements) {
        this._memorySize = numberOfElements;
        return this;
    }

    /**
     * Sets the scheduler to be used by the graph
     *
     * @param scheduler an instance of scheduler
     * @return the {@link GraphBuilder}, for a fluent API
     */
    public GraphBuilder withScheduler(Scheduler scheduler) {
        this._scheduler = scheduler;
        return this;
    }

    /**
     * Defines if proxies must be used or not
     *
     * @param value true for use of proxies; false otherwise.
     * @return the {@link GraphBuilder}, for a fluent API
     */
    public GraphBuilder withProxyUsage(boolean value) {
        this._useProxies = value;
        return this;
    }

    /**
     * Declare a plugin to the graph graphBuilder.
     *
     * @param plugin that has to be added
     * @return the {@link GraphBuilder}, for a fluent API
     */
    public GraphBuilder withPlugin(Plugin plugin) {
        if (_plugins == null) {
            _plugins = new Plugin[1];
            _plugins[0] = plugin;
        } else {
            Plugin[] _plugins2 = new Plugin[_plugins.length + 1];
            System.arraycopy(_plugins, 0, _plugins2, 0, _plugins.length);
            _plugins2[_plugins.length] = plugin;
            _plugins = _plugins2;
        }
        return this;
    }

    public GraphBuilder withDeepWorld() {
        _deepPriority = Constants.DEEP_WORLD;
        return this;
    }

    public GraphBuilder withWideWorld() {
        _deepPriority = Constants.WIDE_WORLD;
        return this;
    }

    public GraphBuilder withPostBuildHook(Callback<Graph> postBuildHook) {
        this.postBuildHook = postBuildHook;
        return this;
    }

    public Graph build() {
        if (storage == null) {
            storage = new BlackHoleStorage();
        }
        if (_readOnly) {
            storage = new ReadOnlyStorage(storage);
        }
        if (_scheduler == null) {
            _scheduler = new TrampolineScheduler();
        }
        if (_memorySize == -1) {
            _memorySize = 100000;
        }
        Graph graph;
        if (storageFactory != null) {
            graph = new CoreGraph(storageFactory.build(), _memorySize, _batchSize, _scheduler, _plugins, _deepPriority, _useProxies);
        } else {
            graph = new CoreGraph(storage, _memorySize, _batchSize, _scheduler, _plugins, _deepPriority, _useProxies);
        }

        if (this.postBuildHook != null) {
            this.postBuildHook.on(graph);
        }

        return graph;

    }

}
