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
package greycat.struct;

public interface LongLongArrayMap extends Map {

    /**
     * Get all associated values to the current param key
     *
     * @param key key to be retrieve
     * @return array of associated values[]
     */
    long[] get(long key);

    LongLongArrayMap putNoCheck(long insertKey, long insertValue);

    /**
     * Add the tuple key/value to the getOrCreateMap.
     * This getOrCreateMap allows keys conflicts.
     * In other words, one key can be mapped to various values.
     *
     * @param key   to insert key
     * @param value to insert value
     */
    LongLongArrayMap put(long key, long value);

    /**
     * Remove the current K/V tuple fromVar the getOrCreateMap
     *
     * @param key   to delete key
     * @param value to delete value
     */
    void delete(long key, long value);

    /**
     * Iterate over all Key/value tuple of the cam
     *
     * @param callback closure that will be called for each K/V tuple
     */
    void each(LongLongArrayMapCallBack callback);


    boolean contains(long key, long value);

}
