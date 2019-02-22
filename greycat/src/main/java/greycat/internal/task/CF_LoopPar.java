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
import greycat.DeferCounter;
import greycat.plugin.Job;
import greycat.plugin.SchedulerAffinity;
import greycat.Task;
import greycat.TaskContext;
import greycat.TaskResult;
import greycat.struct.Buffer;

import java.util.Map;

class CF_LoopPar extends CF_Action {

    private final Task _subTask;

    private final String _lower;
    private final String _upper;

    CF_LoopPar(final String p_lower, final String p_upper, final Task p_subTask) {
        super();
        this._subTask = p_subTask;
        this._lower = p_lower;
        this._upper = p_upper;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final String lowerString = ctx.template(_lower);
        final String upperString = ctx.template(_upper);
        final int lower = (int) Double.parseDouble(ctx.template(lowerString));
        final int upper = (int) Double.parseDouble(ctx.template(upperString));
        final TaskResult previous = ctx.result();
        final Exception[] exceptionDuringTask = new Exception[1];
        exceptionDuringTask[0] = null;
        if ((upper - lower) > 0) {
            DeferCounter waiter = ctx.graph().newCounter((upper - lower) + 1);
            for (int i = lower; i <= upper; i++) {
                final int finalI = i;
                _subTask.executeFromUsing(ctx, previous, SchedulerAffinity.ANY_LOCAL_THREAD, new Callback<TaskContext>() {
                    @Override
                    public void on(TaskContext result) {
                        result.defineVariable("i", finalI);
                    }
                }, new Callback<TaskResult>() {
                    @Override
                    public void on(TaskResult result) {
                        if (result != null) {
                            if (result.output() != null) {
                                ctx.append(result.output());
                            }
                            if (result.exception() != null) {
                                exceptionDuringTask[0] = result.exception();
                            }
                            result.free();
                        }
                        waiter.count();
                    }
                });
            }
            waiter.then(new Job() {
                @Override
                public void run() {
                    if (exceptionDuringTask[0] != null) {
                        ctx.endTask(null, exceptionDuringTask[0]);
                    } else {
                        ctx.continueTask();
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
        children_tasks[0] = _subTask;
        return children_tasks;
    }

    @Override
    public void cf_serialize(final Buffer builder, Map<Integer, Integer> dagIDS) {
        builder.writeString(CoreActionNames.LOOP_PAR);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_lower, builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        TaskHelper.serializeString(_upper, builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
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
        return CoreActionNames.LOOP_PAR;
    }


}
