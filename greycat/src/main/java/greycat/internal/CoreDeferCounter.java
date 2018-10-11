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

import greycat.DeferCounter;
import greycat.plugin.Job;
import greycat.Callback;

import java.util.concurrent.atomic.AtomicInteger;

public class CoreDeferCounter implements DeferCounter {

    private final AtomicInteger _nb_down;

    private final int _counter;

    private Job _end;

    public CoreDeferCounter(int nb) {
        this._counter = nb;
        this._nb_down = new AtomicInteger(0);
    }

    @Override
    public void count() {
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

    @Override
    public Callback wrap() {
        return new Callback() {
            @Override
            public void on(Object result) {
                count();
            }
        };
    }

}
