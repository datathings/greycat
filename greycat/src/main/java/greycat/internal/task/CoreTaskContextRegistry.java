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

import greycat.TaskContext;
import greycat.TaskContextRegistry;
import greycat.TaskProgressReport;

import java.util.*;

public class CoreTaskContextRegistry implements TaskContextRegistry {

    private int nextId = 0;
    private final LinkedList<Integer> availableIds = new LinkedList<>();
    private final Map<Integer, TaskContextRecord> contexts = new HashMap<Integer, TaskContextRecord>();

    private final class TaskContextRecord {
        TaskContext ctx;
        long start_timestamp;
        long progress_timestamp;
        TaskProgressReport lastReport;

    }

    @Override
    public synchronized final void register(final TaskContext taskContext) {
        CoreTaskContext casted = (CoreTaskContext) taskContext;
        Integer id = availableIds.pollFirst();
        if (id == null) {
            id = nextId;
            nextId++;
        }
        casted.in_registry_id = id;

        TaskContextRecord rec = new TaskContextRecord();
        rec.ctx = casted;
        rec.start_timestamp = System.currentTimeMillis();
        rec.lastReport = null;

        contexts.put(casted.in_registry_id, rec);
    }

    /*
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
    */

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

            builder.append(",\"properties\":");
            builder.append('[');
            boolean isFirst = true;
            for (String key : registry.ctx.properties().keySet()) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    builder.append(',');
                }
                builder.append('{');
                builder.append("\"" + key + "\":");
                builder.append("\"" + registry.ctx.properties().get(key) + "\"");
                builder.append('}');
            }
            builder.append(']');

            builder.append('}');
        }
        return builder.toString();
    }

    @Override
    public synchronized final String stats() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        boolean isFirst = true;

        Integer[] ids = this.contexts.keySet().toArray(new Integer[this.contexts.size()]);
        for (int i = 0; i < ids.length; i++) {
            Integer key = ids[i];
            if (isFirst) {
                isFirst = false;
            } else {
                builder.append(',');
            }
            builder.append(statsOf(key));
        }
        builder.append(']');

        return builder.toString();
    }

    @Override
    public final void forceStop(final Integer taskContextID) {
        TaskContextRecord rec = this.contexts.get(taskContextID);
        if (rec != null) {
            rec.ctx.terminateTask();
        }
    }

    @Override
    public final void forceStopAll() {
        Integer[] ids = this.contexts.keySet().toArray(new Integer[this.contexts.size()]);
        for (int i = 0; i < ids.length; i++) {
            Integer key = ids[i];
            TaskContextRecord rec = this.contexts.get(key);
            rec.ctx.terminateTask();
        }
    }

    public final void unregister(final int task_id) {
        contexts.remove(task_id);
        availableIds.addLast(task_id);
    }

    public final void reportProgress(final Integer ctx_id, final CoreProgressReport report) {
        TaskContextRecord rec = this.contexts.get(ctx_id);
        if (rec != null) {
            rec.progress_timestamp = System.currentTimeMillis();
            rec.lastReport = report;
        }
    }


}
