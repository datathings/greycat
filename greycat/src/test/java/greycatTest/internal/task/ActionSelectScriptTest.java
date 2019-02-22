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

import greycat.ActionFunction;
import org.junit.Assert;
import org.junit.Test;
import greycat.TaskContext;

import static greycat.Tasks.newTask;
import static greycat.internal.task.CoreActions.*;

public class ActionSelectScriptTest extends AbstractActionTest {

    @Test
    public void test() {
        initGraph();
        newTask()
                .readIndex("nodes")
              //  .select((node, context) -> node.get("name").equals("root"))
                .selectScript("node.get('name') == 'root'")
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertEquals(1, ctx.result().size());
                        Assert.assertEquals(ctx.resultAsNodes().get(0).get("name"), "root");
                    }
                })
                .execute(graph, null);
        removeGraph();
    }

    @Test
    public void test2() {
        initGraph();
        newTask()
                .then(readIndex("nodes"))
                .then(selectScript("false"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertEquals(ctx.result().size(), 0);
                    }
                })
                .execute(graph, null);
        removeGraph();
    }

    @Test
    public void test3() {
        initGraph();
        newTask()
                .then(readIndex("nodes"))
                .then(selectScript("true"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertEquals(ctx.result().size(), 3);
                    }
                })
                .execute(graph, null);
        removeGraph();
    }

}
