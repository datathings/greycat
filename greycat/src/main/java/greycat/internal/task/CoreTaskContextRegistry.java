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

import greycat.TaskContext;
import greycat.TaskContextRegistry;
import greycat.TaskProgressReport;

import java.util.HashMap;
import java.util.Map;

public class CoreTaskContextRegistry implements TaskContextRegistry {

    private int counter = 0;

    private final class TaskContextRecord {
        TaskContext ctx;
        long start_timestamp;
        long progress_timestamp;
        TaskProgressReport lastReport;
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
        rec.lastReport = null;

        contexts.put(casted.in_registry_id, rec);
    }

    @Override
    public synchronized final int registerWith(final TaskContext taskContext, int id) {
        CoreTaskContext casted = (CoreTaskContext) taskContext;
        if (!contexts.containsKey(id)) {
            casted.in_registry_id = id;
            counter++;

            TaskContextRecord rec = new TaskContextRecord();
            rec.ctx = casted;
            rec.start_timestamp = System.currentTimeMillis();
            rec.lastReport = null;

            contexts.put(casted.in_registry_id, rec);
            return id;
        } else {
            return registerWith(taskContext, id + 1);
        }
    }

    @Override
    public String statsOf(int id) {
        StringBuilder builder = new StringBuilder();
        if (contexts.containsKey(id)) {
            TaskContextRecord registry = contexts.get(id);
            builder.append("{");

            builder.append("\"id\":");
            builder.append(String.valueOf(id));

            builder.append(",\"start_timestamp\":");
            builder.append(String.valueOf(registry.start_timestamp));

            builder.append(",\"progress_timestamp\":");
            builder.append(String.valueOf(registry.progress_timestamp));

            builder.append(",\"last_report\":");
            if (registry.lastReport != null) {
                registry.lastReport.toJson(builder);
            } else {
                builder.append("null");
            }

            builder.append('}');
        }
        return builder.toString();
    }

    @Override
    public synchronized final String stats() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        boolean is_first = true;

        Integer[] ids = this.contexts.keySet().toArray(new Integer[this.contexts.size()]);
        for (int i = 0; i < ids.length; i++) {
            Integer key = ids[i];
            TaskContextRecord rec = this.contexts.get(key);

            if (is_first) {
                is_first = false;
            } else {
                builder.append(',');
            }
            builder.append("{");

            builder.append("\"id\":");
            builder.append(String.valueOf(key));

            builder.append(",\"start_timestamp\":");
            builder.append(String.valueOf(rec.start_timestamp));

            builder.append(",\"progress_timestamp\":");
            builder.append(String.valueOf(rec.progress_timestamp));

            builder.append(",\"last_report\":");
            if (rec.lastReport != null) {
                rec.lastReport.toJson(builder);
            } else {
                builder.append("null");
            }

            builder.append('}');
        }

        builder.append(']');
        return builder.toString();
    }

    @Override
    public final void forceStop(final Integer taskContextID) {
        TaskContextRecord rec = this.contexts.get(taskContextID);
        if (rec != null) {
            CoreTaskContext ctx = (CoreTaskContext) rec.ctx;
            ctx.ext_stop.set(true);
        }
    }

    public final void unregister(final int task_id) {
        contexts.remove(task_id);
    }

    public final void reportProgress(final Integer ctx_id, final CoreProgressReport report) {
        TaskContextRecord rec = this.contexts.get(ctx_id);
        if (rec != null) {
            rec.progress_timestamp = System.currentTimeMillis();
            rec.lastReport = report;
        }
    }


}
