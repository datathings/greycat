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
package greycat.internal.task;

import greycat.*;
import greycat.base.BaseTaskResult;
import greycat.chunk.StateChunk;
import greycat.internal.CoreConstants;
import greycat.internal.task.math.CoreMathExpressionEngine;
import greycat.internal.task.math.MathExpressionEngine;
import greycat.base.BaseNode;
import greycat.struct.Buffer;
import greycat.utility.*;
import greycat.workers.SlaveWorkerStorage;
import greycat.workers.WorkerAffinity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static greycat.utility.L3GMap.GROUP;

class CoreTaskContext implements TaskContext {

    private final Map<String, TaskResult> _globalVariables;
    private final TaskContext _parent;
    private final Graph _graph;

    final Callback<TaskResult> _callback;
    private Callback<TaskContext> _end_hook;

    private Map<String, TaskResult> _localVariables = null;
    private Map<String, TaskResult> _nextVariables = null;
    TaskResult _result;
    private long _world;
    private long _time;
    private final CoreTask _origin;
    private int cursor = 0;
    TaskHook[] _hooks;
    private StringBuilder _output = null;
    private Buffer _silent;
    private Callback<String> _printHook = null;
    private Callback<TaskProgressReport> _progressHook = null;
    private boolean _taskProgressAutoReporting = false;
    private LMap _transactionTracker = null;
    private byte _workerAffinity = WorkerAffinity.GENERAL_PURPOSE_WORKER;

    int in_registry_id;
    AtomicBoolean ext_stop;

    CoreTaskContext(final CoreTask origin, final TaskHook[] p_hooks, final TaskContext parentContext, final TaskResult initial, final Graph p_graph, final Callback<TaskResult> p_callback) {
        this.in_registry_id = -1;

        if (parentContext != null) {
            this.ext_stop = ((CoreTaskContext) parentContext).ext_stop;
        } else {
            this.ext_stop = new AtomicBoolean(false);
        }

        this._origin = origin;
        this._hooks = p_hooks;
        if (parentContext != null) {
            this._time = parentContext.time();
            this._world = parentContext.world();
        } else {
            this._world = 0;
            this._time = Constants.BEGINNING_OF_TIME;
        }
        this._graph = p_graph;
        this._parent = parentContext;
        final CoreTaskContext castedParentContext = (CoreTaskContext) parentContext;
        if (parentContext == null) {
            this._globalVariables = new ConcurrentHashMap<String, TaskResult>();
            this._silent = null;
            this._transactionTracker = null;
        } else {
            this._globalVariables = castedParentContext.globalVariables();
            this._silent = castedParentContext._silent;
            this._transactionTracker = castedParentContext._transactionTracker;
        }
        this._result = initial;
        this._callback = p_callback;
    }

    @Override
    public final TaskContext setEndHook(final Callback<TaskContext> endHook) {
        this._end_hook = endHook;
        return this;
    }

    @Override
    public final Graph graph() {
        return _graph;
    }

    @Override
    public final long world() {
        return this._world;
    }

    @Override
    public final TaskContext setWorld(long p_world) {
        this._world = p_world;
        return this;
    }

    @Override
    public final long time() {
        return this._time;
    }

    @Override
    public final TaskContext setTime(long p_time) {
        this._time = p_time;
        return this;
    }

    @Override
    public Callback<TaskResult> getResultCallback() {
        return _callback;
    }

    @Override
    public byte getWorkerAffinity() {
        return _workerAffinity;
    }

    @Override
    public void setWorkerAffinity(byte affinity) {
        this._workerAffinity = affinity;
    }

