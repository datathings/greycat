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
package greycat.struct;

import greycat.Container;
import greycat.Graph;

public interface EGraph {

    ENode root();

    ENode newNode();

    ENode node(int index);

    EGraph setRoot(ENode eNode);

    EGraph drop(ENode eNode);

    int size();

    /**
     * Tag the object to be freed from the memory.
     * Warning this method is not a clear EGraph and is not supposed to be called manually in case of EGraph attached to a GreyCat Node.
     * This method is mainly for volatile EGraph usages.
     */
    void free();

    Graph graph();

}
