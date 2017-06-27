/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
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

import greycat.Node;
import greycat.ActionFunction;
import org.junit.Assert;
import org.junit.Test;
import greycat.TaskContext;
import greycat.TaskResult;

import static greycat.Tasks.newTask;
import static greycat.internal.task.CoreActions.*;

public class ActionForeachParTest extends AbstractActionTest {

    @Test
    public void test() {
        initGraph();
        final long[] i = {0};
        newTask()
                .then(inject(new long[]{1, 2, 3}))
                .forEachPar(
                        newTask().thenDo(context -> {
                            i[0]++;
                            Assert.assertEquals(context.result().get(0), i[0]);
                            context.continueTask();
                        })
                )
                .thenDo(context -> {
                    TaskResult<Long> longs = context.result();
                    Assert.assertEquals(longs.size(), 3);
                    Assert.assertEquals(longs.get(0), (Long) 1l);
                    Assert.assertEquals(longs.get(1), (Long) 2l);
                    Assert.assertEquals(longs.get(2), (Long) 3l);
                })
                .execute(graph, null);

        newTask().then(readIndex("nodes"))
                .forEachPar(
                        newTask().thenDo(new ActionFunction() {
                            @Override
                            public void eval(TaskContext ctx) {
                                ctx.continueTask();
                            }
                        })
                )
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        TaskResult<Node> nodes = ctx.resultAsNodes();
                        Assert.assertEquals(nodes.size(), 3);
                        Assert.assertEquals(nodes.get(0).get("name"), "n0");
                        Assert.assertEquals(nodes.get(1).get("name"), "n1");
                        Assert.assertEquals(nodes.get(2).get("name"), "root");
                    }
                })
                .execute(graph, null);

        /*
        List<String> paramIterable = new ArrayList<String>();
        paramIterable.add("n0");
        paramIterable.add("n1");
        paramIterable.add("root");
        newTask().then(inject(paramIterable))
                .forEachPar(newTask().thenDo(new ActionFunction() {
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

}
