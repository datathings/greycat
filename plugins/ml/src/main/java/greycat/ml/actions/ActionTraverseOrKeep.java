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
package greycat.ml.actions;

import greycat.*;
import greycat.base.BaseNode;
import greycat.internal.task.TaskHelper;
import greycat.plugin.Job;
import greycat.struct.Buffer;

public class ActionTraverseOrKeep implements Action {

    public static final String NAME = "traverseOrKeep";

    private final String _name;

    public ActionTraverseOrKeep(final String p_name) {
        this._name = p_name;
    }

    @Override
    public final void eval(final TaskContext ctx) {
        final String flatName = ctx.template(_name);
        final TaskResult previousResult = ctx.result();
        if (previousResult != null) {
            final TaskResult finalResult = ctx.newResult();
            final int previousSize = previousResult.size();
            final DeferCounter defer = ctx.graph().newCounter(previousSize);
            for (int i = 0; i < previousSize; i++) {
                final Object loop = previousResult.get(i);
                if (loop instanceof BaseNode) {
                    Node casted = (Node) loop;
                    if (casted.type(flatName) == Type.RELATION) {
                        casted.traverse(flatName, new Callback<Node[]>() {
                            @Override
                            public void on(Node[] result) {
                                if (result != null) {
                                    for (int j = 0; j < result.length; j++) {
                                        finalResult.add(result[j]);
                                    }
                                }
                                defer.count();
                            }
                        });
                    } else {
                        finalResult.add(casted.graph().cloneNode(casted));
                        defer.count();
                    }
                } else {
                    //TODO add closable management
                    finalResult.add(loop);
                    defer.count();
                }
            }
            defer.then(new Job() {
                @Override
                public void run() {
                    ctx.continueWith(finalResult);
                }
            });
        } else {
            ctx.continueTask();
        }
    }

    @Override
    public final void serialize(final Buffer builder) {
        builder.writeString(NAME);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_name, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return NAME;
    }

}