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

import greycat.ActionFunction;
import org.junit.Assert;
import org.junit.Test;
import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.Type;
import greycat.base.BaseNode;
import greycat.TaskContext;
import greycat.TaskResult;

import static greycat.Tasks.newTask;

public class ActionLocalIndexOrUnindexTest {

    @Test
    public void testLocalIndex() {
        Graph graph = new GraphBuilder().build();

        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean succeed) {
                newTask()
                        .declareIndex("rootIdx", "name")
                        .createNode()
                        .setAttribute("name", Type.STRING, "child1")
                        .addToVar("child")
                        .createNode()
                        .setAttribute("name", Type.STRING, "child2")
                        .addToVar("child")
                        .createNode()
                        .setAttribute("name", Type.STRING, "child3")
                        .addToVar("child")
                        .createNode()
                        .setAttribute("name", Type.STRING, "root")
                        .updateIndex("rootIdx")
                        .declareLocalIndex("idxRelation", "name")
                        .addVarTo("idxRelation", "child")
                        .readIndex("rootIdx")
                        .traverse("idxRelation")
                        .thenDo(new ActionFunction() {
                            @Override
                            public void eval(TaskContext ctx) {
                                TaskResult result = ctx.result();
                                Assert.assertEquals(3, result.size());

                                Assert.assertEquals("child1", ((BaseNode) result.get(0)).get("name"));
                                Assert.assertEquals("child2", ((BaseNode) result.get(1)).get("name"));
                                Assert.assertEquals("child3", ((BaseNode) result.get(2)).get("name"));
                            }
                        })
                        .readIndex("rootIdx")
                        .removeVarFrom("idxRelation", "child")
                        .readIndex("rootIdx")
                        .traverse("idxRelation")
                        .thenDo(new ActionFunction() {
                            @Override
                            public void eval(TaskContext ctx) {
                                TaskResult result = ctx.result();
                                Assert.assertEquals(0, result.size());
                            }
                        })
                        .execute(graph, null);


            }
        });
    }
}
