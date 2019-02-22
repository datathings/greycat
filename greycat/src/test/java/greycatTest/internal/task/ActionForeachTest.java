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
package greycatTest.internal.task;

import greycat.*;
import org.junit.Assert;
import org.junit.Test;

import static greycat.internal.task.CoreActions.inject;
import static greycat.internal.task.CoreActions.readIndex;
import static greycat.Tasks.newTask;

public class ActionForeachTest extends AbstractActionTest {

    @Test
    public void testForeachBreak() {
        initGraph();
        final long[] i = {0};
        newTask().then(inject(new long[]{1, 2, 3})).setAsVar("mbs").forEach(
                newTask().setAsVar("mb")
                        .forEach(newTask()).thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        i[0]++;
                        Assert.assertEquals(ctx.result().get(0), i[0]);
                        if (i[0] == 2) {
                            ctx.setVariable("break", true);
                        }
                        ctx.continueTask();
                    }
                })
        ).execute(graph, new Callback<TaskResult>() {
            @Override
            public void on(TaskResult result) {
                Assert.assertEquals(2, i[0]);
            }
        });

        removeGraph();
    }

    @Test
    public void testForeachWhere() {
        initGraph();
        final long[] i = {0};

        newTask()
                .then(inject(new long[]{1, 2, 3}))
                .forEach(
                        newTask().thenDo(new ActionFunction() {
                            @Override
                            public void eval(TaskContext ctx) {
                                i[0]++;
                                Assert.assertEquals(ctx.result().get(0), i[0]);
                                ctx.continueTask();
                            }
                        })
                )
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        TaskResult<Long> result = ctx.result();
                        Assert.assertEquals(result.size(), 3);
                        Assert.assertEquals(result.get(0), (Long) 1l);
                        Assert.assertEquals(result.get(1), (Long) 2l);
                        Assert.assertEquals(result.get(2), (Long) 3l);
                    }
                })
                .execute(graph, null);

        newTask()
                .then(readIndex("nodes"))
                .forEach(
                        newTask()
                                .thenDo(context -> context.continueTask())
                )
                .thenDo(context -> {
                    TaskResult<Node> nodes = context.resultAsNodes();
                    Assert.assertEquals(nodes.size(), 3);
                    Assert.assertEquals(nodes.get(0).get("name"), "n0");
                    Assert.assertEquals(nodes.get(1).get("name"), "n1");
                    Assert.assertEquals(nodes.get(2).get("name"), "root");
                }).execute(graph, null);

        /*
        List<String> paramIterable = new ArrayList<String>();
        paramIterable.add("n0");
        paramIterable.add("n1");
        paramIterable.add("root");
        newTask()
                .then(inject(paramIterable))
                .forEach(
                        newTask()
                                .thenDo(new ActionFunction() {
                                    @Override
                                    public void eval(TaskContext ctx) {
                                        ctx.continueTask();
                                    }
                                })
                )
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        TaskResult<String> names = ctx.result();
                        Assert.assertEquals(names.size(), 3);
                        Assert.assertEquals(names.get(0), "n0");
                        Assert.assertEquals(names.get(1), "n1");
                        Assert.assertEquals(names.get(2), "root");
                    }
                }).execute(graph, null);
*/
        removeGraph();
    }

/*
    @Test
    public void testForeach() {
        initGraph();
        final long[] toTest = {1, 2, 3, 4, 5};
        final int[] index = {0};

        inject(toTest).foreachThen(new Callback<Long>() {
            @Override
            public void on(Long object) {
                Assert.assertEquals(toTest[index[0]], (long) object);
                index[0]++;
            }
        }).execute(graph,null);

        index[0] = 0;
        new CoreTask().readIndexAll("nodes").foreachThen(new Callback<Node>() {
            @Override
            public void on(Node object) {
                object.set("name", "node" + index[0]);
                index[0]++;
            }
        }).readIndexAll("nodes").then(new ActionFunction() {
            @Override
            public void eval(TaskContext context) {
                Node[] result = (Node[]) context.result();
                Assert.assertEquals(3, result.length);
                Assert.assertEquals("node0", result[0].get("name"));
                Assert.assertEquals("node1", result[1].get("name"));
                Assert.assertEquals("node2", result[2].get("name"));
            }
        }).execute(graph,null);
        removeGraph();
    }*/

    /*
    @Test
    public void testForEachMergeVariables() {
        initGraph();
        final int[] index = {0};
        Task forEachTask = new CoreTask().then(new ActionFunction() {
            @Override
            public void eval(TaskContext context) {
                context.setVariable("param" + index[0]++, context.result());
                context.setResult(context.result());
            }
        });

        List<String> paramIterable = new ArrayList<String>();
        paramIterable.add("n0");
        paramIterable.add("n1");
        paramIterable.add("root");
        inject(paramIterable).foreach(forEachTask).readVar("param0").then(new ActionFunction() {
            @Override
            public void eval(TaskContext context) {
                Object result = (String) context.result();
                Assert.assertEquals("n0", result);
            }
        }).readVar("param1").then(new ActionFunction() {
            @Override
            public void eval(TaskContext context) {
                Object result = (String) context.result();
                Assert.assertEquals("n1", result);
            }
        }).readVar("param2").then(new ActionFunction() {
            @Override
            public void eval(TaskContext context) {
                Object result = (String) context.result();
                Assert.assertEquals("root", result);
            }
        }).execute(graph, null);
        removeGraph();
    }*/

}
