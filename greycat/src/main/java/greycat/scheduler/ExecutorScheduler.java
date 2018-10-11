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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @ignore ts
 */
public class ExecutorScheduler implements Scheduler {

    private ExecutorService service;
    private int _workers = -1;

    @Override
    public void dispatch(final byte affinity, final Job job) {
        service.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    job.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void start() {
        if (_workers == -1) {
            this.service = Executors.newCachedThreadPool();
        } else {
            this.service = Executors.newWorkStealingPool(_workers);
        }
    }

    @Override
    public void stop() {
        this.service.shutdown();
        this.service = null;
    }

    @Override
    public int workers() {
        return _workers;
    }

}
