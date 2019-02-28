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
package greycat.multithread.websocket.buffergraph;

import greycat.Callback;
import greycat.multithread.websocket.Constants;
import greycat.multithread.websocket.message.GraphMessage;
import greycat.plugin.Job;
import greycat.plugin.Scheduler;
import greycat.plugin.SchedulerAffinity;
import greycat.scheduler.JobQueue;


import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom Scheduler based on the hybrid scheduler that handle read from message queue, should always be used in combination with the Buffer Storage
 */
public class BufferScheduler implements Scheduler {


    private Worker _worker = null;
    //private ReaderWorker _workerRead = null;
    private final BlockingDeque<Job> globalQueue = new LinkedBlockingDeque<Job>();

    protected final BlockingQueue<GraphMessage> incomingMessages = new LinkedBlockingQueue<>();
    private final Map<Integer, Callback> callbackMap = new ConcurrentHashMap<>();

    public void dispatch(byte affinity, Job job) {
        switch (affinity) {
            case SchedulerAffinity.SAME_THREAD:
                final Thread currentThread = Thread.currentThread();
                if (Thread.currentThread() instanceof Worker) {
                    final Worker currentWorker = (Worker) currentThread;
                    currentWorker.dispatch(job);
                } else {
                    globalQueue.add(job);
                }
                break;
            default:
                globalQueue.add(job);
                break;
        }
    }

    @Override
    public void start() {
        _worker = new Worker();
        _worker.start();
    }

    @Override
    public void stop() {
        if (_worker != null) {
            _worker.running = false;
        }
        _worker = null;
    }


    @Override
    public int workers() {
        return 1;
    }

    private final class Worker extends Thread {

        private final JobQueue localQueue = new JobQueue();
        private final AtomicInteger wip = new AtomicInteger();
        private boolean running = true;

        Worker() {
            setDaemon(false);
        }

        @Override
        public void run() {
            GraphMessage incomingMessage;
            while (running) {
                Job globalPolled = globalQueue.poll();
                if (globalPolled != null) {
                    try {
                        globalPolled.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                while ((incomingMessage = incomingMessages.poll()) != null) {
                    handleMessage(incomingMessage);
                }
            }
        }

        public void dispatch(Job job) {
            GraphMessage incomingMessage;
            localQueue.add(job);
            if (wip.getAndIncrement() == 0) {
                do {
                    final Job polled = localQueue.poll();
                    if (polled != null) {
                        polled.run();
                    }
                    while ((incomingMessage = incomingMessages.poll()) != null) {
                        handleMessage(incomingMessage);
                    }
                } while (wip.decrementAndGet() > 0);
            }
        }

        void handleMessage(GraphMessage message) {
            final Callback resolvedCallback = callbackMap.get(message.getReturnID());
            callbackMap.remove(message.getReturnID());
            switch (message.getOperationId()) {
                case Constants.RESP_GET:
                case Constants.RESP_LOCK:
                    resolvedCallback.on(message.getContent());
                    break;
                case Constants.RESP_PUT:
                case Constants.RESP_REMOVE:
                case Constants.RESP_LOG:
                case Constants.RESP_UNLOCK:
                    resolvedCallback.on(true);
                    break;
            }
        }
    }



    public BlockingQueue<GraphMessage> getIncomingMessages() {
        return incomingMessages;
    }

    public Map<Integer, Callback> getCallbackMap() {
        return callbackMap;
    }


}
