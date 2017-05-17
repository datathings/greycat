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
package greycatTest.internal.task;

import greycat.*;
import greycat.base.BaseTaskResult;
import greycat.internal.heap.HeapBuffer;
import greycat.internal.task.CoreTask;
import greycat.struct.Buffer;
import org.junit.Assert;
import org.junit.Test;

public class BaseTaskResultTest {


    @Test
    public void loadSaveTest() {

        Graph graph = GraphBuilder.newBuilder().build();
        graph.connect((connected) -> {

            BaseTaskResult<Node> result = new BaseTaskResult<Node>(null, false);
            result.add((Node) graph.newNode(0, 0).set("name", Type.STRING, "node").set("value", Type.DOUBLE, 4.8));

            CoreTask task = new CoreTask();
            TaskContext ctx = task.prepare(graph, result, null);

            Buffer buffer = graph.newBuffer();
            ctx.saveToBuffer(buffer);

            TaskContext ctx2 = task.prepare(graph, null, null);
            ctx2.loadFromBuffer(buffer, new Callback<Boolean>() {
                @Override
                public void on(Boolean loadResult) {
                    TaskResult<Node> resultBack = ctx2.resultAsNodes();
                    Assert.assertEquals(result.size(), resultBack.size());
                    Assert.assertEquals(result.get(0).id(), resultBack.get(0).id());
                }
            });

        });
    }

    @Test
    public void sizeTest() {
        BaseTaskResult res = new BaseTaskResult(null, false);
        res.set(1, "");
        res.set(0, "");
        Assert.assertEquals(2, res.size());
    }

}
