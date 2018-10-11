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
package greycat.internal.task;

import greycat.Constants;
import greycat.Node;
import greycat.base.BaseNode;
import greycat.Action;
import greycat.TaskContext;
import greycat.TaskResult;
import greycat.struct.Buffer;

import java.util.regex.Pattern;

class ActionWithout implements Action {

    private final String _patternTemplate;
    private final String _name;

    ActionWithout(final String name, final String stringPattern) {
        if (name == null) {
            throw new RuntimeException("name should not be null");
        }
        if (stringPattern == null) {
            throw new RuntimeException("pattern should not be null");
        }
        this._patternTemplate = stringPattern;
        this._name = name;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final Pattern pattern = Pattern.compile(ctx.template(_patternTemplate));
        final TaskResult previous = ctx.result();
        final TaskResult next = ctx.newResult();
        final int previousSize = previous.size();
        for (int i = 0; i < previousSize; i++) {
            final Object obj = previous.get(i);
            if (obj instanceof BaseNode) {
                final Node casted = (Node) obj;
                Object currentName = casted.get(_name);
                if (currentName == null || !pattern.matcher(currentName.toString()).matches()) {
                    next.add(casted.graph().cloneNode(casted));
                }
            } else {
                next.add(obj);
            }
        }
        ctx.continueWith(next);
    }

    @Override
    public void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.WITHOUT);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_name, builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        TaskHelper.serializeString(_patternTemplate, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }


    @Override
    public final String name() {
        return CoreActionNames.WITHOUT;
    }
    
}
