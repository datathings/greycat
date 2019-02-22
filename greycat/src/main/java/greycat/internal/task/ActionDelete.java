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
package greycat.internal.task;

import greycat.*;
import greycat.base.BaseNode;
import greycat.plugin.Job;
import greycat.struct.Buffer;

class ActionDelete implements Action {

    @Override
    public void eval(final TaskContext ctx) {
        TaskResult previous = ctx.result();
        DeferCounter counter = ctx.graph().newCounter(previous.size());
        for (int i = 0; i < previous.size(); i++) {
            if (previous.get(i) instanceof BaseNode) {
                ((Node) previous.get(i)).drop(new Callback() {
                    @Override
                    public void on(Object result) {
                        counter.count();
                    }
                });
            }
        }
        counter.then(new Job() {
            @Override
            public void run() {
                previous.clear();
                ctx.continueTask();
            }
        });
    }

    @Override
    public void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.DELETE);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.DELETE;
    }

}
