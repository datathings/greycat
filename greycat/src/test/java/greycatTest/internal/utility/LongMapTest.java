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

import greycat.utility.LMap;
import org.junit.Assert;
import org.junit.Test;

public class LongMapTest {

    @Test
    public void test() {
        LMap map = new LMap(false);
        Assert.assertTrue(map.add(1L));
        Assert.assertTrue(map.add(2L));
        Assert.assertTrue(map.add(3L));
        Assert.assertTrue(map.add(4L));
        Assert.assertTrue(map.add(5L));
        Assert.assertEquals(5, map.size());
        Assert.assertFalse(map.add(1L));
        Assert.assertEquals(5, map.size());
        for (int i = 6; i < 100; i++) {
            map.add(i);
        }
        Assert.assertEquals(5 + (100 - 6), map.size());
    }

}
