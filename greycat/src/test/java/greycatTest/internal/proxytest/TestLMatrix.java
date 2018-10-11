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
package greycatTest.internal.proxytest;

import greycat.*;
import greycat.struct.LMatrix;
import org.junit.Assert;
import org.junit.Test;

public class TestLMatrix {

    @Test
    public void testMatrix() {
        Graph graph = GraphBuilder
                .newBuilder()
                .build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                final Node node = graph.newNode(0, 0);
                final LMatrix matrix = (LMatrix) node.getOrCreate("matrix", Type.LMATRIX);
                matrix.init(3, 3);
                matrix.set(0, 0, 0);
                matrix.set(1, 1, 1);
                matrix.set(2, 2, 2);

                Assert.assertTrue(matrix.get(0,0)==0);
                Assert.assertTrue(matrix.get(1,1)==1);
                Assert.assertTrue(matrix.get(2,2)==2);
                node.travelInTime(1, new Callback<Node>() {
                    @Override
                    public void on(final Node node_t1) {
                        final LMatrix matrix_t1 = (LMatrix) node_t1.getOrCreate("matrix", Type.LMATRIX);
                        Assert.assertTrue(matrix_t1.get(0,0)==0);
                        Assert.assertTrue(matrix_t1.get(1,1)==1);
                        Assert.assertTrue(matrix_t1.get(2,2)==2);

                        matrix_t1.set(0, 0, 10);
                        matrix_t1.set(1, 1, 11);
                        matrix_t1.set(2, 2, 12);
                        graph.save(new Callback<Boolean>() {
                            @Override
                            public void on(Boolean saved) {

                                node_t1.travelInTime(0, new Callback<Node>() {
                                    @Override
                                    public void on(Node node2) {
                                        LMatrix matrix_t2 = (LMatrix) node2.getOrCreate("matrix", Type.LMATRIX);

                                        Assert.assertTrue(matrix_t2.get(0,0)==0);
                                        Assert.assertTrue(matrix_t2.get(1,1)==1);
                                        Assert.assertTrue(matrix_t2.get(2,2)==2);

                                        node2.travelInTime(1, new Callback<Node>() {
                                            @Override
                                            public void on(Node node3) {
                                                LMatrix matrix_t3 = (LMatrix) node3.getOrCreate("matrix", Type.LMATRIX);
                                                Assert.assertTrue(matrix_t3.get(0,0)==10);
                                                Assert.assertTrue(matrix_t3.get(1,1)==11);
                                                Assert.assertTrue(matrix_t3.get(2,2)==12);

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
