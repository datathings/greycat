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
package greycat.internal.task;

import greycat.Action;
import greycat.Constants;
import greycat.TaskContext;
import greycat.struct.Buffer;

public class ActionStopTimer implements Action {
    private final String _timerName;
    private final String _msg;
    private final String _counterName;
    private final String _displayEach;

    ActionStopTimer(final String message, final String timerName, final String counterName, final String displayEach) {
        this._timerName = timerName;
        this._msg = message;
        this._counterName = counterName;
        this._displayEach = displayEach;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final String counter = ctx.template(_counterName);
        final String displayEach = ctx.template(_displayEach);

        boolean disp = false;
        long cnt = 0;
        if (counter == null || displayEach == null || counter.equals("") || counter.equals("")) {
            disp = true;
        } else {
            try {
                cnt = Long.parseLong(counter);
                long each = Long.parseLong(displayEach);
                if (each == 0 || each == 1 || cnt % each == 0) {
                    disp = true;
                }
            } catch (Exception ex) {
                ctx.endTask(null, ex);
            }
        }
        if (disp) {
            final String timer = ctx.template(_timerName);
            long startTime = Long.parseLong(timer);
            long endTime = System.currentTimeMillis();

            double finaltime = endTime - startTime;
            StringBuilder s = new StringBuilder();
            s.append(_msg);
            if (cnt != 0) {
                s.append(", iter: ");
                s.append(cnt);
            }
            s.append(", elapsed time: ");

            if (finaltime < 1000) {
                s.append(finaltime).append(" ms");
            } else if (finaltime < 60000) {
                finaltime = finaltime / 1000;
                finaltime = Math.floor(finaltime * 100) / 100;
                s.append(finaltime).append(" s");
            } else if (finaltime < 3600000) {
                finaltime = finaltime / 60000;
                int minutes = (int) finaltime;
                int seconds = (int) ((finaltime - minutes) * 60);
                s.append(minutes).append(":").append(seconds).append(" min");
            } else {
                finaltime = finaltime / 3600000;
                int hours = (int) finaltime;
                int minutes = (int) ((finaltime - hours) * 60);
                s.append(hours).append(":").append(minutes).append(" h");
            }
            if (cnt != 0) {
                double spd = cnt * 1000.0 / (endTime - startTime);
                if (spd < 1000) {
                    spd = spd * 100;
                    s.append(", speed: ").append(((long) spd) / 100.0).append(" v/s");
                } else if (spd < 1000000) {
                    spd = spd / 10;
                    s.append(", speed: ").append(((long) spd) / 100.0).append(" kv/s");
                } else {
                    spd = spd / 10000;
                    s.append(", speed: ").append(((long) spd) / 100.0).append(" Mv/s");
                }
            }
            System.out.println(s);
        }
        ctx.continueTask();
    }

    @Override
    public void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.STOP_TIMER);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_timerName, builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        TaskHelper.serializeString(_msg, builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        TaskHelper.serializeString(_counterName, builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        TaskHelper.serializeString(_displayEach, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.STOP_TIMER;
    }
}
