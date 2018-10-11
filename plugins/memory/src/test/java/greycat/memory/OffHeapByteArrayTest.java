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
package greycat.memory;


import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import greycat.memory.primary.POffHeapByteArray;

import java.nio.ByteBuffer;

public class OffHeapByteArrayTest {

    private int fromByteArray(byte[] bytes) {
        return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }

    private int[] byteArraytoIntArray(byte[] bytes) {
        int[] toReturn = new int[bytes.length / 4];
        for (int i = 0; i < toReturn.length; i++) {
            byte[] encodedInt = new byte[]{
                    bytes[4 * i + 3],
                    bytes[4 * i + 2],
                    bytes[4 * i + 1],
                    bytes[4 * i]
            };
            toReturn[i] = fromByteArray(encodedInt);
        }
        return toReturn;
    }
    
    private float fromByteArraytoFLoat(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getFloat();
    }

    private float[] byteArraytoFloatArray(byte[] bytes) {
        float[] toReturn = new float[bytes.length / 4];
        for (int i = 0; i < toReturn.length; i++) {
            byte[] encodedInt = new byte[]{
                    bytes[4 * i + 3],
                    bytes[4 * i + 2],
                    bytes[4 * i + 1],
                    bytes[4 * i]
            };
            toReturn[i] = fromByteArraytoFLoat(encodedInt);
        }
        return toReturn;
    }

    @Test
    public void copyIntArrayTest() {
        int[] heapTable = new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE, 3, 4, 5};

        final long offHeapTableAddr = POffHeapByteArray.allocate(heapTable.length * 4); //int on 4 bytes
        POffHeapByteArray.copyArray(heapTable, offHeapTableAddr,0, heapTable.length);

        byte[] data = new byte[heapTable.length * 4];
        for (int i = 0; i < data.length; i++) {
            data[i] = POffHeapByteArray.get(offHeapTableAddr, i);
        }

        int[] res = byteArraytoIntArray(data);
        Assert.assertEquals(heapTable.length, res.length);
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(heapTable[i], res[i]);
        }
        POffHeapByteArray.free(offHeapTableAddr);
    }

    @Test
    public void copyFloatArrayTest() {
        float[] heapTable = new float[]{Float.MAX_VALUE, Float.MIN_VALUE, 3.7f, 4.2f, 5.4f};

        final long offHeapTableAddr = POffHeapByteArray.allocate(heapTable.length * 4); //float on 4 bytes
        POffHeapByteArray.copyArray(heapTable, offHeapTableAddr,0, heapTable.length);

        byte[] data = new byte[heapTable.length * 4];
        for (int i = 0; i < data.length; i++) {
            data[i] = POffHeapByteArray.get(offHeapTableAddr, i);
        }

        float[] res = byteArraytoFloatArray(data);
        Assert.assertEquals(heapTable.length, res.length);
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(heapTable[i], res[i], 0);
        }

        POffHeapByteArray.free(offHeapTableAddr);

    }

    @Test
    public void copyByteArrayTest() {
        byte[] byteTable = new byte[]{127, 127, 8, 9};

        final long offHeapTableAddr = POffHeapByteArray.allocate(byteTable.length); //byte on 1 bytes
        POffHeapByteArray.copyArray(byteTable, offHeapTableAddr,0, byteTable.length);

        byte[] data = new byte[byteTable.length];
        for (int i = 0; i < data.length; i++) {
            data[i] = POffHeapByteArray.get(offHeapTableAddr, i);
        }

        Assert.assertEquals(byteTable.length, data.length);
        for (int i = 0; i < data.length; i++) {
            Assert.assertEquals(byteTable[i], data[i], 0.1);
        }

        POffHeapByteArray.free(offHeapTableAddr);
    }

    @After
    public void tearDown() throws Exception {
        if (OffHeapConstants.DEBUG_MODE) {
            Assert.assertEquals(OffHeapConstants.SEGMENTS.size(),0);
        }
    }

}
