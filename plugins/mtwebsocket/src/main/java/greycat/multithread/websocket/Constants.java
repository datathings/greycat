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

public class Constants {
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
}
