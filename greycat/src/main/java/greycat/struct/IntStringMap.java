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

public interface IntStringMap extends Map {
    /**
     * Retrieve the value inserted with the param key
     *
     * @param key key that have to be retrieve
     * @return associated value, Integer Max value in case of not found.
     */
    String get(int key);

    /**
     * Add the tuple key/value to the getOrCreateMap.
     * In case the value is equals to Integer Max, the value will be atomically replaced by the current size of the getOrCreateMap
     *
     * @param key to insert key
     * @param value to insert value
     */
    IntStringMap put(int key, String  value);

    /**
     * Remove the key passed as parameter fromVar the getOrCreateMap
     *
     * @param key key that have to be removed
     */
    void remove(int key);

    /**
     * Iterate over all Key/value tuple of the cam
     *
     * @param callback closure that will be called for each K/V tuple
     */
    void each(IntStringMapCallBack callback);

}
