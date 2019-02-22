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
package greycat.utility;

import greycat.Action;
import greycat.TaskHook;
import greycat.TaskContext;

import java.util.HashMap;
import java.util.Map;

public class VerboseHook implements TaskHook {

    private Map<TaskContext, Integer> ctxIdents = new HashMap<TaskContext, Integer>();

    @Override
    public synchronized void start(TaskContext initialContext) {
        ctxIdents.put(initialContext, 0);
        System.out.println("StartTask:" + initialContext);
    }

    @Override
    public synchronized void beforeAction(Action action, TaskContext context) {
        Integer currentPrefix = ctxIdents.get(context);
        for (int i = 0; i < currentPrefix; i++) {
            System.out.print("\t");
        }
        String taskName = action.toString();
        System.out.println(context.template(taskName));
        /*
        for (int i = 0; i < context.ident(); i++) {
            System.out.print("\t");
            System.out.println(context.result().toString());
        }*/
    }

    @Override
    public synchronized void afterAction(Action action, TaskContext context) {
        //NOOP
    }

    @Override
    public synchronized void beforeTask(TaskContext parentContext, TaskContext context) {
        Integer currentPrefix = ctxIdents.get(parentContext);
        ctxIdents.put(context, currentPrefix + 1);
    }

    @Override
    public synchronized void afterTask(TaskContext context) {
        ctxIdents.remove(context);
    }

    @Override
    public synchronized void end(TaskContext finalContext) {
        System.out.println("EndTask:" + finalContext.toString());
    }
}
