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
package org.kevoree.modeling.addons.rest;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.junit.Assert;
import org.mwg.Callback;
import org.mwg.Graph;
import org.mwg.GraphBuilder;
import org.mwg.Node;
import org.mwg.internal.scheduler.NoopScheduler;

public class GetPostTest {

   // @Test
    public void test() throws Exception {
        final Graph graph = new GraphBuilder().withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean o) {
                for (int i = 0; i < 10; i++) {
                    Node nodeLoop = graph.newNode(0, i);
                    nodeLoop.set("name", "node" + i);
                    nodeLoop.set("load", i);
                    Node subProcessLoop = graph.newNode(0, i);
                    subProcessLoop.set("name", "process" + i);
                    subProcessLoop.set("load", i);
                    nodeLoop.add("processes", subProcessLoop);
                    graph.index("nodes", nodeLoop, "name", null);
                }
                RestGateway gateway = RestGateway.expose(graph, 8050);
                gateway.start();

                try {
                    HttpResponse<String> jsonResponse = Unirest.get("http://localhost:8050/fromIndexAll(nodes)?time=10")
                            .header("accept", "application/json")
                            .queryString("time", System.currentTimeMillis())
                            .asString();
                    Assert.assertEquals("[\n" +
                            "{\"world\":0,\"time\":10,\"id\":1,\"name\":\"node0\",\"load\":0,\"processes\":[2]},\n" +
                            "{\"world\":0,\"time\":10,\"id\":4,\"name\":\"node1\",\"load\":1,\"processes\":[5]},\n" +
                            "{\"world\":0,\"time\":10,\"id\":6,\"name\":\"node2\",\"load\":2,\"processes\":[7]},\n" +
                            "{\"world\":0,\"time\":10,\"id\":8,\"name\":\"node3\",\"load\":3,\"processes\":[9]},\n" +
                            "{\"world\":0,\"time\":10,\"id\":10,\"name\":\"node4\",\"load\":4,\"processes\":[11]},\n" +
                            "{\"world\":0,\"time\":10,\"id\":12,\"name\":\"node5\",\"load\":5,\"processes\":[13]},\n" +
                            "{\"world\":0,\"time\":10,\"id\":14,\"name\":\"node6\",\"load\":6,\"processes\":[15]},\n" +
                            "{\"world\":0,\"time\":10,\"id\":16,\"name\":\"node7\",\"load\":7,\"processes\":[17]},\n" +
                            "{\"world\":0,\"time\":10,\"id\":18,\"name\":\"node8\",\"load\":8,\"processes\":[19]},\n" +
                            "{\"world\":0,\"time\":10,\"id\":20,\"name\":\"node9\",\"load\":9,\"processes\":[21]}\n" +
                            "]\n", jsonResponse.getBody().toString());

                    jsonResponse = Unirest.get("http://localhost:8050/fromIndexAll(nodes)/traverse(processes)?time=10")
                            .header("accept", "application/json")
                            .queryString("time", System.currentTimeMillis())
                            .asString();
                    Assert.assertEquals("[\n" +
                            "{\"world\":0,\"time\":10,\"id\":17,\"name\":\"process7\",\"load\":7},\n" +
                            "{\"world\":0,\"time\":10,\"id\":2,\"name\":\"process0\",\"load\":0},\n" +
                            "{\"world\":0,\"time\":10,\"id\":19,\"name\":\"process8\",\"load\":8},\n" +
                            "{\"world\":0,\"time\":10,\"id\":5,\"name\":\"process1\",\"load\":1},\n" +
                            "{\"world\":0,\"time\":10,\"id\":21,\"name\":\"process9\",\"load\":9},\n" +
                            "{\"world\":0,\"time\":10,\"id\":7,\"name\":\"process2\",\"load\":2},\n" +
                            "{\"world\":0,\"time\":10,\"id\":9,\"name\":\"process3\",\"load\":3},\n" +
                            "{\"world\":0,\"time\":10,\"id\":11,\"name\":\"process4\",\"load\":4},\n" +
                            "{\"world\":0,\"time\":10,\"id\":13,\"name\":\"process5\",\"load\":5},\n" +
                            "{\"world\":0,\"time\":10,\"id\":15,\"name\":\"process6\",\"load\":6}\n" +
                            "]\n", jsonResponse.getBody().toString());

                    jsonResponse = Unirest.get("http://localhost:8050/fromIndexAll(nodes)/traverse(processes)/with(name,process9)?time=10&world=0")
                            .header("accept", "application/json")
                            .queryString("time", System.currentTimeMillis())
                            .asString();
                    Assert.assertEquals("[\n" +
                            "{\"world\":0,\"time\":10,\"id\":21,\"name\":\"process9\",\"load\":9}\n" +
                            "]\n", jsonResponse.getBody().toString());

                    //world 1 should be empty
                    jsonResponse = Unirest.get("http://localhost:8050/fromIndexAll(nodes)/traverse(processes)/with(name,process9)?time=10&world=1")
                            .header("accept", "application/json")
                            .queryString("time", System.currentTimeMillis())
                            .asString();
                    Assert.assertEquals("[\n" +
                            "\n" +
                            "]\n", jsonResponse.getBody().toString());


                    //Now test the post
                    Unirest.post("http://localhost:8050/fromIndexAll(nodes)/traverse(processes)/with(name,process9)?time=10&world=0")
                            .header("accept", "application/json")
                            .queryString("time", System.currentTimeMillis())
                            .body("load=42,meta=Hello").asBinary();
                    jsonResponse = Unirest.get("http://localhost:8050/fromIndexAll(nodes)/traverse(processes)/with(name,process9)?time=10&world=0")
                            .header("accept", "application/json")
                            .queryString("time", System.currentTimeMillis())
                            .asString();
                    Assert.assertEquals("[\n" +
                            "{\"world\":0,\"time\":10,\"id\":21,\"name\":\"process9\",\"load\":\"42\",\"meta\":\"Hello\"}\n" +
                            "]\n", jsonResponse.getBody().toString());

                } catch (Exception e) {
                    e.printStackTrace();
                }

                graph.disconnect(null);
                gateway.stop();

            }
        });
    }


}
