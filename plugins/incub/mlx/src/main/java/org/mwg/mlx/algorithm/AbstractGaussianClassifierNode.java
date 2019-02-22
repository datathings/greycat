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
import org.mwg.plugin.NodeState;

/**
 * Created by andrey.boytsov on 17/05/16.
 */
public abstract class AbstractGaussianClassifierNode extends AbstractClassifierSlidingWindowManagingNode {

    /**
     * Prefix for sum attribute. For each class its class label will be appended to
     * this key prefix.
     */
    protected static final String INTERNAL_SUM_KEY_PREFIX = "_sum_";

    /**
     * Prefix for sum of squares attribute. For each class its class label will be appended to
     * this key prefix.
     */
    protected static final String INTERNAL_SUMSQUARE_KEY_PREFIX = "_sumSquare_";

    /**
     * Prefix for number of measurements attribute. For each class its class label will be appended to
     * this key prefix.
     */
    protected static final String INTERNAL_TOTAL_KEY_PREFIX = "_total_";

    public AbstractGaussianClassifierNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    @Override
    protected void removeAllClassesHook(NodeState state) {
        int classes[] = state.getFromKeyWithDefault(KNOWN_CLASSES_LIST_KEY, new int[0]);
        for (int curClass : classes) {
            state.setFromKey(INTERNAL_TOTAL_KEY_PREFIX + curClass, Type.INT, 0);
            state.setFromKey(INTERNAL_SUM_KEY_PREFIX + curClass, Type.DOUBLE_ARRAY, new double[0]);
            state.setFromKey(INTERNAL_SUMSQUARE_KEY_PREFIX + curClass, Type.DOUBLE_ARRAY, new double[0]);
        }
    }


}
