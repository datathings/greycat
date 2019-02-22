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
package greycatTest.internal;

import greycat.*;
import greycat.struct.Buffer;
import org.junit.Assert;
import org.junit.Test;

public class MiniFlagTest {

    private long cacheSize = 10000;

    @Test
    public void heapTest() {
        flagTest(new GraphBuilder().withMemorySize(cacheSize).build());
    }

    private void flagTest(final Graph graph) {
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                long available = graph.space().available();

                Node node = graph.newNode(0, 0);
                node.set("name", Type.STRING, "hello");

                //   ((HeapChunkSpace) graph.space()).printMarked();

                graph.space().save(false,false, null,new Callback<Buffer>() {
                    @Override
                    public void on(Buffer result) {

                        // System.out.println("<=============>");

                        //   ((HeapChunkSpace) graph.space()).printMarked();

                        graph.lookup(0, 0, node.id(), new Callback<Node>() {
                            @Override
                            public void on(Node result) {
                                node.free();
                                result.free();
                                graph.save(new Callback<Boolean>() {
                                    @Override
                                    public void on(Boolean result) {
                                        long availableAfter = graph.space().available();
                                        Assert.assertEquals(available, availableAfter);
                                        graph.disconnect(new Callback<Boolean>() {
                                            @Override
                                            public void on(Boolean result) {

                                            }
                                        });
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
