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
package greycat;

public abstract class Log {

    public static final byte TRACE = 5;
    public static final byte DEBUG = 4;
    public static final byte INFO = 3;
    public static final byte WARNING = 2;
    public static final byte ERROR = 1;
    public static final byte OFF = 0;

    public static byte LOG_LEVEL = INFO;
    public static boolean LOG_SYSTEM_OUT = true; // all logs also go to system.out

    public abstract Log trace(String message, Object... params);

    public abstract Log debug(String message, Object... params);

    public abstract Log info(String message, Object... params);

    public abstract Log warn(String message, Object... params);

    public abstract Log error(String message, Object... params);

    public abstract Log activateRemote(Graph localGraph);

}
