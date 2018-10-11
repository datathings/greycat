/**
 * Copyright 2017-2018 The GreyCat Authors.  All rights reserved.
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
package org.mwg.mlx.algorithm.classifier;

import org.mwg.Graph;
import org.mwg.mlx.algorithm.AbstractClassifierSlidingWindowManagingNode;
import org.mwg.plugin.NodeState;

/**
 * Created by andre on 5/9/2016.
 */
public class StreamDecisionTreeNode extends AbstractClassifierSlidingWindowManagingNode{

    //TODO We BADLY need to keep the tree between received data points.

    public StreamDecisionTreeNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    @Override
    protected int predictValue(NodeState state, double[] value) {
        return 0;
    }

    @Override
    protected double getLikelihoodForClass(NodeState state, double[] value, int classNum) {
        final int predictedClass = predictValue(state, value);
        //No real likelihood. Just yes or no.
        return (classNum==predictedClass)?1.0:0.0;
    }

    @Override
    protected void updateModelParameters(NodeState state, double[] valueBuffer, int[] resultBuffer, double[] value, int classNumber) {
        //TODO No tree? Initialize with the leaf.

        //TODO If there is a tree already:
        //TODO Go to the leaf
    }

    @Override
    protected boolean addValueBootstrap(NodeState state, double[] value, int classNum){
        //-1 because we will add 1 value to the buffer later.
        //while (getCurrentBufferLength() > (getMaxBufferLength()-1)) {
          //  removeFirstValueFromBuffer();
        //}
        return super.addValueBootstrap(state, value, classNum);
    }

    @Override
    protected void removeAllClassesHook(NodeState state) {
        //Nothing
    }
}
