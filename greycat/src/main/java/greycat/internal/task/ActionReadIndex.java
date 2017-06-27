/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
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
import greycat.Index;
import greycat.struct.Buffer;

class ActionReadIndex implements Action {

    private final String _name;
    private final String[] _params;

    ActionReadIndex(final String p_indexName, final String... p_query) {
        if (p_indexName == null) {
            throw new RuntimeException("indexName should not be null");
        }
        _name = p_indexName;
        _params = p_query;
    }

    @Override
    public final void eval(final TaskContext ctx) {
        final String name = ctx.template(_name);
        final String[] query = ctx.templates(_params);
        ctx.graph().index(ctx.world(), ctx.time(), name, new Callback<NodeIndex>() {
            @Override
            public void on(NodeIndex resolvedIndex) {
                if (resolvedIndex != null) {
                    resolvedIndex.find(new Callback<Node[]>() {
                        @Override
                        public void on(Node[] result) {
                            resolvedIndex.free();
                            ctx.continueWith(ctx.wrap(result));
                        }
                    }, ctx.world(), ctx.time(), query);
                } else {
                    ctx.continueWith(ctx.newResult());
                }
            }
        });
    }

    @Override
    public final void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.READ_INDEX);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_name, builder, true);
        if (_params != null && _params.length > 0) {
            builder.writeChar(Constants.TASK_PARAM_SEP);
            TaskHelper.serializeStringParams(_params, builder);
        }
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }


    @Override
    public final String name() {
        return CoreActionNames.READ_INDEX;
    }

}
