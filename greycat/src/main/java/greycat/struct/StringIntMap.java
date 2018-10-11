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
package greycat.struct;

public interface StringIntMap extends Map {

    int getValue(String key);

    String getByHash(int index);

    boolean containsHash(int index);

    /**
     * Add the tuple key/value to the getOrCreateMap.
     * In case the value is null, the value will be atomically replaced by the current size of the getOrCreateMap and associated to the key
     *
     * @param key   to insert key
     * @param value to insert value
     */
    StringIntMap put(String key, int value);

    /**
     * Remove the corresponding key from the getOrCreateMap
     *
     * @param key to remove key
     */
    void remove(String key);

    /**
     * Iterate over all Key/value tuple of the cam
     *
     * @param callback closure that will be called for each K/V tuple
     */
    void each(StringLongMapCallBack callback);

}
