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
package greycat.plugin;

import greycat.Container;

/**
 * Node state, used to access all stored state element of a particular node
 */
public interface NodeState extends Container {

    /**
     * Returns the id of the world this state is attached to.
     *
     * @return the world id
     */
    long world();

    /**
     * Returns the time this state is attached to.
     *
     * @return current resolved time
     */
    long time();

    /**
     * Iterate over NodeState elements
     *
     * @param callBack the method to be called for each state.
     */
    void each(NodeStateCallback callBack);

}
