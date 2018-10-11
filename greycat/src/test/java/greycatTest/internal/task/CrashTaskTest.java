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

import greycat.*;
import org.junit.Assert;
import org.junit.Test;

import static greycat.internal.task.CoreActions.inject;
import static greycat.Tasks.newTask;

public class CrashTaskTest extends AbstractActionTest {

    @Test
    public void test() {
        initGraph();
        final boolean[] passed = {false};
        Task t = newTask()
                .then(inject("input"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        //produce an NPE
                        String a = null;
                        a.toString();
                    }
                });
        TaskContext ctx = t.prepare(graph, null, new Callback<TaskResult>() {
            @Override
            public void on(TaskResult result) {
                passed[0] = true;
                Assert.assertNotNull(result.exception());
            }
        });
        ctx.silentSave();//hide the exception
        t.executeUsing(ctx);
        Assert.assertTrue(passed[0]);
        removeGraph();
    }

}
