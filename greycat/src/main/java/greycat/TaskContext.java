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
package greycat;

import greycat.struct.Buffer;
import greycat.utility.LMap;
import greycat.utility.Tuple;

public interface TaskContext {

    TaskContext setEndHook(final Callback<TaskContext> endHook);

    Graph graph();

    long world();

    TaskContext setWorld(long world);

    long time();

    TaskContext setTime(long time);

    Tuple<String, TaskResult>[] variables();

    TaskResult variable(String name);

    int intVar(String name);

    double doubleVar(String name);

    long longVar(String name);

    boolean boolVar(String name);

    boolean isGlobal(String name);

    TaskResult wrap(Object input);

    TaskResult wrapClone(Object input);

    TaskResult newResult();

    TaskContext declareVariable(String name);

    void undeclareVariable(String name);

    TaskContext defineVariable(String name, Object initialResult);

    TaskContext defineVariableForSubTask(String name, Object initialResult);

    TaskContext setGlobalVariable(String name, Object value);

    TaskContext setVariable(String name, Object value);

    TaskContext addToGlobalVariable(String name, Object value);

    TaskContext addToVariable(String name, Object value);

    //Object based results
    TaskResult result();

    TaskResult<Node> resultAsNodes();

    TaskResult<String> resultAsStrings();

    int intResult();

    double doubleResult();

    long longResult();

    boolean boolResult();

    void continueTask();

    void continueWith(TaskResult nextResult);

    void endTask(TaskResult nextResult, Exception e);

    String template(String input);

    String[] templates(String[] inputs);

    void append(String additionalOutput);

    void silentSave();

    Buffer notifier();

    void saveToBuffer(Buffer buffer);

    void loadFromBuffer(Buffer buffer, Callback<Boolean> loaded);

    Callback<String> printHook();

    void setPrintHook(Callback<String> callback);

    Callback<TaskProgressReport> progressHook();

    void reportProgress(double progress, String comment);

    void setProgressHook(Callback<TaskProgressReport> hook);
    void setProgressAutoReport(boolean activate);

    void initTracker();

    void removeTracker();

    LMap tracker();

    void track(Node ptr);

}
