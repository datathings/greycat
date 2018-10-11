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
package greycatTest.internal.task;

import greycat.Callback;
import greycat.Task;
import greycat.TaskContext;
import greycat.TaskResult;
import org.junit.Assert;
import org.junit.Test;

import static greycat.internal.task.CoreActions.inject;
import static greycat.Tasks.newTask;

public class TaskOutputTest extends AbstractActionTest {

    @Test
    public void test() {
        initGraph();
        final boolean[] passed = {false};
        Task t = newTask()
                .then(inject("input"))
                .defineAsVar("myVar")
                .print("{{myVar}}");
        TaskContext ctx = t.prepare(graph, null, new Callback<TaskResult>() {
            @Override
            public void on(TaskResult result) {
                passed[0] = true;
                Assert.assertEquals(result.output(), "input");
                result.free();
            }
        });
        ctx.silentSave();
        t.executeUsing(ctx);
        Assert.assertTrue(passed[0]);
        removeGraph();
    }

    @Test
    public void forEachTest() {
        initGraph();
        final boolean[] passed = {false};
        Task t = newTask()
                .print("{")
                .loop("1", "5", newTask().print("{{i}}"))
                .print("}");
        TaskContext ctx=t.prepare(graph, null, new Callback<TaskResult>() {
                    @Override
                    public void on(TaskResult result) {
                        passed[0] = true;
                        Assert.assertEquals(result.output(), "{12345}");
                        result.free();
                    }
                });
        ctx.silentSave();
        t.executeUsing(ctx);
        Assert.assertTrue(passed[0]);
        removeGraph();
    }

}
