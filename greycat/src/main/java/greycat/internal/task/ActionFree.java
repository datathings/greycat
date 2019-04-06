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
import greycat.base.BaseTaskResult;
import greycat.struct.Buffer;

class ActionFree implements Action {

    ActionFree() {
    }

    @Override
    public void eval(final TaskContext ctx) {
        TaskResult result = ctx.result();
        if (result == null) {
            ctx.continueTask();
        } else {
            for (int i = 0; i < result.size(); i++) {
                final Object loop = result.get(i);
                if (loop instanceof BaseNode) {
                    ((BaseNode) loop).free();
                }
            }
            result.clear();
            ctx.continueWith(ctx.newResult());
        }
    }

    @Override
    public void serialize(final Buffer builder) {
        builder.writeString(name());
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.FREE;
    }
}
