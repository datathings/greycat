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
package greycatTest.internal.tree;

import greycat.*;
import greycat.internal.custom.VolatileTreeResult;
import greycat.struct.EStructArray;
import greycat.struct.EStruct;
import org.junit.Test;

import java.util.Random;


public class TestSort {

    @Test
    public void testsort() {
        final Graph graph = new GraphBuilder()
                .withMemorySize(10000)
                .build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                int max = 20;
                int maxinsert = 200;
                Random random = new Random();
                random.setSeed(1234);

                for (int i = 0; i < max; i++) {
                    int capacity = i + 50;
                    Node kmf = graph.newNode(0, 0);
                    EStructArray egraph = (EStructArray) kmf.getOrCreate("test", Type.ESTRUCT_ARRAY);
                    EStruct root = egraph.newEStruct();
                    egraph.setRoot(root);
                    VolatileTreeResult vr = new VolatileTreeResult(root, capacity);

                    for (int k = 0; k < maxinsert; k++) {
                        vr.insert(new double[]{k}, k, random.nextDouble());
                    }
                    vr.sort(true);

                    for (int k = 0; k < vr.size() - 1; k++) {
                        assert (vr.distance(k) < vr.distance(k + 1));
                    }

                    vr.sort(false);
                    for (int k = 0; k < vr.size() - 1; k++) {
                        assert (vr.distance(k) > vr.distance(k + 1));
                    }
                    kmf.free();
                    graph.save(null);
                }

            }
        });


    }
}
