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
package greycat.ml;

import greycat.struct.matrix.RandomInterface;

public class CRandom implements RandomInterface {


//    uint32_t gdnn_generator__next_int(gnode_t *node) {
//        uint64_t oldstate = gnode__get_ulong(node, g_state);
//        uint64_t inc = gnode__get_ulong(node, g_inc);
//
//        gnode__set_ulong(node, g_state, oldstate * 6364136223846793005ULL + (inc | 1));
//
//        uint32_t xorshifted = (uint32_t) (((oldstate >> 18u) ^ oldstate) >> 27u);
//        uint32_t rot = (uint32_t) (oldstate >> 59u);
//        return (xorshifted >> rot) | (xorshifted << ((-rot) & 31));
//    }

    private long _seed;
    private double _value = 0;

    /**
     * {@native ts
     * return (Math as any).trunc(Math.random() * 0x7fffffff);
     * }
     */
    public int nextInt() {
        long oldstate = _seed;
        _seed = _seed * 6364136223846793005L + (1);
        int xorshifted = (int) (((oldstate >> 18) ^ oldstate) >> 27);
        int rot = (int) (oldstate >> 59);
        int res = (xorshifted >> rot) | (xorshifted << ((-rot) & 31));
        if (res >= 0) {
            return res;
        } else {
            return -res;
        }
    }

    @Override
    public double nextDouble() {
        int ui32_ran = nextInt();
        return ((double) ui32_ran) / 0x7fffffff;
    }

    @Override
    public double nextGaussian() {
        if (_value != 0) {
            double temp = _value;
            _value = 0;
            return temp;
        } else {
            double v1;
            double v2;
            double s;
            do {
                v1 = 2 * nextDouble() - 1; // between -1 and 1
                v2 = 2 * nextDouble() - 1; // between -1 and 1
                s = v1 * v1 + v2 * v2;
            } while (s >= 1 || s == 0);
            double multiplier = Math.sqrt(-2 * Math.log(s) / s);
            _value = v2 * multiplier;
            return v1 * multiplier;
        }
    }

    public long getSeed() {
        return _seed;
    }

    @Override
    public void setSeed(long seed) {
        this._seed = seed;
        this._value = 0;
    }

    @Override
    public int nextInt(int min, int max) {
        // TODO Assaad
        return 0;
    }

}
