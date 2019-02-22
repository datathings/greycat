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
import greycat.internal.task.CoreActions;
import org.junit.Assert;
import org.junit.Test;
import greycat.scheduler.NoopScheduler;

import static greycat.Tasks.newTask;
import static greycat.internal.task.CoreActions.*;

public class FlagTest {
    @Test
    public void traverse() {
        Graph graph = new GraphBuilder()
                .withMemorySize(30000)
                .withScheduler(new NoopScheduler())
                .build();

        graph.connect(result -> {

            String relName = "children";

            Node n1 = graph.newNode(0, 13);
            graph.save(null);
            long initcache = graph.space().available();


            Node n2 = graph.newNode(0, 13);
            Node n3 = graph.newNode(0, 13);
            Node n4 = graph.newNode(0, 13);

            n1.addToRelation(relName, n2);
            n1.addToRelation(relName, n3);
            n1.addToRelation(relName, n4);


            Node n5 = graph.newNode(0, 13);
            Node n6 = graph.newNode(0, 13);
            n2.addToRelation(relName, n5);
            n2.addToRelation(relName, n6);

            Node n7 = graph.newNode(0, 13);
            Node n8 = graph.newNode(0, 13);
            n3.addToRelation(relName, n7);
            n3.addToRelation(relName, n8);

            Node n9 = graph.newNode(0, 13);
            Node n10 = graph.newNode(0, 13);
            n4.addToRelation(relName, n9);
            n4.addToRelation(relName, n10);

            n2.free();
            n3.free();
            n4.free();
            n5.free();
            n6.free();
            n7.free();
            n8.free();
            n9.free();
            n10.free();

            graph.save(null);
            Assert.assertTrue(graph.space().available() == initcache);

            Task traverse = newTask();
            traverse.then(defineAsGlobalVar("parent")).then(CoreActions.traverse(relName)).thenDo(new ActionFunction() {
                @Override
                public void eval(TaskContext ctx) {

                    TaskResult<Node> children = ctx.resultAsNodes();
                    if (children.size() != 0) {
                        ctx.continueWith(ctx.wrap(graph.cloneNode(children.get(0))));
                    } else {
                        ctx.continueWith(null);
                    }
                }
            }).ifThen(new ConditionalFunction() {
                @Override
                public boolean eval(TaskContext ctx) {
                    return (ctx.result() != null);
                }
            }, traverse);


            Task mainTask = newTask().then(travelInTime("13")).then(travelInWorld("0")).then(inject(n1)).pipe(traverse);
            mainTask.execute(graph, new Callback<TaskResult>() {
                @Override
                public void on(TaskResult result) {
                    graph.save(null);
                    Assert.assertTrue(graph.space().available() == initcache);
                    if (result != null) {
                        result.free();
                    }
                }

            });

            graph.save(null);
            Assert.assertTrue(graph.space().available() == initcache);
        });
    }


    /*
    @Test
    public void traverseOrKeep() {
        Graph graph = new GraphBuilder()
                .withMemorySize(30000)
                .withScheduler(new NoopScheduler())
                .build();

        graph.connect(result -> {

            String relName = "children";

            Node n1 = graph.newNode(0, 13);
            graph.save(null);
            long initcache = graph.space().available();


            Node n2 = graph.newNode(0, 13);
            Node n3 = graph.newNode(0, 13);
            Node n4 = graph.newNode(0, 13);

            n1.addToRelation(relName, n2);
            n1.addToRelation(relName, n3);
            n1.addToRelation(relName, n4);


            Node n5 = graph.newNode(0, 13);
            Node n6 = graph.newNode(0, 13);
            n2.addToRelation(relName, n5);
            n2.addToRelation(relName, n6);

            Node n7 = graph.newNode(0, 13);
            Node n8 = graph.newNode(0, 13);
            n3.addToRelation(relName, n7);
            n3.addToRelation(relName, n8);

            Node n9 = graph.newNode(0, 13);
            Node n10 = graph.newNode(0, 13);
            n4.addToRelation(relName, n9);
            n4.addToRelation(relName, n10);

            n2.free();
            n3.free();
            n4.free();
            n5.free();
            n6.free();
            n7.free();
            n8.free();
            n9.free();
            n10.free();

            graph.save(null);
            Assert.assertTrue(graph.space().available() == initcache);


            Task traverse = task();

            traverse
                    .then(defineAsGlobalVar("parent"))
                    .then(new ActionTraverseOrKeep(relName))
                    .thenDo(new ActionFunction() {
                        @Override
                        public void eval(TaskContext context) {
                            TaskResult<Integer> count = context.variable("count");
                            int c = 0;
                            if (count != null) {
                                c = count.get(0) + 1;
                            }
                            context.setGlobalVariable("count", context.wrap(c));

                            TaskResult<Node> children = context.resultAsNodes();
                            if (children != null && children.size() != 0) {
                                context.continueWith(context.wrapClone(children.get(0)));
                            } else {
                                context.continueWith(null);
                            }
                        }
                    }).ifThen(new ConditionalFunction() {
                @Override
                public boolean eval(TaskContext context) {
                    int x = (int) context.variable("count").get(0);
                    return (x != 3);
                }
            }, traverse);

            Task mainTask = task().then(setTime("13")).then(setWorld("0")).then(inject(n1)).map(traverse);
            mainTask.execute(graph, new Callback<TaskResult>() {
                @Override
                public void on(TaskResult result) {
                    if (result != null) {
                        result.free();
                    }
                    graph.save(null);
                    Assert.assertEquals(graph.space().available(), initcache);
                }

            });
            graph.save(null);
            Assert.assertTrue(graph.space().available() == initcache);
        });
    }*/

}
