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

import greycat.ConditionalFunction;
import greycat.Task;
import greycat.Callback;
import greycat.Constants;
import greycat.plugin.SchedulerAffinity;
import greycat.TaskContext;
import greycat.TaskResult;
import greycat.struct.Buffer;

import java.util.Map;

class CF_IfThenElse extends CF_Action {

    private ConditionalFunction _condition;
    private Task _thenSub;
    private Task _elseSub;
    private String _conditionalScript;

    CF_IfThenElse(final ConditionalFunction cond, final Task p_thenSub, final Task p_elseSub, final String conditionalScript) {
        super();
        if (cond == null) {
            throw new RuntimeException("condition should not be null");
        }
        if (p_thenSub == null) {
            throw new RuntimeException("thenSub should not be null");
        }
        if (p_elseSub == null) {
            throw new RuntimeException("elseSub should not be null");
        }
        this._conditionalScript = conditionalScript;
        this._condition = cond;
        this._thenSub = p_thenSub;
        this._elseSub = p_elseSub;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final Task selectedNextTask;
        if (_condition.eval(ctx)) {
            selectedNextTask = _thenSub;
        } else {
            selectedNextTask = _elseSub;
        }
        selectedNextTask.executeFrom(ctx, ctx.result(), SchedulerAffinity.SAME_THREAD, new Callback<TaskResult>() {
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
    }

    @Override
    public Task[] children() {
        Task[] children_tasks = new Task[2];
        children_tasks[0] = _thenSub;
        children_tasks[1] = _elseSub;
        return children_tasks;
    }

    @Override
    public void cf_serialize(final Buffer builder, Map<Integer, Integer> dagIDS) {
        if (_conditionalScript == null) {
            throw new RuntimeException("Closure is not serializable, please use Script version instead!");
        }
        builder.writeString(CoreActionNames.IF_THEN_ELSE);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_conditionalScript, builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        CoreTask castedSubThen = (CoreTask) _thenSub;
        int castedSubThenHash = castedSubThen.hashCode();
        if (dagIDS == null || !dagIDS.containsKey(castedSubThenHash)) {
            builder.writeChar(Constants.SUB_TASK_OPEN);
            castedSubThen.serialize(builder, dagIDS);
            builder.writeChar(Constants.SUB_TASK_CLOSE);
        } else {
            builder.writeString("" + dagIDS.get(castedSubThenHash));
        }
        builder.writeChar(Constants.TASK_PARAM_SEP);
        CoreTask castedSubElse = (CoreTask) _elseSub;
        int castedSubElseHash = castedSubElse.hashCode();
        if (dagIDS == null || !dagIDS.containsKey(castedSubElseHash)) {
            builder.writeChar(Constants.SUB_TASK_OPEN);
            castedSubElse.serialize(builder, dagIDS);
            builder.writeChar(Constants.SUB_TASK_CLOSE);
        } else {
            builder.writeString("" + dagIDS.get(castedSubElseHash));
        }
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.IF_THEN_ELSE;
    }

}
