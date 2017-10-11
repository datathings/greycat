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
package greycat.internal.task.newactions;

import greycat.Action;
import greycat.Constants;
import greycat.TaskContext;
import greycat.internal.task.CoreActionNames;
import greycat.internal.task.TaskHelper;
import greycat.struct.Buffer;

public class ActionGetElement implements Action {


    private final String _elemID;

    ActionGetElement(final String elemID) {
        this._elemID = elemID;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final String flatID = ctx.template(_elemID);
        int id;
        try {
            id = Integer.parseInt(flatID);
            if(id<ctx.result().size()) {
                ctx.continueWith(ctx.wrap(ctx.result().get(id)));
            }
            else {
                ctx.continueWith(null);
            }
        } catch (Throwable t) {
            ctx.continueWith(null);
        }
    }

    @Override
    public void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.CREATE_TYPED_NODE);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_elemID, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }


    @Override
    public final String name() {
        return CoreActionNames.GET_ELEMENT;
    }
}
