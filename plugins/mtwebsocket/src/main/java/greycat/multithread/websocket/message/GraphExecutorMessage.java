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
package greycat.multithread.websocket.message;

import greycat.multithread.websocket.message.GraphMessage;
import greycat.struct.Buffer;

import java.util.concurrent.BlockingQueue;

/**
 * Type of message used by the Graph Executor as input
 */
public class GraphExecutorMessage extends GraphMessage {
    private final BlockingQueue<GraphMessage> outputQueue;

    /**
     *
     * @param outputQueue message queue on which the result message should be sent
     * @param operationId id of the operation to execute
     * @param returnID channel hash or callback id
     * @param content of the message
     * @param originalCallBack callback id from the client if existing
     */
    public GraphExecutorMessage(BlockingQueue<GraphMessage> outputQueue, byte operationId, int returnID, Buffer content, Buffer originalCallBack) {
        super(operationId, returnID, content, originalCallBack);
        this.outputQueue = outputQueue;
    }

    public BlockingQueue<GraphMessage> getOutputQueue() {
        return outputQueue;
    }


}
