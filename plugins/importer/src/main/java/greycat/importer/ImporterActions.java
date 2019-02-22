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
package greycat.importer;

import greycat.Action;

public class ImporterActions {

    public static final String READFILES = "readFiles";

    public static final String READLINES = "readLines";

    public static final String SPLIT = "split";

    /*
    public static final String READJSON = "readJson";

    public static final String JSONMATCH = "jsonMatch";
*/
    public static Action split(String path) {
        return new ActionSplit(path);
    }

    public static Action readLines(String path) {
        return new ActionReadLines(path);
    }

    public static Action readFiles(String pathOrVar) {
        return new ActionReadFiles(pathOrVar);
    }

    /*
    public static Action readJson(String pathOrVar) {
        return action(READJSON, pathOrVar);
    }
*/
}
