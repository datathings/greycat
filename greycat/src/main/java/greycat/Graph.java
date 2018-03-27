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
package greycat;

import greycat.plugin.*;
import greycat.struct.Buffer;
import greycat.chunk.ChunkSpace;

import java.util.HashMap;
import java.util.Map;

/**
 * Graph is the main structure of GreyCat.
 * Use the {@link GraphBuilder} to get an instance.
 */
public interface Graph {

    Graph setProperty(String key, Object value);

    Object getProperty(String key);
    
    /**
     * Creates a new (generic) {@link Node Node} in the Graph and returns the new Node.
     *
     * @param world initial world of the node
     * @param time  initial time of the node
     * @return newly created node
     */
    Node newNode(long world, long time);

    /**
     * Creates a new {@link Node Node} using the {@link NodeFactory} previously declared with the {@link GraphBuilder#withPlugin(Plugin)} method and returns the new Node.
     *
     * @param world    initial world of the node
     * @param time     initial time of the node
     * @param nodeType name of the {@link NodeFactory} to be used, as previously declared with the {@link GraphBuilder#withPlugin(Plugin)} method.
     * @return newly created node
     */
    Node newTypedNode(long world, long time, String nodeType);

    Node newTypedNodeFrom(long world, long time, int nodeType);

    /**
     * @ignore ts
     */
    <A extends Node> A newTypedNode(long world, long time, String nodeType, Class<A> type);

    /**
     * Create a copy of this node that can be freed independently
     *
     * @param origin node to be cloned
     * @return cloned node (aka pointer)
     */
    Node cloneNode(Node origin);

    /**
     * Asynchronous lookup of a particular node.<br>
     * Based on the tuple &lt;World, Time, Node_ID&gt; this method seeks a {@link Node} in the Graph and returns it to the callback.
     *
     * @param world    The world identifier in which the Node must be searched.
     * @param time     The time at which the Node must be resolved.
     * @param id       The unique identifier of the {@link Node} ({@link Node#id()}) researched.
     * @param callback The task to be called when the {@link Node} is retrieved.
     * @param <A>      the type of the parameter returned in the callback (should extend {@link Node}).
     */
    <A extends Node> void lookup(long world, long time, long id, Callback<A> callback);

    /**
     * Asynchronous lookup of a nodes with different worlds and times.<br>
     * Based on the tuple &lt;World, Time, Node_ID&gt; this method seeks a {@link Node} in the Graph and returns it to the callback.
     *
     * @param worlds   The worlds at which Nodes must be resolved.
     * @param times    The times at which Nodes must be resolved.
     * @param ids      The unique identifier of {@link Node} array ({@link Node#id()}) researched.
     * @param callback The task to be called when the {@link Node} is retrieved.
     */
    void lookupBatch(long worlds[], long times[], long[] ids, Callback<Node[]> callback);

    /**
     * Asynchronous lookup of a nodes.<br>
     * Based on the tuple &lt;World, Time, Node_ID&gt; this method seeks a {@link Node} in the Graph and returns it to the callback.
     *
     * @param world    The world identifier in which the Node must be searched.
     * @param time     The time at which the Node must be resolved.
     * @param ids      The unique identifier of {@link Node} array ({@link Node#id()}) researched.
     * @param callback The task to be called when the {@link Node} is retrieved.
     */
    void lookupAll(long world, long time, long[] ids, Callback<Node[]> callback);

    /**
     * Asynchronous lookup of a nodes.<br>
     * Based on the tuple &lt;World, Time, Node_ID&gt; this method seeks a {@link Node} in the Graph and returns it to the callback.
     *
     * @param world    The world identifier in which the Node must be searched.
     * @param from     The time at which the range extract should start.
     * @param to       The time at which the range extract should end.
     * @param id       The unique identifier of the {@link Node} ({@link Node#id()}) researched.
     * @param callback The task to be called when the {@link Node} is retrieved.
     */
    void lookupTimes(long world, long from, long to, long id, int limit, Callback<Node[]> callback);

    /**
     * Creates a spin-off world from the world given as parameter.<br>
     * The forked world can then be altered independently of its parent.<br>
     * Every modification in the parent world will nevertheless be inherited.<br>
     * In case of concurrent change, changes in the forked world overrides changes from the parent.
     *
     * @param world origin world id
     * @return newly created child world id (to be used later in lookup method for instance)
     */
    long fork(long world);

    /**
     * Triggers a save task for the current graph.<br>
     * This method synchronizes the physical storage with the current in-memory graph, for the last n used elements.
     *
     * @param callback called when the save is finished. The parameter specifies whether or not the task succeeded.
     */
    void savePartial(Callback<Boolean> callback);

