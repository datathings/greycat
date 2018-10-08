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

import greycat.plugin.SchedulerAffinity;
import greycat.struct.Buffer;

public interface Task {

    /**
     * Chains actions.
     *
     * @param nextAction to chain
     * @return this task to chain
     */
    Task then(Action nextAction);

    /**
     * Chains action functions.
     *
     * @param nextActionFunction next action function to chain
     * @return this task to chain
     */
    Task thenDo(ActionFunction nextActionFunction);

    /**
     * Executes a give task until a given condition evaluates to true.
     *
     * @param task to execute
     * @param cond condition to check
     * @return this task to chain
     */
    Task doWhile(Task task, ConditionalFunction cond);

    /**
     * Executes a give task until a given condition evaluates to true.
     *
     * @param task       to execute
     * @param condScript condition to check (JavaScript)
     * @return this task to chain
     */
    Task doWhileScript(Task task, String condScript);

    /**
     * Executes a task in a range.
     *
     * @param from    range start
     * @param to      range end
     * @param subTask to execute
     * @return this task to chain
     */
    Task loop(String from, String to, Task subTask);

    /**
     * Parallel version of {@link #loop(String, String, Task)}.
     * Executes a task in a range. Steps can be executed in parallel. Creates as many threads as elements in the collection.
     *
     * @param from    range start
     * @param to      range end
     * @param subTask task to execute
     * @return this task to chain
     */
    Task loopPar(String from, String to, Task subTask);

    /**
     * Iterates through a collection and calls the sub task for each element.
     *
     * @param subTask sub task to call for each element
     * @return this task to chain
     */
    Task forEach(Task subTask);

    /**
     * Parallel version of {@link #forEach(Task)}.
     * All sub tasks can be called in parallel. Creates as many threads as elements in the collection.
     *
     * @param subTask sub task to call for each element
     * @return this task to chain
     */
    Task forEachPar(Task subTask);

    /**
     * Flat a TaskResult containing TaskResult to a flat TaskResult
     *
     * @return this task to chain
     */
    Task flat();

    /**
     * Iterates through a collection and calls the sub task for each element and then aggregates all results in an array of array manner.
     *
     * @param subTask sub task to call for each element
     * @return this task to chain
     */
    Task map(Task subTask);

    /**
     * Parallel version of {@link #map(Task)}.
     * Iterates through a collection and calls the sub task for each element in parallel and then aggregates all results in an array of array manner.
     *
     * @param subTask sub task to call for each element
     * @return this task to chain
     */
    Task mapPar(Task subTask);

    /**
     * Executes a sub task if a given condition is evaluated to true.
     *
     * @param cond condition to check
     * @param then sub task to execute if the condition is evaluated to true
     * @return this task to chain
     */
    Task ifThen(ConditionalFunction cond, Task then);

    /**
     * Executes a sub task if a given condition is evaluated to true.
     *
     * @param condScript condition to check (JavaScript)
     * @param then       sub task to execute if the condition is evaluated to true
     * @return this task to chain
     */
    Task ifThenScript(String condScript, Task then);

    /**
     * Executes a sub task if a given condition is evaluated to true.
     *
     * @param cond    condition to check
     * @param thenSub sub task to execute if the condition is evaluate to true
     * @param elseSub sub task to execute if the condition is evaluated to false
     * @return this task to chain
     */
    Task ifThenElse(ConditionalFunction cond, Task thenSub, Task elseSub);

    /**
     * Executes a sub task if a given condition is evaluated to true.
     *
     * @param condScript condition to check (JavaScript)
     * @param thenSub    sub task to execute if the condition is evaluate to true
     * @param elseSub    sub task to execute if the condition is evaluated to false
     * @return this task to chain
     */
    Task ifThenElseScript(String condScript, Task thenSub, Task elseSub);

    /**
     * Similar to {@link #doWhile(Task, ConditionalFunction)} but the task is at least executed once.
     *
     * @param cond condition to check
     * @param task to execute
     * @return this task to chain
     */
    Task whileDo(ConditionalFunction cond, Task task);

    /**
     * Similar to {@link #doWhile(Task, ConditionalFunction)} but the task is at least executed once.
     *
     * @param condScript condition to check (JavaScript)
     * @param task       to execute
     * @return this task to chain
     */
    Task whileDoScript(String condScript, Task task);

    /**
     * Executes and waits for a number of given sub tasks.
     * The result of these sub tasks is immediately enqueued and available in the next sub task in a array of array manner.
     *
     * @param subTasks that needs to be executed
     * @return this task to chain
     */
    Task pipe(Task... subTasks);

    /**
     * Parallel version of {@link #pipe(Task...)}.
     * Executes and waits a number of given sub tasks.
     * The result of these sub tasks is immediately enqueued and available in the next sub task in a array of array manner.
     *
     * @param subTasks that have to be executed
     * @return this task to chain
     */
    Task pipePar(Task... subTasks);

    /**
     * Executes a given sub task in an isolated environment.
     *
     * @param subTask to execute
     * @return this task to chain
     */
    Task pipeTo(Task subTask, String... vars);

    Task traverseTimeline(String start, String end, String limit);

    Task traverseTimelineSparse(String start, String end, String limit);

