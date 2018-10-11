/**
 * Copyright 2017-2018 The GreyCat Authors.  All rights reserved.
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
package greycat.scheduler;

import greycat.plugin.Job;
import greycat.plugin.Scheduler;
import greycat.plugin.SchedulerAffinity;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @ignore ts
 */
public class HybridScheduler implements Scheduler {

    private Worker[] _workers = null;
    private final BlockingDeque<Job> globalQueue = new LinkedBlockingDeque<Job>();
    private int nbWorkers = -1;

    @Override
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
        int nbcore = this.nbWorkers;
        if (nbcore == -1) {
            nbcore = Runtime.getRuntime().availableProcessors();
        }
        _workers = new Worker[nbcore];
        for (int i = 0; i < _workers.length; i++) {
            _workers[i] = new Worker();
            _workers[i].start();
        }
    }

    @Override
    public void stop() {
        if (_workers != null) {
            for (int i = 0; i < _workers.length; i++) {
                _workers[i].running = false;
            }
            _workers = null;
        }
    }

    @Override
    public int workers() {
        return _workers.length;
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
            while (running) {
                Job globalPolled = null;
                try {
                    globalPolled = globalQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (globalPolled != null) {
                    try {
                        globalPolled.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void dispatch(Job job) {
            localQueue.add(job);
            if (wip.getAndIncrement() == 0) {
                do {
                    final Job polled = localQueue.poll();
                    if (polled != null) {
                        polled.run();
                    }
                } while (wip.decrementAndGet() > 0);
            }
        }

    }

}
