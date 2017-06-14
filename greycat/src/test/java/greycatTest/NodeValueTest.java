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
package greycatTest;

import greycat.*;
import greycat.internal.CoreNodeValue;
import greycat.scheduler.NoopScheduler;
import org.junit.Test;

public class NodeValueTest {

    /**
     * @ignore ts
     */
    //@Test
    public void test() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                NodeValue nodeValue = (NodeValue) g.newTypedNode(0, 0, CoreNodeValue.NAME);
                long before = System.currentTimeMillis();
                long size = 5000000;
                for (int i = 0; i < size; i++) {
                    int finalI = i;
                    g.lookup(0, i, nodeValue.id(), new Callback<NodeValue>() {
                        @Override
                        public void on(NodeValue result) {
                            result.setValue(finalI * 2.0d);
                            result.free();
                        }
                    });
                }
                long after = System.currentTimeMillis();
                double timeSecond = (after - before) / 1000d;
                System.out.println(timeSecond);
                System.out.println(size / timeSecond);
            }
        });
    }

}
