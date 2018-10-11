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
package greycat;

import greycat.plugin.Job;

/**
 * DeferCounter provides a mean to wait for an amount of events before running a method.
 */
public interface DeferCounter {

    /**
     * Notifies the counter that an awaited event has occurred.<br>
     * If the total amount of awaited events is reached, the task registered by the {@link #then(Job) then} method is executed.
     */
    void count();

    /**
     * Get the number of events still expected
     * @return the number of events still expected
     */
    int getCount();

    /**
     * Registers the task, in form of a {@link Job}, to be called when all awaited events have occurred.
     * @param job The task to be executed
     */
    void then(Job job);

    /**
     * Wrap into a callback.
     * @return the callback
     */
    Callback wrap();

}
