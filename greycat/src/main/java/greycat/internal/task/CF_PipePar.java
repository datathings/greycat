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

import greycat.Constants;
import greycat.plugin.Job;
import greycat.Task;
import greycat.Callback;
import greycat.DeferCounter;
import greycat.plugin.SchedulerAffinity;
import greycat.TaskContext;
import greycat.TaskResult;
import greycat.struct.Buffer;

import java.util.Map;

class CF_PipePar extends CF_Action {

    private final Task[] _subTasks;

    CF_PipePar(final Task... p_subTasks) {
        super();
        _subTasks = p_subTasks;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final TaskResult previous = ctx.result();
        final int subTasksSize = _subTasks.length;
        final TaskResult next = ctx.newResult();
        next.allocate(subTasksSize);
        final DeferCounter waiter = ctx.graph().newCounter(subTasksSize);
        final Exception[] exceptionDuringTask = new Exception[1];
        exceptionDuringTask[0] = null;
        for (int i = 0; i < subTasksSize; i++) {
            int finalI = i;
            _subTasks[i].executeFrom(ctx, previous, SchedulerAffinity.ANY_LOCAL_THREAD, new Callback<TaskResult>() {
                @Override
                public void on(TaskResult subTaskResult) {
                    if (subTaskResult != null) {
                        if (subTaskResult.output() != null) {
                            ctx.append(subTaskResult.output());
                        }
                        if (subTaskResult.exception() != null) {
                            exceptionDuringTask[0] = subTaskResult.exception();
                        }
                    }
                    next.set(finalI, subTaskResult);
                    waiter.count();
                }
            });
        }
        waiter.then(new Job() {
            @Override
            public void run() {
                TaskResult endResult = next;
                if (subTasksSize == 1) {
                    endResult = (TaskResult) next.get(0);
                }
                if (exceptionDuringTask[0] != null) {
                    ctx.endTask(endResult, exceptionDuringTask[0]);
                } else {
                    ctx.continueWith(endResult);
                }
            }
        });
    }

    @Override
    public Task[] children() {
        return _subTasks;
    }

    @Override
    public void cf_serialize(final Buffer builder, Map<Integer, Integer> dagIDS) {
        builder.writeString(CoreActionNames.PIPE_PAR);
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
        return CoreActionNames.PIPE_PAR;
    }

}