    /**
     * Triggers a save task for the current graph.<br>
     * This method synchronizes the physical storage with the current in-memory graph.
     *
     * @param callback called when the save is finished. The parameter specifies whether or not the task succeeded.
     */
    void save(Callback<Boolean> callback);

    /**
     * Triggers a save task for the current graph, but without notifying storage, instead we collect notification buffer to process afterward.
     * This method synchronizes the physical storage with the current in-memory graph.
     *
     * @param callback called when the save is finished. The parameter specifies whether or not the task succeeded.
     */
    void saveSilent(Callback<Buffer> callback);

    void savePartialSilent(Callback<Buffer> callback);

    boolean isConnected();

    /**
     * Connects the current graph to its storage (mandatory before any other method call)
     *
     * @param callback Called when the connection is done. The parameter specifies whether or not the connection succeeded.
     */
    void connect(Callback<Boolean> callback);

    /**
     * Disconnects the current graph from its storage (a save will be trigger safely before the exit)
     *
     * @param callback Called when the disconnection is completed. The parameter specifies whether or not the disconnection succeeded.
     */
    void disconnect(Callback<Boolean> callback);

    /**
     * Retrieve a named global index, at a precise world and time.<br>
     * Creates the index and configure it, if it does not exist.
     *
     * @param world    The world id in which the index has to be looked for
     * @param name     The name of the index
     * @param callback The callback to be called when the index lookup is complete.
     */
    void declareIndex(long world, String name, Callback<NodeIndex> callback, String... indexedAttributes);

    /**
     * Retrieve a named global index, at a precise world and time.<br>
     * Creates the index and configure it, if it does not exist.
     *
     * @param world    The world id in which the index has to be looked for
     * @param time     The time at which the index has to be looked for
     * @param name     The name of the index
     * @param callback The callback to be called when the index lookup is complete.
     */
    void declareTimedIndex(long world, long time, String name, Callback<NodeIndex> callback, String... indexedAttributes);

    /**
     * Retrieve a named global index, at a precise world and time.<br>
     * Returns null to the callback if it does not exist.
     *
     * @param world    The world id in which the index has to be looked for
     * @param time     The time at which the index has to be looked for
     * @param name     The name of the index
     * @param callback The callback to be called when the index lookup is complete.
     */
    void index(long world, long time, String name, Callback<NodeIndex> callback);

    /**
     * Retrieve the list of indexes.
     *
     * @param world    The world id in which the search must be performed.
     * @param time     The timepoint at which the search must be performed.
     * @param callback Called when the retrieval is complete. Returns the retrieved indexes names, empty array otherwise.
     */
    void indexNames(long world, long time, Callback<String[]> callback);

    /**
     * Utility method to create a waiter based on a counter
     *
     * @param expectedEventsCount number of events expected before running a task.
     * @return The waiter object.
     */
    DeferCounter newCounter(int expectedEventsCount);

    /**
     * Utility method to create a sync waiter based on a counter
     *
     * @param expectedEventsCount number of events expected before running a task.
     * @return The waiter object.
     */
    DeferCounterSync newSyncCounter(int expectedEventsCount);

    /**
     * Retrieves the current resolver
     *
     * @return current resolver
     */
    Resolver resolver();

    /**
     * Retrieves the current scheduler
     *
     * @return current scheduler
     */
    Scheduler scheduler();

    /**
     * Retrieves the current space
     *
     * @return current space
     */
    ChunkSpace space();

    /**
     * Retrieves the current storage
     *
     * @return current storage
     */
    Storage storage();

    /**
     * Creates a new buffer for serialization and loading methods
     *
     * @return newly created buffer
     */
    Buffer newBuffer();

    /**
     * Creates a new query that can be executed on the graph.
     *
     * @return newly created query
     */
    Query newQuery();

    /**
     * Free the array of nodes (sequentially call the free method on all nodes)
     *
     * @param nodes the array of nodes to free
     */
    void freeNodes(Node[] nodes);

    /**
     * Retrieve the default task hook factory
     *
     * @return the current default task hook factory
     */
    TaskHook[] taskHooks();

    ActionRegistry actionRegistry();

    NodeRegistry nodeRegistry();

    TypeRegistry typeRegistry();

    Graph setMemoryFactory(MemoryFactory factory);

    Graph addGlobalTaskHook(TaskHook taskHook);

    Graph addConnectHook(Callback<Callback<Boolean>> onConnect);

    void remoteNotify(Buffer buffer);
}
