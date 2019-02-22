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
package greycatTest.internal.chunk;

import greycat.Graph;
import greycat.chunk.ChunkType;
import greycat.chunk.StateChunk;
import greycat.plugin.MemoryFactory;
import greycat.plugin.Storage;
import greycat.struct.*;
import greycatTest.internal.MockStorage;
import org.junit.Assert;
import org.junit.Test;
import greycat.GraphBuilder;
import greycat.Type;
import greycat.chunk.ChunkSpace;
import greycat.scheduler.NoopScheduler;

public abstract class AbstractEStructArrayTest {

    private MemoryFactory factory;

    public AbstractEStructArrayTest(MemoryFactory factory) {
        this.factory = factory;
    }

    @Test
    public void simpleUsageTest() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(null);
        ChunkSpace space = factory.newSpace(100, -1, g, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        //test embedded graph attribute
        EStructArray egraph = (EStructArray) chunk.getOrCreateAt(0, Type.ESTRUCT_ARRAY);
        //test primitive attribute
        EStruct eStruct = egraph.newEStruct();
        egraph.setRoot(eStruct);
        eStruct.set("name", Type.STRING, "hello");
        Assert.assertEquals("{\"name\":\"hello\"}", eStruct.toString());
        //test single eRelation
        EStruct secondEStruct = egraph.newEStruct();
        secondEStruct.set("name", Type.STRING, "secondNode");
        eStruct.set("children", Type.ESTRUCT, secondEStruct);
        EStruct retrieved = (EStruct) eStruct.get("children");
        Assert.assertEquals(retrieved.toString(), retrieved.toString());
        //test eRelation
        ERelation eRelation = (ERelation) eStruct.getOrCreate("testRel", Type.ERELATION);
        for (int i = 0; i < 3; i++) {
            EStruct loopNode = egraph.newEStruct();
            loopNode.set("name", Type.STRING, "node_" + i);
            eRelation.add(loopNode);
        }
        ERelation resolvedERelation = (ERelation) eStruct.get("testRel");
        Assert.assertEquals(3, resolvedERelation.size());
        Assert.assertEquals("[2,3,4]", resolvedERelation.toString());

        space.free(chunk);
        space.freeAll();
    }

    @Test
    public void setCostTest() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(null);
        ChunkSpace space = factory.newSpace(100, -1, g, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        EStructArray egraph = (EStructArray) chunk.getOrCreateAt(0, Type.ESTRUCT_ARRAY);
        EStruct eStruct = egraph.newEStruct();
        for (int i = 0; i < 1000000; i++) {
            eStruct.setAt(i, Type.INT, i);
        }
        for (int i = 0; i < 1000000; i++) {
            Assert.assertEquals(i, (int) eStruct.getAt(i));
        }
        egraph.free();
        space.free(chunk);
        space.freeAll();
    }

    @Test
    public void miniLoadSaveTest() {
        Storage mock = new MockStorage();
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).withStorage(mock).build();
        g.connect(null);

        StateChunk chunk = (StateChunk) g.space().createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        EStructArray structArray = (EStructArray) chunk.getOrCreateAt(0, Type.ESTRUCT_ARRAY);
        EStruct eStruct = structArray.newEStruct();
        eStruct.set("name", Type.STRING, "hello");

        g.save(null);
        g.disconnect(null);

