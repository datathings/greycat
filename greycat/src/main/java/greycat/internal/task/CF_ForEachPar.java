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
import greycat.TaskResultIterator;
import greycat.struct.Buffer;
import greycat.utility.Tuple;

import java.util.Map;

class CF_ForEachPar extends CF_Action {

    private final Task _subTask;

    CF_ForEachPar(final Task p_subTask) {
        super();
        _subTask = p_subTask;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final TaskResult previousResult = ctx.result();
        final TaskResultIterator it = previousResult.iterator();
        final int previousSize = previousResult.size();
        if (previousSize == -1) {
            throw new RuntimeException("Foreach on non array structure are not supported yet!");
        }
        final DeferCounter waiter = ctx.graph().newCounter(previousSize);
        final Job[] dequeueJob = new Job[1];
        final Exception[] exceptionDuringTask = new Exception[1];
        exceptionDuringTask[0] = null;
        dequeueJob[0] = new Job() {
            @Override
            public void run() {
                final Tuple<Integer, Object> loop = it.nextWithIndex();
                if (loop != null) {
                    _subTask.executeFromUsing(ctx, ctx.wrap(loop.right()), SchedulerAffinity.ANY_LOCAL_THREAD, new Callback<TaskContext>() {
                        @Override
                        public void on(TaskContext result) {
                            result.defineVariable("i", loop.left());
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
                            dequeueJob[0].run();
                        }
                    });
                }
            }
        };
        //create max // worker for this forEach
        final int nbThread = ctx.graph().scheduler().workers();
        for (int i = 0; i < nbThread; i++) {
            dequeueJob[0].run();
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
    }

    @Override
    public final Task[] children() {
        Task[] children_tasks = new Task[1];
        children_tasks[0] = _subTask;
        return children_tasks;
    }

    @Override
    public final void cf_serialize(final Buffer builder, Map<Integer, Integer> dagIDS) {
        builder.writeString(CoreActionNames.FOR_EACH_PAR);
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
        return CoreActionNames.FOR_EACH_PAR;
    }
}
