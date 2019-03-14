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
     *
     * @param taskBuffer
     * @return true if the task has been queued; false otherwise.
     */
    public boolean submit(byte[] taskBuffer) {
        return tasksQueue.offer(taskBuffer);
    }

    /**
     *
     * @param c
     * @return
     */
    public boolean addAll(Collection<? extends byte[]> c) {
        return tasksQueue.addAll(c);
    }

    /**
     * Non-Blocking retrieve of message
     * @return
     */
    public byte[] poll() {
        return tasksQueue.poll();
    }

    /**
     * Blocking retrieve of message
     *
     * @return
     * @throws InterruptedException
     */
    public byte[] take() throws InterruptedException {
        return tasksQueue.take();
    }

    /**
     * Blocking retrieve of message, with timeout
     *
     * @return
     * @throws InterruptedException
     */
    public byte[] poll(long timeout, TimeUnit unit) throws InterruptedException {

        return tasksQueue.poll(timeout, unit);
    }

}
