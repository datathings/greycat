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
package org.mwg.benchmark;

import com.eclipsesource.json.JsonArray;

public class JsonHandler {

    public static final JsonArray global = new JsonArray();

    public static final String METRIC_TEMPORAL_INSERT = "temporalInsert";

    public static final String METRIC_TEMPORAL_READ = "temporalRead";

    public static final String METRIC_TEMPORAL_TRAVERSE_THEN_READ = "temporalTraverseThenRead";

}
