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
import greycat.Action;
import greycat.TaskHook;
import org.junit.Assert;
import org.junit.Test;
import greycat.GraphBuilder;
import greycat.TaskContext;

import static greycat.Tasks.newTask;
import static greycat.internal.task.CoreActions.*;

public class HookTest {

    @Test
    public void test() {
        Graph g = GraphBuilder.newBuilder().build();
        //Graph g = GraphBuilder.newBuilder().withPlugin(new VerbosePlugin()).build();
        g.connect(result -> {

            int[] count = new int[1];
            count[0] = 0;

            newTask()
                    .addHook(new TaskHook() {
                        @Override
                        public void start(TaskContext initialContext) {
                            count[0]++;
                        }

                        @Override
                        public void beforeAction(Action action, TaskContext context) {
                            count[0]++;
                        }

                        @Override
                        public void afterAction(Action action, TaskContext context) {
                            count[0]++;
                        }

                        @Override
                        public void beforeTask(TaskContext parentContext, TaskContext context) {
                            count[0]++;
                        }

                        @Override
                        public void afterTask(TaskContext context) {
                            count[0]++;
                        }

                        @Override
                        public void end(TaskContext finalContext) {
                            count[0]++;
                        }
                    })
                    .then(inject(new int[]{1, 2, 3}))
                    .forEach(newTask().then(setAsVar("{{result}}")))
                    .execute(g, null);

            Assert.assertEquals(18, count[0]);

        });
    }

}
