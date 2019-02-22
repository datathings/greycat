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

public class ActionTraverseTimelineTest extends AbstractActionTest {

    public ActionTraverseTimelineTest() {
        super();
        initGraph();
    }

    @Test
    public void testCreateNode() {
        Task t = newTask();
        t.createNode();
        t.loop("0", "20",
                newTask()
                        .travelInTime("{{i}}")
                        .setAttribute("time", Type.LONG, "{{=i+1000}}")
        );
        t.traverseTimeline(Constants.BEGINNING_OF_TIME_STR, Constants.END_OF_TIME_STR, "5");
        t.execute(graph, new Callback<TaskResult>() {
            @Override
            public void on(TaskResult result) {
                Assert.assertEquals(1,result.size());
                Assert.assertEquals(5,((TaskResult)result.get(0)).size());
                result.free();
            }
        });
    }

}
