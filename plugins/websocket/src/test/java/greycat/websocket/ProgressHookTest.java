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
package greycat.websocket;

import greycat.*;
import greycatTest.internal.MockStorage;

public class ProgressHookTest {

    public static void main(String[] args) {
        Graph graph = GraphBuilder
                .newBuilder()
                .withStorage(new MockStorage())
                .build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {


                WSSharedServer srv = new WSSharedServer(graph, 4000);
                srv.start();
                System.out.println("Server started 4000");

                WSClient client = new WSClient("ws://localhost:4000/ws");
                Graph emptyGraph = GraphBuilder
                        .newBuilder()
                        .withStorage(client)
                        .build();
                emptyGraph.connect(result1 -> {

                    Task t = Tasks.newTask()
                            .createNode()
                            .setAttribute("name", Type.STRING, "toto")
                            .addToVar("nodes")
                            .createNode()
                            .setAttribute("name", Type.STRING, "tata")
                            .addToVar("nodes")
                            .readVar("nodes")
                            .forEach(
                                    Tasks.newTask()
                                            .travelInTime("2")
                                    .attribute("name")
                                    .println(("{{result}}"))
                            );

                    TaskContext context = t.prepare(emptyGraph, null, result2 -> {
                        if(result2.exception()!=null) {
                            result2.exception().printStackTrace();
                        }
                        if(result2.output() != null) {
                            System.out.println(result2.output());
                        }
                        emptyGraph.disconnect(cb->{
                            srv.stop();
                            graph.disconnect(null);
                        });
                    });
                    context.setProgressHook(progress -> {
                        System.out.println("Progress:" + progress);
                    });
                    t.executeRemotelyUsing(context);

                });
            }
        });
    }
}
