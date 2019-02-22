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
import greycat.plugin.Resolver;
import greycat.plugin.SchedulerAffinity;
import greycat.struct.Buffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CF_Remote extends CF_Action {

    private final Task _subTask;

    CF_Remote(final Task p_subTask) {
        super();
        this._subTask = p_subTask;
    }

    @Override
    public final void eval(final TaskContext ctx) {
        TaskContext preparedRemote = this._subTask.prepare(ctx.graph(), ctx.result(), new Callback<TaskResult>() {
            @Override
            public void on(TaskResult result) {
                ctx.continueWith(result);
            }
        });
        this._subTask.executeRemotelyUsing(preparedRemote);
    }

    @Override
    public Task[] children() {
        Task[] children_tasks = new Task[1];
        children_tasks[0] = _subTask;
        return children_tasks;
    }

    @Override
    public void cf_serialize(final Buffer builder, Map<Integer, Integer> dagIDS) {
        builder.writeString(CoreActionNames.REMOTE);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        final CoreTask castedAction = (CoreTask) _subTask;
        final int castedActionHash = castedAction.hashCode();
        if (dagIDS == null || !dagIDS.containsKey(castedActionHash)) {
            builder.writeChar(Constants.SUB_TASK_OPEN);
            castedAction.serialize(builder, dagIDS);
            builder.writeChar(Constants.SUB_TASK_CLOSE);
        } else {
            builder.writeString("" + dagIDS.get(castedActionHash));
        }
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.REMOTE;
    }

}
