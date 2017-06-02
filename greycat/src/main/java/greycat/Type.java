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
package greycat;

/**
 * Defines the constants used in GreyCat.
 */
public class Type {

    /**
     * Primitive Types
     */
    public static final byte BOOL = 1;
    public static final byte STRING = 2;
    public static final byte LONG = 3;
    public static final byte INT = 4;
    public static final byte DOUBLE = 5;

    public static final byte DOUBLE_ARRAY = 6;
    public static final byte LONG_ARRAY = 7;
    public static final byte INT_ARRAY = 8;
    public static final byte STRING_ARRAY = 9;
    public static final byte BOOL_ARRAY = 28;


    public static final byte LONG_TO_LONG_MAP = 10;
    public static final byte LONG_TO_LONG_ARRAY_MAP = 11;
    public static final byte STRING_TO_INT_MAP = 12;

    public static final byte RELATION = 13;
    public static final byte RELATION_INDEXED = 14;

    public static final byte DMATRIX = 15;
    public static final byte LMATRIX = 16;

    public static final byte EGRAPH = 17;
    public static final byte ENODE = 18;
    public static final byte ERELATION = 19;

    public static final byte TASK = 20;
    public static final byte TASK_ARRAY = 21;

    public static final byte KDTREE = 22;
    public static final byte NDTREE = 23;

    public static final byte NODE = 24;
    public static final byte NODE_ARRAY = 25;

    public static final byte INT_TO_INT_MAP = 26;
    public static final byte INT_TO_STRING_MAP = 27;

    /**
     * Convert a type that represent a byte to a readable String representation
     *
     * @param p_type byte encoding a particular type
     * @return readable string representation of the type
     */
    public static String typeName(byte p_type) {
        switch (p_type) {
            /* Primitives */
            case Type.BOOL:
                return "BOOL";
            case Type.STRING:
                return "STRING";
            case Type.LONG:
                return "LONG";
            case Type.INT:
                return "INT";
            case Type.DOUBLE:
                return "DOUBLE";
            /* Arrays */
            case Type.DOUBLE_ARRAY:
                return "DOUBLE_ARRAY";
            case Type.LONG_ARRAY:
                return "LONG_ARRAY";
            case Type.INT_ARRAY:
                return "INT_ARRAY";
            case Type.STRING_ARRAY:
                return "STRING_ARRAY";
            case Type.BOOL_ARRAY:
                return "BOOL_ARRAY";
            /* Maps */
            case Type.LONG_TO_LONG_MAP:
                return "LONG_TO_LONG_MAP";
            case Type.LONG_TO_LONG_ARRAY_MAP:
                return "LONG_TO_LONG_ARRAY_MAP";
            case Type.STRING_TO_INT_MAP:
                return "STRING_TO_INT_MAP";
            case Type.RELATION:
                return "RELATION";
            case Type.RELATION_INDEXED:
                return "RELATION_INDEXED";
            case Type.DMATRIX:
                return "DMATRIX";
            case Type.LMATRIX:
                return "LMATRIX";
            case Type.EGRAPH:
                return "EGRAPH";
            case Type.ENODE:
                return "ENODE";
            case Type.ERELATION:
                return "ERELATION";
            case Type.TASK:
                return "TASK";
            case Type.TASK_ARRAY:
                return "TASK_ARRAY";
            case Type.KDTREE:
                return "KDTREE";
            case Type.NDTREE:
                return "NDTREE";
            case Type.INT_TO_INT_MAP:
                return "INT_TO_INT_MAP";
            case Type.INT_TO_STRING_MAP:
                return "INT_TO_STRING_MAP";
            default:
                return "unknown";
        }
    }

    public static byte typeFromName(String name) {
        switch (name) {
            case "BOOL":
                return Type.BOOL;
            case "STRING":
                return Type.STRING;
            case "LONG":
                return Type.LONG;
            case "INT":
                return Type.INT;
            case "DOUBLE":
                return Type.DOUBLE;
            case "DOUBLE_ARRAY":
                return Type.DOUBLE_ARRAY;
            case "LONG_ARRAY":
                return Type.LONG_ARRAY;
            case "INT_ARRAY":
                return Type.INT_ARRAY;
            case "STRING_ARRAY":
                return Type.STRING_ARRAY;
            case "BOOL_ARRAY":
                return Type.BOOL_ARRAY;
            case "LONG_TO_LONG_MAP":
                return Type.LONG_TO_LONG_MAP;
            case "LONG_TO_LONG_ARRAY_MAP":
                return Type.LONG_TO_LONG_ARRAY_MAP;
            case "STRING_TO_INT_MAP":
                return Type.STRING_TO_INT_MAP;
            case "RELATION":
                return Type.RELATION;
            case "RELATION_INDEXED":
                return Type.RELATION_INDEXED;
            case "DMATRIX":
                return Type.DMATRIX;
            case "LMATRIX":
                return Type.LMATRIX;
            case "EGRAPH":
                return Type.EGRAPH;
            case "ENODE":
                return Type.ENODE;
            case "ERELATION":
                return Type.ERELATION;
            case "TASK":
                return Type.TASK;
            case "TASK_ARRAY":
                return Type.TASK_ARRAY;
            case "KDTREE":
                return Type.KDTREE;
            case "NDTREE":
                return Type.NDTREE;
            case "INT_TO_INT_MAP":
                return Type.INT_TO_INT_MAP;
            case "INT_TO_STRING_MAP":
                return Type.INT_TO_STRING_MAP;
            default:
                return -1;
        }
    }

}
