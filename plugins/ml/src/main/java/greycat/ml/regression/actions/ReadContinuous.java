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
package greycat.ml.regression.actions;

import greycat.*;
import greycat.internal.task.CoreActions;
import greycat.internal.task.TaskHelper;
import greycat.ml.regression.PolynomialNode;
import greycat.struct.Buffer;

public class ReadContinuous implements Action {

    public final static String NAME = "readContinuous";
    private final Task polyTask;
    private final String _relName;

    public ReadContinuous(final String relName) {
        if (relName == null) {
            throw new RuntimeException("name should not be null");
        }
        this._relName = relName;

        polyTask = Tasks.newTask()
                .then(CoreActions.traverse(relName))
                .then(CoreActions.attribute(PolynomialNode.VALUE));
    }


    @Override
    public void eval(final TaskContext context) {

        polyTask.executeWith(context.graph(), context.result(), new Callback<TaskResult>() {
            @Override
            public void on(TaskResult result) {
                context.continueWith(result);
            }
        });
    }


    @Override
    public final void serialize(Buffer builder) {
        builder.writeString(NAME);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_relName, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return NAME;
    }

}
