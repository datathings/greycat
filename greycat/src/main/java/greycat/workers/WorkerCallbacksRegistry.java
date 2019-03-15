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

import greycat.Callback;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @ignore ts
 */
public class WorkerCallbacksRegistry {

    public WorkerCallbacksRegistry() {
    }
    private static final int MIN_INTEGER = -2147483648;
    private static final int MAX_INTEGER = 2147483647;
    private HashMap<Integer, Callback> callbacks = new HashMap<>();
    private AtomicInteger callbackIds = new AtomicInteger(MIN_INTEGER);

    public int register(Callback cb) {
        int id = callbackIds.getAndIncrement();
        if (id == MAX_INTEGER) {
            callbackIds.set(MIN_INTEGER);
        }
        callbacks.put(id, cb);
        return id;
    }

    public Callback get(int callbackId) {
        return callbacks.get(callbackId);
    }

    public Callback remove(int callbackId) {
        return callbacks.remove(callbackId);
    }

}
