/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket;


public class SecWSClient extends WSClient {


    public SecWSClient(String p_url, String authKey) {
        super(p_url+"?gc-auth-key="+authKey);
    }

}