    /**
     * Parses a string to build the current task.
     * Syntax is as follows: actionName(param).actionName2(param2)...
     * In case actionName() is empty, the default task is get(name).
     * This results in the following: children.name should be read as get(children).get(name)
     *
     * @param input string definition of the task
     * @return this task to chain
     */
    Task parse(final String input, final Graph graph);

    Task loadFromBuffer(final Buffer buffer, final Graph graph);

    Task saveToBuffer(final Buffer buffer);

    /**
     * Creates a hook to extend the Task API.
     *
     * @param hook
     * @return this task to chain
     */
    Task addHook(TaskHook hook);

    /**
     * Executes the defined chain of tasks on a graph in an asynchronous way.
     *
     * @param graph    the graph where the execution is applied to
     * @param callback to notify when the chain of tasks has been executed
     */
    void execute(final Graph graph, final Callback<TaskResult> callback);

    /**
     * Executes the defined chain of tasks on a graph in an asynchronous way on remote storage.
     *
     * @param graph    the graph where the execution is applied to
     * @param callback to notify when the chain of tasks has been executed
     */
    void executeRemotely(final Graph graph, final Callback<TaskResult> callback);

    /**
     * Executes a prepared task context on remote storage.
     * See {@link #prepare(Graph, Object, Callback)}.
     *
     * @param preparedContext the prepared task context
     */
    void executeRemotelyUsing(final TaskContext preparedContext);

    /**
     * Synchronous version of {@link #execute(Graph, Callback)}.
     *
     * @param graph where the execcution is applied to
     * @return the task result
     */
    TaskResult executeSync(final Graph graph);

    /**
     * Executes the defined chain of tasks with an initial task result object.
     *
     * @param graph    where the execution is applied to
     * @param initial  initial object of the task result
     * @param callback to notify when the chain of tasks has been executed
     */
    void executeWith(final Graph graph, final Object initial, final Callback<TaskResult> callback);

    /**
     * Prepares a context in order to initialize the task context.
     *
     * @param graph    where the execution is applied to
     * @param initial  initial object for the task context
     * @param callback to notify when the preparation is executed
     * @return the prepared task context
     */
    TaskContext prepare(final Graph graph, final Object initial, final Callback<TaskResult> callback);

    /**
     * Executes a prepared task context.
     * See {@link #prepare(Graph, Object, Callback)}.
     *
     * @param preparedContext the prepared task context
     */
    void executeUsing(TaskContext preparedContext);

    /**
     * @param parentContext
     * @param initial       initial task result
     * @param affinity      defines the thread affinity, see {@link SchedulerAffinity}
     * @param callback      notifies when the execution is done
     */
    void executeFrom(final TaskContext parentContext, final TaskResult initial, final byte affinity, final Callback<TaskResult> callback);

    /**
     * Executes a prepared task context.
     * Similar to {@link #executeUsing(TaskContext)} but the task context can be initialized within a callback.
     *
     * @param parentContext      context of the parent task
     * @param initial            initial object for the task context
     * @param affinity           defines the thread affinity, see {@link SchedulerAffinity}
     * @param contextInitializer callback to initialize the context
     * @param callback           notifies when the execution is done
     */
    void executeFromUsing(final TaskContext parentContext, final TaskResult initial, final byte affinity, final Callback<TaskContext> contextInitializer, final Callback<TaskResult> callback);

    /* Default core actions */

    Task travelInWorld(String world);

    Task travelInTime(String time);

    Task inject(Object input);

    Task defineAsGlobalVar(String name);

    Task defineAsVar(String name);

    Task declareGlobalVar(String name);

    Task declareVar(String name);

    Task readVar(String name);

    Task setAsVar(String name);

    Task addToVar(String name);

    Task setAttribute(String name, int type, String value);

    Task timeSensitivity(String delta, String offset);

    Task forceAttribute(String name, int type, String value);

    Task remove(String name);

    Task attributes();

    Task timepoints(String from, String to);

    Task countTimepoints(String from, String to);

    Task attributesWithType(int filterType);

    Task addVarTo(String relName, String varName);

    Task removeVarFrom(String relName, String varFrom);

    Task traverse(String name, String... params);

    Task attribute(String name, String... params);

    Task readIndex(String indexName, String... query);

    Task updateIndex(String name);

    Task unindexFrom(String name);

    Task declareIndex(String indexName, String... indexedAttributesNames);

    Task declareLocalIndex(String name, String... attributes);

    Task declareTimedIndex(String indexName, String... indexedAttributesNames);

    Task indexNames();

    Task selectWith(String name, String pattern);

    Task selectWithout(String name, String pattern);

    Task select(TaskFunctionSelect filterFunction);

    Task selectScript(String script);

    Task selectObject(TaskFunctionSelectObject filterFunction);

    Task log(String name);

    Task print(String name);

    Task println(String name);

    Task executeExpression(String expression);

    Task createNode();

    Task createTypedNode(String type);

    Task save();

    Task saveAbove(String threshold);

    Task startTransaction();

    Task stopTransaction();

    Task script(String script);

    Task asyncScript(String ascript);

    Task lookup(String nodeId);

    Task lookupAll(String nodeIds);

    Task clearResult();

    Task cloneNodes();

    Task action(String name, String... params);

    Task flipVar(String name);

    Task atomic(Task protectedTask, String... variablesToLock);

    Task remote(Task sub);

    Task delete();

    Task startTimer(String timerName);

    Task stopTimer(String message, String timerName, String counterName, String displayEach);

    Task getElement(String index);
}
