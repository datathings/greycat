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
import greycat.Node;
import greycat.Task;
import greycat.Callback;
import greycat.base.BaseNode;
import greycat.plugin.Resolver;
import greycat.plugin.SchedulerAffinity;
import greycat.TaskContext;
import greycat.TaskResult;
import greycat.struct.Buffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CF_Atomic extends CF_Action {

    private final String[] _variables;
    private final Task _subTask;

    CF_Atomic(final Task p_subTask, final String... variables) {
        super();
        this._subTask = p_subTask;
        this._variables = variables;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final List<TaskResult> collected = new ArrayList<TaskResult>();
        for (int i = 0; i < _variables.length; i++) {
            String varName = _variables[i];
            TaskResult resolved;
            if (varName.equals("result")) {
                resolved = ctx.result();
            } else {
                resolved = ctx.variable(varName);
            }
            if (resolved != null) {
                collected.add(resolved);
            }
        }
        //acquire lock
        final Resolver resolver = ctx.graph().resolver();
        for (int i = 0; i < collected.size(); i++) {
            TaskResult toLock = collected.get(i);
            for (int j = 0; j < toLock.size(); j++) {
                Object o = toLock.get(j);
                if (o instanceof BaseNode) {
                    resolver.externalLock((Node) o);
                }
            }
        }
        _subTask.executeFrom(ctx, ctx.result(), SchedulerAffinity.SAME_THREAD, new Callback<TaskResult>() {
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
                }
                for (int i = 0; i < collected.size(); i++) {
                    TaskResult toLock = collected.get(i);
                    for (int j = 0; j < toLock.size(); j++) {
                        Object o = toLock.get(j);
                        if (o instanceof BaseNode) {
                            resolver.externalUnlock((Node) o);
                        }
                    }
                }
                if (exceptionDuringTask != null) {
                    ctx.endTask(result, exceptionDuringTask);
                } else {
                    ctx.continueWith(result);
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
        builder.writeString(CoreActionNames.ATOMIC);
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
        for (int i = 0; i < _variables.length; i++) {
            builder.writeChar(Constants.TASK_PARAM_SEP);
            TaskHelper.serializeString(_variables[i], builder, true);
        }
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.ATOMIC;
    }

}
