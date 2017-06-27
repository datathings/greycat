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

public class ActionScriptTest extends AbstractActionTest {

    /*
    @Test
    public void testSimpleScript() {
        initGraph();
        newTask()
                .then(readIndex("nodes"))
                .asyncScript("ctx.setVariable(\"val1\",55).continueTask();")
                .asyncScript("ctx.setVariable(\"val2\",56).continueTask();")
                .asyncScript("ctx.setVariable(\"val4\",70).setVariable(\"val8\",999).continueTask();")
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertEquals(55, ctx.variable("val1").get(0));
                        Assert.assertEquals(56, ctx.variable("val2").get(0));
                        Assert.assertEquals(70, ctx.variable("val4").get(0));
                        Assert.assertEquals(999, ctx.variable("val8").get(0));
                        ctx.continueWith(ctx.wrap(true));
                    }
                })
                .execute(graph, new Callback<TaskResult>() {
                    @Override
                    public void on(TaskResult result) {
                        Assert.assertEquals(true, result.get(0));
                        removeGraph();
                    }
                });
    }
    */

    /*
    @Test
    public void testPrintScript() {
        initGraph();
        newTask()
                .script("'hello'") //will put result in the result
                .defineAsGlobalVar("myVar")
                .println("{{result}}")
                .loop("0", "2", newTask().script("print(myVar.get(0))"))
                .execute(graph, new Callback<TaskResult>() {
                    @Override
                    public void on(TaskResult result) {
                        Assert.assertNull(result.exception());
                        Assert.assertEquals("hello\nhello\nhello\nhello\n", result.output());
                        removeGraph();
                    }
                });
    }*/

/*
    @Test
    public void testVarScript() {
        initGraph();
        newTask()
                .script("'time='+new Date(1484123443411).getTime()") //will put result in the result
                .defineAsGlobalVar("myVar")
                .script("print(myVar.get(0))")
                .inject("3")
                .println("{{result}}")
                .execute(graph, new Callback<TaskResult>() {
                    @Override
                    public void on(TaskResult result) {
                        Assert.assertNull(result.exception());
                        Assert.assertEquals("time=1484123443411\n3\n", result.output());
                        removeGraph();
                    }
                });
    }
    */

}
