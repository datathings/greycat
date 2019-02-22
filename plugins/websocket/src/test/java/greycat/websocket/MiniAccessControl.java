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
package greycat.websocket;

import greycat.*;
import greycat.scheduler.NoopScheduler;
import greycatTest.internal.MockStorage;
import org.junit.Assert;

import static greycat.Tasks.newTask;

public class MiniAccessControl {

    public static void main(String[] args) {
        MockStorage raw_storage = new MockStorage();

        Graph master = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).withStorage(raw_storage).build();
        master.connect(null);

        //init from master
        master.declareIndex(0, "nodes", new Callback<NodeIndex>() {
            @Override
            public void on(NodeIndex nodesIndex) {
                Node priv = master.newNode(0, 0).setGroup(1);
                priv.set("name", Type.STRING, "private");
                nodesIndex.update(priv);

                Node pub = master.newNode(0, 0).setGroup(0);
                pub.set("name", Type.STRING, "public");
                nodesIndex.update(pub);

            }
        }, "name");

        master.save(null);

        MiniFilteredStorage mac = new MiniFilteredStorage(raw_storage, new int[]{1});

        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).withStorage(mac).build();
        g.connect(null);
        newTask().travelInTime("0").readIndex("nodes").flat().execute(g, new Callback<TaskResult>() {
            @Override
            public void on(TaskResult result) {
                Assert.assertEquals(1, result.size());
                Assert.assertEquals("{\"world\":0,\"time\":0,\"id\":3,\"name\":\"public\"}", result.get(0).toString());
            }
        });

        Node priv2 = g.newNode(0, 0).setGroup(1);
        priv2.set("name", Type.STRING, "private2");

        g.save(null);

        master.lookup(0, 0, priv2.id(), new Callback<Node>() {
            @Override
            public void on(Node result) {
                Assert.assertNull(result);
            }
        });

    }

}
