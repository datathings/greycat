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
package greycat.internal.task.math;

import greycat.TaskContext;
import greycat.ConditionalFunction;

import java.util.HashMap;
import java.util.Map;

public class MathConditional {

    private MathExpressionEngine _engine;
    private String _expression;

    public MathConditional(String mathExpression) {
        this._expression = mathExpression;
        this._engine = CoreMathExpressionEngine.parse(mathExpression);
    }

    public ConditionalFunction conditional() {
        return new ConditionalFunction() {
            @Override
            public boolean eval(TaskContext ctx) {
                Map<String, Double> variables = new HashMap<String, Double>();
                variables.put("PI", Math.PI);
                variables.put("TRUE", 1.0);
                variables.put("FALSE", 0.0);
                return (_engine.eval(null, ctx, variables) >= 0.5);
            }
        };
    }

    @Override
    public String toString() {
        return "cond(\'" + _expression + "\')";
    }

}
