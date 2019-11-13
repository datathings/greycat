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
package greycatTest;

import greycat.*;
import greycat.scheduler.NoopScheduler;
import greycatTest.internal.MockStorage;
import org.junit.Assert;
import org.junit.Test;

public class GuardTest {

    @Test
    public void test() {
        MockStorage storage = new MockStorage();
        Graph g = GraphBuilder.newBuilder().withMemorySize(10).withScheduler(new NoopScheduler()).withStorage(storage).build();
        g.connect(null);

        for (int i = 0; i < 100; i++) {
            Node n = g.newNode(0, 0);
            n.free();
        }
        g.save(null);
        long cacheSizeBefore = g.space().cacheSize();
        Assert.assertEquals(404, cacheSizeBefore);

        long cleaned = g.space().clean(25);
        Assert.assertEquals(cleaned, 101);
        Assert.assertEquals(cacheSizeBefore - cleaned, g.space().cacheSize());

        g.disconnect(null);
    }

}
