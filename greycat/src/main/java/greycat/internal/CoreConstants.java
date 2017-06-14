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
package greycat.internal;

import greycat.Constants;

import java.util.Arrays;

public class CoreConstants extends Constants {

    /**
     * Fills an array with a boolean value.
     * @param target the array to fill
     * @param elem the value to put in each cell
     */
    /**
     * {@native ts
     * for(var i=0;i<target.length;i++){ target[i] = elem; }
     * }
     */
    public static void fillBooleanArray(boolean[] target, boolean elem) {
        Arrays.fill(target, elem);
    }//TODO: Move to a utility class

    /**
     * ChunkFlags
     */
    //public static final short DIRTY_BIT_INDEX = 0;


    //public static final short REMOVED_BIT_INDEX = 1;
    //public static final int REMOVED_BIT = 1 << REMOVED_BIT_INDEX;

    /**
     * Keys constants
     */
    //public static final int KEYS_SIZE = 3;

    public static final int PREFIX_TO_SAVE_SIZE = 2;

    public static final long[] NULL_KEY = new long[]{END_OF_TIME, END_OF_TIME, END_OF_TIME};

    public static final long[] GLOBAL_UNIVERSE_KEY = new long[]{NULL_LONG, NULL_LONG, NULL_LONG};

    public static final long[] GLOBAL_DICTIONARY_KEY = new long[]{NULL_LONG, 0, 0};

    public static final long[] GLOBAL_INDEX_KEY = new long[]{NULL_LONG, 1, 0};

    public static final String INDEX_ATTRIBUTE = "index";

    public static final String REVERSE_INDEX_ATTRIBUTE = "r_index";

    /**
     * Map constants
     */

    /**
     * Error messages
     */
    public static final String DISCONNECTED_ERROR = "Please connect your graph, prior to any usage of it";

    /**
     * SuperTimeTree
     */

    public static final long[] TREE_SCALES = {1000, 10000, 100000, 1000000};

    /**
     * Error messages
     */
    public static String DEAD_NODE_ERROR = "This Node has been tagged destroyed, please don't use it anymore!";


}
