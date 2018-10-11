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
import greycat.struct.Buffer;

class ActionLog implements Action {

    private final String _value;

    ActionLog(final String p_value) {
        this._value = p_value;
    }

    @Override
    public void eval(final TaskContext ctx) {
        System.out.println(ctx.template(_value));
        ctx.continueTask();
    }

    @Override
    public final void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.LOG);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_value, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.LOG;
    }

}
