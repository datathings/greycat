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
package greycat.internal.custom;

import greycat.Graph;
import greycat.Node;
import greycat.Type;
import greycat.base.BaseNode;
import greycat.struct.Profile;
import greycat.struct.ProfileResult;
import greycat.utility.HashHelper;

public class NDTreeNode extends BaseNode implements Profile {

    public static String NAME = "NDTreeNode";
    public static String BOUND_MIN = "bound_min";
    public static String BOUND_MAX = "bound_max";
    public static String RESOLUTION = "resolution";

    private static String E_TREE = "etree";

    public NDTreeNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    private NDTree _ndTree = null;

    private NDTree getTree() {
        if (_ndTree == null) {
            _ndTree = (NDTree) getOrCreate(E_TREE, HashHelper.hash(NDTree.NAME));
        }
        return _ndTree;
    }

    @Override
    public final Node set(String name, int type, Object value) {
        if (name.equals(BOUND_MIN)) {
            setMinBound((double[]) value);
        } else if (name.equals(BOUND_MAX)) {
            setMaxBound((double[]) value);
        } else if (name.equals(RESOLUTION)) {
            setResolution((double[]) value);
        } else {
            super.set(name, type, value);
        }
        return this;
    }

    @Override
    public final void setDistance(final int distanceType) {
        getTree().setDistance(distanceType);
    }

    @Override
    public final void setResolution(final double[] resolution) {
        getTree().setResolution(resolution);
    }

    @Override
    public final void setMinBound(final double[] min) {
        getTree().setMinBound(min);
    }

    @Override
    public final void setMaxBound(final double[] max) {
        getTree().setMaxBound(max);
    }

    @Override
    public final void insert(final double[] keys, final long value) {
        getTree().insert(keys, value);
    }

    @Override
    public void setBufferSize(int bufferSize) {
        _ndTree.setBufferSize(bufferSize);
    }

    @Override
    public final void profile(final double[] keys) {
        getTree().insert(keys, 1);
    }

    @Override
    public final void profileWith(final double[] keys, final long occurrence) {
        getTree().insert(keys, occurrence);
    }

    @Override
    public final ProfileResult queryAround(final double[] keys, final int nbElem) {
        return getTree().queryAround(keys, nbElem);
    }

    @Override
    public final ProfileResult queryRadius(final double[] keys, final double radius) {
        return getTree().queryRadius(keys, radius);
    }

    @Override
    public final ProfileResult queryBoundedRadius(final double[] keys, final double radius, final int max) {
        return getTree().queryBoundedRadius(keys, radius, max);
    }


    @Override
    public final ProfileResult queryArea(final double[] min, final double[] max) {
        return getTree().queryArea(min, max);
    }

    @Override
    public final long size() {
        return getTree().size();
    }

    @Override
    public final long treeSize() {
        return getTree().treeSize();
    }
}
