package greycat.websocket;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.plugin.TaskExecutor;
import greycatTest.internal.MockStorage;

import java.io.IOException;
import java.net.ServerSocket;

public class RemoteLogTest {

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
                graph2.log().activateRemote();
                graph2.connect(new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        graph2.log().info("Say Hello from {}", "GreyCat remote");


                        graph2.disconnect(new Callback<Boolean>() {
                            @Override
                            public void on(Boolean result) {
                                graph.disconnect(new Callback<Boolean>() {
                                    @Override
                                    public void on(Boolean result) {
                                        graphServer.stop();
                                    }
                                });
                            }
                        });

                    }
                });

            }
        });

    }

}
