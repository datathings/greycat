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

import greycat.Action;
import greycat.Callback;
import greycat.Constants;
import greycat.TaskContext;
import greycat.chunk.ChunkSpace;
import greycat.struct.Buffer;

class ActionSaveAbove implements Action {

    private final String _limit;

    ActionSaveAbove(String l) {
        _limit = l;
    }

    @Override
    public void eval(final TaskContext ctx) {
        try {
            double limit = Double.parseDouble(ctx.template(_limit));
            ChunkSpace space = ctx.graph().space();
            long dirties = space.dirties();
            long all = space.available() + dirties;
            double status = dirties * 1.0 / all;
            if (status >= limit) {
                final Buffer notifier = ctx.notifier();
                if (notifier == null) {
                    ctx.graph().save(new Callback<Boolean>() {
                        @Override
                        public void on(Boolean result) {
                            ctx.continueTask();
                        }
                    });
                } else {
                    ctx.graph().saveSilent(new Callback<Buffer>() {
                        @Override
                        public void on(final Buffer result) {
                            notifier.writeAll(result.data());
                            result.free();
                            ctx.continueTask();
                        }
                    });
                }
            } else {
                ctx.continueTask();
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.continueTask();
        }
    }

    @Override
    public void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.SAVE_ABOVE);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_limit, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }


    @Override
    public final String name() {
        return CoreActionNames.SAVE_ABOVE;
    }

}
