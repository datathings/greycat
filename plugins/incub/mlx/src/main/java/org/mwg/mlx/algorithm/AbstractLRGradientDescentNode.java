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
import org.mwg.utility.Enforcer;

/**
 * Created by andre on 4/29/2016.
 */
public abstract class AbstractLRGradientDescentNode extends AbstractLinearRegressionNode{

    public static final String GD_ERROR_THRESH_KEY = "gdErrorThreshold";
    public static final String GD_ITERATION_THRESH_KEY = "gdIterationThreshold";

    public static final int DEFAULT_GD_ITERATIONS_COUNT = 10000;

    public static final double DEFAULT_LEARNING_RATE = 0.0001;

    /**
     * Attribute key - Learning rate
     */
    public static final String LEARNING_RATE_KEY = "_LearningRate";

    public AbstractLRGradientDescentNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    private static final Enforcer agdEnforcer = new Enforcer()
            .asInt(GD_ITERATION_THRESH_KEY)
            .asNonNegativeOrNanDouble(GD_ERROR_THRESH_KEY)
            .asNonNegativeDouble(LEARNING_RATE_KEY);

    @Override
    public void setProperty(String propertyName, byte propertyType, Object propertyValue) {
        agdEnforcer.check(propertyName, propertyType, propertyValue);
        super.setProperty(propertyName, propertyType, propertyValue);
    }
}
