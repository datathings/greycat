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
package greycat.plugin;

import greycat.Node;
import greycat.Callback;
import greycat.internal.CoreNodeValue;
import greycat.utility.Tuple;

/**
 * Resolver plugin, able to change the semantic of Many World Graph
 */
public interface Resolver {

    /**
     * Initializes the resolver (cache)
     */
    void init();

    void free();

    /**
     * Initializes backend structures for the newly created node passed as parameter
     *
     * @param node     The node to initialize.
     * @param typeCode The coded type param to initialize
     */
    void initNode(Node node, long typeCode);

    /**
     * Initializes a newly created world, and sets the parent relationship.
     *
     * @param parentWorld The parent world
     * @param childWorld  The new world to initialize.
     */
    void initWorld(long parentWorld, long childWorld);

    /**
     * Frees a node structure so it might be recycled.
     *
     * @param node The node to free.
     */
    void freeNode(Node node);

    /**
     * Retrieve the string representation of the type of node passed as parameter
     *
     * @param node The node to extract the type from
     * @return long encoded type
     */
    int typeCode(Node node);

    void end(Node node);

    /**
     * Creates and schedules a lookup task.
     *
     * @param world    The world identifier
     * @param time     The timepoint.
     * @param id       The id of the node to retrieve.
     * @param callback Called when the node is retrieved.
     * @param <A>      type of the callback parameter
     */
    <A extends Node> void lookup(long world, long time, long id, Callback<A> callback);

    void lookupBatch(long worlds[], long times[], long[] ids, Callback<Node[]> callback);

    void lookupPTimes(long world, long[] times, long id, Callback<Node[]> callback);

    void lookupAll(long world, long time, long ids[], Callback<Node[]> callback);

    void lookupTimes(long world, long rfrom, long rto, long id, int limit, Callback<Node[]> callback);

    void lookupTimesSparse(long world, long rfrom, long rto, long id, int limit, Callback<Node[]> callback);

    /**
     * Resolves the state of a node, to access attributes, relations, and indexes.
     *
     * @param node The node for which the state must be collected.
     * @return The resolved state of the node.
     */
    NodeState resolveState(Node node);

    /**
     * Align the state of a node, to access attributes, relations, and indexes.
     *
     * @param node The node for which the state must be collected.
     * @return The resolved state of the node.
     */
    NodeState alignState(Node node);

    /**
     * @param node         The node for which the state must be collected.
     * @param world        The world for which the new state must be created.
     * @param relativeTime The time for which the new state must be created. Time should be RELATIVE to first creation of node (see offset method of WorldOrderChunk)
     * @return The newly empty created state of the node.
     */
    NodeState newState(Node node, long world, long relativeTime);

    /**
     * Resolves the timePoints of a node.
     *
     * @param node              The node for which timepoints are requested.
     * @param beginningOfSearch The earliest timePoint of the search (included).
     * @param endOfSearch       The latest timePoint of the search (included).
     * @param callback          Called when finished, with the list of timepoints included in the bounds for this node.
     */
    void resolveTimepoints(Node node, long beginningOfSearch, long endOfSearch, Callback<long[]> callback);

    void countTimepoints(Node node, long beginningOfSearch, long endOfSearch, Callback<Long> callback);

    /**
     * Maps a String to a unique long. Can be reversed using {@link #hashToString(int)}.
     *
     * @param name              The string value to be mapped.
     * @param insertIfNotExists indicate if the string has to be inserted if not existing in the global dictionary
     * @return The unique long identifier for the string.
     */
    int stringToHash(String name, boolean insertIfNotExists);

    /**
     * Returns the String associated to a hash.
     *
     * @param key The long key.
     * @return The string value associated to the long key.
     */
    String hashToString(int key);

    void externalLock(Node node);

    void externalUnlock(Node node);

    void setTimeSensitivity(Node node, long deltaTime, long delta);

    Tuple<Long, Long> getTimeSensitivity(Node node);

    void drop(Node target, Callback callback);

    void batchInsert(Node target, long[] times, double[] values);

}
