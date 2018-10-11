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
package greycatTest.internal;

import greycat.Graph;
import greycat.GraphBuilder;
import greycat.scheduler.NoopScheduler;
import org.junit.Assert;
import org.junit.Test;

public class SaveTest {

    @Test
    public void test() {
        Graph g = GraphBuilder.newBuilder()
                .withMemorySize(100)
                .withSaveBatchSize(2)
                .withStorage(new MockStorage())
                .withScheduler(new NoopScheduler())
                .build();
        g.connect(null);
        Assert.assertEquals(((MockStorage) g.storage()).backend.size(), 0);
        g.save(null);
        Assert.assertEquals(((MockStorage) g.storage()).backend.size(), 0);
        g.newNode(0, 0);
        g.save(null);
        Assert.assertEquals(((MockStorage) g.storage()).backend.size(), 5); //gen + 4 chunk
        g.newNode(0, 0);
        //g.save(null);
        g.savePartial(null);
        Assert.assertEquals(((MockStorage) g.storage()).backend.size(), 7); //+2 chunk (batch size)
        g.savePartial(null);
        Assert.assertEquals(((MockStorage) g.storage()).backend.size(), 9); //+2 chunk (batch size)
        g.save(null);
        Assert.assertEquals(((MockStorage) g.storage()).backend.size(), 9); //+2 chunk (batch size)
    }

}
