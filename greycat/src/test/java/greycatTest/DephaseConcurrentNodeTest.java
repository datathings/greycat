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

import greycat.Graph;
import greycat.GraphBuilder;
import greycat.Node;
import greycat.Type;
import greycat.scheduler.NoopScheduler;
import org.junit.Assert;
import org.junit.Test;

public class DephaseConcurrentNodeTest {

    @Test
    public void test() {
        int cache_size = 3000;
        Graph graph = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).withMemorySize(cache_size).build();
        graph.connect(null);
        long cache_remaining = graph.space().available();
        Node n = graph.newNode(0, 0);
        long n_id = n.id();
        for (int i = 0; i < 1000; i++) {
            graph.lookup(0, i, n_id, result -> {
                result.forceSetAt(0, Type.LONG, result.time());
                result.free();
            });
        }
        n.free();
        graph.save(null);
        Assert.assertEquals(cache_remaining, graph.space().available());
        //ok now every flags should zero, let's test unPhased pointers
        final Node[] ptrs = {null, null};
        graph.lookup(0, 1000, n_id, result -> {
            ptrs[0] = result;
            ptrs[1] = graph.cloneNode(result);//clone unPhased node
        });
        ptrs[0].forceSetAt(0, Type.LONG, ptrs[0].time());
        long second_ptr_resolve = (long) ptrs[1].getAt(0);
        Assert.assertEquals(ptrs[1].time(), second_ptr_resolve);
        //ptrs[1].typeAt(0);
        n.free();
        ptrs[0].free();
        ptrs[1].free();
        graph.save(null);

    }

}
