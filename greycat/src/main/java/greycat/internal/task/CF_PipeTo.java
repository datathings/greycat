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

class CF_PipeTo extends CF_Action {

    private final Task _subTask;
    private final String[] _targets;

    CF_PipeTo(final Task p_subTask, final String... p_targets) {
        super();
        if (p_subTask == null) {
            throw new RuntimeException("subTask should not be null");
        }
        _subTask = p_subTask;
        _targets = p_targets;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final TaskResult previous = ctx.result();
        _subTask.executeFrom(ctx, previous, SchedulerAffinity.SAME_THREAD, new Callback<TaskResult>() {
            @Override
            public void on(TaskResult result) {
                Exception exceptionDuringTask = null;
                if (result != null) {
                    if (result.output() != null) {
                        ctx.append(result.output());
                    }
                    if (result.exception() != null) {
                        exceptionDuringTask = result.exception();
                    }
                    if (_targets != null && _targets.length > 0) {
                        for (int i = 0; i < _targets.length; i++) {
                            ctx.setVariable(_targets[i], result);
                        }
                    } else {
                        result.free();
                    }
                }
                if (exceptionDuringTask != null) {
                    ctx.endTask(previous, exceptionDuringTask);
                } else {
                    ctx.continueWith(previous);
                }
            }
        });
    }

    @Override
    public Task[] children() {
        Task[] children_tasks = new Task[1];
        children_tasks[0] = _subTask;
        return children_tasks;
    }

    @Override
    public void cf_serialize(final Buffer builder, Map<Integer, Integer> dagIDS) {
        builder.writeString(CoreActionNames.PIPE_TO);
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
        if (_targets != null && _targets.length > 0) {
            builder.writeChar(Constants.TASK_PARAM_SEP);
            TaskHelper.serializeStringParams(_targets, builder);
        }
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.PIPE_TO;
    }

}
