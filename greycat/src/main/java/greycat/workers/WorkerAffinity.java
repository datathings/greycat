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
 * Created by Gregory NAIN on 2019-03-25.
 */
public class WorkerAffinity {

    public static final byte SESSION_WORKER = 1;
    public static final byte TASK_WORKER = 2;
    public static final byte GENERAL_PURPOSE_WORKER = 3;

    public static Map<Byte, String> byteToStringMap  = initMap();

    private static Map<Byte, String> initMap() {
        Map<Byte, String> localMap  = new HashMap<>();
        localMap.put(SESSION_WORKER, "SESSION_WORKER");
        localMap.put(TASK_WORKER, "TASK_WORKER");
        localMap.put(GENERAL_PURPOSE_WORKER, "GENERAL_PURPOSE_WORKER");
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
