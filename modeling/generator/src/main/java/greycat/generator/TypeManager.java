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
package greycat.generator;

import greycat.Node;
import greycat.Task;
import greycat.internal.custom.KDTree;
import greycat.internal.custom.NDTree;
import greycat.struct.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TypeManager {
    private static Set<String> primitives = new HashSet<String>(Arrays.asList("Bool", "Boolean", "String", "Long", "Int", "Integer", "Double"));

    public static String typeName(String type) {
        String typeName;
        switch (type) {
            case "Boolean":
                typeName = "Type.BOOL";
                break;
            case "Bool":
                typeName = "Type.BOOL";
                break;
            case "String":
                typeName = "Type.STRING";
                break;
            case "Long":
                typeName = "Type.LONG";
                break;
            case "Integer":
                typeName = "Type.INT";
                break;
            case "Int":
                typeName = "Type.INT";
                break;
            case "Double":
                typeName = "Type.DOUBLE";
                break;
            case "DoubleArray":
                typeName = "Type.DOUBLE_ARRAY";
                break;
            case "LongArray":
                typeName = "Type.LONG_ARRAY";
                break;
            case "IntArray":
                typeName = "Type.INT_ARRAY";
                break;
            case "StringArray":
                typeName = "Type.STRING_ARRAY";
                break;
            case "LongToLongMap":
                typeName = "Type.LONG_TO_LONG_MAP";
                break;
            case "LongToLongArrayMap":
                typeName = "Type.LONG_TO_LONG_ARRAY_MAP";
                break;
            case "StringToIntMap":
                typeName = "Type.STRING_TO_INT_MAP";
                break;
            case "DMatrix":
                typeName = "Type.DMATRIX";
                break;
            case "LMatrix":
                typeName = "Type.LMATRIX";
                break;
            case "EGraph":
                typeName = "Type.EGRAPH";
                break;
            case "ENode":
                typeName = "Type.ENODE";
                break;
            case "KDTree":
                typeName = "Type.KDTREE";
                break;
            case "NDTree":
                typeName = "Type.NDTREE";
                break;
            case "IntToIntMap":
                typeName = "Type.INT_TO_INT_MAP";
                break;
            case "IntToStringMap":
                typeName = "Type.INT_TO_STRING_MAP";
                break;
            case "Task":
                typeName = "Type.TASK";
                break;
            case "TaskArray":
                typeName = "Type.TASK_ARRAY";
                break;
            case "Node":
                typeName = "Type.NODE";
                break;
            default:
                typeName = type + ".TYPE_HASH";
        }

        return typeName;
    }

    public static String cassTsName(String type) {
        String className;
        switch (type) {
            case "Bool":
            case "Boolean":
                return "boolean";
            case "String":
                return "string";
            case "Long":
            case "Double":
            case "Int":
            case "Integer":
                return "number";
        }
        return "any";
    }

    public static String cassName(String type) {
        String className;

        switch (type) {
            case "Bool":
                className = boolean.class.getCanonicalName();
                break;
            case "String":
                className = String.class.getCanonicalName();
                break;
            case "Long":
                className = long.class.getCanonicalName();
                break;
            case "Int":
                className = int.class.getCanonicalName();
                break;
            case "Double":
                className = double.class.getCanonicalName();
                break;
            case "DoubleArray":
                className = DoubleArray.class.getCanonicalName();
                break;
            case "LongArray":
                className = LongArray.class.getCanonicalName();
                break;
            case "IntArray":
                className = IntArray.class.getCanonicalName();
                break;
            case "StringArray":
                className = StringArray.class.getCanonicalName();
                break;
            case "LongToLongMap":
                className = LongLongMap.class.getCanonicalName();
                break;
            case "LongToLongArrayMap":
                className = LongLongArrayMap.class.getCanonicalName();
                break;
            case "StringToIntMap":
                className = StringIntMap.class.getCanonicalName();
                break;
            case "DMatrix":
                className = DMatrix.class.getCanonicalName();
                break;
            case "LMatrix":
                className = LMatrix.class.getCanonicalName();
                break;
            case "EGraph":
                className = EGraph.class.getCanonicalName();
                break;
            case "ENode":
                className = ENode.class.getCanonicalName();
                break;
            case "KDTree":
                className = KDTree.class.getCanonicalName();
                break;
            case "NDTree":
                className = NDTree.class.getCanonicalName();
                break;
            case "IntToIntMap":
                className = IntIntMap.class.getCanonicalName();
                break;
            case "IntToStringMap":
                className = IntStringMap.class.getCanonicalName();
                break;
            case "Task":
                className = Task.class.getCanonicalName();
                break;
            case "TaskArray":
                className = Task[].class.getCanonicalName();
                break;
            case "Node":
                className = Node.class.getCanonicalName();
                break;
            default:
                className = type;
        }

        return className;
    }

    public static boolean isPrimitive(String type) {
        return primitives.contains(type);
    }

}


