package greycat.multithread.websocket;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import static io.undertow.util.Methods.OPTIONS;

public abstract class GraphHttpHandler implements HttpHandler {

    private final GraphBuilder builder;
    private final WSServer server;

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