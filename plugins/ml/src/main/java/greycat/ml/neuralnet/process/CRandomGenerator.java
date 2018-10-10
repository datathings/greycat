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
package greycat.ml.neuralnet.process;

import greycat.struct.matrix.RandomInterface;

public class CRandomGenerator implements RandomInterface {


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
    private long _state;
    private int _inc = 1;


    public int nextInt() {
        long oldstate = _state;
        _state = _state * 6364136223846793005L + (_inc | 1);
        int xorshifted = (int) (((oldstate >> 18) ^ oldstate) >> 27);
        int rot = (int) (oldstate >> 59);
        int res = (xorshifted >> rot) | (xorshifted << ((-rot) & 31));
        if(res>=0){
            return res;
        }else {
            return -res;
        }
    }

    @Override
    public double nextDouble() {
        return 0;
    }

    @Override
    public double nextGaussian() {
        return 0;
    }

    @Override
    public void setSeed(long seed) {
        this._seed = seed;
        this._state = seed;
    }

    public void init(long seed, int inc) {
        this._seed = seed;
        this._state = seed;
        this._inc = inc;
    }

}
