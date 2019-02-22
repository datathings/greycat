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
package greycat.importer;

import greycat.Constants;
import greycat.internal.task.TaskHelper;
import greycat.Action;
import greycat.importer.util.IterableLines;
import greycat.TaskContext;
import greycat.struct.Buffer;

class ActionReadLines implements Action {

    private final String _pathOrTemplate;

    ActionReadLines(final String p_pathOrTemplate) {
        this._pathOrTemplate = p_pathOrTemplate;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final String path = ctx.template(_pathOrTemplate);
        ctx.continueWith(new IterableLines(path));
    }

    @Override
    public final void serialize(final Buffer builder) {
        builder.writeString(ImporterActions.READLINES);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_pathOrTemplate, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return ImporterActions.READLINES;
    }
}
