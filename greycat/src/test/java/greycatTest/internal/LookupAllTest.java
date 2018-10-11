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

import greycat.Node;
import org.junit.Assert;
import org.junit.Test;
import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;

public class LookupAllTest {

    @Test
    public void test() {
        final Graph graph = new GraphBuilder().build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean o) {
                final long availableAfterConnect = graph.space().available();
                final Node node0 = graph.newNode(0, 0);
                final long id = node0.id();

                final Node node2 = graph.newNode(0, 0);
                final long id2 = node2.id();

                node0.free();
                node2.free();

                graph.save(new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        //do something with the node
                        graph.lookup(0, 0, id, new Callback<Node>() {
                            @Override
                            public void on(Node result) {
                                //check that the lookup return the same
                                Assert.assertTrue(result.id() == id);
                                result.free();

                                graph.resolver().lookupAll(0, 0, new long[]{id, id2}, new Callback<Node[]>() {
                                    @Override
                                    public void on(Node[] result) {
                                        Assert.assertTrue(result[0].id() == id);
                                        Assert.assertTrue(result[1].id() == id2);

                                        graph.freeNodes(result);

                                        final long availableAfter = graph.space().available();
                                        Assert.assertEquals(availableAfterConnect, availableAfter);

                                        graph.disconnect(null);

                                    }
                                });

                            }
                        });
                    }
                });
            }
        });
    }
}
