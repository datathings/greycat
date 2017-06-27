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
package greycatMLTest.profiling;

import greycat.*;
import greycat.ml.HelperForTest;
import greycat.ml.profiling.GaussianENode;
import greycat.struct.EGraph;
import greycat.struct.ENode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by assaad on 21/06/2017.
 */
public class TestGmmEnode {
    @Test
    public void Test(){
        Graph graph= GraphBuilder
                .newBuilder()
                .build();

        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                Node node = graph.newNode(0,0);

                EGraph eg= (EGraph) node.getOrCreate("graph", Type.EGRAPH);
                ENode en=eg.newNode();
                ENode en2=eg.newNode();
                ENode en3=eg.newNode();

                GaussianENode gaussian= new GaussianENode(en);
                GaussianENode gaussian2= new GaussianENode(en2);
                GaussianENode gaussian3= new GaussianENode(en3);

                double[] key={1.1,2.2,3.3};
                int n=10;

                for(int i=0;i<n;i++){
                    gaussian.learn(key);
                }

                gaussian2.learnWithOccurence(key,n);

                gaussian3.learn(key);
                gaussian3.learnWithOccurence(key,n-1);

                double[] sumsq=gaussian.getSumSq();
                double[] sumsq2=gaussian2.getSumSq();
                double[] sumsq3=gaussian3.getSumSq();


                Assert.assertTrue(HelperForTest.assertArrayEquals(sumsq,sumsq2,1e-7));
                Assert.assertTrue(HelperForTest.assertArrayEquals(sumsq,sumsq3,1e-7));


                double[] sum=gaussian.getSum();
                double[] sum2=gaussian2.getSum();
                double[] sum3=gaussian3.getSum();

                Assert.assertTrue(HelperForTest.assertArrayEquals(sum,sum2,1e-7));
                Assert.assertTrue(HelperForTest.assertArrayEquals(sum,sum3,1e-7));
            }
        });

    }

}
