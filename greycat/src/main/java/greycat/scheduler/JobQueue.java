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

public class JobQueue {

    private JobQueueElem first = null;
    private JobQueueElem last = null;

    public void add(final Job item) {
        final JobQueueElem elem = new JobQueueElem(item, null);
        if (first == null) {
            first = elem;
            last = elem;
        } else {
            last._next = elem;
            last = elem;
        }
    }

    public Job poll() {
        final JobQueueElem value = first;
        first = first._next;
        return value._ptr;
    }

    private class JobQueueElem {
        final Job _ptr;
        JobQueueElem _next;

        private JobQueueElem(final Job ptr, final JobQueueElem next) {
            this._ptr = ptr;
            this._next = next;
        }
    }

}