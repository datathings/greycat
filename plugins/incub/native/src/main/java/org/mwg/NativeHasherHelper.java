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
package org.mwg;

public class NativeHasherHelper {

    static native long tripleHash(byte p0, long p1, long p2, long p3, long max);

    static {
        System.load("/Users/duke/dev/mwDB/plugins/native/src/main/resources/natives.dylib");
    }

    public static void main(String[] args) {
        long before = System.currentTimeMillis();
        long sum = 0;
        for (long i = 0; i < 1000000000; i++) {
            sum += tripleHash((byte) 0, i, i * 2, i * 3, 1000000000L);
        }
        long after = System.currentTimeMillis();
        System.out.println(sum + "/" + (after - before) + " ms");
    }

}
