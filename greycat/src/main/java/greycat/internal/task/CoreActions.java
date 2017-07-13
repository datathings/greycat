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

import greycat.*;

public class CoreActions {

    //Context manipulation zone

    public static Action flat() {
        return new ActionFlat();
    }

    public static Action delete() {
        return new ActionDelete();
    }

    /**
     * Sets the task context to a particular world.
     *
     * @param world the world to which the task context will be set to
     * @return the action to chain
     */
    public static Action travelInWorld(String world) {
        return new ActionTravelInWorld(world);
    }

    /**
     * Switches the time of the task context, i.e. travels the task context in time.
     *
     * @param time time to which the task context is switched to for all nodes
     * @return the action to chain
     */
    public static Action travelInTime(String time) {
        return new ActionTravelInTime(time);
    }

    /**
     * Injects an external object to the current task context.
     *
     * @param input object to be injected into the task context
     * @return the action to chain
     */
    public static Action inject(Object input) {
        return new ActionInject(input);
    }

    /**
     * Stores the task result as a global variable in the task context and starts a new scope (for sub tasks).
     *
     * @param name of the global variable
     * @return the action to chain
     */
    public static Action defineAsGlobalVar(String name) {
        return new ActionDefineAsVar(name, true);
    }

    /**
     * Stores the task result as a local variable in the task context and starts a new scope (for sub tasks).
     *
     * @param name name of the local variable
     * @return the action to chain
     */
    public static Action defineAsVar(String name) {
        return new ActionDefineAsVar(name, false);
    }

    /**
     * Declares a new global variable.
     *
     * @param name of the global variable
     * @return the action to chain
     */
    public static Action declareGlobalVar(String name) {
        return new ActionDeclareVar(true, name);
    }

    /**
     * Declare a new local variable.
     *
     * @param name of the local variable
     * @return the action to chain
     */
    public static Action declareVar(String name) {
        return new ActionDeclareVar(false, name);
    }

    /**
     * Retrieves a stored variable. To reach a particular index, a default array notation can be used.
     * Therefore, A[B] will be interpreted as: extract value stored at index B from the variable A.
     *
     * @param name interpreted as a template
     * @return the action to chain
     */
    public static Action readVar(String name) {
        return new ActionReadVar(name);
    }

    /**
     * Flip the content of named variable with result. Current result will be stored in the variable and the previously stored variable content will be enqueue has current result
     *
     * @param name of the variable
     * @return the action to chain
     */
    public static Action flipVar(String name) {
        return new ActionFlipVar(name);
    }

    /**
     * Stores the current task result into a named variable without starting a new scope.
     *
     * @param name of the variable
     * @return the action to chain
     */
    public static Action setAsVar(String name) {
        return new ActionSetAsVar(name);
    }

    /**
     * Adds the current task result to the named variable.
     *
     * @param name of the variable
     * @return the action to chain
     */
    public static Action addToVar(String name) {
        return new ActionAddToVar(name);
    }

    //Attribute manipulation zone

    /**
     * Sets the value of an attribute for all nodes present in the current result.
     * If value is similar to the previously stored one, nodes will remain unmodified.
     *
     * @param name  must be unique per node
     * @param type  must be one of {@link Type} int values
     * @param value will be interpreted as a template
     * @return the action to chain
     */
    public static Action setAttribute(final String name, final int type, final String value) {
        return new ActionSetAttribute(name, Type.typeName(type), value, false);
    }

    public static Action timeSensitivity(final String delta, final String offset) {
        return new ActionTimeSensitivity(delta, offset);
    }

    public static Action traverseTimeline(String start, String end, String limit) {
        return new ActionTraverseTimeline(start, end, limit);
    }

    /**
     * Forces the value of an attribute for all nodes present in the current result.
     * If value is similar to the previously stored one, nodes will still be modified and their timeline will be affected.
     *
     * @param name  must be unique per node
     * @param type  must be one of {@link Type} int values
     * @param value will be interpreted as template
     * @return the action to chain
     */
    public static Action forceAttribute(String name, int type, String value) {
        return new ActionSetAttribute(name, Type.typeName(type), value, true);
    }

