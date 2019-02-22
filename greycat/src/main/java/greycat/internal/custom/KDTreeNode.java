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
import greycat.struct.Tree;
import greycat.struct.TreeResult;
import greycat.utility.HashHelper;

public class KDTreeNode extends BaseNode implements Tree {

    public static String NAME = "KDTreeNode";
    public static String BOUND_MIN = "bound_min";
    public static String BOUND_MAX = "bound_max";
    public static String RESOLUTION = "resolution";

    private static String E_TREE = "etree";

    public KDTreeNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    private KDTree _kdTree = null;

    private KDTree getTree() {
        if (_kdTree == null) {
            _kdTree = (KDTree) getOrCreate(E_TREE, HashHelper.hash(KDTree.NAME));
        }
        return _kdTree;
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
    public final void setDistance(int distanceType) {
        getTree().setDistance(distanceType);
    }

    @Override
    public final void setResolution(double[] resolution) {
        getTree().setResolution(resolution);
    }

    @Override
    public final void setMinBound(double[] min) {
        getTree().setMinBound(min);
    }

    @Override
    public final void setMaxBound(double[] max) {
        getTree().setMaxBound(max);
    }

    @Override
    public final void insert(double[] keys, long value) {
        getTree().insert(keys, value);
    }

    /*
    @Override
    public void profile(double[] keys, long occurrence) {
        getTree().profile(keys, occurrence);
    }*/

    @Override
    public final TreeResult queryAround(double[] keys, int max) {
        return getTree().queryAround(keys, max);
    }

    @Override
    public final TreeResult queryRadius(double[] keys, double radius) {
        return getTree().queryRadius(keys, radius);
    }

    @Override
    public final TreeResult queryBoundedRadius(double[] keys, double radius, int max) {
        return getTree().queryBoundedRadius(keys, radius, max);
    }

    @Override
    public final TreeResult queryArea(double[] min, double[] max) {
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
