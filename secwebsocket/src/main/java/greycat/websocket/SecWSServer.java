/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket;

import greycat.*;
import greycat.websocket.handlers.GCAuthHandler;
import greycat.websocket.handlers.GCResetPasswordHandler;
import greycat.websocket.handlers.GCSecurityHandler;
import io.undertow.util.StatusCodes;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.xnio.ChannelListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Gregory NAIN on 23/05/2017.
 */
public class SecWSServer extends WSServer {

    private AccessControlManager _acm;
    private static final String AUTH_PARAM_KEY = "gc-auth-key";

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public SecWSServer(GraphBuilder graphBuilder, int p_port, AccessControlManager acm) {
        super(graphBuilder, p_port);
        this._acm = acm;
    }

    @Override
    public void start() {
        handlers.replaceAll((s, httpHandler) -> new GCSecurityHandler(httpHandler, this._acm));
        handlers.put("/auth", new GCAuthHandler(this._acm));
        handlers.put("/renewpasswd", new GCResetPasswordHandler(this._acm));

        executor.scheduleAtFixedRate(connectionsChecker, 20, 20, TimeUnit.SECONDS);

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
            System.out.println("Sessions check");
            SessionManager sessionManager = _acm.getSessionsManager();
            ArrayList<WebSocketChannel> tmpPeers = new ArrayList<>(peers);
            tmpPeers.forEach(webSocketChannel -> {
                PeerInternalListener listener = (PeerInternalListener) ((ChannelListener.SimpleSetter) webSocketChannel.getReceiveSetter()).get();
                String sessionKey = (String) listener.graph().getProperty(AUTH_PARAM_KEY);
                Session session = sessionManager.getSession(sessionKey);
                if (session.isExpired()) {
                    System.out.println("Session " + session.sessionId() + " has expired. Closing WS.");
                    peers.remove(webSocketChannel);
                    try {
                        webSocketChannel.setCloseCode(StatusCodes.UNAUTHORIZED);
                        webSocketChannel.setCloseReason("Session expired.");
                        webSocketChannel.sendClose();
                        webSocketChannel.addCloseTask(channel -> {
                            System.out.println("WS Close listener= => Clearing session");
                            PeerInternalListener lst = (PeerInternalListener) ((ChannelListener.SimpleSetter) channel.getReceiveSetter()).get();
                            String sessionK = (String) lst.graph().getProperty(AUTH_PARAM_KEY);
                            sessionManager.clearSession(sessionK);
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    session.setLastHit((long) listener.graph().getProperty("ws.last"));
                }
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
                webSocketChannel.getReceiveSetter().set(new PeerInternalListener(graph));
                webSocketChannel.resumeReceives();
                peers.add(webSocketChannel);
            });

        } else {
            throw new RuntimeException("Could not find auth token while starting Secured WebSocket connection. RequestParams: " + webSocketHttpExchange.getQueryString());
        }
    }
}
