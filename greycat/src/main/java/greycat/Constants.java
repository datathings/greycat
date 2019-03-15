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
package greycat;

/**
 * Static constants used
 */
public class Constants {

    public static boolean enableDebug = false;

    public static final int KEY_SIZE = 4;

    // Limit long lengths to 53 bits because of JS limitation
    public static final int LONG_SIZE = 53;

    public static final int PREFIX_SIZE = 16;

    public static final long BEGINNING_OF_TIME = -0x001FFFFFFFFFFFFEL;
    public static final String BEGINNING_OF_TIME_STR = BEGINNING_OF_TIME + "";

    public static final long END_OF_TIME = 0x001FFFFFFFFFFFFEL;
    public static final String END_OF_TIME_STR = END_OF_TIME + "";

    public static final long BEGINNING_OF_TIME_PLUS_ONE = BEGINNING_OF_TIME + 1;
    public static final String BEGINNING_OF_TIME_PLUS_ONE_STR = BEGINNING_OF_TIME_PLUS_ONE + "";

    public static final long END_OF_TIME_MINUS_ONE = END_OF_TIME - 1;
    public static final String END_OF_TIME_MINUS_ONE_STR = END_OF_TIME_MINUS_ONE + "";

    public static final long NULL_LONG = 0x001FFFFFFFFFFFFFL;

    public static final int NULL_INT = 0x7FFFFFFF;

    // Limit limit local index to LONG limit - prefix size
    public static final long KEY_PREFIX_MASK = 0x0000001FFFFFFFFFL;

    public static final String CACHE_MISS_ERROR = "Cache miss error";

    public static final char TASK_PARAM_SEP = ',';

    // public static final char QUERY_KV_SEP = '=';

    public static final char TASK_SEP = '.';

    public static final char TASK_PARAM_OPEN = '(';

    public static final char TASK_PARAM_CLOSE = ')';

    public static final char SUB_TASK_OPEN = '{';

    public static final char SUB_TASK_CLOSE = '}';

    public static final char SUB_TASK_DECLR = '#';

    /**
     * {@native ts
     * public static CHUNK_SEP : number = "|".charCodeAt(0);
     * }
     */
    public static final byte CHUNK_SEP = '|';

    /**
     * {@native ts
     * public static CHUNK_META_SEP : number = "$".charCodeAt(0);
     * }
     */
    public static final byte CHUNK_META_SEP = '$';

    /**
     * {@native ts
     * public static CHUNK_VAL_SEP : number = ":".charCodeAt(0);
     * }
     */
    public static final byte CHUNK_VAL_SEP = ':';

    /**
     * {@native ts
     * public static BLOCK_OPEN : number = "[".charCodeAt(0);
     * }
     */
    public static final byte BLOCK_OPEN = '[';

    /**
     * {@native ts
     * public static BLOCK_CLOSE : number = "]".charCodeAt(0);
     * }
     */
    public static final byte BLOCK_CLOSE = ']';

    /**
     * {@native ts
     * return param != undefined && param != null;
     * }
     */
    /**
     * Checks if a parameter is defined (!= null)
     *
     * @param param The parameter to test
     * @return true if not null, false otherwise.
     */
    public static boolean isDefined(Object param) {
        return param != null;
    }

    /**
     * {@native ts
     * return src === other
     * }
     */
    /**
     * Tests if an object is equal to another. This is an indirection method to offer an alternative implementation for JS
     *
     * @param src   the first object
     * @param other the second object
     * @return true if objects are equal, false otherwise.
     */
    public static boolean equals(String src, String other) {
        return src.equals(other);
    }

    public static boolean longArrayEquals(long[] src, long[] other) {
        if (src.length != other.length) {
            return false;
        }
        for (int i = 0; i < src.length; i++) {
            if (src[i] != other[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@native ts
     * public static BUFFER_SEP : number = "#".charCodeAt(0);
     * }
     */
    public static final byte BUFFER_SEP = '#';

    /**
     * Chunk Save/Load special chars
     */
    /**
     * {@native ts
     * public static KEY_SEP : number = ";".charCodeAt(0);
     * }
     */
    public static final byte KEY_SEP = ';';

    public static final int MAP_INITIAL_CAPACITY = 8;

    public static int BOOL_TRUE = 1;

    public static int BOOL_FALSE = 0;

    public static boolean DEEP_WORLD = true;

    public static boolean WIDE_WORLD = false;

    /**
     * @native ts
     * return isNaN(toTest);
     */
    /**
     * Tests if an element is Not A Number
     *
     * @param toTest the element to test
     * @return false if the element is a number
     */
    public static boolean isNaN(double toTest) {
        return Double.NaN == toTest;
    }

    public static final long EMPTY_HASH = -1;

}

