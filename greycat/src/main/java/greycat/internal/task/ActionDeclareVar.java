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

import greycat.Constants;
import greycat.Action;
import greycat.TaskContext;
import greycat.Tasks;
import greycat.struct.Buffer;

class ActionDeclareVar implements Action {

    private final String _name;
    private final boolean _isGlobal;

    ActionDeclareVar(final boolean isGlobal, final String p_name) {
        if (p_name == null) {
            throw new RuntimeException("name should not be null");
        }
        this._name = p_name;
        this._isGlobal = isGlobal;
    }

    @Override
    public final void eval(final TaskContext ctx) {
        if (_isGlobal) {
            ctx.setGlobalVariable(ctx.template(_name), Tasks.emptyResult());
        } else {
            ctx.declareVariable(ctx.template(_name));
        }
        ctx.continueTask();
    }

    @Override
    public final void serialize(final Buffer builder) {
        if (_isGlobal) {
            builder.writeString(CoreActionNames.DECLARE_GLOBAL_VAR);
        } else {
            builder.writeString(CoreActionNames.DECLARE_VAR);
        }
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_name, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }


    @Override
    public final String name() {
        return (_isGlobal?CoreActionNames.DECLARE_GLOBAL_VAR:CoreActionNames.DECLARE_VAR);
    }
}
