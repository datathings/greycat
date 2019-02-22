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
package greycatTest.internal.tree;

import greycat.*;
import greycat.internal.custom.NDTree;
import greycat.internal.custom.IndexManager;
import greycat.struct.EStructArray;
import greycat.struct.NDManager;

import java.util.Random;

/**
 * Created by assaad on 09/05/2017.
 */
public class TestNDIndexer {

    public static void main(String[] args) {
        Graph graph= GraphBuilder.newBuilder().build();

        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                Node n=graph.newNode(0,0);
                EStructArray egraph= (EStructArray) n.getOrCreate("egraph", Type.ESTRUCT_ARRAY);
                NDManager manager=new IndexManager();

                NDTree ntree=new NDTree(egraph,manager);

                int dim=4;
                double[] min=new double[dim];
                double[] max=new double[dim];
                double[] res=new double[dim];
                double[] key=new double[dim];

                for(int i=0;i<dim;i++){
                    min[i]=0;
                    max[i]=1.0;
                    res[i]=0.001;
                }

                ntree.setMinBound(min);
                ntree.setMaxBound(max);
                ntree.setResolution(res);

                Random random=new Random();


                for(int i=0;i<100000;i++){
                    for(int j=0;j<dim;j++){
                        key[j]=random.nextDouble();
                    }
                    ntree.insert(key,i);
                }

                System.out.println("ntree size: "+ntree.size());





            }
        });

    }
}
