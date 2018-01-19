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
import greycat.plugin.NodeState;
import greycat.struct.EStructArray;
import greycat.utility.Enforcer;

public class GaussianSlotsNode extends BaseNode {
    public final static String NAME = "GaussianSlotsNode";

    private static final String GSEGRAPH = "gsegraph";

    public static final String PERIOD_SIZE = "PERIOD_SIZE"; //The period over which the profile returns to the initial slot
    public static final long PERIOD_SIZE_DEF = 24 * 3600 * 1000; //By default it is 24 hours

    public static final String NUMBER_OF_SLOTS = "numberOfSlots"; //Number of slots to create in the profile, default is 1
    public static final int NUMBER_OF_SLOTS_DEF = 2;

    public static final int TIME_SENSITIVITY_FACTOR = 4;

    private static final Enforcer enforcer = new Enforcer()
            .asPositiveInt(NUMBER_OF_SLOTS)
            .asPositiveLong(PERIOD_SIZE);


    private GaussianSlotsEGraph gsgraph = null;


    public GaussianSlotsNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }


    @Override
    public Node set(String name, int type, Object value) {
        enforcer.check(name, type, value);

        if (!load()) {
            EStructArray eg = (EStructArray) super.getOrCreate(GSEGRAPH, Type.ESTRUCT_ARRAY);
            gsgraph = new GaussianSlotsEGraph(eg);
        }
        switch (name) {

            case PERIOD_SIZE:
                this.setTimeSensitivity((long) value * TIME_SENSITIVITY_FACTOR, 0);
                return super.set(name, type, value);
            case Gaussian.VALUES:
                gsgraph.learn(getSlotNumber(), (double[]) value);
                return this;
            case NUMBER_OF_SLOTS:
                gsgraph.setNumberOfSlots((int) value);
                return super.set(name, type, value);
        }
        throw new RuntimeException("can't set anything other than precisions or values on this node!");
    }

    public void learn(double[] values) {
        //this should be fine no need to fix here
        set(Gaussian.VALUES, Type.DOUBLE_ARRAY, values);
    }

    public double[] predict() {
        if (!load()) {
            return null;
        }
        GaussianWrapper backend = gsgraph.getGaussian(getSlotNumber());
        return backend.getAvg();
    }

    public double[] getAvg(int slot){
        if (!load()) {
            return null;
        }
        GaussianWrapper backend = gsgraph.getGaussian(slot);
        return backend.getAvg();
    }

    public double[] getStd(int slot){
        if (!load()) {
            return null;
        }
        GaussianWrapper backend = gsgraph.getGaussian(slot);
        return backend.getSTD();
    }

    @Override
    public Object get(String attributeName) {
        if (!load()) {
            return null;
        }
        GaussianWrapper backend = gsgraph.getGaussian(getSlotNumber());
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
        if (gsgraph != null) {
            return true;
        } else {
            EStructArray eg = (EStructArray) super.get(GSEGRAPH);
            if (eg != null) {
                gsgraph = new GaussianSlotsEGraph(eg);
                return true;
            } else {
                return false;
            }
        }
    }


    private int getSlotNumber() {
        long t = time();
        NodeState resolved = unphasedState();
        int slots = resolved.getWithDefault(NUMBER_OF_SLOTS, NUMBER_OF_SLOTS_DEF);
        long period = resolved.getWithDefault(PERIOD_SIZE, PERIOD_SIZE_DEF);
        return getIntTime(t, slots, period);
    }

    public static int getIntTime(long time, int numOfSlot, long periodSize) {
        if (time < 0 || periodSize < 0) {
            throw new RuntimeException("Time or period size can't be negative");
        }
        if (numOfSlot <= 1) {
            return 0;
        }
        double res = time % periodSize;
        res = Math.floor(res / (Math.floor(periodSize / numOfSlot)));

        return (int) res;
    }


}
