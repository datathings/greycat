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
import greycat.plugin.SchedulerAffinity;
import greycat.Task;
import greycat.TaskContext;
import greycat.TaskResult;
import greycat.struct.Buffer;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class CF_Loop extends CF_Action {

    private final String _lower;
    private final String _upper;
    private final Task _subTask;

    CF_Loop(final String p_lower, final String p_upper, final Task p_subTask) {
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
        final CF_Loop selfPointer = this;
        final AtomicInteger cursor = new AtomicInteger(lower);
        if ((upper - lower) >= 0) {
            final Callback[] recursiveAction = new Callback[1];
            recursiveAction[0] = new Callback<TaskResult>() {
                @Override
                public void on(final TaskResult res) {
                    final int current = cursor.getAndIncrement();
                    Exception exceptionDuringTask = null;
                    if (res != null) {
                        if (res.output() != null) {
                            ctx.append(res.output());
                        }
                        if (res.exception() != null) {
                            exceptionDuringTask = res.exception();
                        }
                        res.free();
                    }
                    if (current > upper || exceptionDuringTask != null) {
                        if (exceptionDuringTask != null) {
                            ctx.endTask(null, exceptionDuringTask);
                        } else {
                            ctx.continueTask();
                        }
                    } else {
                        //recursive call
                        selfPointer._subTask.executeFromUsing(ctx, previous, SchedulerAffinity.SAME_THREAD, new Callback<TaskContext>() {
                            @Override
                            public void on(TaskContext result) {
                                result.defineVariable("i", current);
                            }
                        }, recursiveAction[0]);
                    }
                }
            };
            _subTask.executeFromUsing(ctx, previous, SchedulerAffinity.SAME_THREAD, new Callback<TaskContext>() {
                @Override
                public void on(TaskContext result) {
                    result.defineVariable("i", cursor.getAndIncrement());
                }
            }, recursiveAction[0]);
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
        builder.writeString(CoreActionNames.LOOP);
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
        return CoreActionNames.LOOP;
    }

}
