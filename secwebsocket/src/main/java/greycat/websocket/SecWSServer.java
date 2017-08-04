/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.auth.IdentityManager;
import greycat.websocket.handlers.GCAuthHandler;
import greycat.websocket.handlers.GCResetPasswordHandler;
import greycat.websocket.handlers.GCSecurityHandler;
import io.undertow.util.StatusCodes;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Gregory NAIN on 23/05/2017.
 */
public class SecWSServer extends WSServer {

    private IdentityManager identityManager;
    private static final String AUTH_PARAM_KEY = "gc-auth-key";

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public SecWSServer(GraphBuilder graphBuilder, int p_port, IdentityManager _idManager) {
        super(graphBuilder, p_port);
        this.identityManager = _idManager;
    }

    @Override
    public void start() {
        handlers.replaceAll((s, httpHandler) -> new GCSecurityHandler(httpHandler, this.identityManager));
        handlers.put("/auth", new GCAuthHandler(this.identityManager));
        handlers.put("/renewpasswd", new GCResetPasswordHandler(this.identityManager));

        executor.scheduleAtFixedRate(connectionsChecker, 1, 1, TimeUnit.MINUTES);

        super.start();
    }

    @Override
    public void stop() {
        executor.shutdownNow();
        super.stop();
    }

    private Runnable connectionsChecker = new Runnable() {
        @Override
        public void run() {
            peers.forEach(webSocketChannel -> {
                //TODO check connections
            });
        }
    };

    @Override
    public void onConnect(WebSocketHttpExchange webSocketHttpExchange, WebSocketChannel webSocketChannel) {
        List<String> tokens = webSocketHttpExchange.getRequestParameters().get(AUTH_PARAM_KEY);
        if (tokens != null && tokens.size() == 1) {
            webSocketChannel.setAttribute(AUTH_PARAM_KEY, tokens.get(0));

            final Graph graph = builder.build();
            graph.setProperty("ws.source", webSocketChannel.getSourceAddress().getAddress());
            graph.setProperty(AUTH_PARAM_KEY, tokens.get(0));
            graph.connect(result -> {
                webSocketChannel.getReceiveSetter().set(new SecuredPeerInternalListener(graph));
                webSocketChannel.resumeReceives();
                peers.add(webSocketChannel);
            });

        } else {
            throw new RuntimeException("Could not find auth token while starting Secured WebSocket connection. RequestParams: " + webSocketHttpExchange.getQueryString());
        }
    }

    protected class SecuredPeerInternalListener extends PeerInternalListener {
        SecuredPeerInternalListener(Graph p_graph) {
            super(p_graph);
        }

        @Override
        protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) throws IOException {
            identityManager.onChannelDisconnected((String)webSocketChannel.getAttribute(AUTH_PARAM_KEY));
            super.onClose(webSocketChannel, channel);
        }
    }

    @Override
    protected void process_rpc(Graph graph, byte[] input, WebSocketChannel channel) {
        if(input[0] == WSConstants.HEART_BEAT_PING || input[0] == WSConstants.HEART_BEAT_PONG) {
            super.process_rpc(graph, input, channel);
        } else {
            if(identityManager.onChannelActivity((String)channel.getAttribute(AUTH_PARAM_KEY))) {
                super.process_rpc(graph, input, channel);
            } else {
                channel.setCloseCode(StatusCodes.UNAUTHORIZED);
                channel.setCloseReason("Session expired.");
                try {
                    channel.sendClose();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
