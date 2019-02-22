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

import greycat.utility.HashHelper;
import org.junit.Assert;
import org.junit.Test;
import greycat.internal.CoreConstants;

public class DataHasherTest {

    @Test
    public void dataHash_1Test() {
        long hash = HashHelper.hash(CoreConstants.END_OF_TIME + "");
        //System.out.println("dataHash_1Test: " + hash);
        //64 bits
        //Assert.assertTrue(hash == 1749261374604296L);
        Assert.assertEquals(hash,1160719163);
    }

    @Test
    public void dataHash_2Test() {
        long hash = HashHelper.hash(CoreConstants.BEGINNING_OF_TIME + "");
        //System.out.println("dataHash_2Test: " + hash);
        //64 bits
        //Assert.assertTrue(hash == -7914587012082605L);
        Assert.assertEquals(hash,1930113384);
    }

    @Test
    public void dataHash_3Test() {
        long hash = HashHelper.hash(CoreConstants.BEGINNING_OF_TIME + "");
        long hash2 = HashHelper.hash(CoreConstants.END_OF_TIME + "");
        //System.out.println("dataHash_3Test: " + hash + " -> " + hash2);
        Assert.assertTrue(hash != hash2);
    }

}
