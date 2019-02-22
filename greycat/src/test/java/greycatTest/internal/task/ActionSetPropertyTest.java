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
import greycat.Type;
import greycat.ActionFunction;
import greycat.TaskContext;
import greycat.TaskResult;
import greycat.Tasks;
import org.junit.Assert;
import org.junit.Test;

import static greycat.internal.task.CoreActions.*;

public class ActionSetPropertyTest extends AbstractActionTest {

    public ActionSetPropertyTest() {
        super();
        initGraph();
    }

    @Test
    public void testWithOneNode() {
        final long[] id = new long[1];
        Tasks.newTask()
                .then(inject("node"))
                .then(defineAsGlobalVar("nodeName"))
                .then(createNode())
                .then(setAttribute("name", Type.STRING, "{{nodeName}}"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertNotNull(ctx.result());
                        TaskResult<Node> nodes = ctx.resultAsNodes();
                        Assert.assertEquals("node", nodes.get(0).get("name"));
                        id[0] = nodes.get(0).id();
                    }
                }).execute(graph, null);

        graph.lookup(0, 0, id[0], new Callback<Node>() {
            @Override
            public void on(Node result) {
                Assert.assertEquals("node", result.get("name"));
            }
        });
    }

    @Test
    public void testWithArray() {
        final long[] ids = new long[5];
        Tasks.newTask()
                .then(inject("node"))
                .then(defineAsGlobalVar("nodeName"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Node[] nodes = new Node[5];
                        for (int i = 0; i < 5; i++) {
                            nodes[i] = graph.newNode(0, 0);
                        }
                        ctx.continueWith(ctx.wrap(nodes));
                    }
                })
                .then(setAttribute("name", Type.STRING, "{{nodeName}}"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertNotNull(ctx.result());
                        TaskResult<Node> nodes = ctx.resultAsNodes();
                        for (int i = 0; i < 5; i++) {
                            Assert.assertEquals("node", nodes.get(i).get("name"));
                            ids[i] = nodes.get(i).id();
                        }
                    }
                }).execute(graph, null);

        for (int i = 0; i < ids.length; i++) {
            graph.lookup(0, 0, ids[i], new Callback<Node>() {
                @Override
                public void on(Node result) {
                    Assert.assertEquals("node", result.get("name"));
                }
            });
        }
    }

    @Test
    public void testWithNull() {
        final boolean[] nextCalled = new boolean[1];
        Tasks.newTask()
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        ctx.continueWith(null);
                    }
                })
                .then(setAttribute("name", Type.STRING, "node"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        nextCalled[0] = true;
                    }
                }).execute(graph, null);

        Assert.assertTrue(nextCalled[0]);
    }

}
