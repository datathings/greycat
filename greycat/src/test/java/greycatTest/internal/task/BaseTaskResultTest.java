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
import greycat.utility.L3GMap;
import greycat.utility.Tuple;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

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

    @Test
    public void doubleArrays() {

        Graph graph = GraphBuilder.newBuilder().build();
        graph.connect((connected) -> {

            BaseTaskResult res = new BaseTaskResult(null, false);

            Buffer buffer = new HeapBuffer();

            res.add(new double[]{0d, 1d, 2d, 3d});
            res.add(new double[]{4d, 5d, 6d, 7d});
            res.add(new double[]{8d, 9d, 10d, 11d});

            res.saveToBuffer(buffer);

            L3GMap<List<Tuple<Object[], Integer>>> collector = new L3GMap<List<Tuple<Object[], Integer>>>(true);
            BaseTaskResult loaded = new BaseTaskResult(null, false);
            loaded.load(buffer, 0, graph, collector);

            Assert.assertEquals(res.size(), loaded.size());

            Assert.assertNotNull(loaded.get(0));
            Assert.assertNotNull(loaded.get(1));
            Assert.assertNotNull(loaded.get(2));

            Assert.assertEquals(((double[])res.get(0)).length, ((double[])loaded.get(0)).length);
            Assert.assertEquals(((double[])res.get(1)).length, ((double[])loaded.get(1)).length);
            Assert.assertEquals(((double[])res.get(2)).length, ((double[])loaded.get(2)).length);

            Assert.assertEquals(((double[])res.get(1))[2], ((double[])loaded.get(1))[2], 0);
        });

    }

    @Test
    public void nestedTaskResults() {

        Graph graph = GraphBuilder.newBuilder().build();
        graph.connect((connected) -> {

            BaseTaskResult res = new BaseTaskResult(null, false);

            Buffer buffer = new HeapBuffer();

            BaseTaskResult resA = new BaseTaskResult(null, false);
            resA.add("1");

            BaseTaskResult resB = new BaseTaskResult(null, false);
            resB.add(2);

            BaseTaskResult resC = new BaseTaskResult(null, false);
            resC.add(3d);

            res.add(resA);
            res.add(resB);
            res.add(resC);

            res.saveToBuffer(buffer);

            L3GMap<List<Tuple<Object[], Integer>>> collector = new L3GMap<List<Tuple<Object[], Integer>>>(true);
            BaseTaskResult loaded = new BaseTaskResult(null, false);
            loaded.load(buffer, 0, graph, collector);

            Assert.assertEquals(res.size(), loaded.size());

            Assert.assertNotNull(loaded.get(0));
            Assert.assertNotNull(loaded.get(1));
            Assert.assertNotNull(loaded.get(2));

            Assert.assertEquals(((TaskResult)res.get(0)).size(), ((TaskResult)loaded.get(0)).size());
            Assert.assertEquals(((TaskResult)res.get(1)).size(), ((TaskResult)loaded.get(1)).size());
            Assert.assertEquals(((TaskResult)res.get(2)).size(), ((TaskResult)loaded.get(2)).size());

        });

    }

}
