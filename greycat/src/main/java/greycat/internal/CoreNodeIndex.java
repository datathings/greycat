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
package greycat.internal;

import greycat.*;
import greycat.base.BaseNode;
import greycat.internal.custom.CoreIndexAttribute;
import greycat.utility.HashHelper;

final class CoreNodeIndex extends BaseNode implements NodeIndex {

    static final String NAME = "NodeIndex";

    CoreNodeIndex(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    @Override
    public final void init() {
        getOrCreateAt(0, HashHelper.hash(CoreIndexAttribute.NAME));
    }

    @Override
    public final void declareAttributes(Callback callback, String... attributeNames) {
        ((Index) getAt(0)).declareAttributes(callback, attributeNames);
    }

    @Override
    public final int size() {
        Index i = (Index) getAt(0);
        if (i != null) {
            return i.size();
        } else {
            return 0;
        }
    }

    @Override
    public final long[] all() {
        Index i = (Index) getAt(0);
        if (i != null) {
            return i.all();
        } else {
            return new long[0];
        }
    }

    @Override
    public final Index update(Node node) {
        return ((Index) getOrCreateAt(0, HashHelper.hash(CoreIndexAttribute.NAME))).update(node);
    }

    @Override
    public final Index unindex(Node node) {
        Index i = (Index) getAt(0);
        if (i != null) {
            i.unindex(node);
        }
        return this;
    }

    @Override
    public final Index clear() {
        Index i = (Index) getAt(0);
        if (i != null) {
            i.clear();
        }
        return this;
    }

    @Override
    public final void find(Callback<Node[]> callback, long world, long time, String... params) {
        Index i = (Index) getAt(0);
        if (i != null) {
            i.find(callback, world, time, params);
        } else {
            callback.on(new Node[0]);
        }
    }

    @Override
    public final void findByQuery(Query query, Callback<Node[]> callback) {
        Index i = (Index) getAt(0);
        if (i != null) {
            i.findByQuery(query, callback);
        } else {
            callback.on(new Node[0]);
        }
    }

    @Override
    public final long[] select(String... params) {
        Index i = (Index) getAt(0);
        if (i != null) {
            return i.select(params);
        } else {
            return new long[0];
        }
    }

    @Override
    public final long[] selectByQuery(Query query) {
        Index i = (Index) getAt(0);
        if (i != null) {
            return i.selectByQuery(query);
        } else {
            return new long[0];
        }
    }

    @Override
    public final int[] keys() {
        Index i = (Index) getAt(0);
        if (i != null) {
            return i.keys();
        } else {
            return new int[0];
        }
    }

    @Override
    public final void findFrom(Callback<Node[]> callback, String... params) {
        Index i = (Index) getAt(0);
        if (i != null) {
            i.find(callback, _world, _time, params);
        } else {
            callback.on(new Node[0]);
        }
    }
}
