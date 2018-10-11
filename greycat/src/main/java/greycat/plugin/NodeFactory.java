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
package greycat.plugin;

import greycat.Graph;
import greycat.Node;

/**
 * NodeFactory plugin allows to propose alternative implementations for {@link Node}.<br>
 * This specialization allows ot inject particular behavior into {@link Node} such as machine learning, extrapolation function.
 */
@FunctionalInterface
public interface NodeFactory {

    /**
     * Create a new Node
     *
     * @param world current world
     * @param time  current time
     * @param id    current node id
     * @param graph current graph
     * @return newly created Node object
     */
    Node create(long world, long time, long id, Graph graph);

}
