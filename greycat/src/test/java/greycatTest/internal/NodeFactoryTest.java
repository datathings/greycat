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
package greycatTest.internal;

import greycat.Graph;
import greycat.Node;
import greycat.plugin.NodeFactory;
import greycat.plugin.Plugin;
import org.junit.Assert;
import org.junit.Test;
import greycat.Callback;
import greycat.GraphBuilder;
import greycat.scheduler.NoopScheduler;
import greycat.base.BaseNode;

public class NodeFactoryTest {

    static final String NAME = "HelloWorldNode";

    interface ExNode extends Node {
        String sayHello();
    }

    class ExNodeImpl extends BaseNode implements ExNode {

        public ExNodeImpl(long p_world, long p_time, long p_id, Graph p_graph) {
            super(p_world, p_time, p_id, p_graph);
        }

        @Override
        public Object get(String name) {
            if (name.equals("hello")) {
                return "world";
            }
            return super.get(name);
        }

        @Override
        public String sayHello() {
            return "HelloWorld";
        }
    }

    class ExNodePlugin implements Plugin {

        @Override
        public void start(Graph graph) {
            graph.nodeRegistry().getOrCreateDeclaration(NAME).setFactory(new NodeFactory() {
                @Override
                public Node create(long world, long time, long id, Graph graph) {
                    return new ExNodeImpl(world, time, id, graph);
                }
            });
        }

        @Override
        public void stop() {

        }

    }

    @Test
    public void heapTest() {
        test(new GraphBuilder().withScheduler(new NoopScheduler()).withPlugin(new ExNodePlugin()).build());
    }

    /**
     * @ignore ts
     */
    @Test
    public void offHeapTest() {
        /*
        OffHeapByteArray.alloc_counter = 0;
        OffHeapDoubleArray.alloc_counter = 0;
        OffHeapLongArray.alloc_counter = 0;
        OffHeapStringArray.alloc_counter = 0;

        Unsafe.DEBUG_MODE = true;

        test(new GraphBuilder().withScheduler(new NoopScheduler()).withOffHeapMemory().withMemorySize(10000).saveEvery(20).withPlugin(new BasePlugin().declareNodeType(NAME, new NodeFactory() {
            @Override
            public Node create(long world, long time, long id, Graph graph) {
                return new ExNodeImpl(world, time, id, graph);
            }
        })).build());

        Assert.assertTrue(OffHeapByteArray.alloc_counter == 0);
        Assert.assertTrue(OffHeapDoubleArray.alloc_counter == 0);
        Assert.assertTrue(OffHeapLongArray.alloc_counter == 0);
        Assert.assertTrue(OffHeapStringArray.alloc_counter == 0);
        */
    }

    private void test(final Graph graph) {

        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                Node specializedNode = graph.newTypedNode(0, 0, NAME);

                Assert.assertEquals(specializedNode.nodeTypeName(), "HelloWorldNode");

                String hw = (String) specializedNode.get("hello");
                Assert.assertTrue(hw.equals("world"));

                Node parent = graph.newNode(0, 0);
                parent.addToRelation("children", specializedNode);
                parent.traverse("children", new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        Assert.assertEquals("HelloWorld", ((ExNode) result[0]).sayHello());
                    }
                });

                specializedNode.free();
                graph.disconnect(null);
            }
        });
    }

}
