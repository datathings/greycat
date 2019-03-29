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
import greycat.Log;
import greycat.internal.CoreGraphLog;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @ignore ts
 */
public class GraphWorkerPool {

    private static Log logger = new CoreGraphLog(null);

    private static Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            logger.error("Uncaught exception handler !!!", e);
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
        rootGraphWorker = new GraphWorker(rootBuilder, "RootWorker", false);

        rootGraphWorkerThread = new Thread(rootGraphWorker, "RootWorker_" + rootGraphWorker.getId());
        rootGraphWorkerThread.setUncaughtExceptionHandler(exceptionHandler);
        rootGraphWorkerThread.start();
    }

    public int getRootWorkerMailboxId() {
        return rootGraphWorker.getId();
    }

    public void halt() {
        logger.info("Halting workers pool...");
        workersById.forEach(new BiConsumer<Integer, GraphWorker>() {
            @Override
            public void accept(Integer id, GraphWorker worker) {
                worker.halt();
            }
        });
        logger.debug("Waiting threads");
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
        workersById.clear();
        workersByRef.clear();

        logger.debug("Halting root graph worker");
        rootGraphWorker.halt();
        try {
            logger.debug("Waiting root thread");
            rootGraphWorkerThread.interrupt();
            rootGraphWorkerThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("Halting done.");
    }

    public GraphWorker createGraphWorker(byte workerKind) {
        return createGraphWorkerWithBuilderAndRef(workerKind, rootBuilder, null);
    }

    public GraphWorker createGraphWorkerWithRef(byte workerKind, String ref) {
        return createGraphWorkerWithBuilderAndRef(workerKind, rootBuilder, ref);
    }

    public GraphWorker createGraphWorkerWithBuilder(byte workerKind, GraphBuilder builder) {
        return createGraphWorkerWithBuilderAndRef(workerKind, builder, null);
    }

    public GraphWorker createGraphWorkerWithBuilderAndRef(byte workerKind, GraphBuilder builder, String ref) {

        GraphBuilder slaveWorkerBuilder = builder.clone().withStorage(new SlaveWorkerStorage());

        GraphWorker worker = new GraphWorker(slaveWorkerBuilder, workerKind == WorkerAffinity.GENERAL_PURPOSE_WORKER);

        worker.setName(ref == null ? "Worker" + worker.getId() : ref);
        workersById.put(worker.getId(), worker);
        workersByRef.put(worker.getName(), worker);

        Thread workerThread = new Thread(workersThreadGroup, worker, worker.getName());
        workerThread.setUncaughtExceptionHandler(exceptionHandler);
        workerThread.start();
        threads.put(worker.getId(), workerThread);

        logger.info("Worker " + worker.getName() + "(" + worker.getId() + ") created.");

        return worker;
    }

    public void destroyWorkerById(int id) {

        GraphWorker worker = workersById.get(id);
        if (worker != null) {
            workersByRef.remove(worker.getName());
            workersById.remove(worker.getId());
            worker.halt();
            Thread workerThread = threads.remove(worker.getId());
            if (workerThread != null) {
                try {
                    workerThread.interrupt();
                    workerThread.join();
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
            logger.info("Worker " + worker.getName() + "(" + worker.getId() + ") destroyed.");
        } else {
            logger.warn("Asked for destruction of worker id: " + id + " but the worker was not found.");
        }
    }

    public void destroyWorkerByRef(String ref) {
        GraphWorker w = workersByRef.get(ref);
        if (w != null) {
            destroyWorkerById(w.getId());
        }
    }

    public GraphWorker getWorkerById(int id) {
        return workersById.get(id);
    }

    public GraphWorker getWorkerByRef(String ref) {
        return workersByRef.get(ref);
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
