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

import greycat.*;
import greycat.struct.Buffer;

class ActionDeclareIndex implements Action {

    private final boolean _timed;
    private final String _name;
    private final String[] _attributes;

    ActionDeclareIndex(final boolean timed, final String name, final String... attributes) {
        this._timed = timed;
        this._name = name;
        this._attributes = attributes;
    }

    @Override
    public final void eval(final TaskContext ctx) {
        final TaskResult previousResult = ctx.result();
        final String templatedIndexName = ctx.template(_name);
        final String[] templatedAttributes = ctx.templates(_attributes);
        if (_timed) {
            ctx.graph().declareTimedIndex(ctx.world(), ctx.time(), templatedIndexName, new Callback<NodeIndex>() {
                @Override
                public void on(NodeIndex result) {
                    result.free();
                    ctx.continueTask();
                }
            }, templatedAttributes);
        } else {
            ctx.graph().declareIndex(ctx.world(), templatedIndexName, new Callback<NodeIndex>() {
                @Override
                public void on(NodeIndex result) {
                    result.free();
                    ctx.continueTask();
                }
            }, templatedAttributes);
        }
    }

    @Override
    public final void serialize(final Buffer builder) {
        if (_timed) {
            builder.writeString(CoreActionNames.DECLARE_TIMED_INDEX);
        } else {
            builder.writeString(CoreActionNames.DECLARE_INDEX);
        }
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_name, builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        TaskHelper.serializeStringParams(_attributes, builder);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        if (_timed) {
            return CoreActionNames.DECLARE_TIMED_INDEX;
        } else {
            return CoreActionNames.DECLARE_INDEX;
        }
    }
}
