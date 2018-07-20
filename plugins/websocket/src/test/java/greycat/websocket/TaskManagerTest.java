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
package greycat.websocket;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.plugin.TaskExecutor;
import greycatTest.internal.MockStorage;

import java.io.IOException;
import java.net.ServerSocket;

public class TaskManagerTest {

    public static void main(String[] args) {
        Graph graph = GraphBuilder
                .newBuilder()
                .withStorage(new MockStorage())
                .build();

        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                int port = 8050;
                try {
                    ServerSocket servSock = new ServerSocket(0);
                    port = servSock.getLocalPort();
                    servSock.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                WSSharedServer graphServer = new WSSharedServer(graph, port);
                graphServer.start();

                final Graph graph2 = new GraphBuilder().withMemorySize(10000).withStorage(new WSClient("ws://localhost:" + port + "/ws")).build();
                graph2.connect(new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {

                        TaskExecutor exec = (TaskExecutor) graph2.storage();
                        exec.taskStats(new Callback<String>() {
                            @Override
                            public void on(String result) {
                                System.out.println("stats="+result);
                            }
                        });

                    }
                });

            }
        });

    }

}
