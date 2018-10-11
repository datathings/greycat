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
import greycat.plugin.SchedulerAffinity;
import greycat.TaskContext;
import greycat.TaskResult;
import greycat.TaskResultIterator;
import greycat.struct.Buffer;
import greycat.utility.Tuple;

import java.util.Map;

class CF_Map extends CF_Action {

    private final Task _subTask;

    CF_Map(final Task p_subTask) {
        super();
        _subTask = p_subTask;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final CF_Map selfPointer = this;
        final TaskResult previousResult = ctx.result();
        if (previousResult == null) {
            ctx.continueTask();
        } else {
            final TaskResultIterator it = previousResult.iterator();
            final TaskResult finalResult = ctx.newResult();
            finalResult.allocate(previousResult.size());
            final Callback[] recursiveAction = new Callback[1];
            final TaskResult[] loopRes = new TaskResult[1];
            recursiveAction[0] = new Callback<TaskResult>() {
                @Override
                public void on(final TaskResult res) {
                    Exception exceptionDuringTask = null;
                    if (res != null) {
                        finalResult.add(res);
                        if (res.output() != null) {
                            ctx.append(res.output());
                        }
                        if (res.exception() != null) {
                            exceptionDuringTask = res.exception();
                        }
                    }
                    loopRes[0].free();
                    final Tuple<Integer, Object> nextResult = it.nextWithIndex();
                    if (nextResult != null) {
                        loopRes[0] = ctx.wrap(nextResult.right());
                    } else {
                        loopRes[0] = null;
                    }
                    if (nextResult == null || exceptionDuringTask != null) {
                        if (exceptionDuringTask != null) {
                            ctx.endTask(finalResult, exceptionDuringTask);
                        } else {
                            ctx.continueWith(finalResult);
                        }
                    } else {
                        selfPointer._subTask.executeFromUsing(ctx, loopRes[0], SchedulerAffinity.SAME_THREAD, new Callback<TaskContext>() {
                            @Override
                            public void on(TaskContext result) {
                                result.defineVariable("i", nextResult.left());
                            }
                        }, recursiveAction[0]);
                    }
                }
            };
            final Tuple<Integer, Object> nextRes = it.nextWithIndex();
            if (nextRes != null) {
                loopRes[0] = ctx.wrap(nextRes.right());
                ctx.graph().scheduler().dispatch(SchedulerAffinity.SAME_THREAD, new Job() {
                    @Override
                    public void run() {
                        _subTask.executeFromUsing(ctx, loopRes[0], SchedulerAffinity.SAME_THREAD, new Callback<TaskContext>() {
                            @Override
                            public void on(TaskContext result) {
                                result.defineVariable("i", nextRes.left());
                            }
                        }, recursiveAction[0]);
                    }
                });
            } else {
                ctx.continueWith(finalResult);
            }
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
        builder.writeString(CoreActionNames.MAP);
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
        return CoreActionNames.MAP;
    }

}
