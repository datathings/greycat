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
package greycat.utility;

import greycat.Constants;
import greycat.struct.Buffer;

public class HashHelper {

    private static final long PRIME1 = 2654435761L;

    private static final long PRIME2 = 2246822519L;

    private static final long PRIME3 = 3266489917L;

    private static final long PRIME4 = 668265263L;

    private static final long PRIME5 = 0x165667b1;

    private static final int len = 24;

    public static long longHash(long number, long max) {
        long hash = number % max;
        return hash < 0 ? hash * -1 : hash;

        /*
        if (max <= 0) {
            throw new IllegalArgumentException("Max must be > 0");
        }
        long crc = PRIME5;
        crc += number;
        crc += crc << 17;
        crc *= PRIME4;
        crc *= PRIME1;
        crc += number;
        crc += crc << 17;
        crc *= PRIME4;
        crc *= PRIME1;
        crc += len;
        crc ^= crc >>> 15;
        crc *= PRIME2;
        crc += number;
        crc ^= crc >>> 13;
        crc *= PRIME3;
        crc ^= crc >>> 16;
        */

        /*
        //To check later if we can replace by somthing better
        crc = crc & 0x7FFFFFFFFFFFFFFFL; //convert positive
        crc = crc % max;           // return between 0 and max
        */

        /*
        crc = (crc < 0 ? crc * -1 : crc); // positive
        crc = crc % max;
        return crc;
        */
    }

    public static int intHash(int number, int max) {
        int hash = number % max;
        return hash < 0 ? hash * -1 : hash;
    }

    public static long simpleTripleHash(byte p0, long p1, long p2, long p3, long max) {
        long hash = (((long) p0) ^ p1 ^ p2 ^ p3) % max;
        if (hash < 0) {
            hash = hash * -1;
        }
        return hash;
    }

    public static long tripleHash(byte p0, long p1, long p2, long p3, long max) {
        if (max <= 0) {
            throw new IllegalArgumentException("Max must be > 0");
        }
        long v1 = PRIME5;
        long v2 = v1 * PRIME2 + len;
        long v3 = v2 * PRIME3;
        long v4 = v3 * PRIME4;
        long crc;
        v1 = ((v1 << 13) | (v1 >>> 51)) + p1;
        v2 = ((v2 << 11) | (v2 >>> 53)) + p2;
        v3 = ((v3 << 17) | (v3 >>> 47)) + p3;
        v4 = ((v4 << 19) | (v4 >>> 45)) + p0;
        v1 += ((v1 << 17) | (v1 >>> 47));
        v2 += ((v2 << 19) | (v2 >>> 45));
        v3 += ((v3 << 13) | (v3 >>> 51));
        v4 += ((v4 << 11) | (v4 >>> 53));
        v1 *= PRIME1;
        v2 *= PRIME1;
        v3 *= PRIME1;
        v4 *= PRIME1;
        v1 += p1;
        v2 += p2;
        v3 += p3;
        v4 += PRIME5;
        v1 *= PRIME2;
        v2 *= PRIME2;
        v3 *= PRIME2;
        v4 *= PRIME2;
        v1 += ((v1 << 11) | (v1 >>> 53));
        v2 += ((v2 << 17) | (v2 >>> 47));
        v3 += ((v3 << 19) | (v3 >>> 45));
        v4 += ((v4 << 13) | (v4 >>> 51));
        v1 *= PRIME3;
        v2 *= PRIME3;
        v3 *= PRIME3;
        v4 *= PRIME3;
        crc = v1 + ((v2 << 3) | (v2 >>> 61)) + ((v3 << 6) | (v3 >>> 58)) + ((v4 << 9) | (v4 >>> 55));
        crc ^= crc >>> 11;
        crc += (PRIME4 + len) * PRIME1;
        crc ^= crc >>> 15;
        crc *= PRIME2;
        crc ^= crc >>> 13;
        crc = (crc < 0 ? crc * -1 : crc); // positive
        crc = crc % max;
        return crc;
    }


    /**
     * Returns a random long number
     * @return a random number
     */
    /**
     * {@native ts
     * return Math.random() * 1000000
     * }
     */
    public static long rand() {
        return (long) (Math.random() * Constants.END_OF_TIME);
    }

    /**
     * Tests equality between two elements
     * @param src       The first element
     * @param other     The second element
     * @return True if equals, false otherwise
     * @deprecated
     * @see Constants#equals(String, String)
     */
    /**
     * {@native ts
     * return src === other
     * }
     */
    public static boolean equals(String src, String other) {
        return src.equals(other);
    }

    /**
     * Returns the minimum double value
     * @return the minimum double value
     */
    /**
     * {@native ts
     * return Number.MIN_VALUE;
     * }
     */
    public static double DOUBLE_MIN_VALUE() {
        return Double.MIN_VALUE;
    }

