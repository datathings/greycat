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
import greycat.Node;
import greycat.plugin.Job;
import greycat.Action;
import greycat.Callback;
import greycat.DeferCounter;
import greycat.base.BaseNode;
import greycat.internal.CoreDeferCounter;
import greycat.TaskContext;
import greycat.TaskResult;
import greycat.struct.Buffer;


class ActionTravelInWorld implements Action {

    private final String _world;

    ActionTravelInWorld(final String world) {
        _world = world;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final String flatWorld = ctx.template(_world);
        long parsedWorld;
        try {
            parsedWorld = Long.parseLong(flatWorld);
        } catch (Throwable t) {
            Double d = Double.parseDouble(flatWorld);
            parsedWorld = d.longValue();
        }
        ctx.setWorld(parsedWorld);
        final TaskResult previous = ctx.result();
        if (previous != null) {
            final int previousSize = previous.size();
            //declare batch lookup variable
            long[] worlds = new long[previousSize];
            long[] times = new long[previousSize];
            long[] ids = new long[previousSize];
            final int[] indexes = new int[previousSize];
            int index = 0;
            for (int i = 0; i < previousSize; i++) {
                Object loopObj = previous.get(i);
                if (loopObj instanceof BaseNode) {
                    final Node casted = (Node) loopObj;
                    if (parsedWorld != casted.world()) {
                        worlds[index] = parsedWorld;
                        times[index] = casted.time();
                        ids[index] = casted.id();
                        indexes[index] = i;
                        index++;
                        casted.free();
                    }
                }
            }
            if (index == 0) {
                ctx.continueTask();
            } else {
                //shrink if necessary
                if (index != previousSize) {
                    long[] new_worlds = new long[index];
                    System.arraycopy(new_worlds, 0, worlds, 0, index);
                    worlds = new_worlds;
                    long[] new_times = new long[index];
                    System.arraycopy(new_times, 0, times, 0, index);
                    times = new_times;
                    long[] new_ids = new long[index];
                    System.arraycopy(new_ids, 0, ids, 0, index);
                    ids = new_ids;
                }
                //lookup all
                ctx.graph().resolver().lookupBatch(worlds, times, ids, new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        for (int i = 0; i < result.length; i++) {
                            previous.set(indexes[i], result[i]);
                        }
                        ctx.continueTask();
                    }
                });
            }
        } else {
            ctx.continueTask();
        }
    }

    @Override
    public void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.TRAVEL_IN_TIME);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        builder.writeString(_world);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.TRAVEL_IN_WORLD;
    }

}
