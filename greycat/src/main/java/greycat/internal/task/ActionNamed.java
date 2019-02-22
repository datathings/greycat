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

import greycat.Action;
import greycat.Constants;
import greycat.TaskContext;
import greycat.struct.Buffer;

class ActionNamed implements Action {

    private final String _name;
    private final String[] _params;

    ActionNamed(final String name, final String... params) {
        this._name = name;
        this._params = params;
    }

    @Override
    public void eval(final TaskContext ctx) {
        /*
        final TaskActionFactory actionFactory = ctx.graph().taskAction(this._name);
        if (actionFactory == null) {
            throw new RuntimeException("Unknown task action: " + _params);
        }
        final Action subAction = actionFactory.create(ctx.templates(_params), null);
        */
        final Action subAction = CoreTask.loadAction(ctx.graph().actionRegistry(), _name, _params, null, true);
        if (subAction != null) {
            subAction.eval(ctx);
        } else {
            ctx.continueTask();
        }
    }

    @Override
    public final void serialize(final Buffer builder) {
        builder.writeString(_name);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeStringParams(_params, builder);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }


    @Override
    public final String name() {
        return _name;
    }
}
