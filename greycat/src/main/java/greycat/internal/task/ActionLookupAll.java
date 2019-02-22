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

import greycat.Callback;
import greycat.Constants;
import greycat.Node;
import greycat.Action;
import greycat.TaskContext;
import greycat.struct.Buffer;

class ActionLookupAll implements Action {

    private final String _ids;

    ActionLookupAll(final String p_ids) {
        this._ids = p_ids;
    }

    @Override
    public void eval(final TaskContext ctx) {
        String afterTemplate = ctx.template(_ids).trim();
        if (afterTemplate.startsWith("[")) {
            afterTemplate = afterTemplate.substring(1, afterTemplate.length() - 1);
        }
        String[] values = afterTemplate.split(",");
        long[] ids = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            ids[i] = Long.parseLong(values[i]);
        }
        ctx.graph().lookupAll(ctx.world(), ctx.time(), ids, new Callback<Node[]>() {
            @Override
            public void on(Node[] result) {
                ctx.continueWith(ctx.wrap(result));
            }
        });
    }

    @Override
    public final void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.LOOKUP_ALL);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_ids, builder,true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }


    @Override
    public final String name() {
        return CoreActionNames.LOOKUP_ALL;
    }

}
