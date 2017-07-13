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
import greycat.scheduler.NoopScheduler;
import greycatTest.internal.MockStorage;
import org.junit.Assert;
import org.junit.Test;

public class DeleteTest {

    @Test
    public void test() {
        MockStorage storage = new MockStorage();
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).withStorage(storage).build();
        g.connect(null);
        g.save(null);
        Assert.assertEquals(0, storage.backend.size());
        Node n = g.newNode(0, 0);
        final long n_id = n.id();
        for (int i = 0; i < 10; i++) {
            g.lookup(0, i, n_id, result -> {
                result.forceSetAt(0, Type.LONG, result.time());
                result.free();
            });
        }
        g.save(null);
        Assert.assertEquals(14, storage.backend.size());
        n.drop(null);
        Assert.assertEquals(1, storage.backend.size());//only the generator remains
    }

}
