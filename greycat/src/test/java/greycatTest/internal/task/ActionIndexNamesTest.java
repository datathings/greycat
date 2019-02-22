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
import greycat.ActionFunction;
import org.junit.Assert;
import org.junit.Test;
import greycat.TaskContext;
import greycat.TaskResult;

import static greycat.internal.task.CoreActions.indexNames;
import static greycat.Tasks.newTask;

public class ActionIndexNamesTest {

    @Test
    public void testIndexNames() {
        Graph graph = new GraphBuilder().build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                Node root1 = graph.newNode(0, Constants.BEGINNING_OF_TIME);
                root1.set("name", Type.INT, 1);

                String[] indexes = new String[]{"idx1", "idx2", "idx3"};
                for (int i = 0; i < indexes.length; i++) {
                    graph.declareIndex(0, indexes[i], indexNode -> {
                        indexNode.update(root1);
                    }, "name");
                }

                newTask()
                        .then(indexNames())
                        .thenDo(new ActionFunction() {
                            @Override
                            public void eval(TaskContext ctx) {
                                Assert.assertArrayEquals(indexes, ctx.result().asArray());
                            }
                        })
                        .execute(graph, new Callback<TaskResult>() {
                            @Override
                            public void on(TaskResult result) {
                                graph.disconnect(null);
                            }
                        });
            }
        });
    }
}
