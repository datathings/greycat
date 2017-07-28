/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket;

import greycat.Graph;
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

/**
 * Created by Gregory NAIN on 23/05/2017.
 */
public class SecWSServer extends WSSharedServer {

    private IdentityManager identityManager;
    private static final String AUTH_PARAM_KEY = "gc-auth-key";

    public SecWSServer(Graph p_graph, int p_port, IdentityManager _idManager) {
        super(p_graph, p_port);
        this.identityManager = _idManager;
    }


    @Override
    public void start() {
        handlers.replaceAll((s, httpHandler) -> new GCSecurityHandler(httpHandler, this.identityManager));
        handlers.put("/auth", new GCAuthHandler(this.identityManager));
        handlers.put("/renewpasswd", new GCResetPasswordHandler(this.identityManager));

        super.start();
    }

    @Override
    public void onConnect(WebSocketHttpExchange webSocketHttpExchange, WebSocketChannel webSocketChannel) {
        List<String> tokens = webSocketHttpExchange.getRequestParameters().get(AUTH_PARAM_KEY);
        if (tokens != null && tokens.size() == 1) {
            webSocketChannel.setAttribute(AUTH_PARAM_KEY, tokens.get(0));
            super.onConnect(webSocketHttpExchange, webSocketChannel);
        } else {
            throw new RuntimeException("Could not find auth token while starting Secured WebSocket connection. RequestParams: " + webSocketHttpExchange.getQueryString());
        }
    }

    protected void onChannelClosed(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) {
        identityManager.onChannelDisconnected((String)webSocketChannel.getAttribute(AUTH_PARAM_KEY));
        super.onChannelClosed(webSocketChannel, channel);
    }

    @Override
    protected void process_rpc(byte[] input, WebSocketChannel channel) {
        if(input[0] == WSConstants.HEART_BEAT_PING || input[0] == WSConstants.HEART_BEAT_PONG) {
            super.process_rpc(input, channel);
        } else {
            if(identityManager.onChannelActivity((String)channel.getAttribute(AUTH_PARAM_KEY))) {
                super.process_rpc(input, channel);
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