        g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).withStorage(mock).build();
        g.connect(null);
        g.space().getOrLoadAndMark(ChunkType.STATE_CHUNK, 0, 0, 0, res -> {
            StateChunk loaded = (StateChunk) res;
            EStructArray egraphLoaded = (EStructArray) loaded.getAt(0);
            Assert.assertEquals(1, egraphLoaded.size());
            Assert.assertEquals("hello", egraphLoaded.estruct(0).get("name"));
        });

    }

    @Test
    public void nestedTest() {
        Storage mock = new MockStorage();
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).withStorage(mock).build();
        g.connect(null);

        StateChunk chunk = (StateChunk) g.space().createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        EStructArray structArray = (EStructArray) chunk.getOrCreateAt(0, Type.ESTRUCT_ARRAY);
        EStruct eStruct = structArray.newEStruct();
        eStruct.set("name", Type.STRING, "hello");

        EStructArray nestedStructArray = (EStructArray) eStruct.getOrCreate("nested", Type.ESTRUCT_ARRAY);
        EStruct nestedStruct = nestedStructArray.newEStruct();
        nestedStruct.set("name", Type.STRING, "nestedStruct");
        Assert.assertEquals(1, nestedStructArray.size());


        g.save(null);
        g.disconnect(null);

        Buffer buf = g.newBuffer();
        chunk.save(buf);

        g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).withStorage(mock).build();
        g.connect(null);
        g.space().getOrLoadAndMark(ChunkType.STATE_CHUNK, 0, 0, 0, res -> {
            StateChunk loaded = (StateChunk) res;
            EStructArray egraphLoaded = (EStructArray) loaded.getAt(0);
            Assert.assertEquals(1, egraphLoaded.size());
            EStruct firstStruct = egraphLoaded.estruct(0);
            Assert.assertEquals("hello", firstStruct.get("name"));

            EStructArray nested = firstStruct.getEGraph("nested");
            Assert.assertEquals(1, nested.size());
            EStruct firstNestedStruct = nested.estruct(0);
            Assert.assertEquals("nestedStruct", firstNestedStruct.get("name"));

        });

    }

    @Test
    public void loadSaveTest() {

        Storage mock = new MockStorage();
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).withStorage(mock).build();
        g.connect(null);

        StateChunk chunk = (StateChunk) g.space().createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        chunk.set("name", Type.STRING, "ParentChunk");

        //test embedded graph attribute
        EStructArray egraph = (EStructArray) chunk.getOrCreateAt(0, Type.ESTRUCT_ARRAY);
        //test primitive attribute
        EStruct eStruct = egraph.newEStruct();
        egraph.setRoot(eStruct);
        eStruct.set("self", Type.ESTRUCT, eStruct);
        eStruct.set("name", Type.STRING, "root");
        ERelation eRelation = (ERelation) eStruct.getOrCreate("children", Type.ERELATION);
        for (int i = 0; i < 9999; i++) {
            EStruct loopNode = egraph.newEStruct();
            loopNode.set("name", Type.STRING, "node_" + i);
            eRelation.add(loopNode);
        }
        //long before = System.currentTimeMillis();
        g.save(null);
        long after = System.currentTimeMillis();
        //System.out.println("save time:" + (after - before));
        g.disconnect(null);

        g = GraphBuilder.newBuilder()
                .withScheduler(new NoopScheduler())
                .withStorage(mock)
                .build();
        g.connect(null);
        //final long before2 = System.currentTimeMillis();
        g.space().getOrLoadAndMark(ChunkType.STATE_CHUNK, 0, 0, 0, res -> {
            StateChunk loaded = (StateChunk) res;
            EStructArray egraphLoaded = (EStructArray) loaded.getAt(0);
            //long after2 = System.currentTimeMillis();
            //System.out.println("loading time:" + (after2 - before2));
            Assert.assertEquals(egraph.toString(), egraphLoaded.toString());
            EStruct rootLoaded = egraphLoaded.root();
            Assert.assertTrue(rootLoaded != null);
            Assert.assertEquals(rootLoaded.toString(), rootLoaded.get("self").toString());
        });

    }

    @Test
    public void mixedLoadSaveTest() {
        Storage mock = new MockStorage();

        Graph g = GraphBuilder.newBuilder()
                .withScheduler(new NoopScheduler())
                .withStorage(mock)
                .build();

        g.connect(null);
        StateChunk chunk = (StateChunk) g.space().createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        chunk.setAt(0, Type.STRING, "AdditionalString");
        EStructArray egraph = (EStructArray) chunk.getOrCreateAt(1, Type.ESTRUCT_ARRAY);
        chunk.setAt(2, Type.STRING, "AdditionalStringAfter");
        EStruct eStruct = egraph.newEStruct();
        eStruct.set("name", Type.STRING, "myEnode");


        g.save(null);

        g.disconnect(null);

        g = GraphBuilder.newBuilder()
                .withScheduler(new NoopScheduler())
                .withStorage(mock)
                .build();
        g.connect(null);
        //final long before2 = System.currentTimeMillis();
        g.space().getOrLoadAndMark(ChunkType.STATE_CHUNK, 0, 0, 0, res -> {
            StateChunk loaded = (StateChunk) res;
            Assert.assertEquals(loaded.getAt(0), chunk.getAt(0));
            Assert.assertEquals(loaded.getAt(2), chunk.getAt(2));

            //System.out.println(loaded);

            /*
            EStructArray egraphLoaded = (EStructArray) loaded.get(0);
            //long after2 = System.currentTimeMillis();
            //System.out.println("loading time:" + (after2 - before2));
            Assert.assertEquals(egraph.toString(), egraphLoaded.toString());
            EStruct rootLoaded = egraphLoaded.root();
            Assert.assertTrue(rootLoaded != null);
            Assert.assertEquals(rootLoaded.toString(), rootLoaded.get("self").toString());
            */
        });


    }

    @Test
    public void volatileTest() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(null);

        ChunkSpace space = factory.newSpace(100, -1, g, false);

        EStructArray eStructArray = space.newVolatileGraph();
        EStruct eStruct = eStructArray.newEStruct();

        Assert.assertNotNull(eStructArray);
        Assert.assertNotNull(eStruct);

        LMatrix lmat = (LMatrix) eStruct.getOrCreate("lmat", Type.LMATRIX);
        lmat.appendColumn(new long[]{1, 2, 3});
        lmat.set(1, 0, 5L);

        DMatrix mat = (DMatrix) eStruct.getOrCreate("dmat", Type.DMATRIX);
        mat.appendColumn(new double[]{1.0, 2.0, 3.0});
        mat.set(1, 0, 0.7);

        ERelation eRel = (ERelation) eStruct.getOrCreate("erel", Type.ERELATION);
        eRel.add(eStruct);

        eStructArray.free();
        space.freeAll();

    }


}
