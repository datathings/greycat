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
package org.mwg.mlx.algorithm;

import org.mwg.Graph;
import org.mwg.Type;
import org.mwg.ml.AbstractMLNode;
import org.mwg.plugin.NodeState;
import org.mwg.utility.Enforcer;

/**
 * Created by andre on 4/26/2016.
 * <p>
 * Common superclass for all ML algorithm that use bootstrap mode
 * and
 */
public abstract class AbstractAnySlidingWindowManagingNode extends AbstractMLNode {
    /**
     * Internal keys - those attributes are only for internal use within the node.
     * They are not supposed to be accessed from outside (although it is not banned).
     */
    protected static final String INTERNAL_RESULTS_BUFFER_KEY = "_results";

    /**
     * Attribute key - whether the node is in bootstrap (re-learning) mode
     */
    public static final String BOOTSTRAP_MODE_KEY = "bootstrapMode";
    public static final boolean BOOTSTRAP_MODE_DEF = true;

    /**
     * Buffer size
     */
    public static final String BUFFER_SIZE_KEY = "BufferSize";
    /**
     * Buffer size - default
     */
    public static final int BUFFER_SIZE_DEF = 50;

    /**
     * Number of input dimensions
     */
    public static final String INPUT_DIM_KEY = "InputDimensions";
    /**
     * Number of input dimensions - default
     */
    public static final int INPUT_DIM_UNKNOWN = -1;
    /**
     * Number of input dimensions - default (unknown so far)
     */
    public static final int INPUT_DIM_DEF = INPUT_DIM_UNKNOWN;

    /**
     * Higher error threshold
     */
    public static final String HIGH_ERROR_THRESH_KEY = "HighErrorThreshold";
    /**
     * Higher error threshold - default
     */
    public static final double HIGH_ERROR_THRESH_DEF = 0.1;

    /**
     * Lower error threshold
     */
    public static final String LOW_ERROR_THRESH_KEY = "LowErrorThreshold";
    /**
     * Lower error threshold
     */
    public static final double LOW_ERROR_THRESH_DEF = 0.05;

    /**
     * Attribute key - sliding window of values
     */
    protected static final String INTERNAL_VALUE_BUFFER_KEY = "_valueBuffer";

    public AbstractAnySlidingWindowManagingNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    /**
     * Adjust buffer: adds value to the end of it, removes first value(s) if necessary.
     *
     * @param state Unphased state to get properties and set buffer.
     * @param value New value to be added to buffer.
     * @param bootstrapMode Whether we are in the bootstrap mode
     * @return New buffer.
     */
    protected static double[] adjustValueBuffer(NodeState state, double value[], boolean bootstrapMode) {
        int dimensions = state.getFromKeyWithDefault(INPUT_DIM_KEY, INPUT_DIM_DEF);
        if (dimensions < 0) {
            dimensions = value.length;
            state.setFromKey(INPUT_DIM_KEY, Type.INT, value.length);
        }

        double buffer[] = state.getFromKeyWithDefault(INTERNAL_VALUE_BUFFER_KEY, new double[0]);

        final int bufferLength = buffer.length / dimensions; //Buffer is "unrolled" into 1D array.
        //So adding 1 value to the end and removing (currentBufferLength + 1) - maxBufferLength from the beginning.
        final int maxBufferLength = state.getFromKeyWithDefault(BUFFER_SIZE_KEY, BUFFER_SIZE_DEF);
        final int numValuesToRemoveFromBeginning = bootstrapMode ? 0 : Math.max(0, bufferLength + 1 - maxBufferLength);
        final int newBufferLength = bufferLength + 1 - numValuesToRemoveFromBeginning;

        double newBuffer[] = new double[newBufferLength * dimensions];
        System.arraycopy(buffer, numValuesToRemoveFromBeginning*dimensions, newBuffer, 0, newBuffer.length - dimensions);
        System.arraycopy(value, 0, newBuffer, newBuffer.length - dimensions, dimensions);

        state.setFromKey(INTERNAL_VALUE_BUFFER_KEY, Type.DOUBLE_ARRAY, newBuffer);
        return newBuffer;
    }

    /**
     * @return Class index - index in a value array, where class label is supposed to be
     */
    protected int getMaxBufferLength() {
        return unphasedState().getFromKeyWithDefault(BUFFER_SIZE_KEY, BUFFER_SIZE_DEF);
    }

    //Results buffer is set by further class. .
    private static final Enforcer enforcer = new Enforcer()
            .asPositiveInt(BUFFER_SIZE_KEY)
            .asPositiveDouble(LOW_ERROR_THRESH_KEY)
            .asPositiveDouble(HIGH_ERROR_THRESH_KEY);

    @Override
    public void setProperty(String propertyName, byte propertyType, Object propertyValue) {
        if (INTERNAL_VALUE_BUFFER_KEY.equals(propertyName) || BOOTSTRAP_MODE_KEY.equals(propertyName) ||
                INPUT_DIM_KEY.equals(propertyName) || INTERNAL_RESULTS_BUFFER_KEY.equals(propertyName)) {
            //Nothing. They are unsettable directly
        } else {
            enforcer.check(propertyName, propertyType, propertyValue);
            super.setProperty(propertyName, propertyType, propertyValue);
        }
    }

    /**
     * Defines implementation-specific actions to do before going to bootstrap mode.
     *
     * @param state Node state to get/set properties
     */
    protected abstract void setBootstrapModeHook(NodeState state);

    /**
     * Sets bootstrap mode to new value
     *
     * @param state State to get/set proeprties
     * @param newBootstrapMode New Bootstrap mode
     * @return New node state (if generated) or old state
     */
    protected NodeState setBootstrapMode(NodeState state, boolean newBootstrapMode) {
        NodeState newState = state;
        if (newBootstrapMode) {
            //New state starts now
            newState = phasedState();
            setBootstrapModeHook(newState);
        }
        newState.setFromKey(BOOTSTRAP_MODE_KEY, Type.BOOL, newBootstrapMode);
        return newState;
    }
}
