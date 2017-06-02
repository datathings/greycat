/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
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

import greycat.Node;
import greycat.Action;
import greycat.Constants;
import greycat.Type;
import greycat.base.BaseNode;
import greycat.TaskContext;
import greycat.TaskResult;
import greycat.struct.Buffer;

class ActionSetAttribute implements Action {

    private final String _name;
    private final String _value;
    private final byte _propertyType;
    private final boolean _force;

    ActionSetAttribute(final String name, final String propertyType, final String value, final boolean force) {
        this._name = name;
        this._value = value;
        this._propertyType = Type.typeFromName(propertyType);
        this._force = force;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final TaskResult previousResult = ctx.result();
        final String flatRelationName = ctx.template(_name);
        if (previousResult != null) {
            Object toSet;
            String valueAfterTemplate = ctx.template(this._value);
            switch (_propertyType) {
                case Type.BOOL:
                    toSet = parseBoolean(valueAfterTemplate);
                    break;
                case Type.INT:
                    toSet = TaskHelper.parseInt(valueAfterTemplate);
                    break;
                case Type.DOUBLE:
                    toSet = Double.parseDouble(valueAfterTemplate);
                    break;
                case Type.LONG:
                    toSet = Long.parseLong(valueAfterTemplate);
                    break;
                case Type.DOUBLE_ARRAY:
                case Type.LONG_ARRAY:
                case Type.BOOL_ARRAY:
                case Type.INT_ARRAY: {
                    try {
                        toSet = loadArray(valueAfterTemplate, _propertyType);
                    } catch (RuntimeException e) {
                        toSet = null;
                        ctx.endTask(null, new Exception("Error while parsing array from string:\"" + valueAfterTemplate + "\"\n" + e.toString()));
                    }
                }
                break;
                default:
                    toSet = valueAfterTemplate;
            }
            for (int i = 0; i < previousResult.size(); i++) {
                Object loopObj = previousResult.get(i);
                if (loopObj instanceof BaseNode) {
                    Node loopNode = (Node) loopObj;

                    if (_force) {
                        loopNode.forceSet(flatRelationName, _propertyType, toSet);
                    } else {
                        loopNode.set(flatRelationName, _propertyType, toSet);
                    }
                }
            }
        }
        ctx.continueTask();
    }

    private Object loadArray(String valueAfterTemplate, byte type) throws NumberFormatException {
        String arrayInString = valueAfterTemplate;
        if (arrayInString.charAt(0) == '[') {
            arrayInString = arrayInString.substring(1);
        }
        if (arrayInString.charAt(arrayInString.length() - 1) == ']') {
            arrayInString = arrayInString.substring(0, arrayInString.length() - 1);
        }
        String[] valuesToParse = arrayInString.split(",");

        switch (type) {
            case Type.DOUBLE_ARRAY: {
                double[] finalValues = new double[valuesToParse.length];
                for (int i = 0; i < valuesToParse.length; i++) {
                    finalValues[i] = Double.parseDouble(valuesToParse[i]);
                }
                return finalValues;
            }
            case Type.LONG_ARRAY: {
                long[] finalValues = new long[valuesToParse.length];
                for (int i = 0; i < valuesToParse.length; i++) {
                    finalValues[i] = Long.parseLong(valuesToParse[i]);
                }
                return finalValues;
            }
            case Type.INT_ARRAY: {
                int[] finalValues = new int[valuesToParse.length];
                for (int i = 0; i < valuesToParse.length; i++) {
                    finalValues[i] = Integer.parseInt(valuesToParse[i]);
                }
                return finalValues;
            }
            case Type.BOOL_ARRAY:
                boolean[] finalValues = new boolean[valuesToParse.length];
                for(int i=0;i<valuesToParse.length;i++) {
                    if(valuesToParse[i].equals("true") || valuesToParse[i].equals("1")) {
                        finalValues[i] = true;
                    } else if(valuesToParse[i].equals("false") || valuesToParse[i].equals("0")) {
                        finalValues[i] = false;
                    } else {
                        throw new RuntimeException(valuesToParse[i] + " is not a correct boolean. Accepted value: [true,false, 1, 0]");
                    }
                }
        }
        return null;

    }

    private boolean parseBoolean(String booleanValue) {
        final String lower = booleanValue.toLowerCase();
        return (lower.equals("true") || lower.equals("1"));
    }

    @Override
    public void serialize(final Buffer builder) {
        if (_force) {
            builder.writeString(CoreActionNames.FORCE_ATTRIBUTE);
        } else {
            builder.writeString(CoreActionNames.SET_ATTRIBUTE);
        }
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_name, builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        TaskHelper.serializeType(_propertyType, builder);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        TaskHelper.serializeString(_value, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }


    @Override
    public final String name() {
        return (_force?CoreActionNames.FORCE_ATTRIBUTE:CoreActionNames.SET_ATTRIBUTE);
    }

}

