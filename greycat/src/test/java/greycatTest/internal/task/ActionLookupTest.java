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

import static greycat.internal.task.CoreActions.*;
import static greycat.Tasks.newTask;

public class ActionLookupTest extends AbstractActionTest {

    @Test
    public void testLookup() {
        initGraph();

        newTask()
                .then(readIndex("nodes"))
                .thenDo(context -> {
                    TaskResult<Node> nodes = context.resultAsNodes();
                    for (int i = 0; i < nodes.size(); i++) {
                        context.addToVariable("ids", nodes.get(i).id());
                    }
                    context.continueTask();
                })
                .then(travelInTime("10"))
                .then(lookupAll("{{ids}}"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        TaskResult<Node> nodes = ctx.resultAsNodes();
                        Assert.assertEquals(nodes.get(0).toString(), "{\"world\":0,\"time\":10,\"id\":1,\"name\":\"n0\",\"value\":8}");
                        Assert.assertEquals(nodes.get(1).toString(), "{\"world\":0,\"time\":10,\"id\":2,\"name\":\"n1\",\"value\":3}");
                        Assert.assertEquals(nodes.get(2).toString(), "{\"world\":0,\"time\":10,\"id\":3,\"name\":\"root\",\"children\":[1,2]}");

                    }
                })
                .then(print("{{result}}"))
                .execute(graph, null);

        removeGraph();
    }
}
