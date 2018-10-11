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
package greycatTest;

import greycat.*;
import greycat.scheduler.NoopScheduler;
import greycat.utility.LArray;
import org.junit.Assert;
import org.junit.Test;

public class NotifierTest {

    @Test
    public void test() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(null);

        Node n = g.newNode(0, 0);
        n.set("name", Type.STRING, "myNode");

        LArray collector = new LArray();

        int registrationId = n.listen(new NodeListener() {
            @Override
            public void on(long[] changeTimes) {
                for (int i = 0; i < changeTimes.length; i++) {
                    collector.add(changeTimes[i]);
                }
            }
        });
        g.save(null);

        Assert.assertEquals(1, collector.size());
        Assert.assertEquals(0, collector.get(0));

        n.travelInTime(42, new Callback<Node>() {
            @Override
            public void on(Node result) {
                result.set("name", Type.STRING, "myNode2");
            }
        });

        g.save(null);
        Assert.assertEquals(2, collector.size());
        Assert.assertEquals(0, collector.get(0));
        Assert.assertEquals(42, collector.get(1));

        n.unlisten(registrationId);

        n.travelInTime(100, new Callback<Node>() {
            @Override
            public void on(Node result) {
                result.set("name", Type.STRING, "myNode100");
            }
        });

        Assert.assertEquals(2, collector.size());
        
    }

}
