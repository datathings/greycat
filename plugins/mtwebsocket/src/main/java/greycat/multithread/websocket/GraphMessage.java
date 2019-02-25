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

import greycat.struct.Buffer;

/**
 * Standard type of message used by the different queues
 */
public class GraphMessage {

    private final Buffer content;
    private final Buffer originalCallBack;
    private final int returnID;
    private final byte operationId;

    /**
     *
     * @param operationId id of the operation to execute
     * @param returnID channel hash or callback id
     * @param content of the message
     * @param originalCallBack callback id from the client if existing
     */
    public GraphMessage(byte operationId, int returnID, Buffer content, Buffer originalCallBack) {
        this.content = content;
        this.returnID = returnID;
        this.operationId = operationId;
        this.originalCallBack = originalCallBack;
    }

    public Buffer getContent() {
        return content;
    }

    public int getReturnID() {
        return returnID;
    }

    public byte getOperationId() {
        return operationId;
    }

    public Buffer getOriginalCallBack() {
        return originalCallBack;
    }
}
