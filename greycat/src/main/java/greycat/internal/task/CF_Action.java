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
package greycat.internal.task;

import greycat.Action;
import greycat.Task;
import greycat.TaskContext;
import greycat.struct.Buffer;

import java.util.Map;

public abstract class CF_Action implements Action {

    abstract public Task[] children();

    abstract public void cf_serialize(final Buffer builder, Map<Integer, Integer> dagIDS);

    @Override
    public abstract void eval(TaskContext ctx);

    @Override
    public void serialize(final Buffer builder) {
        throw new RuntimeException("serialization error !!!");
    }

    abstract public String name();
}
