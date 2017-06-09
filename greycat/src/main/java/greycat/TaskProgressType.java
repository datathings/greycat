/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
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
package greycat;

public class TaskProgressType {

    public static final byte START_TASK = 0;
    public static final byte START_SUB_TASK = 1;
    public static final byte START_ACTION = 2;
    public static final byte END_TASK = 3;
    public static final byte END_SUB_TASK = 4;
    public static final byte END_ACTION = 5;
    public static final byte ACTION_PROGRESS = 6;

    public static String toString(byte value) {
        switch (value) {
            case START_TASK:
                return "START_TASK";
            case START_SUB_TASK:
                return "START_SUB_TASK";
            case START_ACTION:
                return "START_ACTION";
            case END_TASK:
                return "END_TASK";
            case END_SUB_TASK:
                return "END_SUB_TASK";
            case END_ACTION:
                return "END_ACTION";
            case ACTION_PROGRESS:
                return "ACTION_PROGRESS";
            default:
                return "Unknown progress type:" + value;
        }
    }

}
