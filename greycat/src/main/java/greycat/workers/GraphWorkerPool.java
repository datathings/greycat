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

import greycat.GraphBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @ignore ts
 */
public class GraphWorkerPool {

    private static Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            System.err.println("Uncaught exception handler !!!");
            e.printStackTrace();
        }
    };

    private static volatile GraphWorkerPool INSTANCE;
    private static Object mutex = new Object();

    public static GraphWorkerPool getInstance() {
        GraphWorkerPool result = INSTANCE;
        if (result == null) {
            synchronized (mutex) {
                result = INSTANCE;
                if (result == null)
                    INSTANCE = result = new GraphWorkerPool();
            }
        }
        return result;
    }

    public static void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler handler) {
        exceptionHandler = handler;
        INSTANCE.resetExceptionsHandler();
    }

    private ThreadGroup workersThreadGroup;
    private GraphWorker rootGraphWorker;
    private Thread rootGraphWorkerThread;
    private Map<Integer, GraphWorker> workersById = new HashMap<>();
    private Map<Integer, Thread> threads = new HashMap<>();
    private Map<String, GraphWorker> workersByRef = new HashMap<>();

    private GraphBuilder rootBuilder;

    private GraphWorkerPool() {
    }

    public void initialize(GraphBuilder rootBuilder) {
        this.rootBuilder = rootBuilder;

        workersThreadGroup = new ThreadGroup("GreyCat workersById group");

        //ROOT GRAPH
        rootGraphWorker = new GraphWorker(rootBuilder, "RootWorker");

        rootGraphWorkerThread = new Thread(rootGraphWorker, "RootWorker_" + rootGraphWorker.getMailboxId());
        rootGraphWorkerThread.setUncaughtExceptionHandler(exceptionHandler);
        rootGraphWorkerThread.start();
    }

    public int getRootWorkerMailboxId() {
        return rootGraphWorker.getMailboxId();
    }

    public void halt() {
       //System.out.println("Halting workersById");
        workersById.forEach(new BiConsumer<Integer, GraphWorker>() {
            @Override
            public void accept(Integer id, GraphWorker worker) {
                worker.halt();
            }
        });
        //System.out.println("Waiting threads");
        threads.forEach(new BiConsumer<Integer, Thread>() {
            @Override
            public void accept(Integer id, Thread thread) {
                try {
                    thread.interrupt();
                    thread.join(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        //System.out.println("Halting root graph worker");
        rootGraphWorker.halt();
        try {
            //System.out.println("Waiting root thread");
            rootGraphWorkerThread.interrupt();
            rootGraphWorkerThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //System.out.println("Halting done.");
    }

    public int createGraphWorker() {
        return createGraphWorkerWithBuilderAndRef(rootBuilder, null);
    }

    public int createGraphWorkerWithRef(String ref) {
        return createGraphWorkerWithBuilderAndRef(rootBuilder, ref);
    }

    public int createGraphWorkerWithBuilder(GraphBuilder builder) {
        return createGraphWorkerWithBuilderAndRef(builder, null);
    }

    public int createGraphWorkerWithBuilderAndRef(GraphBuilder builder, String ref) {

        GraphBuilder slaveWorkerBuilder = builder.clone().withStorage(new SlaveWorkerStorage());

        GraphWorker worker = new GraphWorker(slaveWorkerBuilder);
        worker.setWorker(true);
        worker.setName(ref == null ? "Worker" + worker.getMailboxId() : ref);
        workersById.put(worker.getMailboxId(), worker);
        workersByRef.put(worker.getName(), worker);

        Thread workerThread = new Thread(workersThreadGroup, worker, worker.getName());
        workerThread.setUncaughtExceptionHandler(exceptionHandler);
        workerThread.start();
        threads.put(worker.getMailboxId(), workerThread);

        return worker.getMailboxId();
    }

    public void destroyWorkerById(int id) {
        GraphWorker worker = workersById.get(id);
        if (worker != null) {
            workersByRef.remove(worker.getName());
            workersById.remove(worker.getMailboxId());
            worker.halt();
            Thread workerThread = threads.remove(worker.getMailboxId());
            if (workerThread != null) {
                try {
                    workerThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void destroyWorkerByRef(String ref) {
        GraphWorker w = workersByRef.get(ref);
        if (w != null) {
            destroyWorkerById(w.getMailboxId());
        }
    }

    private void resetExceptionsHandler() {
        rootGraphWorkerThread.setUncaughtExceptionHandler(exceptionHandler);
        threads.forEach(new BiConsumer<Integer, Thread>() {
            @Override
            public void accept(Integer id, Thread thread) {
                thread.setUncaughtExceptionHandler(exceptionHandler);
            }
        });
    }


}
