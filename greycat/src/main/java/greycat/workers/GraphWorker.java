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
package greycat.workers;

import greycat.*;
import greycat.base.BaseTaskResult;
import greycat.chunk.Chunk;
import greycat.internal.CoreGraphLog;
import greycat.internal.heap.HeapBuffer;
import greycat.internal.task.CoreProgressReport;
import greycat.plugin.Job;
import greycat.struct.Buffer;
import greycat.struct.BufferIterator;
import greycat.utility.Base64;
import greycat.chunk.ChunkKey;
import greycat.chunk.KeyHelper;
import greycat.utility.L3GMap;
import greycat.utility.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @ignore ts
 */
public class GraphWorker implements Runnable {

    protected WorkerMailbox mailbox;
    protected int mailboxId;
    protected Graph workingGraphInstance;
    protected GraphBuilder workingGraphBuilder;
    protected WorkerCallbacksRegistry callbacksRegistry;
    protected ArrayList<byte[]> pendingConnectionTasks;
    private AtomicBoolean graphReady = new AtomicBoolean(false);

    private String name;
    private boolean haltRequested = false;

    public GraphWorker(GraphBuilder workingGraphBuilder, boolean canProcessGeneralTaskQueue) {
        this.workingGraphBuilder = workingGraphBuilder;
        mailbox = new WorkerMailbox(canProcessGeneralTaskQueue);
        callbacksRegistry = new WorkerCallbacksRegistry();
        mailboxId = MailboxRegistry.getInstance().addMailbox(mailbox);
        if (workingGraphBuilder.storage instanceof SlaveWorkerStorage) {
            ((SlaveWorkerStorage) workingGraphBuilder.storage).setWorkerMailboxId(mailboxId, callbacksRegistry);
        }
    }

