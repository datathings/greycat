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

import greycat.Callback;
import greycat.Node;
import greycat.ActionFunction;
import greycat.TaskContext;
import greycat.TaskResult;
import org.junit.Assert;
import org.junit.Test;

import static greycat.Tasks.newTask;
import static greycat.internal.task.CoreActions.*;

public class ActionNewNodeTest extends AbstractActionTest {

    public ActionNewNodeTest() {
        super();
        initGraph();
    }

    @Test
    public void testCreateNode() {
        final long id[] = new long[1];
        newTask()
                .then(inject(15))
                .then(defineAsGlobalVar("world"))
                .then(travelInWorld("{{world}}"))
                .then(inject(587))
                .then(defineAsGlobalVar("time"))
                .then(travelInTime("{{time[0]}}"))
                .then(createNode())
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertNotNull(ctx.result());
                        TaskResult<Node> n = ctx.resultAsNodes();
                        id[0] = n.get(0).id();
                        Assert.assertEquals(15, n.get(0).world());
                        Assert.assertEquals(587, n.get(0).time());
                    }
                }).execute(graph, null);
        graph.lookup(15, 587, id[0], new Callback<Node>() {
            @Override
            public void on(Node result) {
                Assert.assertNotEquals(null, result);
            }
        });
    }

}
