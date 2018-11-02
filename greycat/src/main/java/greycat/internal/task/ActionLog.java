/**
 * Copyright 2017-2018 The GreyCat Authors.  All rights reserved.
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
package greycat.internal.task;

import greycat.Constants;
import greycat.Action;
import greycat.TaskContext;
import greycat.struct.Buffer;

public class ActionLog implements Action {

    public static String LEVEL_INFO = "info";

    public static String LEVEL_DEBUG = "debug";

    public static String LEVEL_WARN = "warn";

    private static String LEVEL_WARNING = "warning";

    public static String LEVEL_ERROR = "error";

    private final String _value;
    private final String _level;

    ActionLog(final String p_value, String p_level) {
        this._value = p_value;
        this._level = p_level;
    }

    @Override
    public void eval(final TaskContext ctx) {
        String message = ctx.template(_value);
        if (_level == null) {
            ctx.graph().log().info(message);
        } else if (_level.equals(LEVEL_INFO)) {
            ctx.graph().log().info(message);
        } else if (_level.equals(LEVEL_DEBUG)) {
            ctx.graph().log().debug(message);
        } else if (_level.equals(LEVEL_WARN) || _level.equals(LEVEL_WARNING)) {
            ctx.graph().log().warn(message);
        } else if (_level.equals(LEVEL_ERROR)) {
            ctx.graph().log().error(message);
        }
        ctx.continueTask();
    }

    @Override
    public final void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.LOG);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_value, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.LOG;
    }

}
