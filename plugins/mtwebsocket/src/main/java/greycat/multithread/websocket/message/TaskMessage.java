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
