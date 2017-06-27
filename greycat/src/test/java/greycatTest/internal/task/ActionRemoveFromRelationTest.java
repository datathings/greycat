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
import greycat.struct.Relation;
import greycat.ActionFunction;
import org.junit.Assert;
import org.junit.Test;
import greycat.Callback;
import greycat.TaskContext;
import greycat.TaskResult;

import static greycat.Tasks.newTask;
import static greycat.internal.task.CoreActions.*;

public class ActionRemoveFromRelationTest extends AbstractActionTest {

    public ActionRemoveFromRelationTest() {
        super();
        initGraph();
    }

    @Test
    public void testWithOneNode() {
        Node relatedNode = graph.newNode(0, 0);

        final long[] id = new long[1];
        newTask().then(createNode())
                .then(inject(relatedNode))
                .then(defineAsGlobalVar("x"))
                .then(addVarTo("friend", "x"))
                .then(removeVarFrom("friend", "x"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertNotNull(ctx.result());
                        Node node = ctx.resultAsNodes().get(0);
                        Assert.assertEquals(((Relation) node.get("friend")).size(), 0);
                        id[0] = node.id();
                    }
                }).execute(graph, null);


        graph.lookup(0, 0, id[0], new Callback<Node>() {
            @Override
            public void on(Node result) {
                Assert.assertEquals(((Relation) result.get("friend")).size(), 0);
                result.free();
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
                .then(removeVarFrom("friend", "x"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertNotNull(ctx.result());
                        TaskResult<Node> nodes = ctx.resultAsNodes();
                        for (int i = 0; i < 5; i++) {
                            Assert.assertEquals(((Relation) nodes.get(i).get("friend")).size(), 0);
                            ids[i] = nodes.get(i).id();
                        }
                    }
                }).execute(graph, null);

        for (int i = 0; i < ids.length; i++) {
            graph.lookup(0, 0, ids[i], new Callback<Node>() {
                @Override
                public void on(Node result) {
                    Assert.assertEquals(((Relation) result.get("friend")).size(), 0);
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
                .then(removeVarFrom("friend", "x"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        nextCalled[0] = true;
                    }
                }).execute(graph, null);

        Assert.assertTrue(nextCalled[0]);
    }

}
