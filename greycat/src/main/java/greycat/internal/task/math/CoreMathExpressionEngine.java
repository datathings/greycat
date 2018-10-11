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

import greycat.Node;
import greycat.TaskContext;
import greycat.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class CoreMathExpressionEngine implements MathExpressionEngine {

    static final char decimalSeparator = '.';
    static final char minusSign = '-';
    private final MathToken[] _cacheAST;

    private CoreMathExpressionEngine(String expression) {
        _cacheAST = buildAST(shuntingYard(expression));
    }

    public static synchronized MathExpressionEngine parse(String p_expression) {
        return new CoreMathExpressionEngine(p_expression);
    }

    /**
     * @native ts
     * return !isNaN(+st);
     */
    static boolean isNumber(String st) {
        if (st.charAt(0) == minusSign && st.length() == 1)
            return false;
        for (int i = 0; i < st.length(); i++) {
            char ch = st.charAt(i);
            if (!isDigit(ch) && ch != minusSign && ch != decimalSeparator) {
                return false;
            }
        }
        return true;
    }

    /**
     * @native ts
     * var cc = c.charCodeAt(0);
     * if ( cc >= 0x30 && cc <= 0x39 ){
     * return true ;
     * }
     * return false ;
     */
    static boolean isDigit(char c) {
        return Character.isDigit(c);
    }

    /**
     * @native ts
     * var cc = c.charCodeAt(0);
     * if ( ( cc >= 0x41 && cc <= 0x5A ) || ( cc >= 0x61 && cc <= 0x7A ) ){
     * return true ;
     * }
     * return false ;
     */
    static boolean isLetter(char c) {
        return Character.isLetter(c);
    }

    /**
     * @native ts
     * var cc = c.charCodeAt(0);
     * if ( ( cc >= 0x0009 && cc <= 0x000D ) || ( cc == 0x0020 ) || ( cc == 0x0085 ) || ( cc == 0x00A0 ) ){
     * return true ;
     * }
     * return false ;
     */
    static boolean isWhitespace(char c) {
        return Character.isWhitespace(c);
    }


    /**
     * Implementation of the <i>Shunting Yard</i> algorithm to transform an
     * infix mathexp to a RPN mathexp.
     *
     * @param expression The input mathexp in infx.
     * @return A RPN representation of the mathexp, with each token as a list
     * member.
     */
    private List<String> shuntingYard(String expression) {
        List<String> outputQueue = new ArrayList<String>();
        Stack<String> stack = new Stack<String>();
        MathExpressionTokenizer tokenizer = new MathExpressionTokenizer(expression);
        String lastFunction = null;
        String previousToken = null;
        while (tokenizer.hasNext()) {
            String token = tokenizer.next();
            if (MathEntities.getINSTANCE().functions.keySet().contains(token.toUpperCase())) {
                stack.push(token);
                lastFunction = token;
            } else if (",".equals(token)) {
                while (!stack.isEmpty() && !("(".equals(stack.peek()))) {
                    outputQueue.add(stack.pop());
                }
                if (stack.isEmpty()) {
                    throw new RuntimeException("Parse error for function '"
                            + lastFunction + "'");
                }
            } else if (MathEntities.getINSTANCE().operators.keySet().contains(token)) {
                MathOperation o1 = MathEntities.getINSTANCE().operators.get(token);
                String token2 = stack.isEmpty() ? null : stack.peek();
                while (MathEntities.getINSTANCE().operators.keySet().contains(token2)
                        && ((o1.isLeftAssoc() && o1.getPrecedence() <= MathEntities.getINSTANCE().operators
                        .get(token2).getPrecedence()) || (o1
                        .getPrecedence() < MathEntities.getINSTANCE().operators.get(token2)
                        .getPrecedence()))) {
                    outputQueue.add(stack.pop());
                    token2 = stack.isEmpty() ? null : stack.peek();
                }
                stack.push(token);
            } else if ("(".equals(token)) {
                if (previousToken != null) {
                    if (isNumber(previousToken)) {
                        throw new RuntimeException("Missing operator at character position " + tokenizer.getPos());
                    }
                }
                stack.push(token);
            } else if (")".equals(token)) {
                while (!stack.isEmpty() && !("(".equals(stack.peek()))) {
                    outputQueue.add(stack.pop());
                }
                if (stack.isEmpty()) {
                    throw new RuntimeException("Mismatched parentheses");
                }
                stack.pop();
                if (!stack.isEmpty()
                        && MathEntities.getINSTANCE().functions.keySet().contains(stack.peek().toUpperCase())) {
                    outputQueue.add(stack.pop());
                }
            } else {
                outputQueue.add(token);
            }
            previousToken = token;
        }
        while (!stack.isEmpty()) {
            String element = stack.pop();
            if ("(".equals(element) || ")".equals(element)) {
                throw new RuntimeException("Mismatched parentheses");
            }
            outputQueue.add(element);
        }
        return outputQueue;
    }

    @Override
    public final double eval(final Node context, final TaskContext taskContext, final Map<String, Double> variables) {
        if (this._cacheAST == null) {
            throw new RuntimeException("Call parse before");
        }
        Stack<Double> stack = new Stack<Double>();
        for (int ii = 0; ii < _cacheAST.length; ii++) {
            MathToken mathToken = _cacheAST[ii];
            switch (mathToken.type()) {
                case 0:
                    double v1 = stack.pop();
                    double v2 = stack.pop();
                    MathOperation castedOp = (MathOperation) mathToken;
                    stack.push(castedOp.eval(v2, v1));
                    break;
                case 1:
                    MathFunction castedFunction = (MathFunction) mathToken;
                    double[] p = new double[castedFunction.getNumParams()];
                    for (int i = castedFunction.getNumParams() - 1; i >= 0; i--) {
                        p[i] = stack.pop();
                    }
                    stack.push(castedFunction.eval(p));
                    break;
                case 2:
                    MathDoubleToken castedDouble = (MathDoubleToken) mathToken;
                    stack.push(castedDouble.content());
                    break;
                case 3:
                    MathFreeToken castedFreeToken = (MathFreeToken) mathToken;
                    Double resolvedVar = null;
                    if (variables != null) {
                        resolvedVar = variables.get(castedFreeToken.content());
                    }
                    if (resolvedVar != null) {
                        stack.push(resolvedVar);
                    } else {
                        if ("TIME".equals(castedFreeToken.content())) {
                            stack.push((double) context.time());
                        } else {
                            String tokenName = castedFreeToken.content().trim();
                            Object resolved = null;
                            String cleanName = null;
                            if (context != null) {
                                if (tokenName.length() > 0 && tokenName.charAt(0) == '{' && tokenName.charAt(tokenName.length() - 1) == '}') {
                                    resolved = context.get(castedFreeToken.content().substring(1, tokenName.length() - 1));
                                    cleanName = castedFreeToken.content().substring(1, tokenName.length() - 1);
                                } else {
                                    resolved = context.get(castedFreeToken.content());
                                    cleanName = castedFreeToken.content();
                                }
                                if (cleanName.length() > 0 && cleanName.charAt(0) == '$') {
                                    cleanName = cleanName.substring(1);
                                }
                            }
                            if (taskContext != null) {
                                if (resolved == null) {
                                    if (tokenName.charAt(tokenName.length() - 1) == ']') { //array access
                                        int indexStart = -1;
                                        int indexArray = -1;
                                        for (int i = tokenName.length() - 3; i >= 0; i--) {
                                            if (tokenName.charAt(i) == '[') {
                                                indexStart = i + 1;
                                                break;
                                            }
                                        }

                                        if (indexStart != -1) {
                                            indexArray = parseInt(tokenName.substring(indexStart, tokenName.length() - 1));
                                            tokenName = tokenName.substring(0, indexStart - 1);
                                        }

                                        TaskResult varRes = taskContext.variable(tokenName);
                                        if(varRes == null && tokenName.equals("result")) {
                                            varRes = taskContext.result();
                                        }
                                        if (varRes != null && varRes.size() > indexArray) {
                                            resolved = varRes.get(indexArray);
                                        }
                                    } else {
                                        TaskResult varRes = taskContext.variable(tokenName);
                                        if(varRes == null && tokenName.equals("result")) {
                                            varRes = taskContext.result();
                                        }
                                        if(varRes != null) {
                                            resolved = varRes.get(0);
                                        }
                                    }
                                }
                            }
                            if (resolved != null) {
                                double resultAsDouble = parseDouble(resolved.toString());
                                variables.put(cleanName, resultAsDouble);
                                String valueString = resolved.toString();
                                if (valueString.equals("true")) {
                                    stack.push(1.0);
                                } else if (valueString.equals("false")) {
                                    stack.push(0.0);
                                } else {
                                    try {
                                        stack.push(resultAsDouble);
                                    } catch (Exception e) {
                                        //noop
                                    }
                                }
                            } else {
                                throw new RuntimeException("Unknow variable for name " + castedFreeToken.content());
                            }
                        }
                    }
                    break;
            }
        }
        Double result = stack.pop();
        if (result == null) {
            return 0;
        } else {
            return result;
        }
    }

    private MathToken[] buildAST(List<String> rpn) {
        MathToken[] result = new MathToken[rpn.size()];
        for (int ii = 0; ii < rpn.size(); ii++) {
            String token = rpn.get(ii);
            if (MathEntities.getINSTANCE().operators.keySet().contains(token)) {
                result[ii] = MathEntities.getINSTANCE().operators.get(token);
            } else if (MathEntities.getINSTANCE().functions.keySet().contains(token.toUpperCase())) {
                result[ii] = MathEntities.getINSTANCE().functions.get(token.toUpperCase());
            } else {

                if (token.length() > 0 && isLetter(token.charAt(0))) {
                    result[ii] = new MathFreeToken(token);
                } else {
                    try {
                        double parsed = parseDouble(token);
                        result[ii] = new MathDoubleToken(parsed);
                    } catch (Exception e) {
                        result[ii] = new MathFreeToken(token);
                    }
                }
            }
        }
        return result;
    }

    /**
     * @native ts
     * return parseFloat(val);
     */
    private double parseDouble(String val) {
        return Double.parseDouble(val);
    }

    /**
     * @native ts
     * return parseInt(val);
     */
    private int parseInt(String val) {
        return Integer.parseInt(val);
    }


}
