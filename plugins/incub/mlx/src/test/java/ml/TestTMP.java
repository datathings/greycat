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
package ml;

import org.mwg.Callback;
import org.mwg.Graph;
import org.mwg.GraphBuilder;
import org.mwg.Node;
import org.mwg.internal.scheduler.NoopScheduler;
import org.mwg.internal.task.CoreTask;

import static org.mwg.task.Actions.inject;
import static org.mwg.task.Actions.then;

public class TestTMP {

    public static void main(String[] args) {
        final Graph graph = new GraphBuilder().withScheduler(new NoopScheduler()).build();

        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                Node root = graph.newNode(0, 0);
                root.set("name", "root");
                root.set("enter", true);

                final long id = root.id();

                Node n1 = graph.newNode(0, 0);
                n1.set("name", "n1");
                n1.set("enter", true);

                Node n2 = graph.newNode(0, 0);
                n2.set("name", "n2");
                n2.set("enter", false);


                Node n3 = graph.newNode(0, 0);
                n3.set("name", "n3");
                n3.set("enter", false);


                root.add("fils", n1);
                root.add("fils", n2);
                root.add("fils", n3);

                Node n4 = graph.newNode(0, 0);
                n4.set("name", "n4");
                n4.set("enter", true);


                n1.add("fils", n4);

                Task creationTask = then(new Action() {
                    @Override
                    public void eval(TaskContext context) {
                        Node node = (Node) context.variable("starterNode");
                        System.out.println("Creation: " + node);
                    }
                });


                final int[] recursionNb = new int[]{0};
                Task traverse = new CoreTask();
                traverse.then(new Action() {
                    @Override
                    public void eval(TaskContext context) {
                        recursionNb[0]++;
                    }
                }).fromVar("starterNode").traverse("fils").select(new TaskFunctionSelect() {
                    @Override
                    public boolean select(Node node,TaskContext context) {
                        return (Boolean) node.get("enter");
                    }
                }).asGlobalVar("childNode")
                        .ifThen(new TaskFunctionConditional() {
                            @Override
                            public boolean eval(TaskContext context) {
                                TaskResult<Node> result = context.variable("childNode");

                                if (result.size() > 0) {
                                    context.setGlobalVariable("starterNode", context.wrap(result.get(0)));
                                    Node starter = (Node) context.variable("starterNode");
                                    System.out.println(recursionNb[0] + " 1er ifThen " + starter + " -> " + result);

                                }
                                return result.size() > 0;
                            }
                        }, traverse);
                /*.ifThen(new TaskFunctionConditional() {
                    @Override
                    public boolean eval(TaskContext context) {


                        Node[] result = (Node[]) context.variable("childNode");

                        if(result.length == 0) {
                            Node starter = (Node) context.variable("starterNode");
                            System.out.println(recursionNb[0] + " 2nd ifThen " + starter);
                        }
                        return result.length == 0;
                    }
                },creationTask);*/

                Task mainTask = inject(root).asGlobalVar("starterNode").subTask(traverse).subTask(creationTask);

                mainTask.execute(graph, null);


            }
        });
    }
}
