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

import greycat.struct.*;

public interface Container {

    /**
     * Returns the value of an attribute of the container.
     *
     * @param name The name of the attribute to be read.
     * @return The value of the required attribute in this container for the current timepoint and world.
     * The type of the returned object (i.e. of the attribute) is given by {@link #type(String)} (typed by one of  {@link #type(String)}
     */
    Object get(String name);

    Relation getRelation(String name);

    RelationIndexed getRelationIndexed(String name);

    DMatrix getDMatrix(String name);

    LMatrix getLMatrix(String name);

    EGraph getEGraph(String name);

    LongArray getLongArray(String name);

    IntArray getIntArray(String name);

    DoubleArray getDoubleArray(String name);

    StringArray getStringArray(String name);

    StringIntMap getStringIntMap(String name);

    LongLongMap getLongLongMap(String name);

    IntIntMap getIntIntMap(String name);

    IntStringMap getIntStringMap(String name);

    LongLongArrayMap getLongLongArrayMap(String name);

    LongSet getLongSet(String name);

    IntSet getIntSet(String name);

    /**
     * Returns the value of an attribute of the container.
     *
     * @param index index of attribute.
     * @return The value of the required attribute in this container for the current timepoint and world.
     * The type of the returned object (i.e. of the attribute) is given by {@link #type(String)}
     * (typed by one of the Type)
     */
    Object getAt(int index);

    Object getRawAt(int index);

    Object getTypedRawAt(int index, byte type);

    /**
     * Returns the type of an attribute. The returned value is one of {@link Type}.
     *
     * @param name The name of the attribute for which the type is asked.
     * @return The type of the attribute inform of an int belonging to {@link Type}.
     */
    byte type(String name);

    byte typeAt(int index);

    /**
     * Sets the value of an attribute of this container (for its current world and time for Node container).<br>
     *
     * @param name  Must be unique per node.
     * @param type  Must be one of {@link Type} int value.
     * @param value Must be consistent with the propertyType.
     * @return The node for fluent API.
     */
    Container set(String name, byte type, Object value);

    /**
     * Sets the value of an attribute of this container (for its current world and time for Node container).<br>
     *
     * @param index Must be unique per node.
     * @param type  Must be one of {@link Type} int value.
     * @param value Must be consistent with the propertyType.
     * @return The node for fluent API.
     */
    Container setAt(int index, byte type, Object value);

    /**
     * Removes an attribute from the container.
     *
     * @param name The name of the attribute to remove.
     * @return The node for fluent API.
     */
    Container remove(String name);

    Container removeAt(int index);

    /**
     * Gets or creates atomically a complex mutable attribute (e.g. Maps).<br>
     *
     * @param name The name of the object to create. Must be unique per node.
     * @param type The type of the attribute. Must be one of {@link Type} int value.
     * @return An instance that can be altered at the current world and time.
     */
    Object getOrCreate(String name, byte type);

    /**
     * Gets or creates atomically a complex mutable attribute (e.g. Maps).<br>
     *
     * @param index The name of the object to create. Must be unique per node.
     * @param type  The type of the attribute. Must be one of {@link Type} int value.
     * @return An instance that can be altered at the current world and time.
     */
    Object getOrCreateAt(int index, byte type);

    <A> A getWithDefault(String key, A defaultValue);

    <A> A getAtWithDefault(int key, A defaultValue);

    Container rephase();

}