    @Override
    public final Tuple<String, TaskResult>[] variables() {
        Map<String, TaskResult> collected = new HashMap<String, TaskResult>();
        String[] globalKeys = _globalVariables.keySet().toArray(new String[_globalVariables.size()]);
        for (int i = 0; i < globalKeys.length; i++) {
            collected.put(globalKeys[i], _globalVariables.get(globalKeys[i]));
        }
        if (_localVariables != null) {
            String[] localKeys = _localVariables.keySet().toArray(new String[_localVariables.size()]);
            for (int i = 0; i < localKeys.length; i++) {
                collected.put(localKeys[i], _localVariables.get(localKeys[i]));
            }
        }

        //recursive_collect(this, collected);
        //flatResult
        String[] collectedKeys = collected.keySet().toArray(new String[collected.size()]);
        Tuple<String, TaskResult>[] result = new Tuple[collectedKeys.length];
        for (int i = 0; i < collectedKeys.length; i++) {
            result[i] = new Tuple<String, TaskResult>(collectedKeys[i], collected.get(collectedKeys[i]));
        }
        return result;
    }

    private void recursive_collect(TaskContext ctx, Map<String, TaskResult> collector) {
        Map<String, TaskResult> localVariables = ((CoreTaskContext) ctx).localVariables();
        if (localVariables != null) {
            String[] localKeys = localVariables.keySet().toArray(new String[localVariables.size()]);
            for (int i = 0; i < localKeys.length; i++) {
                if (!collector.containsKey(localKeys[i])) {
                    collector.put(localKeys[i], localVariables.get(localKeys[i]));
                }
            }
        }
        if (((CoreTaskContext) ctx)._parent != null) {
            recursive_collect(((CoreTaskContext) ctx)._parent, collector);
        }
    }

    @Override
    public final TaskResult variable(final String name) {
        if (name == null) {
            return null;
        }
        TaskResult resolved = this._globalVariables.get(name);
        if (resolved == null) {
            resolved = internal_deep_resolve(name);
        }
        return resolved;
    }

    @Override
    public int intVar(String name) {
        TaskResult res = variable(name);
        if (res != null && res.size() > 0) {
            return (int) res.get(0);
        }
        return -1;
    }

    @Override
    public double doubleVar(String name) {
        TaskResult res = variable(name);
        if (res != null && res.size() > 0) {
            return (double) res.get(0);
        }
        return -1d;
    }

    @Override
    public long longVar(String name) {
        TaskResult res = variable(name);
        if (res != null && res.size() > 0) {
            return (long) res.get(0);
        }
        return -1L;
    }

    @Override
    public boolean boolVar(String name) {
        TaskResult res = variable(name);
        if (res != null && res.size() > 0) {
            return (boolean) res.get(0);
        }
        _graph.log().warn("variable is incorrect, returning false");
        return false;
    }

    @Override
    public boolean isGlobal(String name) {
        return _globalVariables.containsKey(name);
    }

    private TaskResult internal_deep_resolve(final String name) {
        TaskResult resolved = null;
        if (this._localVariables != null) {
            resolved = this._localVariables.get(name);
        }
        if (resolved == null && this._parent != null) {
            final CoreTaskContext castedParent = (CoreTaskContext) _parent;
            if (castedParent._nextVariables != null) {
                resolved = castedParent._nextVariables.get(name);
                if (resolved != null) {
                    return resolved;
                }
            }
            return castedParent.internal_deep_resolve(name);
        } else {
            return resolved;
        }
    }

    @Override
    public TaskResult wrap(Object input) {
        //if(input instanceof TaskResult){
        //    return (TaskResult) input;
        //} else {
        return new BaseTaskResult(input, false);
        //}
    }

    @Override
    public TaskResult wrapClone(Object input) {
        return new BaseTaskResult(input, true);
    }

    @Override
    public TaskResult newResult() {
        return new BaseTaskResult(null, false);
    }

    @Override
    public TaskContext declareVariable(final String name) {
        if (this._localVariables == null) {
            this._localVariables = new HashMap<String, TaskResult>();
        }
        this._localVariables.put(name, new BaseTaskResult(null, false));
        return this;
    }

    @Override
    public void undeclareVariable(final String name) {
        if (this._localVariables != null) {
            this._localVariables.remove(name);
        }
    }


