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
package greycat.search;

import greycat.*;
import greycat.struct.Tree;

import static greycat.Tasks.newTask;

public class WaveSearch extends BaseEngine {

    @Override
    public void explore(Callback<Node> callback) {
        long indexWorld = _graph.fork(_iWorld);
        Node indexNode = _graph.newNode(indexWorld, Constants.BEGINNING_OF_TIME);
        Tree indexSolutions = (Tree) indexNode.getOrCreate("solutions", Type.KDTREE);
        Task t = newTask();
        t.loop("0", "9", newTask()).execute(_graph, new Callback<TaskResult>() {
            @Override
            public void on(TaskResult result) {
                result.free();
                callback.on(indexNode);
            }
        });
    }

}
