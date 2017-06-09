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

import greycat.Graph;
import greycat.Node;
import greycat.NodeValue;
import greycat.Type;
import greycat.base.BaseNode;
import greycat.utility.HashHelper;

public class CoreNodeValue extends BaseNode implements NodeValue {

    private static int cached = -1;

    public static String NAME = "NodeValue";

    CoreNodeValue(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
        if (cached == -1) {
            cached = HashHelper.hash("value");
        }
    }

    @Override
    public final double getValue() {
        return (double) getAt(cached);
    }

    @Override
    public final void setValue(double newValue) {
        setAt(cached, Type.DOUBLE, newValue);
    }

    @Override
    public Node setAt(int index, byte type, Object value) {
        if (index != cached) {
            throw new RuntimeException("Bad API usage, NodeValue cannot contains other attributes");
        }
        return super.setAt(index, type, value);
    }

    @Override
    public final String toString() {
        return "{\"world\":" + world() + ",\"time\":" + time() + ",\"id\":" + id() + ",\"value\":" + getValue() + "}";
    }

}
