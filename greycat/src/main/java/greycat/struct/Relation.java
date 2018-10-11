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
package greycat.struct;

import greycat.Node;

public interface Relation {

    long[] all();

    int size();

    long get(int index);

    void set(int index, long value);

    Relation add(long newValue);

    Relation addAll(long[] newValues);

    Relation addNode(Node node);

    /**
     * Insert a long (node id) into a relationship at a particular index,
     *
     * @param newValue node id to insert
     * @param index    insert to insert, note that bigger index will be shifted
     * @return this Relation, fluent API
     */
    Relation insert(int index, long newValue);

    Relation remove(long oldValue);

    Relation delete(int oldValue);

    Relation clear();

}
