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
import greycat.TaskContext;
import greycat.TaskFunctionSelectObject;
import greycat.Tasks;
import greycat.internal.task.CoreActions;
import org.junit.Assert;
import org.junit.Test;

public class ActionSelectObjectTest extends AbstractActionTest {

    @Test
    public void testSelectOneObject() {
        initGraph();
        startMemoryLeakTest();
        Tasks.newTask()
                .then(CoreActions.inject(55))
                .then(CoreActions.selectObject((object, context) -> false))
                .thenDo(context -> Assert.assertEquals(context.result().size(), 0))
                .execute(graph, null);

        Tasks.newTask()
                .then(CoreActions.inject(55))
                .then(CoreActions.selectObject(new TaskFunctionSelectObject() {
                    @Override
                    public boolean select(Object object, TaskContext context) {
                        return true;
                    }
                }))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertNotEquals(ctx.result().size(), 0);
                        Assert.assertEquals(55, ctx.result().get(0));
                    }
                })
                .execute(graph, null);

        Tasks.newTask()
                .then(CoreActions.inject(55))
                .then(CoreActions.selectObject(new TaskFunctionSelectObject() {
                    @Override
                    public boolean select(Object object, TaskContext context) {
                        return (Integer) object == 55;
                    }
                }))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertNotEquals(ctx.result().size(), 0);
                        Assert.assertEquals(55, ctx.result().get(0));
                    }
                })
                .execute(graph, null);

        endMemoryLeakTest();
        removeGraph();
    }

}