    /**
     * Removes an attribute from a node or an array of nodes.
     * The node (or the array) must be initialised in the previous task
     *
     * @param name the name of the attribute to remove
     * @return the action to chain
     */
    public static Action remove(String name) {
        return new ActionRemove(name);
    }

    /**
     * Retrieves all attribute names of nodes present in the previous task result.
     *
     * @return the action to chain
     */
    public static Action attributes() {
        return new ActionAttributes(null);
    }

    /**
     * Gets and filters all attribute names of nodes present in the previous result.
     *
     * @param filterType type of attributes to filter
     * @return the action to chain
     */
    public static Action attributesWithTypes(int filterType) {
        return new ActionAttributes(Type.typeName(filterType));
    }

    /**
     * Adds nodes present in the named variable to the named traverse in all nodes present in the current task result.
     *
     * @param relName name of the traverse
     * @param varName the name of the variable containing the nodes to add. It can use templates "{{}}".
     * @return the action to chain
     */
    public static Action addVarTo(String relName, String varName) {
        return new ActionAddRemoveVarTo(true, relName, varName);
    }

    /**
     * Removes nodes present in the named variable from the named traverse in all nodes present in the current result.
     *
     * @param relName name of the traverse.
     * @param varFrom the name of the variable containing the nodes to remove. It can use templates "{{}}".
     * @return the action to chain
     */
    public static Action removeVarFrom(String relName, String varFrom) {
        return new ActionAddRemoveVarTo(false, relName, varFrom);
    }

    /**
     * Retrieves any nodes contained in a relations of the nodes present in the current result.
     *
     * @param name of property to retrieve
     * @return the action to chain
     */
    public static Action traverse(String name, String... params) {
        return new ActionTraverseOrAttribute(false, false, name, params);
    }

    /**
     * Retrieves any attribute(s) contained in the nodes present in the current result.
     *
     * @param name of property to retrieve
     * @return the action to chain
     */
    public static Action attribute(String name, String... params) {
        return new ActionTraverseOrAttribute(true, false, name, params);
    }

    //Index manipulation zone

    /**
     * Retrieves indexed nodes matching the query.
     *
     * @param indexName name of the index to use
     * @param query     query to filter nodes, such as 'name', 'FOO' to look for nodes with name == FOO
     * @return the action to chain
     */
    public static Action readIndex(String indexName, String... query) {
        return new ActionReadIndex(indexName, query);
    }

    /**
     * Adds node to the named global index; updates if the node is already indexed
     *
     * @param name of the index
     * @return the action to chain
     */
    public static Action updateIndex(String name) {
        return new ActionUpdateIndex(name, true);
    }

    /**
     * Removes the node from the named global index.
     *
     * @param name of the index
     * @return the action to chain
     */
    public static Action unindexFrom(String name) {
        return new ActionUpdateIndex(name, false);
    }

    public static Action declareIndex(String name, String... attributes) {
        return new ActionDeclareIndex(false, name, attributes);
    }

    public static Action declareLocalIndex(String name, String... attributes) {
        return new ActionDeclareLocalIndex(name, attributes);
    }

    public static Action declareTimedIndex(String name, String... attributes) {
        return new ActionDeclareIndex(true, name, attributes);
    }

    /**
     * Retrieves all index names.
     *
     * @return the action to chain
     */
    public static Action indexNames() {
        return new ActionIndexNames();
    }

    /**
     * Filters the previous result to keep nodes, which named attribute has a specific value.
     *
     * @param name    the name of the attribute used to filter
     * @param pattern the value nodes must have for this attribute
     * @return the action to chain
     */
    public static Action selectWith(String name, String pattern) {
        return new ActionWith(name, pattern);
    }

    /**
     * Filters the previous result to keep nodes, which named attribute does not have a given value.
     *
     * @param name    the name of the attribute used to filter
     * @param pattern the value nodes must not have for this attribute
     * @return the action to chain
     */
    public static Action selectWithout(String name, String pattern) {
        return new ActionWithout(name, pattern);
    }

    /**
     * Filters the previous result to get nodes that comply to the condition specified in {@code filterFunction}
     *
     * @param filterFunction condition that nodes have to respect
     * @return the action to chain
     */
    public static Action select(TaskFunctionSelect filterFunction) {
        return new ActionSelect(null, filterFunction);
    }

