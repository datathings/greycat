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

class ActionFlipVar implements Action {

    private final String _name;

    ActionFlipVar(final String name) {
        if (name == null) {
            throw new RuntimeException("name should not be null");
        }
        this._name = name;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final TaskResult previousGlobalVar = ctx.variable(_name);
        final TaskResult nextResult = previousGlobalVar.clone();
        previousGlobalVar.fillWith(ctx.result());
        ctx.continueWith(nextResult);
    }

    @Override
    public void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.FLIP_VAR);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_name, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.FLIP_VAR;
    }

}
