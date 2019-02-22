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

import greycat.chunk.ChunkType;
import greycat.struct.*;
import greycat.utility.HashHelper;
import org.junit.Assert;
import org.junit.Test;
import greycat.Constants;
import greycat.Type;
import greycat.chunk.ChunkSpace;
import greycat.chunk.StateChunk;
import greycat.plugin.MemoryFactory;

import java.util.HashMap;

public abstract class AbstractStateChunkTest {

    protected MemoryFactory factory;

    public AbstractStateChunkTest(MemoryFactory factory) {
        this.factory = factory;
    }

    // @Test
    public void speedTest() {

        int nb = 5000000;

        HashMap<Long, Object> hello = new HashMap<Long, Object>();
        long before2 = System.currentTimeMillis();
        for (long i = -nb; i < nb; i++) {
            hello.put(i, i);
        }
        long after2 = System.currentTimeMillis();

        int counter2 = 0;
        long before4 = System.currentTimeMillis();
        for (long i = -nb; i < nb; i++) {
            counter2 += (Long) hello.get(i);
        }
        long after4 = System.currentTimeMillis();

        System.gc();

        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);

        long before = System.currentTimeMillis();
        for (int i = -nb; i < nb; i++) {
            chunk.setAt(i, Type.LONG, i);
        }
        long after = System.currentTimeMillis();

        System.gc();

        long before3 = System.currentTimeMillis();
        int counter = 0;
        for (int i = -nb; i < nb; i++) {
            counter += (Long) chunk.getAt(i);
        }
        long after3 = System.currentTimeMillis();

        System.gc();

        StateChunk chunk2 = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        EStructArray eStructArray = (EStructArray) chunk2.getOrCreateAt(0, Type.ESTRUCT_ARRAY);
        EStruct eStruct = eStructArray.newEStruct();
        long before5 = System.currentTimeMillis();
        for (int i = -nb; i < nb; i++) {
            eStruct.setAt(i, Type.LONG, i);
        }
        long after5 = System.currentTimeMillis();

        System.gc();

        int counter3 = 0;
        long before6 = System.currentTimeMillis();
        for (int i = -nb; i < nb; i++) {
            counter3 += (Long) eStruct.getAt(i);
        }
        long after6 = System.currentTimeMillis();

        System.out.println("node:" + (after - before) + "-" + (after3 - before3));
        System.out.println("enode:" + (after5 - before5) + "-" + (after6 - before6));
        System.out.println("jmap:" + (after2 - before2) + "-" + (after4 - before4));
        System.out.println(counter + "-" + counter2 + "-" + counter3);

