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

import greycat.Constants;
import greycat.Node;
import greycat.internal.task.math.MathExpressionEngine;
import greycat.Action;
import greycat.base.BaseNode;
import greycat.internal.task.math.CoreMathExpressionEngine;
import greycat.TaskContext;
import greycat.TaskResult;
import greycat.struct.Buffer;

import java.util.HashMap;
import java.util.Map;

class ActionExecuteExpression implements Action {

    final private MathExpressionEngine _engine;
    final private String _expression;

    ActionExecuteExpression(final String mathExpression) {
        this._expression = mathExpression;
        this._engine = CoreMathExpressionEngine.parse(mathExpression);
    }

    @Override
    public void eval(final TaskContext ctx) {
        final TaskResult previous = ctx.result();
        final TaskResult<Double> next = ctx.newResult();
        final int previousSize = previous.size();
        for (int i = 0; i < previousSize; i++) {
            final Object loop = previous.get(i);
            Map<String, Double> variables = new HashMap<String, Double>();
            variables.put("PI", Math.PI);
            variables.put("TRUE", 1.0);
            variables.put("FALSE", 0.0);
            if (loop instanceof BaseNode) {
                next.add(_engine.eval((Node) loop, ctx, variables));
                ((BaseNode) loop).free();
            } else {
                next.add(_engine.eval(null, ctx, variables));
            }
        }
        //optimization to avoid iteration on previous result for free
        previous.clear();
        ctx.continueWith(next);
    }

    @Override
    public void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.EXECUTE_EXPRESSION);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_expression, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.EXECUTE_EXPRESSION;
    }

}
