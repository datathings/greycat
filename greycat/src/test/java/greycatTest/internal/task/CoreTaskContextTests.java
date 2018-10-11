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

import greycat.Graph;
import greycat.ActionFunction;
import org.junit.Assert;
import org.junit.Test;
import greycat.Callback;
import greycat.GraphBuilder;
import greycat.TaskContext;

import static greycat.Tasks.newTask;
import static greycat.internal.task.CoreActions.*;

public class CoreTaskContextTests {

    @Test
    public void testArrayInTemplate() {
        Graph graph = new GraphBuilder().build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                newTask()
                        .then(inject(4))
                        .then(defineAsGlobalVar("i"))
                        .then(inject(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}))
                        .then(defineAsGlobalVar("array"))
                        .then(readVar("array"))
                        .thenDo(new ActionFunction() {
                            @Override
                            public void eval(TaskContext ctx) {
                                Assert.assertEquals("5", ctx.template("{{array[4]}}"));
                                Assert.assertEquals("9", ctx.template("{{result[8]}}"));
                                Assert.assertEquals("[1,2,3,4,5,6,7,8,9]", ctx.template("{{result}}"));
                                Assert.assertEquals("[1,2,3,4,5,6,7,8,9]", ctx.template("{{array}}"));

                                /*
                                boolean exceptionCaught = false;
                                try {
                                    context.template("{{result[]}}");
                                } catch (RuntimeException e) {
                                    exceptionCaught = true;
                                }
                                Assert.assertTrue(exceptionCaught);

                                exceptionCaught = false;
                                try {
                                    System.out.println(context.template("{{result[9]}}"));;
                                } catch (RuntimeException e) {
                                    exceptionCaught = true;
                                }
                                Assert.assertTrue(exceptionCaught);
*/
                                Assert.assertEquals("9.1", ctx.template("{{=((1 + 2) + (array[6] - 4) * 2) + 0.1 }}"));
                            }
                        })
                        .execute(graph, null);
            }
        });
    }
}
