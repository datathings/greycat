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
package greycatTest;

import greycat.*;
import greycat.plugin.NodeFactory;
import greycat.scheduler.NoopScheduler;
import greycatTest.utility.ExecutableNode;
import org.junit.Assert;
import org.junit.Test;

import static greycat.Tasks.newTask;

public class DeriveAttributeTest {

    static Task gTask = newTask().executeExpression("x + y");

    @Test
    public void test() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(null);
        g.nodeRegistry().getOrCreateDeclaration(ExecutableNode.NAME).setFactory(new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new ExecutableNode(world, time, id, graph, gTask);
            }
        });
        final Node n = g.newTypedNode(0, 0, ExecutableNode.NAME);
        n.set("x", Type.DOUBLE, 5.1d);
        n.set("y", Type.DOUBLE, 1.5d);
        n.traverseAt(0, new Callback<Double>() {
            public void on(Double i) {
                Assert.assertEquals("6.6", i + "");
            }
        });
        n.traverse("exec", new Callback<Double>() {
            public void on(Double i) {
                Assert.assertEquals("6.6", i + "");
            }
        });
        Task t = newTask().attribute("exec");
        t.executeWith(g, n, new Callback<TaskResult>() {
            @Override
            public void on(TaskResult result) {
                Assert.assertEquals("6.6", result.get(0) + "");
            }
        });
        Assert.assertEquals("{\"world\":0,\"time\":0,\"id\":1,\"x\":5.1,\"y\":1.5}", n.toString());
    }

}
