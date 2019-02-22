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
package org.mwg.mlx;

import org.mwg.Graph;
import org.mwg.Node;
import org.mwg.ml.MLPlugin;
import org.mwg.mlx.algorithm.classifier.BatchDecisionTreeNode;
import org.mwg.mlx.algorithm.classifier.GaussianClassifierNode;
import org.mwg.mlx.algorithm.classifier.GaussianNaiveBayesianNode;
import org.mwg.mlx.algorithm.classifier.LogisticRegressionClassifierNode;
import org.mwg.mlx.algorithm.regression.*;
import org.mwg.plugin.NodeFactory;

public class MLXPlugin extends MLPlugin {

    public MLXPlugin() {
        super();
        declareNodeType(LinearRegressionWithPeriodicityNode.NAME, new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new LinearRegressionWithPeriodicityNode(world, time, id, graph);
            }
        });
        declareNodeType(LinearRegressionNode.NAME, new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new LinearRegressionNode(world, time, id, graph);
            }
        });
        declareNodeType(LinearRegressionBatchGDNode.NAME, new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new LinearRegressionBatchGDNode(world, time, id, graph);
            }
        });
        declareNodeType(LinearRegressionSGDNode.NAME, new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new LinearRegressionSGDNode(world, time, id, graph);
            }
        });
        declareNodeType(GaussianNaiveBayesianNode.NAME, new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new GaussianNaiveBayesianNode(world, time, id, graph);
            }
        });
        declareNodeType(LogisticRegressionClassifierNode.NAME, new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new LogisticRegressionClassifierNode(world, time, id, graph);
            }
        });
        //BatchDecisionTreeNode
        declareNodeType(BatchDecisionTreeNode.NAME, new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new BatchDecisionTreeNode(world, time, id, graph);
            }
        });
        declareNodeType(GaussianClassifierNode.NAME, new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new GaussianClassifierNode(world, time, id, graph);
            }
        });
        declareNodeType(AutoregressionBasedPeriodicityDetector.NAME, new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new AutoregressionBasedPeriodicityDetector(world, time, id, graph);
            }
        });
    }

}
