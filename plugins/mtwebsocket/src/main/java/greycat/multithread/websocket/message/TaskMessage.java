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
package greycat.multithread.websocket.message;

import greycat.struct.Buffer;

public class TaskMessage {
    private final Buffer task;
    private final Buffer callback;
    private final int returnId;
    private final int taskId;

    private Buffer context = null;
    private Buffer printcb = null;
    private Buffer progrescb = null;

    public TaskMessage(Buffer task, Buffer callback, int returnId, int taskId) {
        this.task = task;
        this.callback = callback;
        this.returnId = returnId;
        this.taskId = taskId;
    }

    public Buffer getTask() {
        return task;
    }

    public Buffer getCallback() {
        return callback;
    }

    public int getReturnId() {
        return returnId;
    }

    public int getTaskId() {
        return taskId;
    }

    public Buffer getContext() {
        return context;
    }

    public Buffer getPrintcb() {
        return printcb;
    }

    public Buffer getProgrescb() {
        return progrescb;
    }

    public void setContext(Buffer context) {
        this.context = context;
    }

    public void setHookPrint(Buffer hookcodehash) {
        this.printcb = hookcodehash;
    }

    public void setHookProgress(Buffer progresscodehash) {
        this.progrescb = progresscodehash;
    }

}
