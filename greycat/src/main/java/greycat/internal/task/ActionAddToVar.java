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

import greycat.Constants;
import greycat.Action;
import greycat.TaskContext;
import greycat.TaskResult;
import greycat.struct.Buffer;

class ActionAddToVar implements Action {

    private final String _name;

    ActionAddToVar(final String p_name) {
        if (p_name == null) {
            throw new RuntimeException("p_name should not be null");
        }
        this._name = p_name;
    }

    @Override
    public final void eval(final TaskContext ctx) {
        final TaskResult previousResult = ctx.result();
        if (ctx.isGlobal(_name)) {
            //TODO refactor this
            ctx.addToGlobalVariable(ctx.template(_name), previousResult);
        } else {
            ctx.addToVariable(ctx.template(_name), previousResult);
        }
        ctx.continueTask();
    }

    @Override
    public final void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.ADD_TO_VAR);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_name, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.ADD_TO_VAR;
    }

}
