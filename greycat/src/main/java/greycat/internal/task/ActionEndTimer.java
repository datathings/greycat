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
package greycat.internal.task;

import greycat.Action;
import greycat.Constants;
import greycat.TaskContext;
import greycat.internal.task.CoreActionNames;
import greycat.internal.task.TaskHelper;
import greycat.struct.Buffer;

public class ActionEndTimer implements Action {
    private final String _timerName;
    private final String _msg;

    ActionEndTimer(final String timerName, final String message) {
        this._timerName = timerName;
        this._msg = message;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final String timer = ctx.template(_timerName);
        long startTime = (long) ctx.variable(timer).get(0);
        long endTime = System.currentTimeMillis();

        double finaltime = endTime - startTime;
        String s = _msg + ", elapsed time: ";

        if (finaltime < 1000) {
            s = s + finaltime + " ms";
        } else if (finaltime < 60000) {
            finaltime = finaltime / 1000;
            finaltime = Math.floor(finaltime * 100) / 100;
            s = s + finaltime + " s";
        } else if (finaltime < 3600000) {
            finaltime = finaltime / 60000;
            int minutes = (int) finaltime;
            int seconds = (int) ((finaltime - minutes) * 60);
            s = s + minutes + ":" + seconds + " min";
        } else {
            finaltime = finaltime / 3600000;
            int hours = (int) finaltime;
            int minutes = (int) ((finaltime - hours) * 60);
            s = s + hours + ":" + minutes + " h";
        }
        System.out.println(s);
        ctx.continueTask();
    }

    @Override
    public void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.END_TIMER);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_timerName, builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        TaskHelper.serializeString(_msg, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.END_TIMER;
    }
}