    public GraphWorker(GraphBuilder workingGraphBuilder, String name, boolean canProcessGeneralTaskQueue) {
        this(workingGraphBuilder, canProcessGeneralTaskQueue);
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {

        return (this.name != null ? this.name : "Worker_" + mailboxId);
    }

    public void halt() {
        haltRequested = true;
    }

    public int getId() {
        return mailboxId;
    }

    public boolean submit(byte[] activity) {
        return this.mailbox.submit(activity);
    }

    private final Callback<Buffer> notifyGraphUpdate = (updateContent) -> {
        //WebSocketChannel[] others = peers.toArray(new WebSocketChannel[peers.size()]);
        Buffer notificationBuffer = workingGraphInstance.newBuffer();
        notificationBuffer.write(StorageMessageType.NOTIFY_UPDATE);
        notificationBuffer.write(Constants.BUFFER_SEP);
        notificationBuffer.writeInt(mailboxId);
        notificationBuffer.write(Constants.BUFFER_SEP);
        Base64.encodeIntToBuffer(WorkerCallbacksRegistry.MAX_INTEGER, notificationBuffer);
        notificationBuffer.write(Constants.BUFFER_SEP);
        notificationBuffer.writeAll(updateContent.data());
        byte[] notificationMsg = notificationBuffer.data();
        notificationBuffer.free();
        MailboxRegistry.getInstance().notifyGraphUpdate(mailboxId, notificationMsg);
    };

    @Override
    public void run() {
        //System.out.println(getName() + ": Started");

        //System.out.println(getName() + ": Creating new Graph");
        workingGraphInstance = workingGraphBuilder.build();
        //workingGraphInstance.logDirectory(this.name, "2MB");
        workingGraphInstance.log().debug(getName() + ": New Graph created. Connecting");

        workingGraphInstance.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean connected) {
                workingGraphInstance.log().debug(getName() + ": Graph connected.");
                if(!(workingGraphInstance.storage() instanceof SlaveWorkerStorage)) {
                    workingGraphInstance.storage().listen(GraphWorker.this.notifyGraphUpdate);
                }
                graphReady.set(true);
                if (pendingConnectionTasks != null) {
                    workingGraphInstance.log().debug(getName() + ": Re-enqueuing pending connection tasks.");
                    mailbox.addAll(pendingConnectionTasks);
                    pendingConnectionTasks.clear();
                    pendingConnectionTasks = null;
                }
            }
        });

        while (!haltRequested) {
            try {
                byte[] tmpTaskBuffer = mailbox.take();
                if (tmpTaskBuffer == MailboxRegistry.VOID_TASK_NOTIFY && !haltRequested && mailbox.canProcessGeneralTaskQueue()) {
                    //Checks if a task is available in the taskPool mailbox
                    tmpTaskBuffer = MailboxRegistry.getInstance().getDefaultMailbox().poll();
                }
                if (tmpTaskBuffer != null && tmpTaskBuffer != MailboxRegistry.VOID_TASK_NOTIFY) {
                    try {
                        if (graphReady.get() || (tmpTaskBuffer[0] % 2 == 1)) {
                            //Graph connected or RESPONSE ONLY
                            processBuffer(tmpTaskBuffer);
                        } else {
                            if (pendingConnectionTasks == null) {
                                pendingConnectionTasks = new ArrayList<>();
                            }
                            workingGraphInstance.log().debug(getName() + ": Adding task to pending connection list");
                            pendingConnectionTasks.add(tmpTaskBuffer);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } catch (InterruptedException e) {
                // e.printStackTrace();
            }
        }

        workingGraphInstance.disconnect(new Callback<Boolean>() {
            @Override
            public void on(Boolean disconnected) {
                //System.out.println(getName() + ": Graph disconnected");
            }
        });
    }

    void processBuffer(final byte[] buffer) {

        Buffer taskBuffer = new HeapBuffer();
        taskBuffer.writeAll(buffer);

        final BufferIterator it = taskBuffer.iterator();
        final Buffer bufferTypeBufferView = it.next();
        final Buffer respChannelBufferView = it.next();
        final Buffer callbackBufferView = it.next();

        // --------   DEBUG PRINT ----------
        workingGraphInstance.log().debug(getName() + "\t========= " + getName() + " Received =========");
        workingGraphInstance.log().debug(getName() + "\tType: " + StorageMessageType.byteToString(bufferTypeBufferView.read(0)));
        workingGraphInstance.log().debug(getName() + "\tChannel: " + respChannelBufferView.readInt(0));
        workingGraphInstance.log().debug(getName() + "\tCallback: " + Base64.decodeToIntWithBounds(callbackBufferView, 0, callbackBufferView.length()));
        workingGraphInstance.log().debug(getName() + "\tRaw: " + taskBuffer.toString());

        // --------   DEBUG PRINT END ----------


        int destMailboxId = respChannelBufferView.readInt(0);
        WorkerMailbox destMailbox = MailboxRegistry.getInstance().getMailbox(destMailboxId);
        byte operation = bufferTypeBufferView.read(0);


        switch (operation) {

            case StorageMessageType.NOTIFY_UPDATE: {
                this.workingGraphInstance.remoteNotify(it.next());
            }break;

            case StorageMessageType.RESP_LOG: {
                //Nothing to do, no result
                callbacksRegistry.remove(Base64.decodeToIntWithBounds(callbackBufferView, 0, callbackBufferView.length()));
            }
            break;
            case StorageMessageType.RESP_UNLOCK:
            case StorageMessageType.RESP_PUT: {
                //Boolean result
                Callback<Boolean> cb = callbacksRegistry.remove(Base64.decodeToIntWithBounds(callbackBufferView, 0, callbackBufferView.length()));
                Buffer payload = it.next();
                cb.on(Base64.decodeToIntWithBounds(payload, 0, payload.length()) == 1);
            }
            break;
            case StorageMessageType.RESP_REMOVE: {
                //Boolean result
                Callback<Boolean> cb = callbacksRegistry.remove(Base64.decodeToIntWithBounds(callbackBufferView, 0, callbackBufferView.length()));
                cb.on(null);
                //Buffer payload = it.next();
                //cb.on(Base64.decodeToIntWithBounds(payload, 0, payload.length()) == 1);
            }
            break;
            case StorageMessageType.RESP_LOCK:
            case StorageMessageType.RESP_GET: {
                //Buffer result
                Callback<Buffer> cb = callbacksRegistry.remove(Base64.decodeToIntWithBounds(callbackBufferView, 0, callbackBufferView.length()));
                Buffer payload = new HeapBuffer();
                payload.writeAll(it.next().data());
                cb.on(payload);
            }
            break;
            case StorageMessageType.RESP_TASK: {
                //Buffer result
                Callback<Buffer> cb = callbacksRegistry.remove(Base64.decodeToIntWithBounds(callbackBufferView, 0, callbackBufferView.length()));
                Buffer payload = new HeapBuffer();
                payload.writeAll(it.next().data());
                cb.on(payload);
            }
            break;
            case StorageMessageType.NOTIFY_PRINT: {
                Callback<String> cb = callbacksRegistry.get(Base64.decodeToIntWithBounds(callbackBufferView, 0, callbackBufferView.length()));
                final Buffer printContentView = it.next();
                final String printContent = Base64.decodeToStringWithBounds(printContentView, 0, printContentView.length());
                cb.on(printContent);
            }
            break;

            case StorageMessageType.NOTIFY_PROGRESS: {
                Callback<TaskProgressReport> cb = callbacksRegistry.get(Base64.decodeToIntWithBounds(callbackBufferView, 0, callbackBufferView.length()));
                final CoreProgressReport report = new CoreProgressReport();
                report.loadFromBuffer(it.next());
                cb.on(report);
            }
            break;

            case StorageMessageType.REQ_LOG: {
                workingGraphInstance.setProperty("ws.last", System.currentTimeMillis());
                if (it.hasNext()) {
                    Buffer b = it.next();
                    StringBuilder buf = new StringBuilder();
                    buf.append(Base64.decodeToStringWithBounds(b, 0, b.length()));
                    ((CoreGraphLog) workingGraphInstance.log()).writeMessage(buf);
                }

                final Buffer responseBuffer = workingGraphInstance.newBuffer();
                responseBuffer.write(StorageMessageType.RESP_LOG);
                responseBuffer.write(Constants.BUFFER_SEP);
                responseBuffer.writeAll(respChannelBufferView.data());
                responseBuffer.write(Constants.BUFFER_SEP);
                responseBuffer.writeAll(callbackBufferView.data());
                destMailbox.submit(responseBuffer.data());
                responseBuffer.free();
            }
            break;
            case StorageMessageType.REQ_TASK_STATS: {
                workingGraphInstance.setProperty("ws.last", System.currentTimeMillis());

                final Buffer responseBuffer = workingGraphInstance.newBuffer();
                responseBuffer.write(StorageMessageType.RESP_TASK_STATS);
                responseBuffer.write(Constants.BUFFER_SEP);
                responseBuffer.writeAll(respChannelBufferView.data());
                responseBuffer.write(Constants.BUFFER_SEP);
                responseBuffer.writeAll(callbackBufferView.data());
                responseBuffer.write(Constants.BUFFER_SEP);
                Base64.encodeStringToBuffer(workingGraphInstance.taskContextRegistry().stats(), responseBuffer);
                destMailbox.submit(responseBuffer.data());
                responseBuffer.free();

            }
            break;
            case StorageMessageType.REQ_TASK_STOP: {
                workingGraphInstance.setProperty("ws.last", System.currentTimeMillis());
                if (it.hasNext()) {
                    Buffer view = it.next();
                    int taskCode = Base64.decodeToIntWithBounds(view, 0, view.length());
                    workingGraphInstance.taskContextRegistry().forceStop(taskCode);
                }

                final Buffer responseBuffer = workingGraphInstance.newBuffer();
                responseBuffer.write(StorageMessageType.RESP_TASK_STOP);
                responseBuffer.write(Constants.BUFFER_SEP);
                responseBuffer.writeAll(respChannelBufferView.data());
                responseBuffer.write(Constants.BUFFER_SEP);
                responseBuffer.writeAll(callbackBufferView.data());
                destMailbox.submit(responseBuffer.data());
                responseBuffer.free();
            }
            break;
            case StorageMessageType.HEART_BEAT_PING: {
                final Buffer responseBuffer = workingGraphInstance.newBuffer();
                responseBuffer.write(StorageMessageType.HEART_BEAT_PONG);
                responseBuffer.write(Constants.BUFFER_SEP);
                responseBuffer.writeAll(respChannelBufferView.data());
                responseBuffer.write(Constants.BUFFER_SEP);
                responseBuffer.writeAll(callbackBufferView.data());
                responseBuffer.writeString("ok");
                destMailbox.submit(responseBuffer.data());
                responseBuffer.free();
            }
            break;
            case StorageMessageType.HEART_BEAT_PONG: {
                //Ignore
            }
            break;
            case StorageMessageType.REQ_REMOVE: {
                workingGraphInstance.setProperty("ws.last", System.currentTimeMillis());
                final List<ChunkKey> rkeys = new ArrayList<ChunkKey>();
                while (it.hasNext()) {
                    rkeys.add(KeyHelper.bufferToKey(it.next()));
                }
                process_remove(workingGraphInstance, rkeys.toArray(new ChunkKey[rkeys.size()]), new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        final Buffer responseBuffer = workingGraphInstance.newBuffer();
                        responseBuffer.write(StorageMessageType.RESP_REMOVE);
                        responseBuffer.write(Constants.BUFFER_SEP);
                        responseBuffer.writeAll(respChannelBufferView.data());
                        responseBuffer.write(Constants.BUFFER_SEP);
                        responseBuffer.writeAll(callbackBufferView.data());
                        /*
                        responseBuffer.write(Constants.BUFFER_SEP);
                        Base64.encodeIntToBuffer((result ? 1 : 0), responseBuffer);
                        */
                        destMailbox.submit(responseBuffer.data());
                        responseBuffer.free();
                    }
                });
            }
            break;
            case StorageMessageType.REQ_GET: {
                workingGraphInstance.setProperty("ws.last", System.currentTimeMillis());
                //build keys list
                final List<ChunkKey> keys = new ArrayList<ChunkKey>();
                while (it.hasNext()) {
                    keys.add(KeyHelper.bufferToKey(it.next()));
                }
                process_get(workingGraphInstance, keys.toArray(new ChunkKey[keys.size()]), new Callback<Buffer>() {
                    @Override
                    public void on(Buffer streamResult) {
                        final Buffer responseBuffer = workingGraphInstance.newBuffer();
                        responseBuffer.write(StorageMessageType.RESP_GET);
                        responseBuffer.write(Constants.BUFFER_SEP);
                        responseBuffer.writeAll(respChannelBufferView.data());
                        responseBuffer.write(Constants.BUFFER_SEP);
                        responseBuffer.writeAll(callbackBufferView.data());
                        responseBuffer.write(Constants.BUFFER_SEP);
                        responseBuffer.writeAll(streamResult.data());
                        destMailbox.submit(responseBuffer.data());
                        responseBuffer.free();
                    }
                });
            }
            break;
            case StorageMessageType.REQ_TASK: {
                workingGraphInstance.setProperty("ws.last", System.currentTimeMillis());
                if (it.hasNext()) {
                    final Callback<TaskResult> end = new Callback<TaskResult>() {
                        @Override
                        public void on(TaskResult result) {
                            final Buffer responseBuffer = workingGraphInstance.newBuffer();
                            responseBuffer.write(StorageMessageType.RESP_TASK);
                            responseBuffer.write(Constants.BUFFER_SEP);
                            responseBuffer.writeAll(respChannelBufferView.data());
                            responseBuffer.write(Constants.BUFFER_SEP);
                            responseBuffer.writeAll(callbackBufferView.data());
                            responseBuffer.write(Constants.BUFFER_SEP);
                            result.saveToBuffer(responseBuffer);
                            result.free();
                            taskBuffer.free();
                            destMailbox.submit(responseBuffer.data());
                            responseBuffer.free();
                        }
                    };
                    Task t = Tasks.newTask();
                    try {
                        t.loadFromBuffer(it.next(), workingGraphInstance);
                        TaskContext ctx = t.prepare(workingGraphInstance, null, new Callback<TaskResult>() {
                            @Override
                            public void on(TaskResult result) {
                                //we also dispatch locally
                                if (result.notifications() != null && result.notifications().length() > 0) {
                                    workingGraphInstance.remoteNotify(result.notifications());
                                }
                                end.on(result);
                            }
                        });
                        ctx.silentSave();
                        if (it.hasNext()) {
                            final int printHookCode;
                            Buffer hookCodeView = it.next();
                            if (hookCodeView.length() > 0) {
                                printHookCode = Base64.decodeToIntWithBounds(hookCodeView, 0, hookCodeView.length());
                                ctx.setPrintHook(new Callback<String>() {
                                    @Override
                                    public void on(String result) {

                                        final Buffer printBuffer = workingGraphInstance.newBuffer();
                                        printBuffer.write(StorageMessageType.NOTIFY_PRINT);
                                        printBuffer.write(Constants.BUFFER_SEP);
                                        printBuffer.writeAll(respChannelBufferView.data());
                                        printBuffer.write(Constants.BUFFER_SEP);
                                        Base64.encodeIntToBuffer(printHookCode, printBuffer);
                                        printBuffer.write(Constants.BUFFER_SEP);
                                        Base64.encodeStringToBuffer(result, printBuffer);
                                        destMailbox.submit(printBuffer.data());
                                        printBuffer.free();

                                    }
                                });
                            }
                            final int progressHookCode;
                            Buffer progressHookCodeView = it.next();
                            if (progressHookCodeView.length() > 0) {
                                progressHookCode = Base64.decodeToIntWithBounds(progressHookCodeView, 0, progressHookCodeView.length());
                                ctx.setProgressHook(new Callback<TaskProgressReport>() {
                                    @Override
                                    public void on(TaskProgressReport report) {
                                        final Buffer progressBuffer = workingGraphInstance.newBuffer();
                                        progressBuffer.write(StorageMessageType.NOTIFY_PROGRESS);
                                        progressBuffer.write(Constants.BUFFER_SEP);
                                        progressBuffer.writeAll(respChannelBufferView.data());
                                        progressBuffer.write(Constants.BUFFER_SEP);
                                        Base64.encodeIntToBuffer(progressHookCode, progressBuffer);
                                        progressBuffer.write(Constants.BUFFER_SEP);
                                        report.saveToBuffer(progressBuffer);
                                        destMailbox.submit(progressBuffer.data());
                                        progressBuffer.free();

                                    }
                                });
                            }
                            ctx.loadFromBuffer(it.next(), new Callback<Boolean>() {
                                @Override
                                public void on(Boolean loaded) {
                                    workingGraphInstance.taskContextRegistry().register(ctx);
                                    t.executeUsing(ctx);
                                }
                            });
                        } else {
                            workingGraphInstance.taskContextRegistry().register(ctx);
                            t.executeUsing(ctx);
                        }
                    } catch (Exception e) {
                        end.on(Tasks.emptyResult().setException(e));
                    }
                }
            }
            break;
            case StorageMessageType.REQ_LOCK: {
                workingGraphInstance.setProperty("ws.last", System.currentTimeMillis());
                process_lock(workingGraphInstance, new Callback<Buffer>() {
                    @Override
                    public void on(Buffer result) {

                        final Buffer responseBuffer = workingGraphInstance.newBuffer();
                        responseBuffer.write(StorageMessageType.RESP_LOCK);
                        responseBuffer.write(Constants.BUFFER_SEP);
                        responseBuffer.writeAll(respChannelBufferView.data());
                        responseBuffer.write(Constants.BUFFER_SEP);
                        responseBuffer.writeAll(callbackBufferView.data());
                        responseBuffer.write(Constants.BUFFER_SEP);
                        responseBuffer.writeAll(result.data());
                        result.free();
                        taskBuffer.free();
                        destMailbox.submit(responseBuffer.data());
                        responseBuffer.free();
                    }
                });
            }
            break;
            case StorageMessageType.REQ_UNLOCK: {
                workingGraphInstance.setProperty("ws.last", System.currentTimeMillis());
                process_unlock(workingGraphInstance, it.next(), new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {

                        final Buffer responseBuffer = workingGraphInstance.newBuffer();
                        responseBuffer.write(StorageMessageType.RESP_UNLOCK);
                        responseBuffer.write(Constants.BUFFER_SEP);
                        responseBuffer.writeAll(respChannelBufferView.data());
                        responseBuffer.write(Constants.BUFFER_SEP);
                        responseBuffer.writeAll(callbackBufferView.data());
                        responseBuffer.write(Constants.BUFFER_SEP);
                        Base64.encodeIntToBuffer((result ? 1 : 0), responseBuffer);
                        taskBuffer.free();
                        destMailbox.submit(responseBuffer.data());
                        responseBuffer.free();

                    }
                });
            }
            break;
            case StorageMessageType.REQ_PUT: {
                workingGraphInstance.setProperty("ws.last", System.currentTimeMillis());
                final List<ChunkKey> flatKeys = new ArrayList<ChunkKey>();
                final List<Buffer> flatValues = new ArrayList<Buffer>();
                while (it.hasNext()) {
                    final Buffer keyView = it.next();
                    final Buffer valueView = it.next();
                    if (valueView != null) {
                        flatKeys.add(KeyHelper.bufferToKey(keyView));
                        flatValues.add(valueView);
                    }
                }
                final ChunkKey[] collectedKeys = flatKeys.toArray(new ChunkKey[flatKeys.size()]);
                process_put(workingGraphInstance, collectedKeys, flatValues.toArray(new Buffer[flatValues.size()]), new Job() {
                    @Override
                    public void run() {
                        workingGraphInstance.save(new Callback<Boolean>() {
                            @Override
                            public void on(Boolean result) {
                                final Buffer responseBuffer = workingGraphInstance.newBuffer();
                                responseBuffer.write(StorageMessageType.RESP_PUT);
                                responseBuffer.write(Constants.BUFFER_SEP);
                                responseBuffer.writeAll(respChannelBufferView.data());
                                responseBuffer.write(Constants.BUFFER_SEP);
                                responseBuffer.writeAll(callbackBufferView.data());
                                responseBuffer.write(Constants.BUFFER_SEP);
                                Base64.encodeIntToBuffer((result ? 1 : 0), responseBuffer);
                                taskBuffer.free();
                                destMailbox.submit(responseBuffer.data());
                                responseBuffer.free();
                            }
                        });
                    }
                });
            }
            break;
            default: {
                System.err.println("Operation not yet supported " + (int) operation);
            }
        }
    }


    private void process_lock(Graph graph, Callback<Buffer> callback) {
        workingGraphInstance.storage().lock(callback);
    }

    private void process_unlock(Graph graph, Buffer toUnlock, Callback<Boolean> callback) {
        workingGraphInstance.storage().unlock(toUnlock, callback);
    }

    private void process_put(final Graph graph, final ChunkKey[] keys, final Buffer[] values, Job job) {
        final DeferCounter defer = workingGraphInstance.newCounter(keys.length);
        defer.then(job);
        for (int i = 0; i < keys.length; i++) {
            final int finalI = i;
            ChunkKey tuple = keys[i];
            workingGraphInstance.space().getOrLoadAndMark(tuple.type, tuple.world, tuple.time, tuple.id, new Callback<Chunk>() {
                @Override
                public void on(Chunk memoryChunk) {
                    if (memoryChunk != null) {
                        memoryChunk.loadDiff(values[finalI]);
                        workingGraphInstance.space().unmark(memoryChunk.index());
                    } else {
                        Chunk newChunk = workingGraphInstance.space().createAndMark(tuple.type, tuple.world, tuple.time, tuple.id);
                        if (newChunk != null) {
                            newChunk.loadDiff(values[finalI]);
                            workingGraphInstance.space().unmark(newChunk.index());
                        }
                    }
                    defer.count();
                }
            });
        }
    }

    private void process_remove(final Graph graph, ChunkKey[] keys, final Callback<Boolean> callback) {
        Buffer buffer = workingGraphInstance.newBuffer();
        for (int i = 0; i < keys.length; i++) {
            if (i != 0) {
                buffer.write(Constants.BUFFER_SEP);
            }
            ChunkKey tuple = keys[i];
            KeyHelper.chunckKeyToBuffer(tuple, buffer);
            workingGraphInstance.space().delete(tuple.type, tuple.world, tuple.time, tuple.id);
        }
        workingGraphInstance.storage().remove(buffer, new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                buffer.free();
                callback.on(result);
            }
        });
    }

    private void process_get(final Graph graph, ChunkKey[] keys, final Callback<Buffer> callback) {
        final DeferCounter defer = workingGraphInstance.newCounter(keys.length);
        final Buffer[] buffers = new Buffer[keys.length];
        defer.then(new Job() {
            @Override
            public void run() {
                Buffer stream = workingGraphInstance.newBuffer();
                for (int i = 0; i < buffers.length; i++) {
                    if (i != 0) {
                        stream.write(Constants.BUFFER_SEP);
                    }
                    if (buffers[i] != null) {
                        stream.writeAll(buffers[i].data());
                        buffers[i].free();
                    }
                }
                callback.on(stream);
            }
        });
        for (int i = 0; i < keys.length; i++) {
            final int fixedI = i;
            ChunkKey tuple = keys[i];
            workingGraphInstance.space().getOrLoadAndMark(tuple.type, tuple.world, tuple.time, tuple.id, new Callback<Chunk>() {
                @Override
                public void on(Chunk memoryChunk) {
                    if (memoryChunk != null) {
                        final Buffer toSaveBuffer = workingGraphInstance.newBuffer();
                        memoryChunk.save(toSaveBuffer);
                        workingGraphInstance.space().unmark(memoryChunk.index());
                        buffers[fixedI] = toSaveBuffer;
                    } else {
                        buffers[fixedI] = null;
                    }
                    defer.count();
                }
            });
        }
    }

    public void submitTask(final Task requestedTask, final Callback<TaskResult> requesterCallback) {

        int taskCallbackId = callbacksRegistry.register(new Callback<Buffer>() {
            @Override
            public void on(Buffer response) {
                final BaseTaskResult baseTaskResult = new BaseTaskResult(null, false);
                final L3GMap<List<Tuple<Object[], Integer>>> collector = new L3GMap<List<Tuple<Object[], Integer>>>(true);
                baseTaskResult.load(response, 0, workingGraphInstance, collector);
                workingGraphInstance.remoteNotify(baseTaskResult.notifications());
                baseTaskResult.loadRefs(workingGraphInstance, collector, new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        response.free();
                        requesterCallback.on(baseTaskResult);
                    }
                });
            }
        });

        Buffer taskBuffer = new HeapBuffer();
        //Type REQ_TASK
        taskBuffer.write(StorageMessageType.REQ_TASK);
        taskBuffer.write(Constants.BUFFER_SEP);
        //Resp. Channel
        taskBuffer.writeInt(this.mailboxId);
        taskBuffer.write(Constants.BUFFER_SEP);
        //Callback.id
        Base64.encodeIntToBuffer(taskCallbackId, taskBuffer);
        taskBuffer.write(Constants.BUFFER_SEP);
        requestedTask.saveToBuffer(taskBuffer);

        mailbox.submit(taskBuffer.data());

    }

    public void submitPreparedTask(final Task requestedTask, final TaskContext requestedTaskContext) {

        final int printHookCallbackId;
        final int progressHookCallbackId;
        final int taskCallbackId;

        if (requestedTaskContext != null) {
            final Callback<String> printHook = requestedTaskContext.printHook();
            if (printHook != null) {
                printHookCallbackId = callbacksRegistry.register(printHook);
            } else {
                printHookCallbackId = -1;
            }

            final Callback<TaskProgressReport> progressHook = requestedTaskContext.progressHook();
            if (progressHook != null) {
                progressHookCallbackId = callbacksRegistry.register(progressHook);
            } else {
                progressHookCallbackId = -1;
            }
        } else {
            printHookCallbackId = -1;
            progressHookCallbackId = -1;
        }

        Callback<Buffer> onResult = new Callback<Buffer>() {
            @Override
            public void on(Buffer buffer) {
                if (printHookCallbackId != -1) {
                    //callbacksRegistry.remove(printHookCallbackId);
                }
                if (progressHookCallbackId != -1) {
                    //callbacksRegistry.remove(progressHookCallbackId);
                }

                final BaseTaskResult baseTaskResult = new BaseTaskResult(null, false);
                final L3GMap<List<Tuple<Object[], Integer>>> collector = new L3GMap<List<Tuple<Object[], Integer>>>(true);
                baseTaskResult.load(buffer, 0, workingGraphInstance, collector);
                workingGraphInstance.remoteNotify(baseTaskResult.notifications());
                baseTaskResult.loadRefs(workingGraphInstance, collector, new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        buffer.free();
                        requestedTaskContext.getResultCallback().on(baseTaskResult);
                    }
                });
            }
        };
        taskCallbackId = callbacksRegistry.register(onResult);

        Buffer taskBuffer = new HeapBuffer();
        //Type REQ_TASK
        taskBuffer.write(StorageMessageType.REQ_TASK);
        taskBuffer.write(Constants.BUFFER_SEP);
        //Resp. Channel
        taskBuffer.writeInt(mailboxId);
        taskBuffer.write(Constants.BUFFER_SEP);
        //Callback.id
        Base64.encodeIntToBuffer(taskCallbackId, taskBuffer);
        taskBuffer.write(Constants.BUFFER_SEP);
        requestedTask.saveToBuffer(taskBuffer);

        if (requestedTaskContext != null) {
            taskBuffer.write(Constants.BUFFER_SEP);
            if (printHookCallbackId != -1) {
                Base64.encodeIntToBuffer(printHookCallbackId, taskBuffer);
            }
            taskBuffer.write(Constants.BUFFER_SEP);
            if (progressHookCallbackId != -1) {
                Base64.encodeIntToBuffer(progressHookCallbackId, taskBuffer);
            }
            taskBuffer.write(Constants.BUFFER_SEP);
            requestedTaskContext.saveToBuffer(taskBuffer);
        }

        mailbox.submit(taskBuffer.data());
    }

}