    private TaskResult lazyWrap(Object input) {
        if (input instanceof BaseTaskResult) {
            return (TaskResult) input;
        } else {
            return wrap(input);
        }
    }

    @Override
    public TaskContext defineVariable(final String name, Object initialResult) {
        if (this._localVariables == null) {
            this._localVariables = new HashMap<String, TaskResult>();
        }
        this._localVariables.put(name, lazyWrap(initialResult).clone());
        return this;
    }

    @Override
    public TaskContext defineVariableForSubTask(String name, Object initialResult) {
        if (this._nextVariables == null) {
            this._nextVariables = new HashMap<String, TaskResult>();
        }
        this._nextVariables.put(name, lazyWrap(initialResult).clone());
        return this;
    }

    @Override
    public final TaskContext setGlobalVariable(final String name, final Object value) {
        final TaskResult previous = this._globalVariables.put(name, lazyWrap(value).clone());
        if (previous != null) {
            previous.free();
        }
        return this;
    }

    @Override
    public final TaskContext setVariable(final String name, final Object value) {
        Map<String, TaskResult> target = internal_deep_resolve_map(name);
        if (target == null) {
            if (this._localVariables == null) {
                this._localVariables = new HashMap<String, TaskResult>();
            }
            target = this._localVariables;
        }
        final TaskResult previous = target.put(name, lazyWrap(value).clone());
        if (previous != null) {
            previous.free();
        }
        return this;
    }

    private Map<String, TaskResult> internal_deep_resolve_map(final String name) {
        if (this._localVariables != null) {
            TaskResult resolved = this._localVariables.get(name);
            if (resolved != null) {
                return this._localVariables;
            }
        }
        if (this._parent != null) {
            final CoreTaskContext castedParent = (CoreTaskContext) _parent;
            if (castedParent._nextVariables != null) {
                TaskResult resolved = castedParent._nextVariables.get(name);
                if (resolved != null) {
                    return this._localVariables;
                }
            }
            return ((CoreTaskContext) _parent).internal_deep_resolve_map(name);
        } else {
            return null;
        }
    }

    @Override
    public final TaskContext addToGlobalVariable(final String name, final Object value) {
        TaskResult previous = this._globalVariables.get(name);
        if (previous == null) {
            previous = new BaseTaskResult(null, false);
            this._globalVariables.put(name, previous);
        }
        if (value instanceof BaseTaskResult) {
            TaskResult casted = (TaskResult) value;
            for (int i = 0; i < casted.size(); i++) {
                final Object loop = casted.get(i);
                if (loop instanceof BaseNode) {
                    final Node castedNode = (Node) loop;
                    previous.add(castedNode.graph().cloneNode(castedNode));
                } else {
                    previous.add(loop);
                }
            }
        } else if (value instanceof BaseNode) {
            final Node castedNode = (Node) value;
            previous.add(castedNode.graph().cloneNode(castedNode));
        } else {
            previous.add(value);
        }
        return this;
    }

    @Override
    public final TaskContext addToVariable(final String name, final Object value) {
        Map<String, TaskResult> target = internal_deep_resolve_map(name);
        if (target == null) {
            if (this._localVariables == null) {
                this._localVariables = new HashMap<String, TaskResult>();
            }
            target = this._localVariables;
        }
        TaskResult previous = target.get(name);
        if (previous == null) {
            previous = new BaseTaskResult(null, false);
            target.put(name, previous);
        }
        if (value instanceof BaseTaskResult) {
            TaskResult casted = (TaskResult) value;
            for (int i = 0; i < casted.size(); i++) {
                final Object loop = casted.get(i);
                if (loop instanceof BaseNode) {
                    final Node castedNode = (Node) loop;
                    previous.add(castedNode.graph().cloneNode(castedNode));
                } else {
                    previous.add(loop);
                }
            }
        } else if (value instanceof BaseNode) {
            final Node castedNode = (Node) value;
            previous.add(castedNode.graph().cloneNode(castedNode));
        } else {
            previous.add(value);
        }
        return this;
    }

