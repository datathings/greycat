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

import greycat.*;
import org.junit.Assert;
import org.junit.Test;

import static greycat.internal.task.CoreActions.*;
import static greycat.Tasks.newTask;

public class ActionTravelInTimeTest extends AbstractActionTest {

    @Test
    public void testTravelInTime() {
        initGraph();

        newTask().then(readIndex("nodes"))
                .then(defineAsGlobalVar("nodes"))
                .forEach(newTask().thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        TaskResult<Node> nodes = ctx.resultAsNodes();
                        Assert.assertEquals(Constants.BEGINNING_OF_TIME, nodes.get(0).time());
                        ctx.continueWith(null);
                    }
                }))
                .then(readVar("nodes"))
                .then(travelInTime("10"))
                .forEach(newTask().thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        TaskResult<Node> nodes = ctx.resultAsNodes();
                        Node it = nodes.get(0);
                        Assert.assertEquals(10, it.time());
                        ctx.continueWith(null);
                    }
                }))
                .execute(graph, null);

        removeGraph();
    }

    @Test
    public void testTravelInTime2() {
        initGraph();
        Task t = newTask().travelInTime("42").print("{{time}}");
        TaskContext ctx = t.prepare(graph, null, new Callback<TaskResult>() {
            @Override
            public void on(TaskResult result) {
                Assert.assertEquals("42",result.output());
            }
        });
        ctx.silentSave();
        t.executeUsing(ctx);
        removeGraph();
    }

}
