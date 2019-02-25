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
package greycat.multithread.websocket;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import static io.undertow.util.Methods.OPTIONS;

/**
 *
 */
public abstract class GraphHttpHandler implements HttpHandler {

    private final GraphBuilder builder;
    private final WSServer server;

    /**
     *
     * @param builder
     * @param server
     */
    public GraphHttpHandler(GraphBuilder builder, WSServer server) {
        this.builder = builder;
        this.server = server;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.getRequestMethod() == OPTIONS) {
            optionFunction(exchange);
        } else {
            exchange.dispatch(new Runnable() {
                @Override
                public void run() {
                    GraphBuilder graphBuilder = server.toBufferGraphBuilder(builder);
                    Graph graph = graphBuilder.build();
                    graph.connect(on -> {
                        runTask(exchange, graph, new Callback<Boolean>() {
                            @Override
                            public void on(Boolean result) {
                                graph.disconnect(null);
                            }
                        });
                    });
                }
            });
        }
    }

    public abstract void optionFunction(HttpServerExchange httpServerExchange);

    public abstract void runTask(HttpServerExchange httpServerExchange, Graph graph, Callback<Boolean> done);
}