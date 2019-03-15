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
package greycatTest.workers;

import greycat.*;
import greycat.base.BaseTaskResult;
import greycat.internal.heap.HeapBuffer;
import greycat.struct.Buffer;
import greycat.utility.Base64;
import greycat.utility.L3GMap;
import greycat.utility.Tuple;
import greycat.workers.GraphWorker;
import greycat.workers.MailboxRegistry;
import greycat.workers.StorageMessageType;
import greycat.workers.WorkerMailbox;

import java.util.List;

/**
 * @ignore ts
 */
public class TestGraphWorker extends GraphWorker {


    public TestGraphWorker(GraphBuilder workingGraphBuilder, boolean canProcessGeneralTaskQueue) {
        super(workingGraphBuilder, canProcessGeneralTaskQueue);
    }

    public TestGraphWorker(GraphBuilder workingGraphBuilder, String name, boolean canProcessGeneralTaskQueue) {
        super(workingGraphBuilder, name, canProcessGeneralTaskQueue);
    }

    public Graph getWorkingGraph() {
        return workingGraphInstance;
    }

    public void submitTask(final Task requestedTask, final Callback<TaskResult> requesterCallback, final int destMailboxId) {

        WorkerMailbox destMailbox = MailboxRegistry.getInstance().getMailbox(destMailboxId);

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
        Base64.encodeIntToBuffer(this.mailboxId, taskBuffer);
        taskBuffer.write(Constants.BUFFER_SEP);
        //Callback.id
        Base64.encodeIntToBuffer(taskCallbackId, taskBuffer);
        taskBuffer.write(Constants.BUFFER_SEP);
        requestedTask.saveToBuffer(taskBuffer);

        // --------   DEBUG PRINT ----------
/*
        final BufferIterator it = taskBuffer.iterator();
        final Buffer bufferTypeBufferView = it.next();
        final Buffer respChannelBufferView = it.next();
        final Buffer callbackBufferView = it.next();

        System.out.println("========= TestCase Sends =========");
        System.out.println("Type: " + MessageTypes.toString(bufferTypeBufferView.read(0)));
        System.out.println("Channel: " + Base64.decodeToIntWithBounds(respChannelBufferView, 0, respChannelBufferView.length()));
        System.out.println("Callback: " + Base64.decodeToIntWithBounds(callbackBufferView, 0, callbackBufferView.length()));
        System.out.println("Raw: " + taskBuffer.toString());
*/
        // --------   DEBUG PRINT END ----------

        destMailbox.submit(taskBuffer.data());
        if (destMailboxId == MailboxRegistry.getInstance().getDefaultMailboxId()) {
            MailboxRegistry.getInstance().notifyMailboxes();
        }
    }

    public void submitPreparedTask(final Task requestedTask, final TaskContext requestedTaskContext, final int destMailboxId) {

        WorkerMailbox destMailbox = MailboxRegistry.getInstance().getMailbox(destMailboxId);

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
        Base64.encodeIntToBuffer(mailboxId, taskBuffer);
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

        // --------   DEBUG PRINT ----------
/*
        final BufferIterator it = taskBuffer.iterator();
        final Buffer bufferTypeBufferView = it.next();
        final Buffer respChannelBufferView = it.next();
        final Buffer callbackBufferView = it.next();

        System.out.println("========= TestClass Sends =========");
        System.out.println("Type: " + MessageTypes.toString(bufferTypeBufferView.read(0)));
        System.out.println("Channel: " + Base64.decodeToIntWithBounds(respChannelBufferView, 0, respChannelBufferView.length()));
        System.out.println("Callback: " + Base64.decodeToIntWithBounds(callbackBufferView, 0, callbackBufferView.length()));
        System.out.println("Raw: " + taskBuffer.toString());

        // --------   DEBUG PRINT END ----------
*/
        destMailbox.submit(taskBuffer.data());
        if (destMailboxId == MailboxRegistry.getInstance().getDefaultMailboxId()) {
            MailboxRegistry.getInstance().notifyMailboxes();
        }
    }
}
