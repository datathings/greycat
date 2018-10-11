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

import greycat.Node;
import greycat.ActionFunction;
import org.junit.Assert;
import org.junit.Test;
import greycat.Callback;
import greycat.struct.Relation;
import greycat.TaskContext;
import greycat.TaskResult;

import static greycat.Tasks.newTask;
import static greycat.internal.task.CoreActions.*;

public class ActionAddRemoveVarToTest extends AbstractActionTest {

    public ActionAddRemoveVarToTest() {
        super();
        initGraph();
    }

    @Test
    public void testWithOneNode() {
        Node relatedNode = graph.newNode(0, 0);
        final long[] id = new long[1];
        newTask()
                .then(createNode())
                .then(inject(relatedNode))
                .then(defineAsGlobalVar("x"))
                .then(addVarTo("friend", "x"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Node node = (Node) ctx.result().get(0);
                        Assert.assertNotNull(node);
                        Assert.assertEquals(1, ((Relation) node.get("friend")).size());
                        id[0] = node.id();
                    }
                }).execute(graph, new Callback<TaskResult>() {
            @Override
            public void on(TaskResult result) {
                graph.lookup(0, 0, id[0], new Callback<Node>() {
                    @Override
                    public void on(Node result) {
                        Assert.assertEquals(1, ((long[]) result.get("friend")).length);
                    }
                });
            }
        });
    }

    @Test
    public void testWithArray() {
        Node relatedNode = graph.newNode(0, 0);

        final long[] ids = new long[5];
        newTask()
                .then(inject(relatedNode))
                .then(defineAsGlobalVar("x"))
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
                .then(addVarTo("friend", "x"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        TaskResult<Node> nodes = ctx.resultAsNodes();
                        Assert.assertNotNull(nodes);
                        for (int i = 0; i < 5; i++) {
                            Assert.assertEquals(1, ((Relation) nodes.get(i).get("friend")).size());
                            ids[i] = nodes.get(i).id();
                        }
                    }
                }).execute(graph, null);

        for (int i = 0; i < ids.length; i++) {
            graph.lookup(0, 0, ids[i], new Callback<Node>() {
                @Override
                public void on(Node result) {
                    Assert.assertEquals(1, ((Relation) result.get("friend")).size());
                }
            });
        }


    }

    @Test
    public void testWithNull() {
        Node relatedNode = graph.newNode(0, 0);

        final boolean[] nextCalled = new boolean[1];

        newTask()
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        ctx.continueWith(null);
                    }
                })
                .then(inject(relatedNode))
                .then(defineAsGlobalVar("x"))
                .then(addVarTo("friend", "x"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        nextCalled[0] = true;
                    }
                })
                .execute(graph, null);

        Assert.assertTrue(nextCalled[0]);
    }

}
