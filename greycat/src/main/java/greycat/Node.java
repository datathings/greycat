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

import greycat.utility.Tuple;

/**
 * Node is the base element contained in the {@link Graph}.<br>
 * They belong to a world and time, have attributes (e.g. primitives, relations, and indexes).
 */
public interface Node extends Container {

    /**
     * Returns the world this node belongs to.
     *
     * @return World identifier
     */
    long world();

    /**
     * Returns the timepoint of the node.
     *
     * @return Timestamp value
     */
    long time();

    /**
     * Returns the identifier for this node in the graph.<br>
     * This identifier is constant over timepoints and worlds.
     *
     * @return the node id.
     */
    long id();

    /**
     * Returns the type name of the current node (case of typed node).
     *
     * @return The type name of the current node.
     */
    String nodeTypeName();

    /**
     * Sets the value of an attribute of this node for its current world and time.<br>
     *
     * @param name  Must be unique per node.
     * @param type  Must be one of {@link Type} int value.
     * @param value Must be consistent with the propertyType.
     * @return The node for fluent API.
     */
    Node forceSet(String name, byte type, Object value);

    /**
     * Sets the value of an attribute of this node for its current world and time.<br>
     *
     * @param index Must be unique per node.
     * @param type  Must be one of {@link Type} int value.
     * @param value Must be consistent with the propertyType.
     * @return The node for fluent API.
     */
    Node forceSetAt(int index, byte type, Object value);

    /**
     * Retrieves the named relation.
     *
     * @param relationName name of the relation to retrieve
     * @param callback     callback to be notified when the relation has been resolved
     */
    void relation(String relationName, Callback<Node[]> callback);

    /**
     * Retrieves a relation using an index.
     *
     * @param relationIndex index of the relation
     * @param callback      callback to be notified when the relation has been resolved
     */
    void relationAt(int relationIndex, Callback<Node[]> callback);

    /**
     * Adds a node to a relation.<br>
     * If the relation doesn't exist, it is created on the fly.<br>
     *
     * @param relationName      The name of the relation in which the node is added.
     * @param relatedNode       The node to insert in the relation.
     * @param indexedAttributes The attributes' names to be used for indexing the relatedNode. The relation is not indexed if this parameter is null.
     * @return The node for fluent API.
     */
    Node addToRelation(String relationName, Node relatedNode, String... indexedAttributes);

    /**
     * Adds a node to a relation using the relation index.<br>
     * If the relation doesn't exist, it is created on the fly.<br>
     *
     * @param relationIndex     The index number of the relation in the current node, in which the relatedNode is added.
     * @param relatedNode       The node to insert in the relation.
     * @param indexedAttributes The attributes' names to be used for indexing the relatedNode. The relation is not indexed if this parameter is null.
     * @return The node for fluent API.
     */
    Node addToRelationAt(int relationIndex, Node relatedNode, String... indexedAttributes);

    /**
     * Removes a node from a relation.
     *
     * @param relationName      The name of the relation.
     * @param relatedNode       The node to remove.
     * @param indexedAttributes The attributes' names to be used for finding and removing the relatedNode from the index. This is mandatory if the relation is indexed.
     * @return The node for fluent API.
     */
    Node removeFromRelation(String relationName, Node relatedNode, String... indexedAttributes);

    /**
     * Removes a node from a relation.
     *
     * @param relationIndex     The name of the relation.
     * @param relatedNode       The node to remove.
     * @param indexedAttributes The attributes' names to be used for finding and removing the relatedNode from the index. This is mandatory if the relation is indexed.
     * @return The node for fluent API.
     */
    Node removeFromRelationAt(int relationIndex, Node relatedNode, String... indexedAttributes);

    /**
     * Computes the time dephasing of this node, i.e. the difference between last modification and the timepoint of the current node.
     *
     * @return The amount of time between the current time of the node and the last recorded state chunk time.
     */
    long timeDephasing();

    /**
     * Computes the time dephasing of this node, i.e. the difference between last modification and current node timepoint.
     *
     * @return The amount of time between the current time of the node and the last recorded state chunk time.
     */
    long lastModification();

    /**
     * Forces the creation of a new timepoint of a node for its time.<br>
     * Clones the previous state to the exact time of this node.<br>
     * This cancels the dephasing between the current timepoint of the node and the last record timepoint.
     *
     * @return The node for fluent API.
     */
    Node rephase();

    /**
     * Retrieves all timePoints from the timeLine of this node when alterations occurred.<br>
     * This method also jumps over the world hierarchy to collect all available timepoints.<br>
     * To unbound the search, please use {@link Constants#BEGINNING_OF_TIME} and {@link Constants#END_OF_TIME} as bounds.
     *
     * @param beginningOfSearch (inclusive) earliest bound for the search.
     * @param endOfSearch       (inclusive) latest bound for the search.
     * @param callback          Called when the search is finished. Provides an array containing all the timepoints required.
     */
    void timepoints(long beginningOfSearch, long endOfSearch, Callback<long[]> callback);

    /**
     * Informs GreyCat memory manager that this node object can be freed from the memory.<br>
     * <b>Warning: this MUST be the last method called on this node.</b><br>
     * To work with the node afterwards, a new lookup is mandatory.
     */
    void free();

    /**
     * Returns the graph that have created this node.
     *
     * @return the graph this node belongs to.
     */
    Graph graph();

    /**
     * Travels this object in time.
     * This method is equivalent to a call to lookup with the same ID than the current Node.
     *
     * @param targetTime target time selectWhere this node hasField to be resolved.
     * @param callback   Called whe the travelInTime is complete. Gives the new timed node in parameter.
     * @param <A>        Generic parameter that define the type of the result, should be a sub-type of Node
     */
    <A extends Node> void travelInTime(long targetTime, Callback<A> callback);

    /**
     * Travels this object in time.
     * This method is equivalent to a call to lookup with the same ID than the current Node.
     *
     * @param targetWorld target world selectWhere this node hasField to be resolved.
     * @param callback    Called whe the travelInTime is complete. Gives the new timed node in parameter.
     * @param <A>         Generic parameter that define the type of the result, should be a sub-type of Node
     */
    <A extends Node> void travelInWorld(long targetWorld, Callback<A> callback);

    /**
     * Travels this object in world and time.
     * This method is equivalent to a call to lookup with the same ID than the current Node.
     *
     * @param targetTime  target time selectWhere this node hasField to be resolved.
     * @param targetWorld target world selectWhere this node hasField to be resolved.
     * @param callback    Called whe the travelInTime is complete. Gives the new timed node in parameter.
     * @param <A>         Generic parameter that define the type of the result, should be a sub-type of Node
     */
    <A extends Node> void travel(long targetWorld, long targetTime, Callback<A> callback);

    /*
    long initialTime();

    long lastTime();

    long previousTime();

    long nextTime();
*/

    Node setTimeSensitivity(long deltaTime, long offset);

    Tuple<Long, Long> timeSensitivity();

    /**
     * This method end the lifespan of this node. All request after this time will reply null.
     */
    void end();

    /**
     * Delete completely this node and all time and world variations
     *
     * @param callback
     */
    void drop(Callback callback);

}
