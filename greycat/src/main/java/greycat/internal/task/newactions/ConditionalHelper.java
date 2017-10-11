/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
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
package greycat.internal.task.newactions;

import greycat.ConditionalFunction;
import greycat.TaskContext;

public class ConditionalHelper {

    public static final ConditionalFunction notEmpty = new ConditionalFunction() {
        @Override
        public boolean eval(TaskContext ctx) {
            return (ctx.result() != null && ctx.result().size() > 0);
        }
    };


    public static final ConditionalFunction empty = new ConditionalFunction() {
        @Override
        public boolean eval(TaskContext ctx) {
            return (ctx.result() == null || ctx.result().size() == 0);
        }
    };
}
