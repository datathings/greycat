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
package greycat;

import greycat.taskide.TaskIDE;
import greycat.websocket.WSServer;

public class TaskServerTest {

    public static void main(String[] args) {
        Graph graph = GraphBuilder
                .newBuilder()
                .build();
        graph.connect(result -> {

            //create graph nodes
            Node n0 = graph.newNode(0, Constants.BEGINNING_OF_TIME);
            n0.set("name", Type.STRING, "n0");
            n0.set("value", Type.INT, 8);

            Node n1 = graph.newNode(0, Constants.BEGINNING_OF_TIME);
            n1.set("name", Type.STRING, "n1");
            n1.set("value", Type.INT, 3);

            Node root = graph.newNode(0, Constants.BEGINNING_OF_TIME);
            root.set("name", Type.STRING, "root");
            root.addToRelation("children", n0);
            root.addToRelation("children", n1);

            //create some index
            graph.index(0, Constants.BEGINNING_OF_TIME, "roots", new Callback<NodeIndex>() {
                @Override
                public void on(NodeIndex rootsIndex) {
                    rootsIndex.addToIndex(root, "name");
                    graph.index(0, Constants.BEGINNING_OF_TIME, "nodes", new Callback<NodeIndex>() {
                        @Override
                        public void on(NodeIndex nodesIndex) {
                            nodesIndex.addToIndex(n0, "name");
                            nodesIndex.addToIndex(n1, "name");
                            nodesIndex.addToIndex(root, "name");
                            WSServer server = new WSServer(graph, 4000);
                            TaskIDE.attach(server, graph);
                            server.start();
                            System.out.println("Server started http://localhost:4000/taskide");
                        }
                    });

                }
            });

        });
    }

}
