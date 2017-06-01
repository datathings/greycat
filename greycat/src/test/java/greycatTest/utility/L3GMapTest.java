/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
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
package greycatTest.utility;

import greycat.utility.L3GMap;
import org.junit.Test;

/**
 * Created by Gregory NAIN on 31/05/2017.
 */
public class L3GMapTest {

    @Test
    public void insetTest() {
        L3GMap map = new L3GMap(true);
        map.put(0, -9007199254740990L, 6047313952769L, "");
    }


}
