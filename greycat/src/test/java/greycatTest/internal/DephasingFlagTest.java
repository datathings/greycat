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

import greycat.*;
import greycat.scheduler.NoopScheduler;
import org.junit.Assert;
import org.junit.Test;
import greycat.plugin.NodeState;

public class DephasingFlagTest {

    private long cacheSize = 10000;

    @Test
    public void heapTest() {
        dephaseFlagTest(new GraphBuilder().withMemorySize(cacheSize).withScheduler(new NoopScheduler()).build());
    }

    /**
     * @ignore ts
     */
    @Test
    public void offHeapTest() {
        /*
        OffHeapByteArray.alloc_counter = 0;
        OffHeapDoubleArray.alloc_counter = 0;
        OffHeapLongArray.alloc_counter = 0;
        OffHeapStringArray.alloc_counter = 0;

        Unsafe.DEBUG_MODE = true;

        dephaseFlagTest(new GraphBuilder().withScheduler(new NoopScheduler()).withOffHeapMemory().withMemorySize(cacheSize).saveEvery(cacheSize - 100).build());

        Assert.assertTrue(OffHeapByteArray.alloc_counter == 0);
        Assert.assertTrue(OffHeapDoubleArray.alloc_counter == 0);
        Assert.assertTrue(OffHeapLongArray.alloc_counter == 0);
        Assert.assertTrue(OffHeapStringArray.alloc_counter == 0);
        */
    }

    private void dephaseFlagTest(final Graph graph) {


        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                long init = graph.space().available();

                Node n0 = graph.newNode(0, 0);
                n0.set("name" , Type.STRING, "n0");

                n0.travelInTime(5, new Callback<Node>() {
                    @Override
                    public void on(Node n5) {
                        NodeState dephasedN5 = graph.resolver().newState(n5, 0, 3);
                        //n5.set("name", "n5");
                        n5.free();
                    }
                });

                n0.free();


                graph.save(new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        long nb = graph.space().available();

                        /*
                        if(graph.space() instanceof HeapChunkSpace){
                            ((HeapChunkSpace) graph.space()).printMarked();
                        }*/

                        Assert.assertEquals(init, nb);
                    }
                });

                graph.disconnect(new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {

                    }
                });

            }
        });
    }

}
