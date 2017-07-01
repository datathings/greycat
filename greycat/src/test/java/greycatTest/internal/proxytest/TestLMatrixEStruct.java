/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
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
import greycat.struct.EStruct;
import greycat.struct.EStructArray;
import greycat.struct.LMatrix;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by assaad on 18/04/2017.
 */
public class TestLMatrixEStruct {
    @Test
    public void testMatrixEnode() {
        Graph graph= GraphBuilder
                .newBuilder()
                .build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                Node node = graph.newNode(0,0);

                EStructArray eg= (EStructArray) node.getOrCreate("egraph", Type.ESTRUCT_ARRAY);
                EStruct en =eg.newEStruct();
                eg.setRoot(en);

                LMatrix matrix= (LMatrix)en.getOrCreate("matrix", Type.LMATRIX);

                matrix.init(3,3);
                matrix.set(0,0,0);
                matrix.set(1,1,1);
                matrix.set(2,2,2);
                Assert.assertTrue(matrix.get(0,0)==0);
                Assert.assertTrue(matrix.get(1,1)==1);
                Assert.assertTrue(matrix.get(2,2)==2);


                node.travelInTime(1, new Callback<Node>() {
                    @Override
                    public void on(Node result1) {
                        EStructArray eg= (EStructArray) result1.getOrCreate("egraph", Type.ESTRUCT_ARRAY);
                        EStruct en =eg.root();
                        LMatrix matrix_t1= (LMatrix)en.getOrCreate("matrix", Type.LMATRIX);
                        Assert.assertTrue(matrix_t1.get(0,0)==0);
                        Assert.assertTrue(matrix_t1.get(1,1)==1);
                        Assert.assertTrue(matrix_t1.get(2,2)==2);

                        matrix_t1.set(0,0,10);
                        matrix_t1.set(1,1,11);
                        matrix_t1.set(2,2,12);


                        result1.travelInTime(0, new Callback<Node>() {
                            @Override
                            public void on(Node result2) {
                                EStructArray eg= (EStructArray) result2.getOrCreate("egraph", Type.ESTRUCT_ARRAY);
                                EStruct en =eg.root();
                                LMatrix matrix_t2= (LMatrix)en.getOrCreate("matrix", Type.LMATRIX);
                                Assert.assertTrue(matrix_t2.get(0,0)==0);
                                Assert.assertTrue(matrix_t2.get(1,1)==1);
                                Assert.assertTrue(matrix_t2.get(2,2)==2);
                                result2.travelInTime(1, new Callback<Node>() {
                                    @Override
                                    public void on(Node result3) {
                                        EStructArray eg= (EStructArray) result3.getOrCreate("egraph", Type.ESTRUCT_ARRAY);
                                        EStruct en =eg.root();
                                        LMatrix matrix_t3= (LMatrix)en.getOrCreate("matrix", Type.LMATRIX);
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
}
