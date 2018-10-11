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


class MathExpressionTokenizer {

    private int pos = 0;
    private String input;
    private String previousToken;

    MathExpressionTokenizer(String input) {
        this.input = input.trim();
    }

    boolean hasNext() {
        return (pos < input.length());
    }

    private char peekNextChar() {
        if (pos < (input.length() - 1)) {
            return input.charAt(pos + 1);
        } else {
            return '\0';
        }
    }

    String next() {
        StringBuilder token = new StringBuilder();
        if (pos >= input.length()) {
            return previousToken = null;
        }
        char ch = input.charAt(pos);
        while (CoreMathExpressionEngine.isWhitespace(ch) && pos < input.length()) {
            ch = input.charAt(++pos);
        }
        if (CoreMathExpressionEngine.isDigit(ch)) {
            while ((CoreMathExpressionEngine.isDigit(ch) || ch == CoreMathExpressionEngine.decimalSeparator) && (pos < input.length())) {
                token.append(input.charAt(pos++));
                ch = pos == input.length() ? '\0' : input.charAt(pos);
            }
        } else if (ch == CoreMathExpressionEngine.minusSign && CoreMathExpressionEngine.isDigit(peekNextChar()) && ("(".equals(previousToken) || ",".equals(previousToken) || previousToken == null || MathEntities.getINSTANCE().operators.keySet().contains(previousToken))) {
            token.append(CoreMathExpressionEngine.minusSign);
            pos++;
            token.append(next());
        } else if (CoreMathExpressionEngine.isLetter(ch) || (ch == '_') || (ch == '{') || (ch == '}') || (ch == '$')) {
            while ((CoreMathExpressionEngine.isLetter(ch) || CoreMathExpressionEngine.isDigit(ch) || (ch == '_') || (ch == '{') || (ch == '}') || (ch == '$')) && (pos < input.length())) {
                token.append(input.charAt(pos++));
                ch = pos == input.length() ? '\0' : input.charAt(pos);
            }
            if (pos < input.length()) {
                if (input.charAt(pos) == '[') {
                    token.append(input.charAt(pos++));
                    ch = pos == input.length() ? '\0' : input.charAt(pos);
                    while (CoreMathExpressionEngine.isDigit(ch) && pos < input.length()) {
                        token.append(input.charAt(pos++));
                        ch = pos == input.length() ? '\0' : input.charAt(pos);
                    }
                    if (input.charAt(pos) != ']') {
                        throw new RuntimeException("Error in array definition '" + token + "' at position " + (pos - token.length() + 1));
                    } else {
                        token.append(input.charAt(pos++));
                    }
                }
            }
        } else if (ch == '(' || ch == ')' || ch == ',') {
            token.append(ch);
            pos++;
        } else {
            while (!CoreMathExpressionEngine.isLetter(ch) && !CoreMathExpressionEngine.isDigit(ch) && ch != '_' && !CoreMathExpressionEngine.isWhitespace(ch) && ch != '(' && ch != ')' && ch != ',' && (ch != '{') && (ch != '}') && (ch != '$') && (pos < input.length())) {
                token.append(input.charAt(pos));
                pos++;
                ch = pos == input.length() ? '\0' : input.charAt(pos);
                if (ch == CoreMathExpressionEngine.minusSign) {
                    break;
                }
            }
            if (!MathEntities.getINSTANCE().operators.keySet().contains(token.toString())) {
                throw new RuntimeException("Unknown operator '" + token + "' at position " + (pos - token.length() + 1));
            }
        }
        return previousToken = token.toString();
    }

    int getPos() {
        return pos;
    }

}
