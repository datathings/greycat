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
package greycatTest.utility;

import greycat.Graph;
import greycat.Task;
import greycat.Type;
import greycat.base.BaseNode;
import greycat.utility.HashHelper;

public class ExecutableNode extends BaseNode {

    private final Task _t_0;
    private final String exec = "exec";
    private final int exec_h = HashHelper.hash(exec);
    public static final String NAME = "ExecutableNode";

    public ExecutableNode(long p_world, long p_time, long p_id, Graph p_graph, Task t_0) {
        super(p_world, p_time, p_id, p_graph);
        this._t_0 = t_0;
    }


    @Override
    public int typeAt(int propIndex) {
        if (propIndex == 0 || propIndex == exec_h) {
            return Type.TASK;
        } else {
            return super.typeAt(propIndex);
        }
    }

    @Override
    public Object getAt(int propIndex) {
        if (propIndex == 0  || propIndex == exec_h) {
            return _t_0;
        } else {
            return super.getAt(propIndex);
        }
    }


}
