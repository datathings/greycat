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
import greycat.plugin.Job;
import greycat.struct.Buffer;

class ActionDeclareLocalIndex implements Action {

    private final String _name;
    private final String[] _attributes;

    ActionDeclareLocalIndex(final String name, final String... attributes) {
        this._name = name;
        this._attributes = attributes;
    }

    @Override
    public final void eval(final TaskContext ctx) {
        final TaskResult previousResult = ctx.result();
        final String templatedIndexName = ctx.template(_name);
        final String[] templatedAttributes = ctx.templates(_attributes);
        DeferCounter barrier = ctx.graph().newCounter(previousResult.size());
        barrier.then(new Job() {
            @Override
            public void run() {
                ctx.continueTask();
            }
        });
        for (int i = 0; i < previousResult.size(); i++) {
            Object resolved = previousResult.get(i);
            if (resolved instanceof BaseNode) {
                Node casted = (Node) resolved;
                ((Index) casted.getOrCreate(templatedIndexName, Type.INDEX)).declareAttributes(new Callback() {
                    @Override
                    public void on(Object result) {
                        barrier.count();
                    }
                }, templatedAttributes);
            }
        }
    }

    @Override
    public final void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.DECLARE_LOCAL_INDEX);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_name, builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        TaskHelper.serializeStringParams(_attributes, builder);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.DECLARE_LOCAL_INDEX;
    }
}
