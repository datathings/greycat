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

import greycat.Callback;
import greycat.Constants;
import greycat.Node;
import greycat.Action;
import greycat.TaskContext;
import greycat.struct.Buffer;

class ActionLookup implements Action {

    private final String _id;

    ActionLookup(final String p_id) {
        this._id = p_id;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final long idL = Long.parseLong(ctx.template(_id));
        ctx.graph().lookup(ctx.world(), ctx.time(), idL, new Callback<Node>() {
            @Override
            public void on(Node result) {
                ctx.continueWith(ctx.wrap(result));
            }
        });
    }

    @Override
    public void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.LOOKUP);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_id, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }


    @Override
    public final String name() {
        return CoreActionNames.LOOKUP;
    }

}
