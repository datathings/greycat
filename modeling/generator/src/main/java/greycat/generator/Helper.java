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
package greycat.generator;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import greycat.*;
import greycat.internal.custom.KDTree;
import greycat.internal.custom.NDTree;
import greycat.struct.*;
import greycat.utility.HashHelper;
import greycat.utility.Meta;
import greycat.utility.MetaConst;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Helper {

    static final ClassName gCallback = ClassName.get(Callback.class);
    static final ClassName gNode = ClassName.get(Node.class);
    static final TypeName gNodeArray = ArrayTypeName.of(ClassName.get(Node.class));
    static final ClassName gMeta = ClassName.get(Meta.class);
    static final ClassName gMetaConst = ClassName.get(MetaConst.class);
    static final ClassName gGraph = ClassName.get(Graph.class);
    static final ClassName gIndex = ClassName.get(Index.class);
    static final ClassName gNodeIndex = ClassName.get(NodeIndex.class);
    static final ClassName jlSystem = ClassName.get(java.lang.System.class);
    static final ClassName gRelation = ClassName.get(Relation.class);

    private static Set<String> primitives = new HashSet<String>(Arrays.asList("Bool", "Boolean", "String", "Long", "Int", "Integer", "Double"));
    private static Set<String> primitiveArrays = new HashSet<String>(Arrays.asList("BoolArray", "BooleanArray", "StringArray", "LongArray", "IntArray", "IntegerArray", "DoubleArray"));
    private static Set<String> maps = new HashSet<String>(Arrays.asList("LongToLongMap", "LongToLongArrayMap", "StringToIntMap", "IntToIntMap", "IntToStringMap"));
    private static Set<String> matrices = new HashSet<String>(Arrays.asList("DMatrix", "LMatrix", "IMatrix"));
    private static Set<String> trees = new HashSet<String>(Arrays.asList("KDTree", "NDTree"));

    public static String typeName(String type) {
        String typeName;
        switch (type) {
            case "Boolean":
                typeName = "greycat.Type.BOOL";
                break;
            case "Bool":
                typeName = "greycat.Type.BOOL";
                break;
            case "String":
                typeName = "greycat.Type.STRING";
                break;
            case "Long":
                typeName = "greycat.Type.LONG";
                break;
            case "Integer":
                typeName = "greycat.Type.INT";
                break;
            case "Int":
                typeName = "greycat.Type.INT";
                break;
            case "Double":
                typeName = "greycat.Type.DOUBLE";
                break;
            case "DoubleArray":
                typeName = "greycat.Type.DOUBLE_ARRAY";
                break;
            case "LongArray":
                typeName = "greycat.Type.LONG_ARRAY";
                break;
            case "IntArray":
                typeName = "greycat.Type.INT_ARRAY";
                break;
            case "StringArray":
                typeName = "greycat.Type.STRING_ARRAY";
                break;
            case "LongToLongMap":
                typeName = "greycat.Type.LONG_TO_LONG_MAP";
                break;
            case "LongToLongArrayMap":
                typeName = "greycat.Type.LONG_TO_LONG_ARRAY_MAP";
                break;
            case "StringToIntMap":
                typeName = "greycat.Type.STRING_TO_INT_MAP";
                break;
            case "DMatrix":
                typeName = "greycat.Type.DMATRIX";
                break;
            case "LMatrix":
                typeName = "greycat.Type.LMATRIX";
                break;
            case "IMatrix":
                typeName = "greycat.Type.IMATRIX";
                break;
            case "EStructArray":
                typeName = "greycat.Type.ESTRUCT_ARRAY";
                break;
            case "EStruct":
                typeName = "greycat.Type.ESTRUCT";
                break;
            case "KDTree":
                typeName = "greycat.Type.KDTREE";
                break;
            case "NDTree":
                typeName = "greycat.Type.NDTREE";
                break;
            case "IntToIntMap":
                typeName = "greycat.Type.INT_TO_INT_MAP";
                break;
            case "IntToStringMap":
                typeName = "greycat.Type.INT_TO_STRING_MAP";
                break;
            case "Task":
                typeName = "greycat.Type.TASK";
                break;
            case "TaskArray":
                typeName = "greycat.Type.TASK_ARRAY";
                break;
            case "Node":
                typeName = "greycat.Type.NODE";
                break;
            case "Index":
                typeName = "greycat.Type.INDEX";
                break;
            case "Relation":
                typeName = "greycat.Type.RELATION";
                break;
            case "Reference":
                typeName = "greycat.Type.REFERENCE";
                break;
            default:
                typeName = "" + HashHelper.hash(type);
        }
        return typeName;
    }

    static String classTsName(String type) {
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

    static TypeName clazz(String type) {
        switch (type) {
            case "Bool":
                return TypeName.BOOLEAN.box();
            case "String":
                return ClassName.get(String.class);
            case "Long":
                return TypeName.LONG.box();
            case "Int":
                return TypeName.INT.box();
            case "Double":
                return TypeName.DOUBLE.box();
            case "DoubleArray":
                return ClassName.get(DoubleArray.class);
            case "LongArray":
                return ClassName.get(LongArray.class);
            case "IntArray":
                return ClassName.get(IntArray.class);
            case "StringArray":
                return ClassName.get(StringArray.class);
            case "LongToLongMap":
                return ClassName.get(LongLongMap.class);
            case "LongToLongArrayMap":
                return ClassName.get(LongLongArrayMap.class);
            case "StringToIntMap":
                return ClassName.get(StringIntMap.class);
            case "DMatrix":
                return ClassName.get(DMatrix.class);
            case "LMatrix":
                return ClassName.get(LMatrix.class);
            case "IMatrix":
                return ClassName.get(IMatrix.class);
            case "EStructArray":
                return ClassName.get(EStructArray.class);
            case "EStruct":
                return ClassName.get(EStruct.class);
            case "KDTree":
                return ClassName.get(KDTree.class);
            case "NDTree":
                return ClassName.get(NDTree.class);
            case "IntToIntMap":
                return ClassName.get(IntIntMap.class);
            case "IntToStringMap":
                return ClassName.get(IntStringMap.class);
            case "Task":
                return ClassName.get(Task.class);
            case "TaskArray":
                return ClassName.get(Task[].class);
            case "Node":
                return ClassName.get(Node.class);
            case "NodeValue":
                return ClassName.get(NodeValue.class);
            default:
                return ClassName.bestGuess(type);
        }
    }


    public static boolean isPrimitive(String type) {
        return primitives.contains(type);
    }

    public static boolean isPrimitiveArray(String type) {
        return primitiveArrays.contains(type);
    }

    public static boolean isMap(String type) {
        return maps.contains(type);
    }

    public static boolean isMatrix(String type) {
        return matrices.contains(type);
    }

    public static boolean isTree(String type) {
        return trees.contains(type);
    }

    public static boolean isNDTree(String type) {
        return type.equals("NDTree");
    }

    public static boolean isKDTree(String type) {
        return type.equals("KDTree");
    }

}


