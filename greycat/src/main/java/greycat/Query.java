/**
 * Copyright 2017-2018 The GreyCat Authors.  All rights reserved.
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

/**
 * Defines a set of rules for filtering nodes from the graph.
 */
public interface Query {

    long world();

    Query setWorld(long world);

    long time();

    Query setTime(long time);

    /**
     * Fills this query with elements from a String
     * @param flatQuery the stringified query
     * @return the {@link Query}, for a fluent API
     */
  //  Query parse(String flatQuery);

    /**
     * Adds a filtering element based on the value of an attribute
     * @param attributeName the name of the attribute
     * @param value the value of the attribute for which nodes have to be collected
     * @return the {@link Query}, for a fluent API
     */
    Query add(String attributeName, String value);

    Query addRaw(int attributeNameHash, String value);

    /**
     * Returns the hash code of this query
     * @return the hash code
     */
    long hash();

    /**
     * Returns the attributes used in this query
     * @return the array of attributes used in this query
     */
    int[] attributes();

    /**
     * Returns the values of attributes used in this query to filter nodes
     * @return the values of attributes used in this query to filter nodes
     */
    Object[] values();

}



