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
package greycat.websocket.withWorkers;

import greycat.*;
import greycat.plugin.Plugin;
import greycat.plugin.Scheduler;
import greycat.plugin.SchedulerAffinity;
import greycat.struct.Buffer;
import greycat.workers.WorkerAffinity;

import java.util.Arrays;

import static greycat.Tasks.newTask;

/**
 * @ignore ts
 */
public class PluginForWorkersTest implements Plugin {
    public static final String CREATE_NODE = "createNode";
    public static final String THROW_EXCEPTION = "throwException";
    public static final String THROW_EXCEPTION_2 = "throwException2";
    public static final String PROGRESS_REPORTS = "progressReports";
    public static final String PRINT_HOOK = "printHook";

    public static final String PARENT_TASK_LAUNCHER = "parentTaskLauncher";
    public static final String CHILD_TASK_LAUNCHED = "childTaskLaunched";
    public static final String INTERMEDIATE_TASK_LAUNCHED = "intermediateTaskLaunched";

    @Override
    public void start(Graph graph) {

        graph.actionRegistry().getOrCreateDeclaration(CREATE_NODE).setParams().setFactory(params -> new Action() {
            @Override
            public void eval(TaskContext ctx) {
                Task createNode = Tasks.newTask().declareIndex("nodes", "name").createNode().setAttribute("name", Type.STRING, "Node " + System.currentTimeMillis()).updateIndex("nodes").save();
                createNode.executeFrom(ctx, ctx.result(), SchedulerAffinity.SAME_THREAD, ctx::continueWith);
            }

            @Override
            public void serialize(Buffer builder) {

            }

            @Override
            public String name() {
                return CREATE_NODE;
            }
        });

        graph.actionRegistry().getOrCreateDeclaration(THROW_EXCEPTION).setParams().setFactory(params -> new Action() {
            @Override
            public void eval(TaskContext ctx) {
                Task throwException = Tasks.newTask().thenDo(context -> {
                    context.endTask(context.newResult(), new RuntimeException("Exception given to endTask"));
                });
                throwException.executeFrom(ctx, ctx.result(), SchedulerAffinity.SAME_THREAD, ctx::continueWith);
            }

            @Override
            public void serialize(Buffer builder) {

            }

            @Override
            public String name() {
                return THROW_EXCEPTION;
            }
        });

        graph.actionRegistry().getOrCreateDeclaration(THROW_EXCEPTION_2).setParams().setFactory(params -> new Action() {
            @Override
            public void eval(TaskContext ctx) {
                Task throwException = Tasks.newTask().thenDo(context -> {
                    throw new RuntimeException("Exception thrown in flow");
                }).log("debug", "this should not happen");
                throwException.executeFrom(ctx, ctx.result(), SchedulerAffinity.SAME_THREAD, ctx::continueWith);
            }

            @Override
            public void serialize(Buffer builder) {

            }

            @Override
            public String name() {
                return THROW_EXCEPTION_2;
            }
        });

        graph.actionRegistry().getOrCreateDeclaration(PROGRESS_REPORTS).setParams().setFactory(params -> new Action() {
            @Override
            public void eval(TaskContext ctx) {
                Task progressReports = Tasks.newTask().inject(0).setAsVar("i").doWhile(Tasks.newTask().thenDo(context -> {
                    int i = (int) context.variable("i").get(0);
                    TaskResult taskIdRes = context.variable("taskId");
                    if (taskIdRes != null && taskIdRes.size() > 0) {
                        int taskId = Integer.parseInt((String) taskIdRes.get(0));
                        context.reportProgress(i / 10., "Task_" + taskId + "\tcomment " + i);
                    } else {
                        context.reportProgress(i / 10., "comment " + i);
                    }

                    context.setVariable("i", i + 1);
                    context.continueTask();
                }), context -> ((int) context.variable("i").get(0)) < 10);
                progressReports.executeFrom(ctx, ctx.result(), SchedulerAffinity.SAME_THREAD, ctx::continueWith);
            }

            @Override
            public void serialize(Buffer builder) {

            }

            @Override
            public String name() {
                return PROGRESS_REPORTS;
            }
        });

        graph.actionRegistry().getOrCreateDeclaration(PRINT_HOOK).setParams().setFactory(params -> new Action() {
            @Override
            public void eval(TaskContext ctx) {
                Task progressReports = Tasks.newTask().inject(0).setAsVar("i").doWhile(Tasks.newTask().println("{{i}}").thenDo(context -> {
                    int i = (int) context.variable("i").get(0);
                    context.setVariable("i", i + 1);
                    context.continueTask();
                }), context -> ((int) context.variable("i").get(0)) < 10);
                progressReports.executeFromUsing(ctx, ctx.result(), SchedulerAffinity.SAME_THREAD, newContext -> {
                    newContext.setPrintHook(ctx.printHook());
                }, ctx::continueWith);
            }

            @Override
            public void serialize(Buffer builder) {

            }

            @Override
            public String name() {
                return PRINT_HOOK;
            }
        });

        graph.actionRegistry().getOrCreateDeclaration(PARENT_TASK_LAUNCHER)
                .setFactory(params -> new Action() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Task subTask = Tasks.newTask()
                                .thenDo(taskContext -> {
                                    taskContext.setProgressAutoReport(true);
                                    taskContext.continueTask();
                                })
                                .action(INTERMEDIATE_TASK_LAUNCHED)
                                .thenDo(taskContext -> {
                                    taskContext.setProgressAutoReport(false);
                                    taskContext.continueTask();
                                })
                                ;
                        subTask.executeFrom(ctx, ctx.result(), SchedulerAffinity.ANY_LOCAL_THREAD, ctx::continueWith);
                    }

                    @Override
                    public void serialize(Buffer buffer) {
                    }

                    @Override
                    public String name() {
                        return PARENT_TASK_LAUNCHER;
                    }
                });

        graph.actionRegistry().getOrCreateDeclaration(INTERMEDIATE_TASK_LAUNCHED)
                .setFactory(params -> new Action() {
                    @Override
                    public void eval(TaskContext ctx) {



                        Task subTask = Tasks.newTask().action(CHILD_TASK_LAUNCHED);
                        TaskContext subCtx = subTask.prepare(ctx.graph(), null, null);
                        subCtx.setVariable("workerRef", "subTask");

                        Task subTask2 = Tasks.newTask().action(CHILD_TASK_LAUNCHED);
                        TaskContext subCtx2 = subTask2.prepare(ctx.graph(), null, null);
                        subCtx2.setVariable("workerRef", "subTask2");

                        Task subTask3 = Tasks.newTask().action(CHILD_TASK_LAUNCHED);
                        TaskContext subCtx3 = subTask3.prepare(ctx.graph(), null, null);
                        subCtx3.setVariable("workerRef", "subTask3");

                        ctx.continueWhenAllFinished(
                                Arrays.asList(subTask, subTask2, subTask3),
                                Arrays.asList(subCtx, subCtx2, subCtx3),
                                Arrays.asList("subTask", "subTask2", "subTask3")
                        );
                    }

                    @Override
                    public void serialize(Buffer buffer) {
                    }

                    @Override
                    public String name() {
                        return PARENT_TASK_LAUNCHER;
                    }
                });

        graph.actionRegistry().getOrCreateDeclaration(CHILD_TASK_LAUNCHED).setFactory(params -> new Action() {
            @Override
            public void eval(TaskContext ctx) {
                Task internalSubTask = newTask()
                        .inject(0)
                        .setAsVar("iteration")
                        .doWhile(newTask()
                                        .thenDo(taskContext -> {
                                            int iteration = (int) taskContext.variable("iteration").get(0);
                                            try {
                                                System.out.println(taskContext.template("{{workerRef}}:waiting("+iteration+")..."));
                                                Thread.sleep(500);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            taskContext.setVariable("iteration", iteration + 1);
                                            taskContext.continueTask();

                                        })
                                , doCtx -> ((int)doCtx.variable("iteration").get(0))!=10)

                        .thenDo(taskContext -> {
                            System.out.println(taskContext.template("{{workerRef}}:done with waiting"));
                            taskContext.continueWith(taskContext.newResult().add("5"));
                        });
                internalSubTask.executeFrom(ctx, ctx.result(), SchedulerAffinity.ANY_LOCAL_THREAD, ctx::continueWith);
            }

            @Override
            public void serialize(Buffer builder) {
            }

            @Override
            public String name() {
                return CHILD_TASK_LAUNCHED;
            }
        });

    }

    @Override
    public void stop() {

    }
}
