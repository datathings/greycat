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
import greycat.internal.custom.KDTree;
import greycat.internal.custom.NDTree;
import greycat.plugin.Job;
import greycat.struct.Buffer;
import greycat.struct.Tree;
import greycat.struct.TreeResult;

public class ActionQueryBoundedRadius implements Action {

    private final double[] _key;
    private final int _n;
    private final double _radius;
    private final boolean _fetchNodes;

    public ActionQueryBoundedRadius(final int n, final double radius, final boolean fetchNodes, final double[] key) {
        this._key = key;
        this._n = n;
        this._radius = radius;
        this._fetchNodes = fetchNodes;
    }

    @Override
    public final void eval(final TaskContext ctx) {
        final TaskResult previousResult = ctx.result();
        final TaskResult<Object> nextResult = ctx.newResult();
        if (previousResult != null) {
            final DeferCounter defer = ctx.graph().newCounter(previousResult.size());
            final TaskResultIterator previousResultIt = previousResult.iterator();
            Object iter = previousResultIt.next();
            while (iter != null) {
                if (iter instanceof NDTree || iter instanceof KDTree) {
                    TreeResult tr = ((Tree) iter).queryBoundedRadius(_key, _radius, _n);
                    if (_fetchNodes) {
                        long[] nodeIds = new long[tr.size()];
                        for (int i = 0; i < tr.size(); i++) {
                            nodeIds[i] = tr.value(i);
                        }
                        ctx.graph().lookupAll(ctx.world(), ctx.time(), nodeIds, new Callback<Node[]>() {
                            @Override
                            public void on(Node[] result) {
                                for (int j = 0; j < result.length; j++) {
                                    TaskResult<Object> line = ctx.newResult();
                                    line.add(tr.keys(j));
                                    line.add(result[j]);
                                    line.add(tr.distance(j));
                                    nextResult.add(line);
                                }
                                tr.free();
                                defer.count();
                            }
                        });
                    } else {
                        for (int i = 0; i < tr.size(); i++) {
                            TaskResult<Object> line = ctx.newResult();
                            line.add(tr.keys(i));
                            line.add(tr.value(i));
                            line.add(tr.distance(i));
                            nextResult.add(line);
                        }
                        tr.free();
                        defer.count();
                    }
                } else {
                    defer.count();
                }
                iter = previousResultIt.next();
            }
            defer.then(new Job() {
                @Override
                public void run() {
                    ctx.continueWith(nextResult);
                }
            });
        } else {
            ctx.continueWith(nextResult);
        }
    }

    @Override
    public void serialize(final Buffer builder) {
        builder.writeString(CoreActionNames.READ_INDEX);
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        builder.writeString("" + _n);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        builder.writeString("" + _radius);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        builder.writeString("" + _fetchNodes);
        if (_key != null && _key.length > 0) {
            for (int i = 0; i < _key.length; i++) {
                builder.writeChar(Constants.TASK_PARAM_SEP);
                builder.writeString("" + _key[i]);
            }
        }
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return CoreActionNames.QUERY_BOUNDED_RADIUS;
    }

}