    /**
     * Returns the maximum double value
     * @return the maximum double value
     */
    /**
     * {@native ts
     * return Number.MAX_VALUE;
     * }
     */
    public static double DOUBLE_MAX_VALUE() {
        return Double.MAX_VALUE;
    }

    /**
     * Checks if an object is defined
     * @param param The element to check
     * @return True if defined, null otherwise.
     * @deprecated
     * @see Constants#isDefined(Object)
     */
    /**
     * {@native ts
     * return param != undefined && param != null;
     * }
     */
    public static boolean isDefined(Object param) {
        return param != null;
    }

    /**
     * {@native ts
     * private static byteTable = function(){
     * var table = [];
     * var h = Long.fromBits(0xCAAF1684, 0x544B2FBA);
     * for (var i = 0; i < 256; i++) {
     * for (var j = 0; j < 31; j++) {
     * h = h.shiftRightUnsigned(7).xor(h);
     * h = h.shiftLeft(11).xor(h);
     * h = h.shiftRightUnsigned(10).xor(h);
     * }
     * table[i] = h.toSigned();
     * }
     * return table;
     * }();
     * }
     */
    private static final long[] byteTable = createLookupTable();

    /**
     * {@native ts
     * private static HSTART : Long = Long.fromBits(0xA205B064, 0xBB40E64D);
     * }
     */
    private static final long HSTART = 0xBB40E64DA205B064L;
    //0xBB40E64DA205B064

    /**
     * {@native ts
     * private static HMULT : Long = Long.fromBits(0xE116586D,0x6A5D39EA);
     * }
     */
    private static final long HMULT = 7664345821815920749L;
    //0x6A5D39EAE116586D


    /**
     * Hashes a String
     * @param data  The string to has
     * @return The hash value
     */
    /**
     * {native ts
     * var h = HashHelper.HSTART;
     * var dataLength = data.length;
     * for (var i = 0; i < dataLength; i++) {
     * h = h.mul(HashHelper.HMULT).xor(HashHelper.byteTable[data.charCodeAt(i) & 0xff]);
     * }
     * return h.mod(greycat.internal.CoreConstants.END_OF_TIME).toNumber();
     * }
     */


    /**
     * {@native ts
     * var hash = 0, i, chr, len;
     * if (data.length === 0) return hash;
     * for (i = 0, len = data.length; i < len; i++) {
     * chr   = data.charCodeAt(i);
     * hash  = ((hash << 5) - hash) + chr;
     * hash |= 0; // Convert to 32bit integer
     * }
     * return hash;
     * }
     */
    public static int hash(String data) {
        /*
        long h = HSTART;
        final long hmult = HMULT;
        final long[] ht = byteTable;
        int dataLength = data.length();
        for (int i = 0; i < dataLength; i++) {
            h = (h * hmult) ^ ht[data.codePointAt(i) & 0xff];
        }
        return h % Constants.END_OF_TIME;
        */
        return data.hashCode();
    }

    /**
     * Hashes a byte array.
     * @param data  The bytes to hash
     * @return The hash value
     */
    /**
     * {@native ts
     * var h = HashHelper.HSTART;
     * var dataLength = data.length;
     * for (var i = 0; i < dataLength; i++) {
     * h = h.mul(HashHelper.HMULT).xor(HashHelper.byteTable[data[i] & 0xff]);
     * }
     * return h.mod(greycat.internal.CoreConstants.END_OF_TIME).toNumber();
     * }
     */
    public static long hashBytes(byte[] data) {
        long h = HSTART;
        final long hmult = HMULT;
        final long[] ht = byteTable;
        int dataLength = data.length;
        for (int i = 0; i < dataLength; i++) {
            h = (h * hmult) ^ ht[data[i] & 0xff];
        }
        return h % Constants.END_OF_TIME;
    }

    /**
     * {@native ts
     * var h = HashHelper.HSTART;
     * for (var i = begin; i < end; i++) {
     * h = h.mul(HashHelper.HMULT).xor(HashHelper.byteTable[data.read(i) & 0xff]);
     * }
     * return h.mod(greycat.internal.CoreConstants.END_OF_TIME).toNumber();
     * }
     */
    public static long hashBuffer(final Buffer data, final long begin, final long end) {
        long h = HSTART;
        final long hmult = HMULT;
        final long[] ht = byteTable;
        for (long i = begin; i < end; i++) {
            h = (h * hmult) ^ ht[data.read(i) & 0xff];
        }
        return h % Constants.END_OF_TIME;
    }


    /**
     * @ignore ts
     */
    private static final long[] createLookupTable() {
        long[] byteTable = new long[256];
        long h = 0x544B2FBACAAF1684L;
        for (int i = 0; i < 256; i++) {
            for (int j = 0; j < 31; j++) {
                h = (h >>> 7) ^ h;
                h = (h << 11) ^ h;
                h = (h >>> 10) ^ h;
            }
            byteTable[i] = h;
        }
        return byteTable;
    }


}
