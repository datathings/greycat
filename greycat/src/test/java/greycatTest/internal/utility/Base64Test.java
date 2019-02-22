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
package greycatTest.internal.utility;

import org.junit.Assert;
import org.junit.Test;
import greycat.Constants;
import greycat.internal.heap.HeapMemoryFactory;
import greycat.struct.Buffer;
import greycat.utility.Base64;
import greycat.utility.HashHelper;

public class Base64Test {

    @Test
    public void beginingOfTimeEncodingTest() {
        testLong(Constants.BEGINNING_OF_TIME);
    }

    @Test
    public void typeEncoding() {
        testLong(HashHelper.hash("GaussianGmm"));
    }

    @Test
    public void endOfTimeEncodingTest() {
        testLong(Constants.END_OF_TIME);
    }

    @Test
    public void nullEncodingTest() {
        testLong(Constants.NULL_LONG);
    }

    @Test
    public void zeroEncodingTest() {
        testLong(0l);
    }

    @Test
    public void oneEncodingTest() {
        testLong(1l);
    }

    @Test
    public void randomBigNumTest() {
        testLong(68719476737l);
    }

    @Test
    public void hashTest() {
        testLong(-365393685203911L);
    }

    private void testLong(long val) {
        Buffer buffer = bufferFactory.newBuffer();
        Base64.encodeLongToBuffer(val, buffer);
        long dec = Base64.decodeToLongWithBounds(buffer, 0, buffer.length());
        Assert.assertEquals(val, dec);
    }

    @Test
    public void minIntEncodingTest() {
        testInt(0x80000000);
    }

    @Test
    public void maxIntEncodingTest() {
        testInt(0x7fffffff);
    }

    private void testInt(int val) {
        Buffer buffer = bufferFactory.newBuffer();
        Base64.encodeIntToBuffer(val, buffer);
        int dec = Base64.decodeToIntWithBounds(buffer, 0, buffer.length());
        //System.out.println(val + " -> " + enc + " -> " + dec);
        Assert.assertEquals(val, dec);
        buffer.free();
    }


    /**
     * @native ts
     * this.testDouble(Number.MAX_VALUE);
     */
    @Test
    public void maxDoubleEncodingTest() {
        testDouble(Double.MAX_VALUE);
    }

    /**
     * @native ts
     * this.testDouble(Number.MIN_VALUE);
     */
    @Test
    public void minDoubleEncodingTest() {
        testDouble(HashHelper.DOUBLE_MIN_VALUE());
    }

    /**
     * @native ts
     * this.testDouble(-Number.MAX_VALUE);
     */
    @Test
    public void negMaxDoubleEncodingTest() {
        testDouble(-HashHelper.DOUBLE_MAX_VALUE());
    }

    /**
     * @native ts
     * this.testDouble(-Number.MIN_VALUE);
     */
    @Test
    public void negMinDoubleEncodingTest() {
        testDouble(-HashHelper.DOUBLE_MIN_VALUE());
    }

    @Test
    public void zeroDoubleEncodingTest() {
        testDouble(0);
        testDouble(0.1);
        testDouble(0.25);
        testDouble(0.5);
        testDouble(0.75);
        testDouble(1.1);
        testDouble(2.1);
        testDouble(0.000000000000002);
    }


    private HeapMemoryFactory bufferFactory = new HeapMemoryFactory();

    private void testDouble(double val) {
        Buffer buffer = bufferFactory.newBuffer();
        Base64.encodeDoubleToBuffer(val, buffer);
        double dec = Base64.decodeToDoubleWithBounds(buffer, 0, buffer.length());
        //System.out.println(val + " -> " + enc + " -> " + dec);
        Assert.assertTrue(val == dec);
        buffer.free();

    }


    @Test
    public void boolArrayEncodingTest() {

        for (int i = 0; i < 255; i++) {
            boolean[] tmpArray = new boolean[i];
            for (int j = 0; j < i; j++) {
                tmpArray[j] = Math.random() < 0.5;
            }
            boolArrayInnerTest(tmpArray);
        }
    }

    private void boolArrayInnerTest(boolean[] array) {
        Buffer buffer = bufferFactory.newBuffer();
        Base64.encodeBoolArrayToBuffer(array, buffer);
        boolean[] dec = Base64.decodeToBoolArrayWithBounds(buffer, 0, buffer.length(), array.length);
        //System.out.println(0x7fffffff + " -> " + enc + " -> " + dec);
        Assert.assertTrue(array.length == dec.length);
        for (int i = 0; i < array.length; i++) {
            // Assert.assertEquals("\n" + Arrays.toString(array) + "\n -> "+ enc +" -> \n" + Arrays.toString(dec),array[i], dec[i]);
            Assert.assertEquals(array[i], dec[i]);
        }
    }


}
