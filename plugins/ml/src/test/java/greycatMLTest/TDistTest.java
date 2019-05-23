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
package greycatMLTest;

import greycat.ml.math.TDistribution;
import org.junit.Test;

public class TDistTest {
    @Test
    public void test1(){
        TDistribution tdist = new TDistribution(15);
        double EPS = 0.000001;
        assert(Math.abs(tdist.twoTailsDist(0.1) - 0.921668620070759)<EPS);
        assert(Math.abs(tdist.twoTailsDist(-0.1) - 0.921668620070759)<EPS);
        assert(Math.abs(tdist.twoTailsDist(0.2) - 0.844166776974385)<EPS);
        assert(Math.abs(tdist.twoTailsDist(0.3) - 0.768294534535553)<EPS);
    }
}
