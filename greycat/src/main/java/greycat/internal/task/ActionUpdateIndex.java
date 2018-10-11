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
import greycat.internal.CoreDeferCounter;
import greycat.plugin.Job;
import greycat.base.BaseNode;
import greycat.struct.Buffer;

class ActionUpdateIndex implements Action {

    private final String _name;
    private final boolean _update;

    ActionUpdateIndex(final String name, final boolean update) {
        this._name = name;
        this._update = update;
    }

    @Override
    public final void eval(final TaskContext ctx) {
        final TaskResult previousResult = ctx.result();
        final String templatedIndexName = ctx.template(_name);
        final DeferCounter counter = new CoreDeferCounter(previousResult.size());
        for (int i = 0; i < previousResult.size(); i++) {
            final Object loop = previousResult.get(i);
            if (loop instanceof BaseNode) {
                BaseNode loopBaseNode = (BaseNode) loop;
                long indexTime = ctx.time();
                ctx.graph().index(loopBaseNode.world(), indexTime, templatedIndexName, new Callback<NodeIndex>() {
                    @Override
                    public void on(NodeIndex indexNode) {
                        if (ActionUpdateIndex.this._update) {
                            indexNode.update(loopBaseNode);
                        } else {
                            indexNode.unindex(loopBaseNode);
                        }
                        indexNode.free();
                        counter.count();
                    }
                });
            } else {
                counter.count();
            }
        }
        counter.then(new Job() {
            @Override
            public void run() {
                ctx.continueTask();
            }
        });
    }

    @Override
    public final void serialize(final Buffer builder) {
        if(this._update) {
            builder.writeString(CoreActionNames.UPDATE_INDEX);
        } else {
            builder.writeString(CoreActionNames.UNINDEX_FROM);
        }

        /*
        if (_timed) {
            if (_remove) {
                builder.writeString(CoreActionNames.REMOVE_FROM_GLOBAL_TIMED_INDEX);
            } else {
                builder.writeString(CoreActionNames.ADD_TO_GLOBAL_TIMED_INDEX);
            }
        } else {
            if (_remove) {
                builder.writeString(CoreActionNames.REMOVE_FROM_GLOBAL_INDEX);
            } else {
                builder.writeString(CoreActionNames.ADD_TO_GLOBAL_INDEX);
            }
        }
        */
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_name, builder, true);
        //builder.writeChar(Constants.TASK_PARAM_SEP);
        //TaskHelper.serializeStringParams(_attributes, builder);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.UPDATE_INDEX;
        /*
        return (_remove ?
                (_timed ? CoreActionNames.REMOVE_FROM_GLOBAL_TIMED_INDEX : CoreActionNames.REMOVE_FROM_GLOBAL_INDEX)
                : (_timed ? CoreActionNames.ADD_TO_GLOBAL_TIMED_INDEX : CoreActionNames.ADD_TO_GLOBAL_INDEX));*/
    }
}
