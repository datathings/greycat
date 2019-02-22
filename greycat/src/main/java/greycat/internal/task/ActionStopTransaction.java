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

import greycat.Action;
import greycat.Callback;
import greycat.Constants;
import greycat.TaskContext;
import greycat.struct.Buffer;
import greycat.utility.LMap;

class ActionStopTransaction implements Action {

    @Override
    public final void eval(TaskContext ctx) {
        LMap map = ctx.tracker();
        final Buffer notifier = ctx.notifier();
        if (notifier == null) {
            ctx.graph().space().save(false, false, map, new Callback<Buffer>() {
                @Override
                public void on(final Buffer result) {
                    if (result != null) {
                        result.free();
                    }
                    ctx.removeTracker();
                    ctx.continueTask();
                }
            });
        } else {
            ctx.graph().space().save(true, false, map, new Callback<Buffer>() {
                @Override
                public void on(final Buffer result) {
                    notifier.writeAll(result.data());
                    result.free();
                    ctx.continueTask();
                }
            });
        }
    }

    @Override
    public final void serialize(Buffer builder) {
        builder.writeString(CoreActionNames.STOP_TRANSACTION);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.STOP_TRANSACTION;
    }

}
