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
package greycatTest.internal;

import greycat.Graph;
import greycat.Node;
import greycat.Type;
import greycat.chunk.ChunkType;
import greycat.chunk.StateChunk;
import greycat.internal.BlackHoleStorage;
import greycat.scheduler.NoopScheduler;
import greycat.utility.HashHelper;
import greycat.GraphBuilder;
import greycat.chunk.Chunk;
import greycat.internal.heap.HeapChunkSpace;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @ignore ts
 */
public class Benchmark4Test {

    //@Test
    public void test() {
        int nb = 3000000;
        long init = System.currentTimeMillis();
        HeapChunkSpace space = new HeapChunkSpace(nb * 2,-1, null, false);
        for (int i = 0; i < nb; i++) {
            Chunk c = space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, i);
            //space.putAndMark(ChunkType.STATE_CHUNK, 0, 0, i, c);
            //temp.put(i, c);

        }
        long begin = System.currentTimeMillis();
        for (int i = 0; i < nb; i++) {
            Chunk c = space.getAndMark(ChunkType.STATE_CHUNK, 0, 0, i);
            space.unmark(c.index());
        }
        long after = System.currentTimeMillis();
        System.out.println("total " + (after - init) + "ms, " + ((nb / 1000) / ((double) (after - init) / 1000d)) + " k chunk/s");
        System.out.println("insert " + (begin - init) + "ms, " + ((nb / 1000) / ((double) (begin - init) / 1000d)) + " k chunk/s");
        System.out.println("lookup " + (after - begin) + "ms, " + ((nb / 1000) / ((double) (after - begin) / 1000d)) + " k chunk/s");
    }

    // @Test
    public void test2() {
        /*
        int nb = 3000000;
        long init = System.currentTimeMillis();
        OffHeapChunkSpace space = new OffHeapChunkSpace(nb * 2, nb * 2);
        for (int i = 0; i < nb; i++) {
            StateChunk c = (StateChunk) space.create(ChunkType.STATE_CHUNK, 0, 0, i, null, null);
            space.putAndMark(ChunkType.STATE_CHUNK, 0, 0, i, c);
        }
        long begin = System.currentTimeMillis();
        for (int i = 0; i < nb; i++) {
            StateChunk c = (StateChunk) space.getAndMark(ChunkType.STATE_CHUNK, 0, 0, i);
            if (c != null) {
                space.unmarkChunk(c);
            }
        }
        long after = System.currentTimeMillis();
        System.out.println("total " + (after - init) + "ms, " + ((nb / 1000) / ((double) (after - init) / 1000d)) + " k chunk/s");
        System.out.println("insert " + (begin - init) + "ms, " + ((nb / 1000) / ((double) (begin - init) / 1000d)) + " k chunk/s");
        System.out.println("lookup " + (after - begin) + "ms, " + ((nb / 1000) / ((double) (after - begin) / 1000d)) + " k chunk/s");
        */
    }

    // @Test
    public void test3() {
        int nb = 1000000;
        HeapChunkSpace space = new HeapChunkSpace(nb,-1, null, true);
        Map<Long, Chunk> map = new HashMap<Long, Chunk>();
        for (int i = 0; i < nb; i++) {
            long hashed = HashHelper.tripleHash(ChunkType.STATE_CHUNK, 0, 0, i, nb);
            map.put(hashed, space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, i));
        }
        long begin = System.currentTimeMillis();
        for (int i = 0; i < nb; i++) {
            long hashed = HashHelper.tripleHash(ChunkType.STATE_CHUNK, 0, 0, i, nb);
            Chunk o = map.get(hashed);
            if (o instanceof StateChunk) {

            }
        }
        long after = System.currentTimeMillis();
        long diff = after - begin;
        double diffSecond = diff / 1000d;
        System.out.println((nb / 1000) / diffSecond);
    }

    // @Test
    public void testlookup() {
        Graph graph = new GraphBuilder()
                .withMemorySize(10000)
                .withScheduler(new NoopScheduler())
                .withStorage(new BlackHoleStorage())
                // .withDeepWorld()
                .build();

        final int nb = 1000000;
        graph.connect(result -> {
            Node root = graph.newNode(0, 0);
            Random rand = new Random();
            long begin = System.currentTimeMillis();
            for (int i = 0; i < nb; i++) {
                Node x = graph.newNode(0, 0);
                x.set("value", Type.DOUBLE, rand.nextDouble());
                root.addToRelation("children", x);
                x.free();
                if (i % 1000 == 0) {
                    graph.save(null);
                }
            }
            long after = System.currentTimeMillis();
            long diff = after - begin;
            double diffSecond = diff / 1000d;
            System.out.println(diffSecond);
            System.out.println((nb / 1000) / diffSecond);

        });
    }

}
