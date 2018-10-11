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

import java.util.HashMap;

class MathEntities {

    private static MathEntities INSTANCE = null;

    static MathEntities getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new MathEntities();
        }
        return INSTANCE;
    }

    HashMap<String,MathOperation> operators;
    HashMap<String,MathFunction> functions;

    private MathEntities() {
        operators = new HashMap<String,MathOperation>();
        operators.put("+", new MathOperation("+", 20, true));
        operators.put("-", new MathOperation("-", 20, true));
        operators.put("*", new MathOperation("*", 30, true));
        operators.put("/", new MathOperation("/", 30, true));
        operators.put("%", new MathOperation("%", 30, true));
        operators.put("^", new MathOperation("^", 40, false));

        operators.put("&&", new MathOperation("&&", 4, false));
        operators.put("||", new MathOperation("||", 2, false));
        operators.put(">", new MathOperation(">", 10, false));
        operators.put(">=", new MathOperation(">=", 10, false));
        operators.put("<", new MathOperation("<", 10, false));
        operators.put("<=", new MathOperation("<=", 10, false));
        operators.put("==", new MathOperation("==", 7, false));
        operators.put("!=", new MathOperation("!=", 7, false));

        functions = new HashMap<String,MathFunction>();
        functions.put("NOT", new MathFunction("NOT", 1));
        functions.put("IF", new MathFunction("IF", 3));
        functions.put("RAND", new MathFunction("RAND", 0));
        functions.put("SIN", new MathFunction("SIN", 1));
        functions.put("COS", new MathFunction("COS", 1));
        functions.put("TAN", new MathFunction("TAN", 1));
        functions.put("ASIN", new MathFunction("ASIN", 1));
        functions.put("ACOS", new MathFunction("ACOS", 1));
        functions.put("ATAN", new MathFunction("ATAN", 1));
        functions.put("MAX", new MathFunction("MAX", 2));
        functions.put("MIN", new MathFunction("MIN", 2));
        functions.put("ABS", new MathFunction("ABS", 1));
        functions.put("LOG", new MathFunction("LOG", 1));
        functions.put("ROUND", new MathFunction("ROUND", 2));
        functions.put("FLOOR", new MathFunction("FLOOR", 1));
        functions.put("CEILING", new MathFunction("CEILING", 1));
        functions.put("SQRT", new MathFunction("SQRT", 1));
        functions.put("SECONDS", new MathFunction("SECONDS", 1));
        functions.put("MINUTES", new MathFunction("MINUTES", 1));
        functions.put("HOURS", new MathFunction("HOURS", 1));
        functions.put("DAY", new MathFunction("DAY", 1));
        functions.put("MONTH", new MathFunction("MONTH", 1));
        functions.put("YEAR", new MathFunction("YEAR", 1));
        functions.put("DAYOFWEEK", new MathFunction("DAYOFWEEK", 1));

    }

}
