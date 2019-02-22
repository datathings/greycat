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

import greycat.Constants;
import greycat.ConditionalFunction;
import greycat.Task;
import greycat.Callback;
import greycat.plugin.SchedulerAffinity;
import greycat.TaskContext;
import greycat.TaskResult;
import greycat.struct.Buffer;

import java.util.Map;

class CF_IfThen extends CF_Action {

    private ConditionalFunction _condition;
    private Task _action;
    private String _conditionalScript;

    CF_IfThen(final ConditionalFunction cond, final Task action, final String conditionalScript) {
        super();
        if (cond == null) {
            throw new RuntimeException("condition should not be null");
        }
        if (action == null) {
            throw new RuntimeException("subTask should not be null");
        }
        this._conditionalScript = conditionalScript;
        this._condition = cond;
        this._action = action;
    }

    @Override
    public void eval(final TaskContext ctx) {
        if (_condition.eval(ctx)) {
            _action.executeFrom(ctx, ctx.result(), SchedulerAffinity.SAME_THREAD, new Callback<TaskResult>() {
                @Override
                public void on(TaskResult res) {
                    Exception exceptionDuringTask = null;
                    if (res != null) {
                        if (res.output() != null) {
                            ctx.append(res.output());
                        }
                        if (res.exception() != null) {
                            exceptionDuringTask = res.exception();
                        }
                    }
                    if (exceptionDuringTask != null) {
                        ctx.endTask(res, exceptionDuringTask);
                    } else {
                        ctx.continueWith(res);
                    }
                }
            });
        } else {
            ctx.continueTask();
        }
    }

    @Override
    public Task[] children() {
        Task[] children_tasks = new Task[1];
        children_tasks[0] = _action;
        return children_tasks;
    }

    @Override
    public void cf_serialize(final Buffer builder, Map<Integer, Integer> dagIDS) {
        if (_conditionalScript == null) {
            throw new RuntimeException("Closure is not serializable, please use Script version instead!");
        }
        builder.writeString(CoreActionNames.IF_THEN);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_conditionalScript, builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        final CoreTask castedAction = (CoreTask) _action;
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
        return CoreActionNames.IF_THEN;
    }

}
