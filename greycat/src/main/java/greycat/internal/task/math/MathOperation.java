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
package greycat.internal.task.math;

/**
 * Abstract definition of a supported operator. An operator is defined by
 * its name (pattern), precedence and if it is left- or right associative.
 */
class MathOperation implements MathToken {

    private String oper;
    private int precedence;
    private boolean leftAssoc;

    MathOperation(String oper, int precedence, boolean leftAssoc) {
        this.oper = oper;
        this.precedence = precedence;
        this.leftAssoc = leftAssoc;
    }

    public String getOper() {
        return oper;
    }

    int getPrecedence() {
        return precedence;
    }

    boolean isLeftAssoc() {
        return leftAssoc;
    }

    double eval(double v1, double v2) {
        if (oper.equals("+")) {
            return v1 + v2;
        } else if (oper.equals("-")) {
            return v1 - v2;
        } else if (oper.equals("*")) {
            return v1 * v2;
        } else if (oper.equals("/")) {
            return v1 / v2;
        } else if (oper.equals("%")) {
            return v1 % v2;
        } else if (oper.equals("^")) {
            return Math.pow(v1, v2);
        } else if (oper.equals("&&")) {
            boolean b1 = !(v1 == 0);
            boolean b2 = !(v2 == 0);
            return b1 && b2 ? 1 : 0;
        } else if (oper.equals("||")) {
            boolean b1 = !(v1 == 0);
            boolean b2 = !(v2 == 0);
            return b1 || b2 ? 1 : 0;
        } else if (oper.equals(">")) {
            return v1 > v2 ? 1 : 0;
        } else if (oper.equals(">=")) {
            return v1 >= v2 ? 1 : 0;
        } else if (oper.equals("<")) {
            return v1 < v2 ? 1 : 0;
        } else if (oper.equals("<=")) {
            return v1 <= v2 ? 1 : 0;
        } else if (oper.equals("==")) {
            return v1 == v2 ? 1 : 0;
        } else if (oper.equals("!=")) {
            return v1 != v2 ? 1 : 0;
        }
        return 0;
    }

    @Override
    public int type() {
        return 0;
    }

}