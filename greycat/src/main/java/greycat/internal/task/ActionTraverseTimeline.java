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

import greycat.*;
import greycat.base.BaseNode;
import greycat.plugin.Job;
import greycat.struct.Buffer;

import java.util.regex.Pattern;

class ActionTraverseTimeline implements Action {

    private final String _start;
    private final String _end;
    private final String _limit;

    ActionTraverseTimeline(final String p_start, final String p_end, final String p_limit) {
        this._start = p_start;
        this._end = p_end;
        this._limit = p_limit;
    }

    @Override
    public final void eval(final TaskContext ctx) {
        final long start = Long.parseLong(ctx.template(_start));
        final long end = Long.parseLong(ctx.template(_end));
        final int limit = Integer.parseInt(ctx.template(_limit));
        final TaskResult previous = ctx.result();
        final TaskResult next = ctx.newResult();
        final int previousSize = previous.size();
        int counter = 0;
        for (int i = 0; i < previousSize; i++) {
            final Object obj = previous.get(i);
            if (obj instanceof BaseNode) {
                counter++;
            }
        }
        final DeferCounter deferCounter = ctx.graph().newCounter(counter);
        for (int i = 0; i < previousSize; i++) {
            final Object obj = previous.get(i);
            if (obj instanceof BaseNode) {
                final Node casted = (Node) obj;
                int finalI = i;
                ctx.graph().lookupTimes(casted.world(), start, end, casted.id(), limit, new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        next.set(finalI, ctx.wrap(result));
                        deferCounter.count();
                    }
                });
            } else {
                next.set(i, obj);
            }
        }
        deferCounter.then(new Job() {
            @Override
            public void run() {
                ctx.continueWith(next);
            }
        });
    }

    @Override
    public final void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.TRAVERSE_TIMELINE);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_start, builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        TaskHelper.serializeString(_end, builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        TaskHelper.serializeString(_limit, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.TRAVERSE_TIMELINE;
    }

}
