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

import greycat.Constants;
import greycat.TaskContext;
import greycat.TaskContextRegistry;

import java.util.HashMap;
import java.util.Map;

public class CoreTaskContextRegistry implements TaskContextRegistry {

    private int counter = 0;

    private final class TaskContextRecord {
        TaskContext ctx;
        long start_timestamp;
        double progress;
        long progress_timestamp;
        String progress_comment;
    }

    private final Map<Integer, TaskContextRecord> contexts = new HashMap<Integer, TaskContextRecord>();

    @Override
    public synchronized final void register(final TaskContext taskContext) {
        CoreTaskContext casted = (CoreTaskContext) taskContext;
        casted.in_registry_id = counter;
        counter++;

        TaskContextRecord rec = new TaskContextRecord();
        rec.ctx = casted;
        rec.start_timestamp = System.currentTimeMillis();
        rec.progress = 0.0d;
        rec.progress_comment = null;

        contexts.put(casted.in_registry_id, rec);
    }

    @Override
    public synchronized final String stats() {
        StringBuilder builder = new StringBuilder();
        builder.append(Constants.BLOCK_OPEN);
        boolean is_first = true;

        Integer[] ids = this.contexts.keySet().toArray(new Integer[this.contexts.size()]);
        for (int i = 0; i < ids.length; i++) {
            Integer key = ids[i];
            TaskContextRecord rec = this.contexts.get(key);

            if (is_first) {
                is_first = false;
            } else {
                builder.append(Constants.TASK_PARAM_SEP);
            }
            builder.append(Constants.SUB_TASK_OPEN);
            builder.append("\"");
            builder.append("id");
            builder.append("\"");
            builder.append(Constants.CHUNK_VAL_SEP);
            builder.append(String.valueOf(key));

            builder.append(Constants.TASK_PARAM_SEP);

            builder.append(Constants.SUB_TASK_OPEN);
            builder.append("\"");
            builder.append("start_timestamp");
            builder.append("\"");
            builder.append(Constants.CHUNK_VAL_SEP);
            builder.append(String.valueOf(rec.start_timestamp));

            builder.append(Constants.TASK_PARAM_SEP);

            builder.append(Constants.SUB_TASK_OPEN);
            builder.append("\"");
            builder.append("progress");
            builder.append("\"");
            builder.append(Constants.CHUNK_VAL_SEP);
            builder.append(String.valueOf(rec.progress));

            builder.append(Constants.TASK_PARAM_SEP);

            builder.append(Constants.SUB_TASK_OPEN);
            builder.append("\"");
            builder.append("progress_timestamp");
            builder.append("\"");
            builder.append(Constants.CHUNK_VAL_SEP);
            builder.append(String.valueOf(rec.progress_timestamp));

            builder.append(Constants.TASK_PARAM_SEP);

            builder.append(Constants.SUB_TASK_OPEN);
            builder.append("\"");
            builder.append("comment");
            builder.append("\"");
            builder.append(Constants.CHUNK_VAL_SEP);
            builder.append("\"");
            builder.append(rec.progress_comment);//TODO manage auto-escape
            builder.append("\"");

            builder.append(Constants.SUB_TASK_CLOSE);

        }

        builder.append(Constants.BLOCK_CLOSE);
        return null;
    }

    @Override
    public final void forceStop(final Integer taskContextID) {
        TaskContextRecord rec = this.contexts.get(taskContextID);
        if (rec != null) {
            CoreTaskContext ctx = (CoreTaskContext) rec.ctx;
            ctx.ext_stop = true;
        }
    }

    public final void unregister(final int task_id) {
        contexts.remove(task_id);
    }

    public final void reportProgress(final Integer ctx_id, final double progress, final String comment) {
        TaskContextRecord rec = this.contexts.get(ctx_id);
        if (rec != null) {
            rec.progress = progress;
            rec.progress_comment = comment;
            rec.progress_timestamp = System.currentTimeMillis();
        }
    }


}
