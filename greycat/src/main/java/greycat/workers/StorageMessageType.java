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
package greycat.workers;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Gregory NAIN on 2019-03-06.
 */
public class StorageMessageType {

    public static final String DISCONNECTED_ERROR = "Please connect your WebSocket client first.";

    public static final byte REQ_GET = 0;
    public static final byte REQ_PUT = 1;
    public static final byte REQ_LOCK = 2;
    public static final byte REQ_UNLOCK = 3;
    public static final byte REQ_REMOVE = 4;
    public static final byte REQ_TASK = 5;
    public static final byte RESP_GET = 6;
    public static final byte RESP_PUT = 7;
    public static final byte RESP_REMOVE = 8;
    public static final byte RESP_LOCK = 9;
    public static final byte RESP_UNLOCK = 10;
    public static final byte RESP_TASK = 11;

    public static final byte NOTIFY_UPDATE = 12;
    public static final byte NOTIFY_PRINT = 13;
    public static final byte NOTIFY_PROGRESS = 14;

    public static final byte HEART_BEAT_PING = 15;
    public static final byte HEART_BEAT_PONG = 16;

    public static final byte REQ_TASK_STATS = 17;
    public static final byte RESP_TASK_STATS = 18;

    public static final byte REQ_TASK_STOP = 19;
    public static final byte RESP_TASK_STOP = 20;

    public static final byte REQ_LOG = 21;
    public static final byte RESP_LOG = 22;


    public static Map<Byte, String> byteToStringMap  = initMap();

    private static Map<Byte, String> initMap() {
        Map<Byte, String> localMap  = new HashMap<>();
        localMap.put(REQ_GET, "REQ_GET");
        localMap.put(REQ_PUT, "REQ_PUT");
        localMap.put(REQ_LOCK, "REQ_LOCK");
        localMap.put(REQ_UNLOCK, "REQ_UNLOCK");
        localMap.put(REQ_REMOVE, "REQ_REMOVE");
        localMap.put(REQ_TASK, "REQ_TASK");
        localMap.put(RESP_GET, "RESP_GET");
        localMap.put(RESP_PUT, "RESP_PUT");
        localMap.put(RESP_REMOVE, "RESP_REMOVE");
        localMap.put(RESP_LOCK, "RESP_LOCK");
        localMap.put(RESP_UNLOCK, "RESP_UNLOCK");
        localMap.put(RESP_TASK, "RESP_TASK");
        localMap.put(NOTIFY_UPDATE, "NOTIFY_UPDATE");
        localMap.put(NOTIFY_PRINT, "NOTIFY_PRINT");
        localMap.put(NOTIFY_PROGRESS, "NOTIFY_PROGRESS");
        localMap.put(HEART_BEAT_PING, "HEART_BEAT_PING");
        localMap.put(HEART_BEAT_PONG, "HEART_BEAT_PONG");
        localMap.put(REQ_TASK_STATS, "REQ_TASK_STATS");
        localMap.put(RESP_TASK_STATS, "RESP_TASK_STATS");
        localMap.put(REQ_TASK_STOP, "REQ_TASK_STOP");
        localMap.put(RESP_TASK_STOP, "RESP_TASK_STOP");
        localMap.put(REQ_LOG, "REQ_LOG");
        localMap.put(RESP_LOG, "RESP_LOG");
        return localMap;
    }

    public static String byteToString(byte messageType) {
        String resp = byteToStringMap.get(messageType);
        if(resp == null) {
            resp = "Unknown: " + (int)messageType;
        }
        return resp;
    }




}
