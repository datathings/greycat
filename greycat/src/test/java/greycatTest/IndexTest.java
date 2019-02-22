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
package greycatTest;

import greycat.*;
import greycat.scheduler.NoopScheduler;
import org.junit.Assert;
import org.junit.Test;

import static greycat.Tasks.newTask;

public class IndexTest {

    @Test
    public void test() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                Task t = newTask()
                        .declareIndex("nodes", "name")
                        .createNode().setAttribute("name", Type.STRING, "sensor_1").updateIndex("nodes")
                        .createNode().setAttribute("name", Type.STRING, "sensor_2").updateIndex("nodes")
                        .readIndex("nodes")
                        .readIndex("nodes", "sensor_1");
                t.execute(g, new Callback<TaskResult>() {
                    @Override
                    public void on(TaskResult result) {
                        Assert.assertEquals("{\"result\":[{\"world\":0,\"time\":-9007199254740990,\"id\":2,\"group\":0,\"name\":\"sensor_1\"}]}", result.toString());

                        newTask().readIndex("nodes", "sensor_1").unindexFrom("nodes").readIndex("nodes").execute(g, result2 -> {
                            Assert.assertEquals("{\"result\":[{\"world\":0,\"time\":-9007199254740990,\"id\":3,\"group\":0,\"name\":\"sensor_2\"}]}", result2.toString());
                        });
                    }
                });
            }
        });
    }


    @Test
    public void testLocalIndex() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                Task t = newTask()
                        .createNode()
                        .setAsVar("container")
                        .declareLocalIndex("files", "name")
                        .createNode()
                        .setAttribute("name", Type.STRING, "myFile")
                        .setAsVar("file")
                        .readVar("container")
                        .addVarTo("files", "file")
                        .traverse("files")
                        //.log("Files: {{result}}")
                        .readVar("container")
                        .traverse("files", "myFile");
                        //.log("MyFile: {{result}}");
                t.execute(g, new Callback<TaskResult>() {
                    @Override
                    public void on(TaskResult result) {
                        //System.out.println("res:" + result.get(0));
                        Assert.assertTrue(result.get(0) != null);
                    }
                });
            }
        });
    }

}


