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
import greycat.Task;
import greycat.internal.task.CoreActions;
import org.junit.Assert;
import org.junit.Test;
import greycat.TaskResult;

import static greycat.internal.task.CoreActions.readVar;
import static greycat.internal.task.CoreActions.setAsVar;
import static greycat.Tasks.emptyResult;
import static greycat.Tasks.newTask;

public class DFSTest {

    private void baseGrap(Callback<Node> callback) {
        Graph graph = new GraphBuilder()
                .withMemorySize(30000)
                .build();

        graph.connect(result -> {
            Node n1 = graph.newNode(0, 0);
            n1.set("name", Type.STRING, "n1");

            graph.save(null);
            long initcache = graph.space().available();

            Node n2 = graph.newNode(0, 0);
            n2.set("name", Type.STRING, "n2");

            Node n3 = graph.newNode(0, 0);
            n3.set("name", Type.STRING, "n3");

            n1.addToRelation("left", n2);
            n1.addToRelation("right", n3);

            Node n4 = graph.newNode(0, 0);
            n4.set("name", Type.STRING, "n4");
            n2.addToRelation("left", n4);


            Node n5 = graph.newNode(0, 0);
            n5.set("name", Type.STRING, "n5");
            n3.addToRelation("left", n5);

            Node n6 = graph.newNode(0, 0);
            n6.set("name", Type.STRING, "n6");
            n3.addToRelation("right", n6);


            Node n7 = graph.newNode(0, 0);
            n7.set("name", Type.STRING, "n7");
            n6.addToRelation("left", n7);

            Node n8 = graph.newNode(0, 0);
            n8.set("name", Type.STRING, "n8");
            n6.addToRelation("right", n8);

            n2.free();
            n3.free();
            n4.free();
            n5.free();
            n6.free();
            n7.free();
            n8.free();
            graph.save(null);
            Assert.assertTrue(graph.space().available() == initcache);

            callback.on(n1);
        });
    }


    @Test
    public void traverse() {
        baseGrap(n1 -> {

            if (n1 != null) {
                //DO BFS from n1
                Task dfs = newTask();
                dfs.forEach(
                        newTask()
                                .then(setAsVar("parent"))
                                .then(CoreActions.traverse("left"))
                                .then(setAsVar("left"))
                                .then(readVar("parent"))
                                .then(CoreActions.traverse("right"))
                                .then(setAsVar("right"))
                                .thenDo(context -> {
                                    Node left = null;
                                    if (context.variable("left").size() > 0) {
                                        left = (Node) context.variable("left").get(0);
                                    }
                                    Node right = null;
                                    if (context.variable("right").size() > 0) {
                                        right = (Node) context.variable("right").get(0);
                                    }
                                    TaskResult<Node> nextStep = context.newResult();
                                    if (left != null && right != null) {
                                        if (left.id() < right.id()) {
                                            nextStep.add(left.graph().cloneNode(left));
                                            nextStep.add(right.graph().cloneNode(right));
                                        } else {
                                            nextStep.add(left.graph().cloneNode(left));
                                            nextStep.add(right.graph().cloneNode(right));
                                        }
                                    } else if (left != null) {
                                        nextStep.add(left.graph().cloneNode(left));
                                    }
                                    if (left != null) {
                                        context.addToGlobalVariable("nnl", context.wrap(left.id()));
                                        context.addToGlobalVariable("nnld", context.wrap(left.id() / 2));
                                    }
                                    context.continueWith(nextStep);
                                }).ifThen(context -> (context.result().size() > 0), dfs).thenDo(context -> context.continueTask())).then(readVar("nnl"));

                TaskResult initialResult = emptyResult();
                initialResult.add(n1);

                dfs/*.hook(VerboseHook.instance())/*/ /*.hook(VerboseHook.instance())/*.hook(new TaskHook() {
                    @Override
                    public void on(Action previous, Action next, TaskContext context) {
                        System.out.println(next);
                    }
                })*/.executeWith(n1.graph(), initialResult, new Callback<TaskResult>() {
                    @Override
                    public void on(TaskResult result) {
                        Assert.assertEquals(result.toString(), "{\"result\":[\"2\",\"4\",\"5\",\"7\"]}");
                    }
                });

            }

        });
    }


}
