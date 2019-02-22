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
import greycat.Action;
import greycat.internal.task.TaskHelper;
import greycat.TaskContext;
import greycat.TaskResult;
import greycat.struct.Buffer;

class ActionSplit implements Action {

    private String _splitPattern;

    ActionSplit(String p_splitPattern) {
        this._splitPattern = p_splitPattern;
    }

    @Override
    public void eval(TaskContext ctx) {
        final String splitPattern = ctx.template(this._splitPattern);
        TaskResult previous = ctx.result();
        TaskResult next = ctx.wrap(null);
        for (int i = 0; i < previous.size(); i++) {
            final Object loop = previous.get(0);
            if (loop instanceof String) {
                String[] splitted = ((String) loop).split(splitPattern);
                if (previous.size() == 1) {
                    for (int j = 0; j < splitted.length; j++) {
                        next.add(splitted[j]);
                    }
                } else {
                    next.add(splitted);
                }
            }
        }
        ctx.continueWith(next);
    }

    @Override
    public final void serialize(final Buffer builder) {
        builder.writeString(ImporterActions.SPLIT);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_splitPattern, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return ImporterActions.SPLIT;
    }
}
