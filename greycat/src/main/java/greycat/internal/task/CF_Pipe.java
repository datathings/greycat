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

import greycat.Task;
import greycat.Callback;
import greycat.Constants;
import greycat.plugin.SchedulerAffinity;
import greycat.TaskContext;
import greycat.TaskResult;
import greycat.struct.Buffer;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class CF_Pipe extends CF_Action {

    private final Task[] _subTasks;

    CF_Pipe(final Task... p_subTasks) {
        super();
        _subTasks = p_subTasks;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final TaskResult previous = ctx.result();
        final AtomicInteger cursor = new AtomicInteger(0);
        final int tasksSize = _subTasks.length;
        final TaskResult next;
        if (tasksSize > 1) {
            next = ctx.newResult();
            next.allocate(tasksSize);
        } else {
            next = null;
        }
        final Callback<TaskResult>[] loopcb = new Callback[1];
        loopcb[0] = new Callback<TaskResult>() {
            @Override
            public void on(final TaskResult result) {
                Exception exceptionDuringTask = null;
                final int current = cursor.getAndIncrement();
                if (result != null) {
                    if (tasksSize > 1) {
                        next.add(result);
                    }
                    if (result.output() != null) {
                        ctx.append(result.output());
                    }
                    if (result.exception() != null) {
                        exceptionDuringTask = result.exception();
                    }
                }
                if (current < tasksSize && exceptionDuringTask == null) {
                    _subTasks[current].executeFrom(ctx, previous, SchedulerAffinity.SAME_THREAD, loopcb[0]);
                } else {
                    //end
                    TaskResult nextResult;
                    if (tasksSize > 1) {
                        nextResult = next;
                    } else {
                        nextResult = result;
                    }
                    if (exceptionDuringTask != null) {
                        ctx.endTask(nextResult, exceptionDuringTask);
                    } else {
                        ctx.continueWith(nextResult);
                    }
                }
            }
        };
        final int current = cursor.getAndIncrement();
        if (current < tasksSize) {
            _subTasks[current].executeFrom(ctx, previous, SchedulerAffinity.SAME_THREAD, loopcb[0]);
        } else {
            ctx.continueWith(next);
        }
    }

    @Override
    public Task[] children() {
        return _subTasks;
    }

    @Override
    public void cf_serialize(final Buffer builder, Map<Integer, Integer> dagIDS) {
        builder.writeString(CoreActionNames.PIPE);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        for (int i = 0; i < _subTasks.length; i++) {
            if (i != 0) {
                builder.writeChar(Constants.TASK_PARAM_SEP);
            }
            final CoreTask castedAction = (CoreTask) _subTasks[i];
            final int castedActionHash = castedAction.hashCode();
            if (dagIDS == null || !dagIDS.containsKey(castedActionHash)) {
                builder.writeChar(Constants.SUB_TASK_OPEN);
                castedAction.serialize(builder, dagIDS);
                builder.writeChar(Constants.SUB_TASK_CLOSE);
            } else {
                builder.writeString("" + dagIDS.get(castedActionHash));
            }
        }
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.PIPE;
    }

}
