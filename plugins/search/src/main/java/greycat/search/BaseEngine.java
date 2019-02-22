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

import greycat.Graph;
import greycat.Task;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseEngine implements SearchEngine {

    protected final List<Task> _tasks = new ArrayList<Task>();
    protected final List<Task> _functions = new ArrayList<Task>();
    protected Graph _graph;
    protected long _iWorld;

    public void addAction(Task task) {
        _tasks.add(task);
    }

    public void addFunction(Task function) {
        _functions.add(function);
    }

    public void init(Graph g, long iWorld){
        _graph = g;
        _iWorld = iWorld;
    }

}
