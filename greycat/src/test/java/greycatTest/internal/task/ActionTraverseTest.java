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

import greycat.*;
import org.junit.Assert;
import org.junit.Test;

import static greycat.Tasks.newTask;
import static greycat.internal.task.CoreActions.*;

public class ActionTraverseTest extends AbstractActionTest {

    @Test
    public void test() {
        initGraph();
        newTask().then(readIndex("nodes"))
                .then(traverse("children"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertEquals(ctx.resultAsNodes().get(0).get("name"), "n0");
                        Assert.assertEquals(ctx.resultAsNodes().get(1).get("name"), "n1");
                    }
                })
                .execute(graph, null);
        removeGraph();
    }

    @Test
    public void testParse() {
        initGraph();
        newTask().parse("readIndex(nodes).traverse(children)", graph)
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertEquals(ctx.resultAsNodes().get(0).get("name"), "n0");
                        Assert.assertEquals(ctx.resultAsNodes().get(1).get("name"), "n1");
                    }
                })
                .execute(graph, null);
        removeGraph();
    }


/*
    @Test
    public void myTest() {
        initGraph();


        initGraph();
        final Node node1 = graph.newNode(0, 0);
        node1.set("name", Type.STRING, "node1");
        node1.set("value", Type.INT, 1);

        final Node node2 = graph.newNode(0, 0);
        node2.set("name", Type.STRING, "node2");
        node2.set("value", Type.INT, 2);

        final Node node3 = graph.newNode(0, 12);
        node3.set("name", Type.STRING, "node3");
        node3.set("value", Type.INT, 3);

        final Node root = graph.newNode(0, 0);
        root.set("name", Type.STRING, "root2");

        graph.index(0, 0, "roots", rootIndex -> {
            rootIndex.addToIndex(root, "name");

            RelationIndexed irel = (RelationIndexed) root.getOrCreate("childrenIndexed", Type.RELATION_INDEXED);
            irel.add(node1, "name");
            irel.add(node2, "name");
            //  irel.add(node3, "name");

            root.travelInTime(12, new Callback<Node>() {
                @Override
                public void on(Node root12) {
                    RelationIndexed irel12 = (RelationIndexed) root12.getOrCreate("childrenIndexed", Type.RELATION_INDEXED);
                    irel12.add(node3, "name");
                }
            });

        });

        newTask().travelInTime("0")
                .readIndex("roots", "name", "root2")
                .traverse("childrenIndexed")
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertEquals(2, ctx.result().size());
                        Assert.assertEquals("node1", ctx.resultAsNodes().get(0).get("name"));
                        Assert.assertEquals("node2", ctx.resultAsNodes().get(1).get("name"));
                    }
                }).execute(graph, null);
        removeGraph();
    }
*/

    @Test
    public void testTraverseIndex() {
        initGraph();
        final Node node1 = graph.newNode(0, 0);
        node1.set("name", Type.STRING, "node1");
        node1.set("value", Type.INT, 1);

        final Node node2 = graph.newNode(0, 0);
        node2.set("name", Type.STRING, "node2");
        node2.set("value", Type.INT, 2);

        final Node node3 = graph.newNode(0, 12);
        node3.set("name", Type.STRING, "node3");
        node3.set("value", Type.INT, 3);

        final Node root = graph.newNode(0, 0);
        root.set("name", Type.STRING, "root2");

        graph.declareIndex(0, "roots", rootIndex -> {
            rootIndex.update(root);

            Index irel = (Index) root.getOrCreate("childrenIndexed", Type.INDEX);
            irel.declareAttributes(null, "name");
            irel.update(node1);
            irel.update(node2);
            //  irel.add(node3, "name");

            root.travelInTime(12, new Callback<Node>() {
                @Override
                public void on(Node root12) {
                    Index irel12 = (Index) root12.get("childrenIndexed");
                    irel12.update(node3);
                }
            });

        }, "name");

        newTask()
                .then(travelInTime("0"))
                .then(readIndex("roots", "root2"))
                .then(traverse("childrenIndexed", "node2"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertEquals(1, ctx.result().size());
                        Assert.assertEquals("node2", ctx.resultAsNodes().get(0).get("name"));
                    }
                }).execute(graph, null);

        newTask().then(readIndex("rootIndex", "root2"))
                .then(traverse("childrenIndexed", "node3"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertEquals(0, ctx.result().size());
                    }
                }).execute(graph, null);

        newTask()
                .then(travelInTime("12"))
                .then(readIndex("roots", "root2"))
                .then(traverse("childrenIndexed", "node2"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertEquals(1, ctx.result().size());
                        Assert.assertEquals("node2", ctx.resultAsNodes().get(0).get("name"));
                    }
                }).execute(graph, null);

        newTask().then(travelInTime("0"))
                .then(readIndex("roots", "root2"))
                .then(traverse("childrenIndexed"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertEquals(2, ctx.result().size());
                        Assert.assertEquals("node1", ctx.resultAsNodes().get(0).get("name"));
                        Assert.assertEquals("node2", ctx.resultAsNodes().get(1).get("name"));
                    }
                }).execute(graph, null);

        newTask()
                .then(travelInTime("13"))
                .then(readIndex("roots", "root2"))
                .then(traverse("childrenIndexed"))
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertEquals(3, ctx.result().size());
                        Assert.assertEquals("node1", ctx.resultAsNodes().get(0).get("name"));
                        Assert.assertEquals("node2", ctx.resultAsNodes().get(1).get("name"));
                        Assert.assertEquals("node3", ctx.resultAsNodes().get(2).get("name"));
                    }
                }).execute(graph, null);
        removeGraph();
    }

    @Test
    public void indexedRelationTest() {
        initGraph();
        newTask()
                .createNode()
                .setAttribute("name", Type.STRING, "toto")
                .setAsVar("child")
                .createNode()
                .setAttribute("name", Type.STRING, "parent")
                .setAsVar("parent")
                .declareLocalIndex("children", "name")
                .addVarTo("children", "child")
                .inject("toto")
                .setAsVar("child_name")
                .readVar("parent")
                .traverse("children", "{{child_name}}")
                .println("{{result}}")
                .thenDo(context -> {
                    Assert.assertEquals(1, context.result().size());
                })
                .execute(graph, null);
    }
}
