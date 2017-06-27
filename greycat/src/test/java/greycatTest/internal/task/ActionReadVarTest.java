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

import greycat.ActionFunction;
import greycat.TaskContext;
import org.junit.Assert;
import org.junit.Test;

import static greycat.internal.task.CoreActions.*;
import static greycat.Tasks.*;

public class ActionReadVarTest extends AbstractActionTest {

    @Test
    public void test() {
        initGraph();
        newTask()
                .then(readIndex("nodes"))
                .then(defineAsGlobalVar("x"))
                .then(inject("uselessPayload"))
                .then(readVar("x"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertEquals(ctx.resultAsNodes().get(0).get("name"), "n0");
                        Assert.assertEquals(ctx.resultAsNodes().get(1).get("name"), "n1");
                        Assert.assertEquals(ctx.resultAsNodes().get(2).get("name"), "root");
                    }
                })
                .execute(graph, null);
        removeGraph();
    }

    @Test
    public void testIndex() {
        initGraph();
        newTask()
                .then(readIndex("nodes"))
                .then(defineAsGlobalVar("x"))
                .then(inject("uselessPayload"))
                .then(readVar("x[0]"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertEquals(ctx.resultAsNodes().get(0).get("name"), "n0");
                        Assert.assertEquals(1, ctx.resultAsNodes().size());
                    }
                })
                .execute(graph, null);
        removeGraph();
    }


}
