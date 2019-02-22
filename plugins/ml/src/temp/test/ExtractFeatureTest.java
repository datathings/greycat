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
package greycat.ml;

import greycat.plugin.NodeFactory;
import org.junit.Assert;
import org.junit.Test;

public class ExtractFeatureTest {

    @Test
    public void test() {
        final Graph graph = new GraphBuilder().build();
        graph.nodeRegistry().declaration(NoopRegressionNode.NAME).setFactory(new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new NoopRegressionNode(world, time, id, graph);
            }
        });
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                Node domainNode = graph.newNode(0, 0);
                domainNode.set("value", Type.DOUBLE, 42.2);

                final RegressionNode learningNode = (RegressionNode) graph.newTypedNode(0, 0, "NoopRegressionNode");
                learningNode.addToRelation("sensor", domainNode);
                learningNode.set("from", Type.STRING, "sensor.value");
                learningNode.learn(3, new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        Assert.assertEquals("{\"world\":0,\"time\":0,\"id\":2,\"sensor\":[1],\"from\":\"sensor.value\",\"extracted\":[42.2]}", learningNode.toString());
                    }
                });

                graph.disconnect(null);
            }
        });
    }

    @Test
    public void testMath() {
        final Graph graph = new GraphBuilder().build();
        graph.nodeRegistry().declaration(NoopRegressionNode.NAME).setFactory(new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new NoopRegressionNode(world, time, id, graph);
            }
        });
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                Node domainNode = graph.newNode(0, 0);
                domainNode.set("value", Type.DOUBLE, 2.5);

                final RegressionNode learningNode = (RegressionNode) graph.newTypedNode(0, 0, "NoopRegressionNode");
                learningNode.addToRelation("sensor", domainNode);

                learningNode.set("from", Type.STRING, "sensor.executeExpression(value*3)");
                learningNode.learn(3, new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        Assert.assertEquals("{\"world\":0,\"time\":0,\"id\":2,\"sensor\":[1],\"from\":\"sensor.executeExpression(value*3)\",\"extracted\":[7.5]}", learningNode.toString());
                    }
                });
                graph.disconnect(null);
            }
        });
    }

    @Test
    public void testMathEscaped() {
        final Graph graph = new GraphBuilder().build();
        graph.nodeRegistry().declaration(NoopRegressionNode.NAME).setFactory(new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new NoopRegressionNode(world, time, id, graph);
            }
        });
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                Node domainNode = graph.newNode(0, 0);
                domainNode.set("value", Type.DOUBLE, 2.5);

                final RegressionNode learningNode = (RegressionNode) graph.newTypedNode(0, 0, "NoopRegressionNode");
                learningNode.addToRelation("sensor", domainNode);
                learningNode.set("from", Type.STRING, "sensor.executeExpression('value*3')");
                learningNode.learn(3, new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        Assert.assertEquals("{\"world\":0,\"time\":0,\"id\":2,\"sensor\":[1],\"from\":\"sensor.executeExpression('value*3')\",\"extracted\":[7.5]}", learningNode.toString());
                    }
                });

                graph.disconnect(null);
            }
        });
    }

}
