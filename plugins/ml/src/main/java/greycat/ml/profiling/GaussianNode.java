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
package greycat.ml.profiling;

import greycat.Graph;
import greycat.Node;
import greycat.Type;
import greycat.base.BaseNode;
import greycat.struct.EGraph;

public class GaussianNode extends BaseNode {
    public final static String NAME = "GaussianNode";

    private static final String BACKEND = "backend";

    private EGraph egraph;
    private GaussianENode backend;

    public GaussianNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    public void learn(double[] values) {
        //this should be fine no need to fix here
        set(Gaussian.VALUES, Type.DOUBLE_ARRAY, values);
    }

    public double[] predict() {
        if (load()) {
            return backend.getAvg();
        } else {
            return null;
        }
    }

    private void invalidate() {
        egraph = null;
        backend = null;
    }

    @Override
    public Node set(String name, int type, Object value) {
        rephase();
        invalidate();
        if (!load()) {
            egraph = (EGraph) super.getOrCreate(BACKEND, Type.EGRAPH);
            backend = new GaussianENode(egraph.newNode());
        }
        switch (name) {
            case Gaussian.VALUES:
                backend.learn((double[]) value);
                return this;
            case Gaussian.PRECISIONS:
                backend.setPrecisions((double[]) value);
                return this;
        }
        throw new RuntimeException("can't set anything other than precisions or values on this node!");
    }

    @Override
    public Object get(String attributeName) {
        if (!load()) {
            return null;
        }
        switch (attributeName) {
            case Gaussian.MIN:
                return backend.getMin();
            case Gaussian.MAX:
                return backend.getMax();
            case Gaussian.AVG:
                return backend.getAvg();
            case Gaussian.COV:
                return backend.getCovariance();
            case Gaussian.STD:
                return backend.getSTD();
            case Gaussian.SUM:
                return backend.getSum();
            case Gaussian.SUMSQ:
                return backend.getSumSq();
            case Gaussian.TOTAL:
                return backend.getTotal();
        }
        throw new RuntimeException("Attribute " + attributeName + " not found!");
    }

    private boolean load() {
        if (backend != null) {
            return true;
        } else {
            if (super.get(BACKEND) == null) {
                return false;
            } else {
                egraph = (EGraph) super.get(BACKEND);
                if (egraph.root() == null) {
                    return false;
                }
                backend = new GaussianENode(egraph.root());
                return true;
            }
        }
    }

}
