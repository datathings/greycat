/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket;

import greycat.Graph;
import greycat.auth.IdentityManager;
import greycat.websocket.handlers.GCAuthHandler;
import greycat.websocket.handlers.GCResetPasswordHandler;
import greycat.websocket.handlers.GCSecurityHandler;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

/**
 * Created by Gregory NAIN on 23/05/2017.
 */
public class SecWSServer extends WSServer {


    private IdentityManager identityManager;

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
        super.onConnect(webSocketHttpExchange, webSocketChannel);
        //this.identityManager.connectionEstablished(uuid, webSocketChannel)
    }

    protected void onChannelClosed(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) {
        //super.onChannelClosed(webSocketChannel, channel);

    }

    @Override
    protected void process_rpc(byte[] input, WebSocketChannel channel) {
        if(true) {
            super.process_rpc(input, channel);
        }
    }
}
