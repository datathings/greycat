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
package greycat.internal;

import greycat.Callback;
import greycat.DeferCounterSync;
import greycat.plugin.Job;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class CoreDeferCounterSync implements DeferCounterSync {

    private final AtomicInteger _nb_down;

    private final int _counter;

    /**
     * @ignore ts
     */
    private final CountDownLatch _latch;

    private Job _end;

    /**
     * @native ts
     * this._counter = nb;
     * this._nb_down = new java.util.concurrent.atomic.AtomicInteger(0);
     */
    public CoreDeferCounterSync(int nb) {
        this._counter = nb;
        this._nb_down = new AtomicInteger(0);
        this._latch = new CountDownLatch(nb);
    }

    /**
     * @native ts
     * this._nb_down.set(this._nb_down.get()+1);
     * if(this._nb_down.get() == this._counter){
     *     if (this._end != null) {this._end();}
     * }
     */
    @Override
    public void count() {
        this._latch.countDown();
        int previous;
        int next;
        do {
            previous = this._nb_down.get();
            next = previous + 1;
        } while (!this._nb_down.compareAndSet(previous, next));
        if (next == _counter) {
            if (_end != null) {
                _end.run();
            }
        }
    }

    @Override
    public int getCount() {
        return _nb_down.get();
    }

    @Override
    public void then(Job p_callback) {
        this._end = p_callback;
        if (this._nb_down.get() == _counter) {
            if (p_callback != null) {
                p_callback.run();
            }
        }
    }

    private Object _result = null;

    @Override
    public Callback wrap() {
        return new Callback() {
            @Override
            public void on(Object result) {
                _result = result;
                count();
            }
        };
    }

    /**
     * @native ts
     * while (this._nb_down.get() != this._counter) {
     *  //loop
     * }
     * return this._result;
     */
    @Override
    public Object waitResult() {
        if (this._nb_down.get() == _counter) {
            return _result;
        }
        try {
            this._latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return _result;
    }
}
