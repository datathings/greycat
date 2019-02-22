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
package greycat.internal.task;

import greycat.*;
import greycat.base.BaseNode;
import greycat.struct.Buffer;
import greycat.utility.Tuple;

class ActionCloneNodes implements Action {

    @Override
    public final void eval(final TaskContext ctx) {
        TaskResult previousResult = ctx.result();
        if (previousResult == null) {
            ctx.continueTask();
        } else {
            TaskResult nextResult = ctx.newResult();
            for (int i = 0; i < previousResult.size(); i++) {
                Object source = previousResult.get(i);
                if (source instanceof BaseNode) {
                    nextResult.add(((BaseNode) source).createClone());
                } else {
                    nextResult.add(source);
                }
            }
            ctx.continueWith(nextResult);
        }
    }

    @Override
    public final void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.CLONE_NODES);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }


    @Override
    public final String name() {
        return CoreActionNames.CLONE_NODES;
    }

}