    /**
     * Selects an object complying to the filter function.
     *
     * @param filterFunction condition that objects have to respect
     * @return the action to chain
     */
    public static Action selectObject(TaskFunctionSelectObject filterFunction) {
        return new ActionSelectObject(filterFunction);
    }

    /**
     * Use a JS script to filter nodes
     * The task context is inject in the variable 'context'. The current node is inject in the variable 'node'.
     * <p>
     * Should only be used to serialize {@link #select(TaskFunctionSelect)}
     *
     * @return the action to chain
     */
    public static Action selectScript(String script) {
        return new ActionSelect(script, null);
    }

    //Helper zone

    /**
     * Log the template string in a human readable format.
     *
     * @param value of the action
     * @return the action to chain
     */
    public static Action log(String value) {
        return new ActionLog(value);
    }

    /**
     * Prints the action in a human readable format (without line breaks).
     *
     * @param name of the action
     * @return the action to chain
     */
    public static Action print(String name) {
        return new ActionPrint(name, false);
    }

    /**
     * Prints the action in a human readable format (with line breaks).
     *
     * @param name of the action
     * @return the action to chain
     */
    public static Action println(String name) {
        return new ActionPrint(name, true);
    }


    //Execution manipulation zone

    /**
     * Executes an expression on all nodes given from the previous step
     *
     * @param expression the to execute
     * @return the action to chain
     */
    public static Action executeExpression(String expression) {
        return new ActionExecuteExpression(expression);
    }

    /**
     * Registers a named action to the action registry.
     * This hook allows to extend the Task API with additional actions.
     *
     * @param name   name of the action to add, should correspond to the name of the registered Task plugin.
     * @param params parameters of the action
     * @return the action to chain
     */
    public static Action action(String name, String... params) {
        return new ActionNamed(name, params);
    }

    /**
     * Creates a new node in the [world,time] of the current context.
     *
     * @return the action to chain
     */
    public static Action createNode() {
        return new ActionCreateNode(null);
    }

    /**
     * Creates a new typed node in the [world,time] of the current context.
     *
     * @param type the type name of the node
     * @return the action to chain
     */
    public static Action createTypedNode(String type) {
        return new ActionCreateNode(type);
    }

    /**
     * Saves the graph to a storage.
     *
     * @return the action to chain
     */
    public static Action save() {
        return new ActionSave();
    }

    /**
     * Start a transactional task.
     *
     * @return the action to chain
     */
    public static Action startTransaction() {
        return new ActionStartTransaction();
    }

    /**
     * Start a transactional task.
     *
     * @return the action to chain
     */
    public static Action stopTransaction() {
        return new ActionStopTransaction();
    }


    /**
     * Execute a JS script
     * The task context is inject in the variable 'ctx'
     * <p>
     * Should only be used to serialize {@link Task#thenDo(ActionFunction)}.
     *
     * @return the action to chain
     */
    public static Action script(String script) {
        return new ActionScript(script, false);
    }

    /**
     * Execute an async JS script
     * The task context is inject in the variable 'ctx'
     * <p>
     * Should only be used to serialize {@link Task#thenDo(ActionFunction)}.
     *
     * @return the action to chain
     */
    public static Action asyncScript(String script) {
        return new ActionScript(script, true);
    }

    /**
     * Retrieves in the current [world,time] a named node.
     *
     * @param nodeId id of the node to lookup
     * @return the action to chain
     */
    public static Action lookup(String nodeId) {
        return new ActionLookup(nodeId);
    }

    /**
     * Retrieves in the current [world,time] the named nodes.
     *
     * @param nodeIds ids of the nodes to lookup
     * @return the action to chain
     */
    public static Action lookupAll(String nodeIds) {
        return new ActionLookupAll(nodeIds);
    }

    public static Action timepoints(String from, String to) {
        return new ActionTimepoints(from, to);
    }

    /**
     * Resets the result of the task context.
     *
     * @return the action to chain
     */
    public static Action clearResult() {
        return new ActionClearResult();
    }

    /**
     * Resets the result of the task context.
     *
     * @return the action to chain
     */
    public static Action cloneResult() {
        return new ActionCloneNodes();
    }

}
