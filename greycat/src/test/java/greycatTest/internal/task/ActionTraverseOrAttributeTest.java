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

import static greycat.internal.task.CoreActions.readIndex;
import static greycat.internal.task.CoreActions.traverse;
import static greycat.Tasks.newTask;

public class ActionTraverseOrAttributeTest extends AbstractActionTest {

    @Test
    public void test() {
        initGraph();
        newTask()
                .then(readIndex("nodes"))
                .traverse("children")
                .traverse("name")
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertEquals(ctx.result().get(0), "n0");
                        Assert.assertEquals(ctx.result().get(1), "n1");
                    }
                })
                .execute(graph, null);
        removeGraph();
    }

    @Test
    public void testDefaultSynthax() {
        initGraph();
        newTask()
                .then(readIndex("nodes"))
                .parse("children.name", graph)
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertEquals("n0", ctx.result().get(0));
                        Assert.assertEquals("n1", ctx.result().get(1));
                    }
                })
                .execute(graph, null);
        removeGraph();
    }


    @Test
    public void testParse() {
        initGraph();
        newTask()
                .parse("readIndex(nodes).traverse(children)", graph)
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
    public void testTraverseIndex() {
        initGraph();
        Node node1 = graph.newNode(0, 0);
        node1.set("name", Type.STRING, "node1");
        node1.set("value", Type.INT, 1);

        Node node2 = graph.newNode(0, 0);
        node2.set("name", Type.STRING, "node2");
        node2.set("value", Type.INT, 2);

        Node node3 = graph.newNode(0, 12);
        node3.set("name", Type.STRING, "node3");
        node3.set("value", Type.INT, 3);

        Node root = graph.newNode(0, 0);
        root.set("name", Type.STRING, "root2");


        graph.declareIndex(0, "roots", rootIndex -> {
            rootIndex.update(root);
            Index irel = (Index) root.getOrCreate("childrenIndexed", Type.INDEX);
            irel.declareAttributes(null, "name");
            irel.update(node1);
            irel.update(node2);
            irel.update(node3);
            root.travelInTime(12, new Callback<Node>() {
                @Override
                public void on(Node result) {

                    Index irel12 = (Index) root.getOrCreate("childrenIndexed", Type.INDEX);
                    irel12.update(node3);
                }
            });

        }, "name");

        /*
        readIndex("rootIndex", "name=root2")
                .traverseIndex("childrenIndexed", "name","node2")
                .then(new ActionFunction() {
                    @Override
                    public void eval(TaskContext context) {
                        Assert.assertEquals(1, context.result().size());
                        Assert.assertEquals("node2", context.resultAsNodes().get(0).get("name"));
                    }
                }).execute(graph, null);
*/

        newTask()
                .readIndex("rootIndex", "name", "root2")
                .traverse("childrenIndexed", "name", "node3")
                .thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Assert.assertEquals(0, ctx.result().size());
                    }
                }).execute(graph, null);

        /*
        inject(12).asGlobalVar("time").setTime("{{time}}")
                .readIndex("rootIndex", "name=root2")
                .traverseIndex("childrenIndexed", "name=node2")
                .then(new ActionFunction() {
                    @Override
                    public void eval(TaskContext context) {
                        Assert.assertEquals(1, context.result().size());
                        Assert.assertEquals("node2", context.resultAsNodes().get(0).get("name"));
                    }
                }).execute(graph, null);

        readIndex("rootIndex", "name=root2")
                .traverseIndexAll("childrenIndexed")
                .then(new ActionFunction() {
                    @Override
                    public void eval(TaskContext context) {
                        Assert.assertEquals(2, context.result().size());
                        Assert.assertEquals("node1", context.resultAsNodes().get(0).get("name"));
                        Assert.assertEquals("node2", context.resultAsNodes().get(1).get("name"));
                    }
                }).execute(graph, null);

        inject(13).asGlobalVar("time").setTime("{{time}}")
                .readIndex("rootIndex", "name=root2")
                .traverseIndexAll("childrenIndexed")
                .then(new ActionFunction() {
                    @Override
                    public void eval(TaskContext context) {
                        Assert.assertEquals(3, context.result().size());
                        Assert.assertEquals("node1", context.resultAsNodes().get(0).get("name"));
                        Assert.assertEquals("node2", context.resultAsNodes().get(1).get("name"));
                        Assert.assertEquals("node3", context.resultAsNodes().get(2).get("name"));
                    }
                }).execute(graph, null);

                */

        removeGraph();
    }

}
