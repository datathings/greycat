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
#include <jni.h>

static const long PRIME1 = 2654435761L;
static const long PRIME2 = 2246822519L;
static const long PRIME3 = 3266489917L;
static const long PRIME4 = 668265263L;
static const long PRIME5 = 0x165667b1;
static const int len = 24;

static long tripleHash(jbyte p0, long p1, long p2, long p3, long max) {
    if (max <= 0) {
        return -1;
    }
    unsigned long v1 = PRIME5;
    unsigned long v2 = v1 * PRIME2 + len;
    unsigned long v3 = v2 * PRIME3;
    unsigned long v4 = v3 * PRIME4;
    unsigned long crc;
    v1 = ((v1 << 13) | (v1 >> 51)) + p1;
    v2 = ((v2 << 11) | (v2 >> 53)) + p2;
    v3 = ((v3 << 17) | (v3 >> 47)) + p3;
    v4 = ((v4 << 19) | (v4 >> 45)) + p0;

    v1 += ((v1 << 17) | (v1 >> 47));
    v2 += ((v2 << 19) | (v2 >> 45));
    v3 += ((v3 << 13) | (v3 >> 51));
    v4 += ((v4 << 11) | (v4 >> 53));

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

    v1 += ((v1 << 11) | (v1 >> 53));
    v2 += ((v2 << 17) | (v2 >> 47));
    v3 += ((v3 << 19) | (v3 >> 45));
    v4 += ((v4 << 13) | (v4 >> 51));

    v1 *= PRIME3;
    v2 *= PRIME3;
    v3 *= PRIME3;
    v4 *= PRIME3;

    crc = v1 + ((v2 << 3) | (v2 >> 61)) + ((v3 << 6) | (v3 >> 58)) + ((v4 << 9) | (v4 >> 55));
    crc ^= crc >> 11;
    crc += (PRIME4 + len) * PRIME1;
    crc ^= crc >> 15;
    crc *= PRIME2;
    crc ^= crc >> 13;

    /*
    //To check later if we can replace by somthing better
    crc = crc & 0x7FFFFFFFFFFFFFFFL; //convert positive
    crc = crc % max;           // return between 0 and max
    */

   // crc = (crc < 0 ? crc * -1 : crc); // positive
    crc = crc % max;

    return crc;
}

JNIEXPORT jlong JNICALL
Java_org_mwg_NativeHasherHelper_tripleHash(jbyte p0, jlong p1, jlong p2, jlong p3, jlong max){
    return tripleHash(p0,p1,p2,p3,max);
}


JNIEXPORT jlong JNICALL
JavaCritical_org_mwg_NativeHasherHelper_tripleHash(jbyte p0, jlong p1, jlong p2, jlong p3, jlong max){
    return tripleHash(p0,p1,p2,p3,max);
}
