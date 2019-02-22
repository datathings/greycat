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
package org.mwg.debug;

import org.mwg.task.TaskAction;
import org.mwg.task.TaskContext;
import org.mwg.task.TaskHook;
import org.mwg.utility.HashHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class JsonHook implements TaskHook {

    private TaskTraceRegistry registry;

    JsonHook(TaskTraceRegistry p_registry) {
        this.registry = p_registry;
    }

    private Map<TaskContext, List> buffers = new HashMap<TaskContext, List>();

    private StringBuilder jsonTask = new StringBuilder();

    private int taskID;

    // called at the beginning of the task
    @Override
    public synchronized void start(TaskContext initialContext) {
        taskID = initialContext.hashCode();
        jsonTask.append("{\n\t");
        jsonTask.append("\"id\" : ");
        jsonTask.append(initialContext.hashCode());
        jsonTask.append(",\n\t");
        jsonTask.append("\"action\" : \"task\"");
        jsonTask.append(",\n\t");
        jsonTask.append("\"children\" : [");

    }

    // called at the end of the task
    @Override
    public synchronized void end(TaskContext finalContext) {

        List actions = buffers.get(finalContext);



        //removing the last comma
        removeLastChar(jsonTask, ',');
        jsonTask.append("]\n}");
        removeLastChar(jsonContext, ',');
        jsonContext.append("]\n}");

        registry.tasks.put(taskID, jsonTask.toString());
        registry.contexts.put(taskID, jsonContext.toString());

    }

    // called before every action of the task
    @Override
    public synchronized void beforeAction(TaskAction action, TaskContext context) {
        jsonTask.append("\n");
        jsonTask.append("{\n");
        String taskName = action.toString();
        jsonTask.append("\"id\" : ");
        jsonTask.append((int) HashHelper.hashBytes((action.toString() + action.hashCode() + context.hashCode()).getBytes()));
        jsonTask.append(",\n");
        jsonTask.append("\"action\" : \"");
        jsonTask.append(context.template(taskName));
        jsonTask.append("\"");
        jsonTask.append(",\n");
        jsonTask.append("\"children\" : [");

    }

    // called after every action of the task
    @Override
    public synchronized void afterAction(TaskAction action, TaskContext context) {
        String actionCtxKey = HashHelper.hashBytes((action.toString() + action.hashCode() + context.hashCode()).getBytes()) + "";
        registry.contexts.put(actionCtxKey, context.toString());
    }

    // called before a subtask (the tasks inside a repeat, inside a ifThen...)
    @Override
    public void beforeTask(TaskContext parentContext, TaskContext context) {

    }

    // called after a subtask (the tasks inside a repeat, inside a ifThen...)
    @Override
    public void afterTask(TaskContext context) {

    }


}