        space.free(chunk);
        space.free(chunk2);
        space.freeAll();
    }

    @Test
    public void lArrayTest() {
        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        LongArray larray = (LongArray) chunk.getOrCreateAt(0, Type.LONG_ARRAY);
        Assert.assertEquals(0, larray.size());
        larray.init(3);
        larray.set(0, 1);
        larray.set(1, 2);
        larray.set(2, 3);
        Assert.assertEquals(3, larray.size());
        //test load and save of array
        Buffer buffer = factory.newBuffer();
        chunk.save(buffer);
        /*
        StateChunk chunk2 = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 1);
        chunk2.load(buffer);
        Buffer buffer2 = factory.newBuffer();
        chunk2.save(buffer2);
        Assert.assertTrue(compareBuffers(buffer, buffer2));
        //check consistency
        Assert.assertEquals(3, ((LongArray) chunk2.getAt(0)).size());
        buffer2.free();
        space.free(chunk2);
        */
        buffer.free();

        space.free(chunk);
        space.freeAll();
    }

    @Test
    public void dArrayTest() {
        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        DoubleArray darray = (DoubleArray) chunk.getOrCreateAt(0, Type.DOUBLE_ARRAY);
        Assert.assertEquals(0, darray.size());
        darray.init(3);
        darray.set(0, 1.0);
        darray.set(1, 2.0);
        darray.set(2, 3.0);
        Assert.assertEquals(3, darray.size());
        //test load and save of array
        Buffer buffer = factory.newBuffer();
        chunk.save(buffer);
        StateChunk chunk2 = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 1);
        chunk2.load(buffer);
        Buffer buffer2 = factory.newBuffer();
        chunk2.save(buffer2);
        Assert.assertTrue(compareBuffers(buffer, buffer2));
        //check consistency
        Assert.assertEquals(3, ((DoubleArray) chunk2.getAt(0)).size());
        space.free(chunk);
        space.free(chunk2);
        buffer2.free();
        buffer.free();
        space.freeAll();
    }

    @Test
    public void iArrayTest() {
        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        IntArray darray = (IntArray) chunk.getOrCreateAt(0, Type.INT_ARRAY);
        Assert.assertEquals(0, darray.size());
        darray.init(3);
        darray.set(0, 1);
        darray.set(1, 2);
        darray.set(2, 3);
        Assert.assertEquals(3, darray.size());
        //test load and save of array
        Buffer buffer = factory.newBuffer();
        chunk.save(buffer);
        StateChunk chunk2 = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 1);
        chunk2.load(buffer);
        Buffer buffer2 = factory.newBuffer();
        chunk2.save(buffer2);
        Assert.assertTrue(compareBuffers(buffer, buffer2));
        //check consistency
        Assert.assertEquals(3, ((IntArray) chunk2.getAt(0)).size());
        space.free(chunk);
        space.free(chunk2);
        buffer2.free();
        buffer.free();
        space.freeAll();
    }

    @Test
    public void sArrayTest() {
        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        StringArray sArray = (StringArray) chunk.getOrCreateAt(0, Type.STRING_ARRAY);
        Assert.assertEquals(0, sArray.size());
        sArray.init(3);
        sArray.set(0, "1");
        sArray.set(1, "2");
        sArray.set(2, "3");
        Assert.assertEquals(3, sArray.size());
        //test load and save of array
        Buffer buffer = factory.newBuffer();
        chunk.save(buffer);
        StateChunk chunk2 = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 1);
        chunk2.load(buffer);
        Buffer buffer2 = factory.newBuffer();
        chunk2.save(buffer2);
        Assert.assertTrue(compareBuffers(buffer, buffer2));
        //check consistency
        Assert.assertEquals(3, ((StringArray) chunk2.getAt(0)).size());
        space.free(chunk);
        space.free(chunk2);
        buffer2.free();
        buffer.free();
        space.freeAll();
    }

    @Test
    public void castTest() {
        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);

        chunk.setAt(0, Type.DOUBLE, 1.5d);
        chunk.setAt(0, Type.DOUBLE, 1);
        chunk.setAt(0, Type.DOUBLE, 1L);
        chunk.setAt(0, Type.DOUBLE, 1f);
        chunk.setAt(0, Type.DOUBLE, (byte) 0);

        chunk.setAt(0, Type.INT, 1);
        chunk.setAt(0, Type.INT, 1.5d);
        chunk.setAt(0, Type.INT, 1L);
        chunk.setAt(0, Type.INT, 1f);
        chunk.setAt(0, Type.INT, (byte) 0);

        chunk.setAt(0, Type.LONG, 1);
        chunk.setAt(0, Type.LONG, 1.5d);
        chunk.setAt(0, Type.LONG, 1L);
        chunk.setAt(0, Type.LONG, 1f);
        chunk.setAt(0, Type.LONG, (byte) 0);

        space.free(chunk);
        space.freeAll();
    }

    @Test
    public void saveLoadPrimitif() {
        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);

        //init chunk selectWith primitives
        chunk.setAt(0, Type.BOOL, true);
        Assert.assertEquals(chunk.getAt(0), true);

        chunk.setAt(1, Type.STRING, "hello");
        Assert.assertEquals(chunk.getAt(1), "hello");

        chunk.setAt(2, Type.DOUBLE, 1.0);
        Assert.assertEquals(chunk.getAt(2), 1.0);

        chunk.setAt(3, Type.LONG, 1000l);
        Assert.assertEquals(chunk.getAt(3), 1000l);

        chunk.setAt(4, Type.INT, 100);
        Assert.assertEquals(chunk.getAt(4), 100);

        chunk.setAt(5, Type.INT, 1);
        Assert.assertEquals(chunk.getAt(5), 1);

        chunk.setAt(5, Type.INT, null);
        Assert.assertEquals(chunk.getAt(5), null);

        Buffer buffer = factory.newBuffer();
        chunk.save(buffer);
        StateChunk chunk2 = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 1);
        chunk2.load(buffer);
        Buffer buffer2 = factory.newBuffer();
        chunk2.save(buffer2);

        Assert.assertTrue(compareBuffers(buffer, buffer2));

        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(chunk.getAt(0), chunk2.getAt(0));
        }

        space.free(chunk);
        space.free(chunk2);
        buffer2.free();
        buffer.free();
        space.freeAll();

    }

    @Test
    public void relationSaveLoadTest() {
        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        chunk.setAt(0, Type.STRING, "hello");
        Assert.assertEquals(chunk.getAt(0), "hello");
        Relation rel = (Relation) chunk.getOrCreateAt(1, Type.RELATION);
        rel.add(1);
        rel.add(2);
        rel.add(3);
        Buffer buffer = factory.newBuffer();
        chunk.save(buffer);
        StateChunk chunk2 = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 1);
        chunk2.load(buffer);
        Buffer buffer2 = factory.newBuffer();
        chunk2.save(buffer2);
        Assert.assertTrue(compareBuffers(buffer, buffer2));

        space.free(chunk);
        space.free(chunk2);
        buffer2.free();
        buffer.free();
        space.freeAll();

    }

    @Test
    public void matrixSaveLoadTest() {
        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        chunk.setAt(0, Type.STRING, "hello");
        Assert.assertEquals(chunk.getAt(0), "hello");
        DMatrix rel = (DMatrix) chunk.getOrCreateAt(1, Type.DMATRIX);
        rel.init(2, 3);

        rel.set(0, 0, 0.0);
        rel.set(0, 1, 1.0);
        rel.set(0, 2, 2.0);

        rel.set(1, 0, 0.5);
        rel.set(1, 1, 1.5);
        rel.set(1, 2, 2.5);

        Buffer buffer = factory.newBuffer();
        chunk.save(buffer);
        StateChunk chunk2 = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 1);
        chunk2.load(buffer);
        Buffer buffer2 = factory.newBuffer();
        chunk2.save(buffer2);
        Assert.assertTrue(compareBuffers(buffer, buffer2));

        space.free(chunk);
        space.free(chunk2);
        buffer2.free();
        buffer.free();
        space.freeAll();

    }

    @Test
    public void lMatrixSaveLoadTest() {
        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);
        chunk.setAt(0, Type.STRING, "hello");
        Assert.assertEquals(chunk.getAt(0), "hello");
        LMatrix rel = (LMatrix) chunk.getOrCreateAt(1, Type.LMATRIX);
        rel.init(2, 3);

        rel.set(0, 0, 0L);
        rel.set(0, 1, 1L);
        rel.set(0, 2, 2L);

        rel.set(1, 0, 10L);
        rel.set(1, 1, 100L);
        rel.set(1, 2, 1000L);

        Buffer buffer = factory.newBuffer();
        chunk.save(buffer);
        StateChunk chunk2 = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 1);
        chunk2.load(buffer);
        Buffer buffer2 = factory.newBuffer();
        chunk2.save(buffer2);
        Assert.assertTrue(compareBuffers(buffer, buffer2));

        space.free(chunk);
        space.free(chunk2);
        buffer2.free();
        buffer.free();
        space.freeAll();

    }

    @Test
    public void saveLoadTest() {

        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);

        //init chunk selectWith primitives
        chunk.setAt(0, Type.BOOL, true);
        Assert.assertEquals(chunk.getAt(0), true);

        chunk.setAt(1, Type.STRING, "hello");
        Assert.assertEquals(chunk.getAt(1), "hello");

        chunk.setAt(2, Type.DOUBLE, 1.0);
        Assert.assertEquals(chunk.getAt(2), 1.0);

        chunk.setAt(3, Type.LONG, 1000l);
        Assert.assertEquals(chunk.getAt(3), 1000l);

        chunk.setAt(4, Type.INT, 100);
        Assert.assertEquals(chunk.getAt(4), 100);

        chunk.setAt(5, Type.INT, 1);
        Assert.assertEquals(chunk.getAt(5), 1);

        chunk.setAt(5, Type.INT, null);
        Assert.assertEquals(chunk.getAt(5), null);

        Buffer buffer = factory.newBuffer();
        chunk.save(buffer);
        StateChunk chunk2 = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 1);
        chunk2.load(buffer);
        Buffer buffer2 = factory.newBuffer();
        chunk2.save(buffer2);

        Assert.assertTrue(compareBuffers(buffer, buffer2));

        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(chunk.getAt(0), chunk2.getAt(0));
        }

        //init chunk selectWith arrays
        ((LongArray) chunk.getOrCreateAt(5, Type.LONG_ARRAY)).initWith(new long[]{0, 1, 2, 3, 4});
        ((DoubleArray) chunk.getOrCreateAt(6, Type.DOUBLE_ARRAY)).initWith(new double[]{0.1, 1.1, 2.1, 3.1, 4.1});
        ((IntArray) chunk.getOrCreateAt(7, Type.INT_ARRAY)).initWith(new int[]{0, 1, 2, 3, 4});

        buffer.free();
        buffer = factory.newBuffer();

        chunk.save(buffer);
        space.free(chunk2);
        chunk2 = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 2);
        chunk2.load(buffer);

        buffer2.free();
        buffer2 = factory.newBuffer();
        chunk2.save(buffer2);

        Assert.assertTrue(compareBuffers(buffer, buffer2));

        //init chunk selectWith some maps

        LongLongMap long2longMap = (LongLongMap) chunk.getOrCreateAt(8, Type.LONG_TO_LONG_MAP);
        long2longMap.put(1, 1);
        long2longMap.put(Constants.END_OF_TIME, Constants.END_OF_TIME);
        long2longMap.put(Constants.BEGINNING_OF_TIME, Constants.BEGINNING_OF_TIME);

        StringIntMap string2longMap = (StringIntMap) chunk.getOrCreateAt(9, Type.STRING_TO_INT_MAP);
        string2longMap.put("1", 1);
        string2longMap.put(Constants.END_OF_TIME + "", 1000000);
        string2longMap.put(Constants.BEGINNING_OF_TIME + "", -1000000);

        LongLongArrayMap long2longArrayMap = (LongLongArrayMap) chunk.getOrCreateAt(10, Type.LONG_TO_LONG_ARRAY_MAP);
        long2longArrayMap.put(1, 1);
        long2longArrayMap.put(Constants.END_OF_TIME, Constants.END_OF_TIME);
        long2longArrayMap.put(Constants.BEGINNING_OF_TIME, Constants.BEGINNING_OF_TIME);
        long2longArrayMap.put(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME);

        buffer.free();
        buffer = factory.newBuffer();
        chunk.save(buffer);
        space.free(chunk2);
        chunk2 = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 3);
        chunk2.load(buffer);

        buffer2.free();
        buffer2 = factory.newBuffer();
        chunk2.save(buffer2);

        Assert.assertEquals(((LongLongMap) chunk2.getAt(8)).size(), 3);
        Assert.assertEquals(((StringIntMap) chunk2.getAt(9)).size(), 3);
        Assert.assertEquals(((LongLongArrayMap) chunk2.getAt(10)).size(), 4);

        Assert.assertTrue(compareBuffers(buffer, buffer2));
        // Assert.assertTrue(1 == nbCount);

        //force reHash
        for (int i = 0; i < 10; i++) {
            chunk.setAt(1000 + i, Type.INT, i);
        }
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(chunk.getAt(1000 + i), i);
        }

        StateChunk chunk3 = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 4);
        chunk3.loadFrom(chunk);
        Buffer buffer3 = factory.newBuffer();
        chunk3.save(buffer3);

        buffer.free();
        buffer = factory.newBuffer();
        chunk.save(buffer);

        Assert.assertTrue(compareBuffers(buffer, buffer3));

        buffer3.free();
        buffer2.free();
        buffer.free();

        space.free(chunk3);
        space.free(chunk2);
        space.free(chunk);

        //create an empty
        StateChunk chunk4 = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 5);
        ((LongArray) chunk4.getOrCreateAt(0, Type.LONG_ARRAY)).initWith(new long[0]);
        Buffer saved4 = factory.newBuffer();
        chunk4.save(saved4);

        StateChunk chunk5 = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 6);
        chunk5.load(saved4);
        Assert.assertEquals(((LongArray) chunk5.getAt(0)).size(), 0);
        space.free(chunk5);
        space.free(chunk4);
        saved4.free();
        space.freeAll();
    }

    @Test
    public void cloneTest() {
        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);

        //init primitives
        chunk.setAt(0, Type.BOOL, true);
        chunk.setAt(1, Type.STRING, "hello");
        chunk.setAt(2, Type.LONG, 1000l);
        chunk.setAt(3, Type.INT, 100);
        chunk.setAt(4, Type.DOUBLE, 1.0);
        //init arrays
        ((DoubleArray) chunk.getOrCreateAt(5, Type.DOUBLE_ARRAY)).initWith(new double[]{1.0, 2.0, 3.0});
        ((LongArray) chunk.getOrCreateAt(6, Type.LONG_ARRAY)).initWith(new long[]{1, 2, 3});
        ((IntArray) chunk.getOrCreateAt(7, Type.INT_ARRAY)).initWith(new int[]{1, 2, 3});
        //init maps
        ((LongLongMap) chunk.getOrCreateAt(8, Type.LONG_TO_LONG_MAP)).put(100, 100);
        ((LongLongArrayMap) chunk.getOrCreateAt(9, Type.LONG_TO_LONG_ARRAY_MAP)).put(100, 100);
        ((StringIntMap) chunk.getOrCreateAt(10, Type.STRING_TO_INT_MAP)).put("100", 100);

        //clone the chunk
        StateChunk chunk2 = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 1);
        chunk2.loadFrom(chunk);
        //test primitives
        Assert.assertTrue(chunk2.typeAt(0) == Type.BOOL);
        Assert.assertTrue((Boolean) chunk.getAt(0));

        Assert.assertTrue(chunk2.typeAt(1) == Type.STRING);
        Assert.assertTrue(HashHelper.equals(chunk2.getAt(1).toString(), "hello"));

        Assert.assertTrue(chunk2.typeAt(2) == Type.LONG);
        Assert.assertTrue((Long) chunk2.getAt(2) == 1000l);

        Assert.assertTrue(chunk2.typeAt(3) == Type.INT);
        Assert.assertTrue((Integer) chunk2.getAt(3) == 100);

        Assert.assertTrue(chunk2.typeAt(4) == Type.DOUBLE);
        Assert.assertTrue((Double) chunk2.getAt(4) == 1.0);

        //test arrays
        Assert.assertTrue(chunk2.typeAt(5) == Type.DOUBLE_ARRAY);
        Assert.assertTrue(((DoubleArray) chunk2.getAt(5)).get(0) == 1.0);

        Assert.assertTrue(chunk2.typeAt(6) == Type.LONG_ARRAY);
        Assert.assertTrue(((LongArray) chunk2.getAt(6)).get(0) == 1);

        Assert.assertTrue(chunk2.typeAt(7) == Type.INT_ARRAY);
        Assert.assertTrue(((IntArray) chunk2.getAt(7)).get(0) == 1);

        //test maps
        Assert.assertEquals(((LongLongMap) chunk2.getAt(8)).get(100), 100);
        Assert.assertEquals(((LongLongArrayMap) chunk2.getAt(9)).get(100)[0], 100);
        Assert.assertEquals(((StringIntMap) chunk2.getAt(10)).getValue("100"), 100);

        //now we test the co-evolution of clone

        //STRINGS
        chunk.setAt(1, Type.STRING, "helloPast");
        Assert.assertTrue(HashHelper.equals(chunk.getAt(1).toString(), "helloPast"));
        Assert.assertTrue(HashHelper.equals(chunk2.getAt(1).toString(), "hello"));

        chunk2.setAt(1, Type.STRING, "helloFuture");
        Assert.assertTrue(HashHelper.equals(chunk2.getAt(1).toString(), "helloFuture"));
        Assert.assertTrue(HashHelper.equals(chunk.getAt(1).toString(), "helloPast"));

        //ARRAYS
        ((DoubleArray) chunk2.getOrCreateAt(5, Type.DOUBLE_ARRAY)).initWith(new double[]{3.0, 4.0, 5.0});
        Assert.assertTrue(((DoubleArray) chunk2.getAt(5)).get(0) == 3.0);
        Assert.assertTrue(((DoubleArray) chunk.getAt(5)).get(0) == 1.0);

        ((LongArray) chunk2.getOrCreateAt(6, Type.LONG_ARRAY)).initWith(new long[]{100, 200, 300});
        Assert.assertTrue(((LongArray) chunk2.getAt(6)).get(0) == 100);
        Assert.assertTrue(((LongArray) chunk.getAt(6)).get(0) == 1);

        ((IntArray) chunk2.getOrCreateAt(7, Type.INT_ARRAY)).initWith(new int[]{100, 200, 300});
        Assert.assertTrue(((IntArray) chunk2.getAt(7)).get(0) == 100);
        Assert.assertTrue(((IntArray) chunk.getAt(7)).get(0) == 1);

        //MAPS

        ((LongLongMap) chunk2.getAt(8)).put(100, 200);
        Assert.assertTrue(((LongLongMap) chunk2.getAt(8)).get(100) == 200);
        Assert.assertTrue(((LongLongMap) chunk.getAt(8)).get(100) == 100);

        ((LongLongArrayMap) chunk2.getAt(9)).put(100, 200);
        Assert.assertTrue(((LongLongArrayMap) chunk2.getAt(9)).get(100)[0] == 200);
        Assert.assertTrue(((LongLongArrayMap) chunk2.getAt(9)).get(100)[1] == 100);
        Assert.assertTrue(((LongLongArrayMap) chunk.getAt(9)).get(100)[0] == 100);

        ((StringIntMap) chunk2.getAt(10)).put("100", 200);
        Assert.assertTrue(((StringIntMap) chunk2.getAt(10)).getValue("100") == 200);
        Assert.assertTrue(((StringIntMap) chunk.getAt(10)).getValue("100") == 100);

        // add something new instead of replacing something -> triggers the shallow copy of the clone
        chunk2.setAt(11, Type.STRING, "newString");
        Assert.assertEquals(chunk2.getAt(11), "newString");

        space.free(chunk);
        space.free(chunk2);
        space.freeAll();

    }

    @Test
    public void protectionTest() {

        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);

        //boolean protection test
        //protectionMethod(chunk, Type.BOOL, null, true);
        protectionMethod(chunk, Type.BOOL, true, false);
        protectionMethod(chunk, Type.BOOL, "Hello", true);

        //protectionMethod(chunk, Type.DOUBLE, null, true);
        protectionMethod(chunk, Type.DOUBLE, 0.5d, false);
        protectionMethod(chunk, Type.DOUBLE, "Hello", true);

        //protectionMethod(chunk, Type.LONG, null, true);
        protectionMethod(chunk, Type.LONG, 100000000l, false);
        protectionMethod(chunk, Type.LONG, 100000000, false);
        protectionMethod(chunk, Type.LONG, "Hello", true);

        //protectionMethod(chunk, Type.INT, null, true);
        protectionMethod(chunk, Type.INT, 10, false);
        protectionMethod(chunk, Type.INT, "Hello", true);

        //protectionMethod(chunk, Type.STRING, null, false);
        protectionMethod(chunk, Type.STRING, "Hello", false);
        protectionMethod(chunk, Type.STRING, true, true);

        //arrays
        //protectionMethod(chunk, Type.DOUBLE_ARRAY, new double[]{0.1d, 0.2d, 0.3d}, false);
        protectionMethod(chunk, Type.DOUBLE_ARRAY, "hello", true);

        //protectionMethod(chunk, Type.LONG_ARRAY, new long[]{10l, 100l, 1000l}, false);
        protectionMethod(chunk, Type.LONG_ARRAY, "hello", true);

        //protectionMethod(chunk, Type.INT_ARRAY, new int[]{10, 100, 1000}, false);
        protectionMethod(chunk, Type.INT_ARRAY, "hello", true);

        //maps
        protectionMethod(chunk, Type.STRING_TO_INT_MAP, "hello", true);
        protectionMethod(chunk, Type.LONG_TO_LONG_MAP, "hello", true);
        protectionMethod(chunk, Type.LONG_TO_LONG_ARRAY_MAP, "hello", true);

        space.free(chunk);
        space.freeAll();
    }

    @Test
    public void typeSwitchTest() {
        ChunkSpace space = factory.newSpace(100,-1, null, false);
        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);

        //init primitives
        chunk.setAt(0, Type.BOOL, true);
        chunk.setAt(1, Type.STRING, "hello");
        chunk.setAt(2, Type.LONG, 1000l);
        chunk.setAt(3, Type.INT, 100);
        chunk.setAt(4, Type.DOUBLE, 1.0);
        //init arrays
        ((DoubleArray) chunk.getOrCreateAt(5, Type.DOUBLE_ARRAY)).initWith(new double[]{1.0, 2.0, 3.0});
        ((LongArray) chunk.getOrCreateAt(6, Type.LONG_ARRAY)).initWith(new long[]{1, 2, 3});
        ((IntArray) chunk.getOrCreateAt(7, Type.INT_ARRAY)).initWith(new int[]{1, 2, 3});
        //init maps
        ((LongLongMap) chunk.getOrCreateAt(8, Type.LONG_TO_LONG_MAP)).put(100, 100);
        ((LongLongArrayMap) chunk.getOrCreateAt(9, Type.LONG_TO_LONG_ARRAY_MAP)).put(100, 100);
        ((StringIntMap) chunk.getOrCreateAt(10, Type.STRING_TO_INT_MAP)).put("100", 100);

        //ok now switch all types

        //switch primitives
        chunk.setAt(10, Type.BOOL, true);
        Assert.assertTrue(chunk.typeAt(10) == Type.BOOL);
        Assert.assertTrue((Boolean) chunk.getAt(10));

        chunk.setAt(0, Type.STRING, "hello");
        Assert.assertTrue(chunk.typeAt(0) == Type.STRING);
        Assert.assertTrue(HashHelper.equals(chunk.getAt(0).toString(), "hello"));

        chunk.setAt(1, Type.LONG, 1000l);
        Assert.assertTrue(chunk.typeAt(1) == Type.LONG);
        Assert.assertTrue((Long) chunk.getAt(1) == 1000l);

        chunk.setAt(2, Type.INT, 100);
        Assert.assertTrue(chunk.typeAt(2) == Type.INT);
        Assert.assertTrue((Integer) chunk.getAt(2) == 100);

        chunk.setAt(3, Type.DOUBLE, 1.0);
        Assert.assertTrue(chunk.typeAt(3) == Type.DOUBLE);
        Assert.assertTrue((Double) chunk.getAt(3) == 1.0);

        //switch arrays
        ((DoubleArray) chunk.getOrCreateAt(4, Type.DOUBLE_ARRAY)).initWith(new double[]{1.0, 2.0, 3.0});
        Assert.assertTrue(chunk.typeAt(4) == Type.DOUBLE_ARRAY);
        Assert.assertTrue(((DoubleArray) chunk.getAt(4)).get(0) == 1.0);

        ((LongArray) chunk.getOrCreateAt(5, Type.LONG_ARRAY)).initWith(new long[]{1, 2, 3});
        Assert.assertTrue(chunk.typeAt(5) == Type.LONG_ARRAY);
        Assert.assertTrue(((LongArray) chunk.getAt(5)).get(0) == 1);

        ((IntArray) chunk.getOrCreateAt(6, Type.INT_ARRAY)).initWith(new int[]{1, 2, 3});
        Assert.assertTrue(chunk.typeAt(6) == Type.INT_ARRAY);
        Assert.assertTrue(((IntArray) chunk.getAt(6)).get(0) == 1);

        //switch maps
        ((LongLongMap) chunk.getOrCreateAt(7, Type.LONG_TO_LONG_MAP)).put(100, 100);
        ((LongLongArrayMap) chunk.getOrCreateAt(8, Type.LONG_TO_LONG_ARRAY_MAP)).put(100, 100);
        ((StringIntMap) chunk.getOrCreateAt(9, Type.STRING_TO_INT_MAP)).put("100", 100);

        space.free(chunk);
        space.freeAll();

    }

    private boolean compareBuffers(Buffer buffer, Buffer buffer2) {
        if (buffer.length() != buffer2.length()) {
            return false;
        }
        for (int i = 0; i < buffer.length(); i++) {
            if (buffer.read(i) != buffer2.read(i)) {
                return false;
            }
        }
        return true;
    }

    private void protectionMethod(StateChunk chunk, int elemType, Object elem, boolean shouldCrash) {
        boolean hasCrash = false;
        try {
            chunk.setAt(0, elemType, elem);
        } catch (Throwable e) {
            hasCrash = true;
        }
        Assert.assertTrue(hasCrash == shouldCrash);
    }

}
