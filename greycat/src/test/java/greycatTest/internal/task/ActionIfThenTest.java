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

import greycat.ActionFunction;
import greycat.ConditionalFunction;
import greycat.Task;
import greycat.TaskContext;
import org.junit.Assert;
import org.junit.Test;

import static greycat.Tasks.newTask;
import static greycat.internal.task.CoreActions.*;

public class ActionIfThenTest extends AbstractActionTest {

    @Test
    public void test() {
        initGraph();
        final boolean[] result = {false, false};

        Task modifyResult0 = newTask().thenDo(new ActionFunction() {
            @Override
            public void eval(TaskContext ctx) {
                result[0] = true;
            }
        });

        Task modifyResult1 = newTask().thenDo(new ActionFunction() {
            @Override
            public void eval(TaskContext ctx) {
                result[0] = true;
            }
        });

        newTask().ifThen(new ConditionalFunction() {
            @Override
            public boolean eval(TaskContext ctx) {
                return true;
            }
        }, modifyResult0).execute(graph, null);

        newTask().ifThen(new ConditionalFunction() {
            @Override
            public boolean eval(TaskContext ctx) {
                return false;
            }
        }, modifyResult0).execute(graph, null);

        Assert.assertEquals(true, result[0]);
        Assert.assertEquals(false, result[1]);
        removeGraph();
    }

    @Test
    public void testChainAfterIfThen() {
        initGraph();
        Task addVarInContext = newTask().then(inject(5)).then(defineAsGlobalVar("variable")).thenDo(new ActionFunction() {
            @Override
            public void eval(TaskContext ctx) {
                ctx.continueTask();
                //empty action
            }
        });

        newTask().ifThen(context -> true, addVarInContext).then(readVar("variable"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Integer val = (Integer) ctx.result().get(0);
                        Assert.assertEquals(5, (int) val);
                    }
                }).execute(graph, null);
        removeGraph();
    }

    @Test
    public void testScriptIf() {
        initGraph();
        newTask().inject("hello").defineAsVar("name").clearResult().ifThenScript("ctx.variable('name').get(0) == 'hello'", newTask().inject("success")).execute(graph, result -> {
            Assert.assertEquals("success", result.get(0));
        });
        newTask().inject("hello2").defineAsVar("name").clearResult().ifThenScript("ctx.variable('name').get(0) == 'hello'", newTask().inject("false")).execute(graph, result -> {
            Assert.assertEquals(0,result.size());
        });
        removeGraph();

    }

    @Test
    public void accessContextVariableInThenTask() {
        initGraph();
        Task accessVar = newTask().thenDo(new ActionFunction() {
            @Override
            public void eval(TaskContext ctx) {
                Integer variable = (Integer) ctx.variable("variable").get(0);
                Assert.assertEquals(5, (int) variable);
                ctx.continueTask();
            }
        });

        newTask().then(inject(5)).then(defineAsGlobalVar("variable")).ifThen(new ConditionalFunction() {
            @Override
            public boolean eval(TaskContext ctx) {
                return true;
            }
        }, accessVar).execute(graph, null);
        removeGraph();
    }
}
