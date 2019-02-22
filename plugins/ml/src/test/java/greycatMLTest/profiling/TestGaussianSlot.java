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
package greycatMLTest.profiling;

import greycat.*;
import greycat.ml.profiling.GaussianSlotsArray;
import greycat.struct.EStructArray;
import org.junit.Test;

public class TestGaussianSlot {
//    @Test
    public void TestGaussianSlot() {
        Graph g = GraphBuilder.newBuilder().build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                Node n = g.newNode(0, 0);
                EStructArray es = (EStructArray) n.getOrCreate("ss", Type.ESTRUCT_ARRAY);
                GaussianSlotsArray garray = new GaussianSlotsArray(es);
                int[] dim = new int[]{7, 24, 5};

                for (int temperature = 0; temperature < 5; temperature++) {
                    for (int hour = 0; hour < 24; hour++) {
                        for (int day = 0; day < 7; day++) {

                            garray.setDimensions(dim);

                            int s = GaussianSlotsArray.calculateSlotFromKeys(new int[]{day, hour, temperature}, dim);
                            System.out.println(temperature + " " + hour + " " + day + " " + s);
                        }
                    }
                }
            }
        });

    }
}
