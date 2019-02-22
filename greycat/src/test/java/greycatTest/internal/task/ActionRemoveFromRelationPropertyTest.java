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
import org.junit.Assert;
import org.junit.Test;

import static greycat.internal.task.CoreActions.*;
import static greycat.Tasks.newTask;

public class ActionRemoveFromRelationPropertyTest extends AbstractActionTest {

    public ActionRemoveFromRelationPropertyTest() {
        super();
        initGraph();
    }

    @Test
    public void testWithOneNode() {
        final long[] id = new long[1];

        newTask()
                .then(inject("nodeName"))
                .then(defineAsGlobalVar("name"))
                .then(createNode())
                .then(setAttribute("name", Type.STRING, "nodeName"))
                .then(remove("name"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        TaskResult<Node> nodes = ctx.resultAsNodes();
                        Assert.assertNotNull(nodes.get(0));
                        Assert.assertNull(nodes.get(0).get("name"));
                        id[0] = nodes.get(0).id();
                    }
                })
                .execute(graph, null);

        graph.lookup(0, 0, id[0], new Callback<Node>() {
            @Override
            public void on(Node result) {
                Assert.assertNull(result.get("name"));
            }
        });
    }

    @Test
    public void testWithArray() {
        final long[] ids = new long[5];
        newTask()
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
                .then(setAttribute("name", Type.STRING, "nodeName"))
                .then(remove("name"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        TaskResult<Node> nodes = ctx.resultAsNodes();
                        Assert.assertNotNull(nodes);

                        for (int i = 0; i < 5; i++) {
                            Assert.assertNull(nodes.get(i).get("name"));
                            ids[i] = nodes.get(i).id();
                        }
                    }
                }).execute(graph, null);

        for (int i = 0; i < ids.length; i++) {
            graph.lookup(0, 0, ids[i], new Callback<Node>() {
                @Override
                public void on(Node result) {
                    Assert.assertNull(result.get("name"));
                }
            });
        }
    }

    @Test
    public void testWithNull() {
        final boolean[] nextCalled = new boolean[1];
        newTask()
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        ctx.continueWith(null);
                    }
                })
                .then(setAttribute("name", Type.STRING, "node"))
                .then(remove("name"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        nextCalled[0] = true;
                    }
                }).execute(graph, null);

        Assert.assertTrue(nextCalled[0]);
    }

}
