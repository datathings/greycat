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

import greycat.internal.CoreConstants;
import greycat.utility.HashHelper;
import org.junit.Assert;
import org.junit.Test;

public class HashHelperTest {

    // Integer.MIN_VALUE == 0x80000000
    public static final int MIN_INT = -2147483648;

    // Integer.MAX_VALUE == 0x7FFFFFFF
    public static final int MAX_INT = 2147483647;

    /* MAX TESTS */

    @Test
    public void stringHash_0Test() {
        int hash = HashHelper.hash("helloMWG");
        //System.out.println("stringHash_0Test: " + hash);
        Assert.assertTrue(hash == -792688181L);
    }

    @Test
    public void stringHash_1Test() {
        int hash = HashHelper.hash("aVeryLongStringThatCanGoOverTheIntegerLimitAfterAHash");
        //System.out.println("stringHash_0Test: " + hash);
        Assert.assertTrue(hash == -302989728);
    }

    //@Test
    public void longHash_0Test() {
        try {
            HashHelper.longHash(1, 0);
            Assert.fail("This should have thrown an exception");
        } catch (Exception e) {
        }
    }

    //@Test
    public void longHash_1Test() {
        try {
            HashHelper.longHash(1, CoreConstants.BEGINNING_OF_TIME);
            Assert.fail("This should have thrown an exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void tripleHash_0Test() {
        try {
            HashHelper.tripleHash((byte) 1, 2, 3, 4, 0);
            Assert.fail("This should have thrown an exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void tripleHash_1Test() {
        try {
            HashHelper.tripleHash((byte) 1, 2, 3, 4, CoreConstants.BEGINNING_OF_TIME);
            Assert.fail("This should have thrown an exception");
        } catch (Exception e) {
        }
    }


    /* HASH TESTS */


    @Test
    public void longHash_3Test() {
        long hash = HashHelper.longHash(CoreConstants.END_OF_TIME, CoreConstants.END_OF_TIME);
        //System.out.println("longHash_3Test: " + hash);
        Assert.assertTrue(hash < CoreConstants.END_OF_TIME);
        //Assert.assertTrue(hash == 673163482434621L);
    }


    @Test
    public void longHash_4Test() {
        long hash = HashHelper.longHash(CoreConstants.END_OF_TIME, 10000);
        //System.out.println("longHash_4Test: " + hash);
        Assert.assertTrue(hash < 10000);
        //Assert.assertTrue(hash == 271);
    }

    @Test
    public void longHash_5Test() {
        long hash = HashHelper.longHash(-156487, 10000);
        //System.out.println("longHash_5Test: " + hash);
        Assert.assertTrue(hash < 10000);
        //Assert.assertTrue(hash == 9854);
    }

    @Test
    public void longHash_6Test() {
        long hash = HashHelper.longHash(0, 10000);
        //System.out.println("longHash_6Test: " + hash);
        Assert.assertTrue(hash < 10000);
        //Assert.assertTrue(hash == 8147);
    }


    @Test
    public void tripleHash_3Test() {
        long hash = HashHelper.tripleHash((byte) 1, 1, 2, 3, CoreConstants.END_OF_TIME);
        //System.out.println("tripleHash_3Test: " + hash);
        Assert.assertTrue(hash < CoreConstants.END_OF_TIME);
        //Assert.assertTrue(hash == 6324531823975995L);
    }

    @Test
    public void tripleHash_4Test() {
        long hash = HashHelper.tripleHash((byte) 2, 1, -1, 3, CoreConstants.END_OF_TIME);
        //System.out.println("tripleHash_4Test: " + hash);
        Assert.assertTrue(hash < CoreConstants.END_OF_TIME);
        //Assert.assertTrue(hash == 2261661239301336L);
    }

    @Test
    public void tripleHash_5Test() {
        long hash = HashHelper.tripleHash((byte) 3, 1, 2, 0, CoreConstants.END_OF_TIME);
        //System.out.println("tripleHash_5Test: " + hash);
        Assert.assertTrue(hash < CoreConstants.END_OF_TIME);
        //Assert.assertTrue(hash == 914239194442175L);
    }

    @Test
    public void tripleHash_6Test() {
        long hash = HashHelper.tripleHash((byte) 4, 0, 0, 0, CoreConstants.END_OF_TIME);
        //System.out.println("tripleHash_6Test: " + hash);
        Assert.assertTrue(hash < CoreConstants.END_OF_TIME);
        //Assert.assertTrue(hash == 1254293488547125L);
    }

    @Test
    public void tripleHash_7Test() {
        long hash = HashHelper.tripleHash((byte) 4, -1, -1, -1, 200);
        //System.out.println("tripleHash_7Test: " + hash);
        Assert.assertTrue(hash < 200);
        //Assert.assertTrue(hash == 169);
    }

    @Test
    public void tripleHash_8Test() {
        long hash = HashHelper.tripleHash((byte) 1, 16, 500000, -132654987, 5000);
        //System.out.println("tripleHash_8Test: " + hash);
        Assert.assertTrue(hash < 5000);
        //Assert.assertTrue(hash == 1380);
    }

    /*
    @Test
    public void stringHashPerfTest() {

        final String val = "myAttributeNamett";
        long before = System.currentTimeMillis();
        long hash = 0;

        for (int i = 0; i < 1000000000; i++) {
            hash += val.hashCode();
        }
        System.out.println("Time:" + (System.currentTimeMillis() - before) + " L:" + hash);

        before = System.currentTimeMillis();
        hash = 0;
        for (int i = 0; i < 1000000000; i++) {
            hash += HashHelper.stringHash2(val);
        }
        System.out.println("Time:" + (System.currentTimeMillis() - before) + " L:" + hash);

        before = System.currentTimeMillis();
        hash = 0;
            byte[] toBytes = val.getBytes();
        for (int i = 0; i < 100000000; i++) {
            hash += DataHasher.hash(toBytes);
        }
        System.out.println("Time:" + (System.currentTimeMillis() - before) + " L:" + hash);
    }
*/

/*
    @Test
    public void bench() {

        //System.out.println(HashHelper.tripleHash((byte) 0, 10, 10, 10, 1000000000));
        long before = System.currentTimeMillis();
        long sum = 0;
        for (long i = 0; i < 1000000000; i++) {
            sum += HashHelper.tripleHash((byte) 0, i, i * 2, i * 3, 1000000000);
        }
        long after = System.currentTimeMillis();
        System.out.println(sum+"/"+(after - before) + " ms");
    }
*/

}
