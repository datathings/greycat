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
package greycat.internal.task;

import greycat.*;
import greycat.base.BaseNode;
import greycat.Action;
import greycat.plugin.Job;
import greycat.plugin.NodeState;
import greycat.plugin.Resolver;
import greycat.struct.Buffer;
import greycat.struct.Relation;
import greycat.TaskContext;
import greycat.TaskResult;
import greycat.utility.HashHelper;
import greycat.utility.LArray;
import greycat.utility.Tuple;

import java.util.ArrayList;
import java.util.List;

class ActionTraverseOrAttribute implements Action {

    private final String _name;
    private final String[] _params;

    private final boolean _isAttribute;
    private final boolean _isUnknown;

    ActionTraverseOrAttribute(final boolean isAttribute, boolean isUnknown, final String p_name, final String... p_params) {
        this._name = p_name;
        this._params = p_params;
        this._isUnknown = isUnknown;
        this._isAttribute = isAttribute;
    }

    @Override
    public final void eval(final TaskContext ctx) {
        final Resolver resolver = ctx.graph().resolver();
        final TaskResult finalResult = ctx.newResult();
        final String flatName = ctx.template(_name);
        final int flatHash = HashHelper.hash(flatName);
        final TaskResult previousResult = ctx.result();
        List<Tuple<Node, Task>> tasks = null;
        if (previousResult != null) {
            final int previousSize = previousResult.size();
            final LArray worlds = new LArray();
            final LArray times = new LArray();
            final LArray ids = new LArray();

            Query query = null;
            for (int i = 0; i < previousSize; i++) {
                final Object loop = previousResult.get(i);
                //TODO manage eNode here
                if (loop instanceof BaseNode) {
                    final Node casted = (Node) loop;
                    switch (casted.typeAt(flatHash)) {
                        case Type.RELATION:
                            Relation relation = (Relation) casted.getAt(flatHash);
                            if (relation != null) {
                                int relSize = relation.size();
                                for (int k = 0; k < relSize; k++) {
                                    worlds.add(casted.world());
                                    times.add(casted.time());
                                    ids.add(relation.get(k));
                                }
                            }
                            break;
                        case Type.INDEX:
                            final Index relationIndexed = (Index) casted.getAt(flatHash);
                            if (relationIndexed != null) {
                                final int[] keys = relationIndexed.keys();
                                if (_params != null && _params.length > 0) {
                                    query = ctx.graph().newQuery();
                                    for (int k = 0; k < keys.length && k < _params.length; k++) {
                                        query.addRaw(keys[i], ctx.template(_params[k]));
                                    }
                                } else {
                                    query = null;
                                }
                                if (query != null) {
                                    final long[] candidates = relationIndexed.selectByQuery(query);
                                    for (int k = 0; k < candidates.length; k++) {
                                        worlds.add(casted.world());
                                        times.add(casted.time());
                                        ids.add(candidates[k]);
                                    }
                                } else {
                                    long[] all = relationIndexed.all();
                                    for (int k = 0; k < all.length; k++) {
                                        worlds.add(casted.world());
                                        times.add(casted.time());
                                        ids.add(all[k]);
                                    }
                                }
                            }
                            break;
                        case Type.TASK:
                            if (tasks == null) {
                                tasks = new ArrayList<Tuple<Node, Task>>();
                            }
                            tasks.add(new Tuple<Node, Task>(ctx.graph().cloneNode(casted), (Task) casted.getAt(flatHash)));
                            break;
                        default:
                            Object resolved = casted.get(flatName);
                            if (resolved != null) {
                                finalResult.add(resolved);
                            }
                            break;
                    }
                    casted.free();
                } else {
                    finalResult.add(loop);
                }
            }
            final Query finalQuery = query;
            Callback secondStep = new Callback() {
                @Override
                public void on(Object result) {
                    if (ids.size() == 0) {
                        previousResult.clear();
                        ctx.continueWith(finalResult);
                    } else {
                        resolver.lookupBatch(worlds.all(), times.all(), ids.all(), new Callback<Node[]>() {
                            @Override
                            public void on(Node[] result) {
                                for (int i = 0; i < result.length; i++) {
                                    final Node resolvedNode = result[i];
                                    if (resolvedNode != null) {
                                        if (finalQuery == null) {
                                            finalResult.add(resolvedNode);
                                        } else {
                                            final NodeState resolvedState = resolver.resolveState(resolvedNode);
                                            boolean exact = true;
                                            for (int j = 0; j < finalQuery.attributes().length; j++) {
                                                Object obj = resolvedState.getAt(finalQuery.attributes()[j]);
                                                if (finalQuery.values()[j] == null) {
                                                    if (obj != null) {
                                                        exact = false;
                                                        break;
                                                    }
                                                } else {
                                                    if (obj == null) {
                                                        exact = false;
                                                        break;
                                                    } else {
                                                        if (obj instanceof long[]) {
                                                            if (finalQuery.values()[j] instanceof long[]) {
                                                                if (!Constants.longArrayEquals((long[]) finalQuery.values()[j], (long[]) obj)) {
                                                                    exact = false;
                                                                    break;
                                                                }
                                                            } else {
                                                                exact = false;
                                                                break;
                                                            }
                                                        } else {
                                                            if (!Constants.equals(finalQuery.values()[j].toString(), obj.toString())) {
                                                                exact = false;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            if (exact) {
                                                finalResult.add(resolvedNode);
                                            }
                                        }
                                    }
                                }
                                previousResult.clear();
                                ctx.continueWith(finalResult);
                            }
                        });
                    }
                }
            };
            if (tasks != null) {
                DeferCounter deferCounter = ctx.graph().newCounter(tasks.size());
                deferCounter.then(new Job() {
                    @Override
                    public void run() {
                        secondStep.on(null);
                    }
                });
                for (int i = 0; i < tasks.size(); i++) {
                    Tuple<Node, Task> tt = tasks.get(i);
                    final TaskContext taskContext = tt.right().prepare(ctx.graph(), tt.left(), new Callback<TaskResult>() {
                        @Override
                        public void on(TaskResult result) {
                            tt.left().free();
                            if (result.size() == 1) {
                                finalResult.add(result.get(0));
                            } else {
                                finalResult.add(result);
                            }
                            deferCounter.count();
                        }
                    });
                    tt.right().executeUsing(taskContext);
                }

            } else {
                secondStep.on(null);
            }
        } else {
            ctx.continueTask();
        }
    }

    @Override
    public void serialize(final Buffer builder) {
        if (_isUnknown) {
            builder.writeString(_name);
        } else {
            if (_isAttribute) {
                builder.writeString(CoreActionNames.ATTRIBUTE);
                builder.writeChar(Constants.TASK_PARAM_OPEN);
                builder.writeString(_name);
                builder.writeChar(Constants.TASK_PARAM_CLOSE);
            } else {
                builder.writeString(CoreActionNames.TRAVERSE);
                builder.writeChar(Constants.TASK_PARAM_OPEN);
                builder.writeString(_name);
                if (_params != null && _params.length > 0) {
                    builder.writeChar(Constants.TASK_PARAM_SEP);
                    TaskHelper.serializeStringParams(_params, builder);
                }
                builder.writeChar(Constants.TASK_PARAM_CLOSE);
            }
        }
    }


    @Override
    public final String name() {
        return (_isUnknown ? _name : (_isAttribute ? CoreActionNames.ATTRIBUTE : CoreActionNames.TRAVERSE));
    }

}