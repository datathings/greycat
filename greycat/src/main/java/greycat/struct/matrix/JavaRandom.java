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
package greycat.struct.matrix;

import java.util.Random;

public class JavaRandom implements RandomInterface {

    private Random random = new Random();

    public final double nextDouble() {
        return random.nextDouble();
    }

    public final double nextGaussian() {
        return random.nextGaussian();
    }

    public final void setSeed(long seed) {
        random.setSeed(seed);
    }

    @Override
    public int nextIntBetween(int origin, int bound) {
        if (origin >= bound)
            throw new IllegalArgumentException("origin must be smaller than bound");

        return random.nextInt(bound - origin) + origin;

    }

}
