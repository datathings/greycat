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
import greycat.internal.task.TaskHelper;
import greycat.ml.regression.PolynomialNode;
import greycat.struct.Buffer;

import static greycat.Tasks.newTask;
import static greycat.internal.task.CoreActions.*;

public class SetPrecision implements Action {

    public final static String NAME = "setPrecision";
    private final Task polyTask;

    private final String _relName;
    private final String _value;

    public SetPrecision(final String relName, final String c_value) {
        if (relName == null || c_value == null) {
            throw new RuntimeException("name or value should not be null");
        }
        this._relName = relName;
        this._value = c_value;
        polyTask = newTask()
                .then(defineAsVar("origin"))
                .then(traverse(_relName))
                .then(setAttribute(PolynomialNode.PRECISION, Type.DOUBLE, _value))
                .then(readVar("origin"));
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
    public final void serialize(final Buffer builder) {
        builder.writeString(NAME);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_relName, builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        TaskHelper.serializeString(_value, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return NAME;
    }
}
