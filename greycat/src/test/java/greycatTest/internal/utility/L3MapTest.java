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


import greycat.utility.L3Map;
import org.junit.Assert;
import org.junit.Test;

public class L3MapTest {

    @Test
    public void test() {
        L3Map map = new L3Map(false);
        map.put(1, 1, 1, -1);
        Assert.assertTrue(map.contains(1, 1, 1));
        Assert.assertEquals(1, map.size());

        for (int i = 0; i < 100; i++) {
            map.put(i, i ^ 2, i ^ 3, -1);
        }
        Assert.assertEquals(101, map.size());
        map.put(1, 1, 1, -1);
        Assert.assertEquals(101, map.size());

        for (int i = 0; i < 100; i++) {
            Assert.assertTrue(map.contains(i, i ^ 2, i ^ 3));
        }

        Assert.assertTrue(!map.contains(100,100,100));

    }

}
