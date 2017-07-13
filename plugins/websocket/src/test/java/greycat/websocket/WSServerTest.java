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

import greycat.*;
import greycat.struct.Buffer;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import org.junit.Assert;
import org.junit.Test;
import greycat.chunk.StateChunk;
import greycat.base.BaseNode;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;

public class WSServerTest {

    public static void main(String[] args) {
        final Graph graph = new GraphBuilder()
                .withMemorySize(10000)
                .build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean connectResult) {
                WSServer graphServer = new WSServer(graph, 8050);
                graphServer.addHandler("hello", new ResourceHandler(new ClassPathResourceManager(this.getClass().getClassLoader(), "hello")).addWelcomeFiles("index.html").setDirectoryListingEnabled(true));
                graphServer.start();
                System.out.println("Connected!");


                Node root = graph.newNode(0, 0);
                root.set("name", Type.STRING, "root");

                Node n0 = graph.newNode(0, 0);
                n0.set("name", Type.STRING, "n0");

                Node n1 = graph.newNode(0, 0);
                n1.set("name", Type.STRING, "n0");

                root.addToRelation("children", n0);
                root.addToRelation("children", n1);

                graph.declareIndex(0, "nodes", new Callback<NodeIndex>() {
                    @Override
                    public void on(NodeIndex indexNode) {
                        indexNode.update(root);
                        System.out.println(indexNode.toString());
                        StateChunk chunk = (StateChunk) graph.space().get(((BaseNode) indexNode)._index_stateChunk);
                        Buffer buffer = graph.newBuffer();
                        chunk.save(buffer);

                        System.out.println(new String(buffer.data()));
                        System.out.println(chunk.index());
                    }
                }, "name");
            }
        });
    }

    @Test
    public void test() {

        final Graph graph = new GraphBuilder()
                .withMemorySize(10000)
                .build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                Node node = graph.newNode(0, 0);
                node.set("name", Type.STRING, "hello");

                graph.declareIndex(0, "nodes", new Callback<NodeIndex>() {

                    @Override
                    public void on(NodeIndex indexNode) {
                        indexNode.update(node);


                    }
                }, "name");

                //   graph.index("nodes", node, "name", null);

                Assert.assertEquals("{\"world\":0,\"time\":0,\"id\":1,\"name\":\"hello\"}", node.toString());

                int port = 8050;
                try {
                    ServerSocket servSock = new ServerSocket(0);
                    port = servSock.getLocalPort();
                    servSock.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                WSServer graphServer = new WSServer(graph, port);
                graphServer.start();
                final CountDownLatch latch = new CountDownLatch(1);
                final Graph graph2 = new GraphBuilder().withMemorySize(10000).withStorage(new WSClient("ws://localhost:" + port + "/ws")).build();
                graph2.connect(new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result1) {
                        graph2.index(0, 0, "nodes", new Callback<NodeIndex>() {
                            @Override
                            public void on(NodeIndex indexNodes) {
                                indexNodes.findFrom(new Callback<Node[]>() {
                                    @Override
                                    public void on(Node[] result1) {
                                        Assert.assertEquals(result1[0].toString(), node.toString());

                                        Node newNode = graph2.newNode(0, 0);
                                        newNode.set("name", Type.STRING, "hello2");

                                        Assert.assertEquals("{\"world\":0,\"time\":0,\"id\":137438953473,\"name\":\"hello2\"}", newNode.toString());

                                        graph2.declareIndex(0, "nodes", new Callback<NodeIndex>() {
                                            @Override
                                            public void on(NodeIndex graph2Nodes) {
                                                graph2Nodes.update(newNode);
                                                graph2Nodes.find(new Callback<Node[]>() {
                                                    @Override
                                                    public void on(Node[] result) {
                                                        Assert.assertEquals(2, result.length);
                                                    }
                                                }, graph2Nodes.world(), graph2Nodes.time());
                                            }
                                        }, "name");
                                        graph2.save(new Callback<Boolean>() {
                                            @Override
                                            public void on(Boolean result) {
                                                //ok now try to access new node from graph

                                                graph.index(0, 0, "nodes", new Callback<NodeIndex>() {
                                                    @Override
                                                    public void on(NodeIndex grapIndex) {
                                                        grapIndex.find(new Callback<Node[]>() {
                                                            @Override
                                                            public void on(Node[] result) {
                                                                Assert.assertEquals(2, result.length);
                                                                Assert.assertEquals(result[0].toString(), "{\"world\":0,\"time\":0,\"id\":1,\"name\":\"hello\"}");
                                                                Assert.assertEquals(result[1].toString(), "{\"world\":0,\"time\":0,\"id\":137438953473,\"name\":\"hello2\"}");
                                                                latch.countDown();
                                                            }
                                                        }, grapIndex.world(), grapIndex.time());
                                                    }
                                                });

                                            }
                                        });
                                    }
                                });
                            }
                        });

                    }
                });

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });


    }

}
