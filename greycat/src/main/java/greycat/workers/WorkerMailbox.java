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

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @ignore ts
 */
public class WorkerMailbox {

    private boolean isWorker;

    private BlockingQueue<byte[]> tasksQueue = new LinkedBlockingQueue<byte[]>();

    public boolean isworker() {
        return isWorker;
    }

    public WorkerMailbox setWorker(boolean worker) {
        isWorker = worker;
        return this;
    }

    /**
     * Submits a work task in form of a byte array to the queue.
     * @param taskBuffer the work task to queue.
     * @return true if the task has been queued; false otherwise.
     */
    public boolean submit(byte[] taskBuffer) {
        return tasksQueue.offer(taskBuffer);
    }

    /**
     * Enqueues all the working tasks given as parameter.
     * @param c the collection of work tasks to queue
     * @return true if all have been added; false otherwise.
     */
    public boolean addAll(Collection<? extends byte[]> c) {
        return tasksQueue.addAll(c);
    }

    /**
     * Non-Blocking retrieve of message
     * @return a working task submitted to the queue; null is the queue is empty.
     */
    public byte[] poll() {
        return tasksQueue.poll();
    }

    /**
     * Blocking retrieve of message
     *
     * @return a working task submitted to the queue.
     * @throws InterruptedException In case of interruption while waiting
     */
    public byte[] take() throws InterruptedException {
        return tasksQueue.take();
    }

    /**
     * Blocking retrieve of message, with timeout.
     *
     * @param timeout the quantity of time to wait
     * @param unit the unit of the quantity of time to wait
     * @return a working task submitted to the queue; null if returned on timeout
     * @throws InterruptedException In case of interruption while waiting
     */
    public byte[] poll(long timeout, TimeUnit unit) throws InterruptedException {
        return tasksQueue.poll(timeout, unit);
    }

}
