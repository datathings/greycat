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

class ActionReadVar implements Action {

    private final String _origin;
    private final String _name;
    private final int _index;

    ActionReadVar(String p_name) {
        this._origin = p_name;
        int indexEnd = -1;
        int indexStart = -1;
        int cursor = p_name.length() - 1;
        while (cursor > 0) {
            char c = p_name.charAt(cursor);
            if (c == ']') {
                indexEnd = cursor;
            } else if (c == '[') {
                indexStart = cursor + 1;
            }
            cursor--;
        }
        if (indexEnd != -1 && indexStart != -1) {
            _index = TaskHelper.parseInt(p_name.substring(indexStart, indexEnd));
            _name = p_name.substring(0, indexStart - 1);
        } else {
            _index = -1;
            _name = p_name;
        }
    }

    @Override
    public void eval(final TaskContext ctx) {
        final String evaluatedName = ctx.template(_name);
        TaskResult varResult;
        if (_index != -1) {
            varResult = ctx.wrap(ctx.variable(evaluatedName).get(_index));
        } else {
            varResult = ctx.variable(evaluatedName);
        }
        if (varResult != null) {
            varResult = varResult.clone();
        } else {
            varResult = ctx.newResult();
        }
        ctx.continueWith(varResult);
    }

    @Override
    public void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.READ_VAR);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_origin, builder,true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }


    @Override
    public final String name() {
        return CoreActionNames.READ_VAR;
    }

}
