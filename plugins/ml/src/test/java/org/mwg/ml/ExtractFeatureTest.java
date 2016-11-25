package org.mwg.ml;

import org.junit.Assert;
import org.junit.Test;
import org.mwg.*;
import org.mwg.base.BasePlugin;
import org.mwg.core.task.Actions;
import org.mwg.plugin.NodeFactory;

public class ExtractFeatureTest {

    @Test
    public void test() {
        final Graph graph = new GraphBuilder().withPlugin(new BasePlugin().declareNodeType(NoopRegressionNode.NAME, new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new NoopRegressionNode(world, time, id, graph);
            }
        })).build();
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
        final Graph graph = new GraphBuilder().withPlugin(new BasePlugin().declareNodeType(NoopRegressionNode.NAME, new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new NoopRegressionNode(world, time, id, graph);
            }
        })).build();
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
        final Graph graph = new GraphBuilder().withPlugin(new BasePlugin().declareNodeType(NoopRegressionNode.NAME, new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new NoopRegressionNode(world, time, id, graph);
            }
        })).build();
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