    Map<String, TaskResult> globalVariables() {
        return this._globalVariables;
    }

    Map<String, TaskResult> localVariables() {
        return this._localVariables;
    }

    @Override
    public final TaskResult result() {
        return this._result;
    }

    @Override
    public TaskResult<Node> resultAsNodes() {
        return (TaskResult<Node>) _result;
    }

    @Override
    public TaskResult<String> resultAsStrings() {
        return (TaskResult<String>) _result;
    }

    @Override
    public int intResult() {
        if (_result != null && _result.size() > 0) {
            return (int) _result.get(0);
        }
        return -1;
    }

    @Override
    public double doubleResult() {
        if (_result != null && _result.size() > 0) {
            return (double) _result.get(0);
        }
        return -1;
    }

    @Override
    public long longResult() {
        if (_result != null && _result.size() > 0) {
            return (long) _result.get(0);
        }
        return -1;
    }

    @Override
    public boolean boolResult() {
        if (_result != null && _result.size() > 0) {
            return (boolean) _result.get(0);
        }
        _graph.log().warn("result is incorrect, returning false");
        return false;
    }

    @Override
    public final void continueWith(TaskResult nextResult) {
        final TaskResult previousResult = this._result;
        if (previousResult != null && previousResult != nextResult) {
            previousResult.free();
        }
        _result = nextResult;
        continueTask();
    }

    @Override
    public final void continueTask() {
        if (this.ext_stop.get()) {
            endTask(newResult(), new RuntimeException("stopped from external!"));
            return;
        }
        final TaskHook[] globalHooks = this._graph.taskHooks();
        final Action currentAction = _origin.actions[cursor];
        //next step now...
        if (_hooks != null) {
            for (int i = 0; i < _hooks.length; i++) {
                _hooks[i].afterAction(currentAction, this);
            }
        }
        if (globalHooks != null) {
            for (int i = 0; i < globalHooks.length; i++) {
                globalHooks[i].afterAction(currentAction, this);
            }
        }
        if (_transactionTracker != null) {
            internal_track_result((BaseTaskResult) _result);
        }
        cursor++;
        final Action nextAction;
        if (cursor == _origin.insertCursor) {
            nextAction = null;
        } else {
            nextAction = _origin.actions[cursor];
        }
        if (nextAction == null) {
            endTask(null, null);
        } else {
            if (_hooks != null) {
                for (int i = 0; i < _hooks.length; i++) {
                    _hooks[i].beforeAction(nextAction, this);
                }
            }
            if (globalHooks != null) {
                for (int i = 0; i < globalHooks.length; i++) {
                    globalHooks[i].beforeAction(nextAction, this);
                }
            }
            if (this._taskProgressAutoReporting) {
                reportProgress(0, null);
            }
            final int previousCursor = cursor;
            try {
                nextAction.eval(this);
            } catch (Exception e) {
                if (cursor == previousCursor) {
                    endTask(null, e);
                } else {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @native ts
     * return null;
     */
    @Override
    public final int[] suspendTask() {
        if (!(this.graph().storage() instanceof SlaveWorkerStorage)) {
            throw new RuntimeException("suspend task can only be used with GraphWorker");
        }
        SlaveWorkerStorage slaveWorkerStorage = (SlaveWorkerStorage) this.graph().storage();
        final TaskContext self = this;
        int callbackId = slaveWorkerStorage.getCallbacksRegistry().register(result -> {
            self.continueWith(new BaseTaskResult(result, false));
        });
        return new int[]{slaveWorkerStorage.getWorkerMailboxId(), callbackId};
    }

    @Override
    public void endTask(final TaskResult preFinalResult, final Exception e) {
        if (this.in_registry_id != -1) {
            CoreTaskContextRegistry reg = (CoreTaskContextRegistry) this.graph().taskContextRegistry();
            reg.unregister(this.in_registry_id);
        }
        if (preFinalResult != null) {
            if (_result != null) {
                _result.free();
            }
            _result = preFinalResult;
        }
        final TaskHook[] globalHooks = this._graph.taskHooks();
        /* Clean */
        if (_end_hook != null) {
            _end_hook.on(this);
        }
        if (this._localVariables != null) {
            Set<String> localValues = this._localVariables.keySet();
            String[] flatLocalValues = localValues.toArray(new String[localValues.size()]);
            for (int i = 0; i < flatLocalValues.length; i++) {
                this._localVariables.get(flatLocalValues[i]).free();
            }
        }
        if (this._nextVariables != null) {
            Set<String> nextValues = this._nextVariables.keySet();
            String[] flatNextValues = nextValues.toArray(new String[nextValues.size()]);
            for (int i = 0; i < flatNextValues.length; i++) {
                this._nextVariables.get(flatNextValues[i]).free();
            }
        }
        if (this._parent == null) {
            Set<String> globalValues = this._globalVariables.keySet();
            String[] globalFlatValues = globalValues.toArray(new String[globalValues.size()]);
            for (int i = 0; i < globalFlatValues.length; i++) {
                this._globalVariables.get(globalFlatValues[i]).free();
            }
        }
        /* End Clean */
        if (_hooks != null) {
            for (int i = 0; i < _hooks.length; i++) {
                if (this._parent == null) {
                    _hooks[i].end(this);
                } else {
                    _hooks[i].afterTask(this);
                }
            }
        }
        if (globalHooks != null) {
            for (int i = 0; i < globalHooks.length; i++) {
                if (this._parent == null) {
                    globalHooks[i].end(this);
                } else {
                    globalHooks[i].afterTask(this);
                }
            }
        }
        if (this._callback != null) {
            if (e != null) {
                if (_result == null) {
                    _result = new BaseTaskResult(null, false);
                }
                _result.setException(e);
                if (_silent == null && _parent == null) {
                    e.printStackTrace();
                }
            }
            if (_output != null) {
                if (_result == null) {
                    _result = new BaseTaskResult(null, false);
                }
                _result.setOutput(_output.toString());
                if (_silent == null && _parent == null) {
                    System.out.println(_result.output());
                }
            }
            if (_silent != null) {
                if (_result == null) {
                    _result = new BaseTaskResult(null, false);
                }
                _result.setNotifications(_silent);
            }
            this._callback.on(_result);
        } else {
            if (e != null) {
                e.printStackTrace();
            }
            if (_output != null) {
                System.out.print(_output.toString());
            }
            if (this._result != null) {
                this._result.free();
            }
        }
    }

    @SuppressWarnings("Duplicates")
    final void execute() {
        final Action current = _origin.actions[cursor];
        if (_hooks != null) {
            for (int i = 0; i < _hooks.length; i++) {
                if (_parent == null) {
                    _hooks[i].start(this);
                } else {
                    _hooks[i].beforeTask(_parent, this);
                }
                _hooks[i].beforeAction(current, this);
            }
        }
        final TaskHook[] globalHooks = this._graph.taskHooks();
        if (globalHooks != null) {
            for (int i = 0; i < globalHooks.length; i++) {
                if (_parent == null) {
                    globalHooks[i].start(this);
                } else {
                    globalHooks[i].beforeTask(_parent, this);
                }
                globalHooks[i].beforeAction(current, this);
            }
        }
        try {
            current.eval(this);
        } catch (Exception e) {
            if (cursor == 0) {
                endTask(null, e);
            } else {
                e.printStackTrace();
            }
        }
    }

    @Override
    public final String template(String input) {
        if (input == null) {
            return null;
        }
        int cursor = 0;
        StringBuilder buffer = null;
        int previousPos = -1;
        while (cursor < input.length()) {
            char currentChar = input.charAt(cursor);
            char previousChar = '0';
            char nextChar = '0';
            if (cursor > 0) {
                previousChar = input.charAt(cursor - 1);
            }
            if (cursor + 1 < input.length()) {
                nextChar = input.charAt(cursor + 1);
            }
            if (currentChar == '{' && previousChar == '{') {
                previousPos = cursor + 1;
            } else if (previousPos != -1 && currentChar == '}' && previousChar == '}') {
                if (buffer == null) {
                    buffer = new StringBuilder();
                    buffer.append(input.substring(0, previousPos - 2));
                }
                String contextKey = input.substring(previousPos, cursor - 1).trim();
                if (contextKey.length() > 0 && contextKey.charAt(0) == '=') { //Math expression
                    final MathExpressionEngine mathEngine = CoreMathExpressionEngine.parse(contextKey.substring(1));
                    double value = mathEngine.eval(null, this, new HashMap<String, Double>());
                    //supress ".0" if it exists
                    String valueStr = value + "";
                    for (int i = valueStr.length() - 1; i >= 0; i--) {
                        if (valueStr.charAt(i) == '.') {
                            valueStr = valueStr.substring(0, i);
                            break;
                        } else if (valueStr.charAt(i) != '0') {
                            break;
                        }
                    }
                    buffer.append(valueStr);
                } else {//variable name or array access
                    //check if it is an array access
                    int indexArray = -1;
                    if (contextKey.charAt(contextKey.length() - 1) == ']') {
                        int indexStart = -1;
                        for (int i = contextKey.length() - 3; i >= 0; i--) {
                            if (contextKey.charAt(i) == '[') {
                                indexStart = i + 1;
                                break;
                            }
                        }
                        if (indexStart != -1) {
                            indexArray = TaskHelper.parseInt(contextKey.substring(indexStart, contextKey.length() - 1));
                            contextKey = contextKey.substring(0, indexStart - 1);
                            if (indexArray < 0) {
                                throw new RuntimeException("Array index out of range: " + indexArray);
                            }
                        }
                    }
                    TaskResult foundVar = variable(contextKey);
                    if (foundVar == null) {
                        switch (contextKey) {
                            case "result": {
                                foundVar = result();
                            }
                            break;
                            case "time": {
                                foundVar = wrap(_time);
                            }
                            break;
                            case "world": {
                                foundVar = wrap(_world);
                            }
                            break;
                        }
                    }
                    if (foundVar != null) {
                        if (foundVar.size() == 1 || indexArray != -1) {
                            //show element of array
                            Object toShow = null;
                            if (indexArray == -1) {
                                toShow = foundVar.get(0);
                            } else {
                                toShow = foundVar.get(indexArray);
                            }
                            buffer.append(toShow);
                        } else {
                            //show all
                            TaskResultIterator it = foundVar.iterator();
                            buffer.append("[");
                            boolean isFirst = true;
                            Object next = it.next();
                            while (next != null) {
                                if (isFirst) {
                                    isFirst = false;
                                } else {
                                    buffer.append(",");
                                }
                                buffer.append(next);
                                next = it.next();
                            }
                            buffer.append("]");
                        }
                    }
                }
                previousPos = -1;
            } else {
                if (previousPos == -1 && buffer != null) {
                    //check if we are not opening a {{
                    if (currentChar == '{' && nextChar == '{') {
                        //noop
                    } else {
                        buffer.append(input.charAt(cursor));
                    }
                }
            }
            cursor++;
        }
        if (buffer == null) {
            return input;
        } else {
            return buffer.toString();
        }
    }

    @Override
    public final String[] templates(String[] inputs) {
        if (inputs == null) {
            return null;
        }
        String[] result = new String[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            result[i] = template(inputs[i]);
        }
        return result;
    }

    @Override
    public final synchronized void append(String additionalOutput) {
        if (_printHook != null) {
            _printHook.on(additionalOutput);
        } else {
            if (_output == null) {
                _output = new StringBuilder();
            }
            _output.append(additionalOutput);
        }
    }

    @Override
    public synchronized final void silentSave() {
        _silent = _graph.newBuffer();
    }

    @Override
    public final Buffer notifier() {
        return _silent;
    }

    @Override
    public final void saveToBuffer(Buffer buffer) {
        Base64.encodeLongToBuffer(_world, buffer);
        buffer.write(CoreConstants.CHUNK_SEP);
        Base64.encodeLongToBuffer(_time, buffer);
        buffer.write(CoreConstants.CHUNK_SEP);
        Base64.encodeIntToBuffer((_taskProgressAutoReporting ? 1 : 0), buffer);
        buffer.write(CoreConstants.CHUNK_SEP);
        if (_result != null) {
            _result.saveToBuffer(buffer);
        }
        Tuple<String, TaskResult>[] variables = variables();
        for (int i = 0; i < variables.length; i++) {
            Base64.encodeStringToBuffer(variables[i].left(), buffer);
            variables[i].right().saveToBuffer(buffer);
        }
    }

    private int readInt(final Buffer buffer, final int begin) {
        int cursor = begin;
        while (cursor < buffer.length()) {
            byte current = buffer.read(cursor);
            if (current == Constants.CHUNK_SEP) {
                if (begin != cursor) {
                    _taskProgressAutoReporting = (Base64.decodeToIntWithBounds(buffer, begin, cursor) == 1 ? true : false);
                    break;
                }
            } else {
                cursor++;
            }
        }
        return cursor;
    }

    private int readLong(final Buffer buffer, final int begin, final boolean world) {
        int cursor = begin;
        while (cursor < buffer.length()) {
            byte current = buffer.read(cursor);
            if (current == Constants.CHUNK_SEP) {
                if (begin != cursor) {
                    if (world) {
                        _world = Base64.decodeToLongWithBounds(buffer, begin, cursor);
                        break;
                    } else {
                        _time = Base64.decodeToLongWithBounds(buffer, begin, cursor);
                        break;
                    }
                }
            } else {
                cursor++;
            }
        }
        return cursor;
    }

    private int readResult(final Buffer buffer, final int begin, final L3GMap<List<Tuple<Object[], Integer>>> collector) {
        int cursor = begin;
        String name = null;
        while (cursor < buffer.length()) {
            byte current = buffer.read(cursor);
            if (current == Constants.BLOCK_OPEN) {
                if (begin != cursor) {
                    name = Base64.decodeToStringWithBounds(buffer, begin, cursor);
                }
                //read result
                BaseTaskResult loadedResult = new BaseTaskResult(null, false);
                cursor = loadedResult.load(buffer, cursor, _graph, collector);
                if (name == null) {
                    _result = loadedResult;
                } else {
                    //TODO improve to manage global variable
                    if (this._localVariables == null) {
                        this._localVariables = new HashMap<String, TaskResult>();
                    }
                    this._localVariables.put(name, loadedResult);
                }
                return cursor;
            } else {
                cursor++;
            }
        }
        return cursor;
    }

    @Override
    public final void loadFromBuffer(Buffer buffer, Callback<Boolean> loaded) {
        L3GMap<List<Tuple<Object[], Integer>>> collector = new L3GMap<List<Tuple<Object[], Integer>>>(true);
        int cursor = 0;
        cursor = readLong(buffer, cursor, true);
        cursor++;
        cursor = readLong(buffer, cursor, false);
        cursor++;
        cursor = readInt(buffer, cursor);
        cursor++;
        while (cursor < buffer.length()) {
            cursor = readResult(buffer, cursor, collector);
            cursor++;
        }
        int collectorSize = collector.size();
        if (collectorSize == 0) {
            if (loaded != null) {
                loaded.on(true);
            }
        } else {
            long[] worlds = new long[collectorSize];
            long[] times = new long[collectorSize];
            long[] ids = new long[collectorSize];
            for (int i = 0; i < collectorSize; i++) {
                worlds[i] = collector.keys[i * GROUP];
                times[i] = collector.keys[i * GROUP + 1];
                ids[i] = collector.keys[i * GROUP + 2];
            }
            _graph.lookupBatch(worlds, times, ids, new Callback<Node[]>() {
                @Override
                public void on(Node[] result) {
                    for (int i = 0; i < collectorSize; i++) {
                        List<Tuple<Object[], Integer>> subCollector = collector.get(worlds[i], times[i], ids[i]);
                        if (subCollector != null) {
                            for (int j = 0; j < subCollector.size(); j++) {
                                Tuple<Object[], Integer> tuple = subCollector.get(j);
                                tuple.left()[tuple.right()] = result[i];
                            }
                        }
                    }
                    if (loaded != null) {
                        loaded.on(true);
                    }
                }
            });
        }
    }

    @Override
    public final Callback<String> printHook() {
        return this._printHook;
    }

    @Override
    public final void setPrintHook(final Callback<String> callback) {
        this._printHook = callback;
    }

    @Override
    public final Callback<TaskProgressReport> progressHook() {
        return this._progressHook;
    }

    private String _parentActionPath;

    private String currentActionPath() {
        if (_parentActionPath == null) {
            if (_parent != null) {
                _parentActionPath = ((CoreTaskContext) _parent).currentActionPath() + ".";
            } else {
                _parentActionPath = "";
            }
        }
        return this._parentActionPath + cursor;
    }

    private String _sumPath;

    private String sumPath() {
        if (_sumPath == null) {
            if (_parent != null) {
                _sumPath = ((CoreTaskContext) _parent).sumPath() + ".";
            } else {
                _sumPath = "";
            }
            _sumPath = _sumPath + _origin.insertCursor;
        }
        return this._sumPath;
    }


    private CoreProgressReport currentReport;

    @Override
    public final void reportProgress(final double progress, final String comment) {

        Callback<TaskProgressReport> progressHook = this._progressHook;
        TaskContext localParent = _parent;
        while (progressHook == null && localParent != null) {
            progressHook = localParent.progressHook();
            localParent = ((CoreTaskContext) localParent)._parent;
        }

        int regId = this.in_registry_id;
        localParent = _parent;
        while (regId == -1 && localParent != null) {
            regId = ((CoreTaskContext) localParent).in_registry_id;
            localParent = ((CoreTaskContext) localParent)._parent;
        }

        if (progressHook != null || regId != -1) {
            if (currentReport == null) {
                currentReport = new CoreProgressReport();
            }
            currentReport.setActionPath(currentActionPath())
                    .setSumPath(sumPath())
                    .setProgress(progress);
            if (comment != null) {
                currentReport.setComment(comment);
            } else {
                currentReport.setComment(_origin.actions[cursor].name());
            }
        }

        if (regId != -1) {
            CoreTaskContextRegistry reg = (CoreTaskContextRegistry) this.graph().taskContextRegistry();
            reg.reportProgress(regId, currentReport);
        }

        if (progressHook != null) {
            progressHook.on(currentReport);
        }
    }


    @Override
    public final void setProgressHook(final Callback<TaskProgressReport> hook) {
        this._progressHook = hook;
    }

    @Override
    public void setProgressAutoReport(boolean activate) {
        this._taskProgressAutoReporting = activate;
    }

    @Override
    public final void initTracker() {
        this._transactionTracker = new LMap(false);
    }

    @Override
    public void removeTracker() {
        this._transactionTracker = null;
    }

    private void internal_track_result(BaseTaskResult p_res) {
        if (p_res != null) {
            for (int i = 0; i < p_res.size(); i++) {
                Object o = p_res.get(i);
                if (o instanceof BaseNode) {
                    track((Node) o);
                } else if (o instanceof BaseTaskResult) {
                    internal_track_result((BaseTaskResult) o);
                }
            }
        }
    }

    @Override
    public final LMap tracker() {
        return _transactionTracker;
    }

    @Override
    public final void track(final Node ptr) {
        if (_transactionTracker != null) {
            final StateChunk chunk = (StateChunk) graph().resolver().resolveState(ptr);
            if (!chunk.inSync()) {
                _transactionTracker.add(ptr.id());
            }
        }
    }

    @Override
    public final String toString() {
        return "{result:" + _result.toString() + "}";
    }

}
