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
package greycat.struct;

public interface Profile extends Tree {

    void setBufferSize(int bufferSize);

    /**
     * Update profile according to all dimension passed as keys
     *
     * @param keys vector of double composing the key
     */
    void profile(final double[] keys);

    /**
     * Update profile according to all dimension passed as keys and number of occurence (kind of weight of the multi-dimensional keys)
     *
     * @param keys       vector of double composing the key
     * @param occurrence increment to be added to the profile identified the key
     */
    void profileWith(final double[] keys, final long occurrence);

    /**
     * Query the tree to collect value around (neighborhood) the keys passed as parameter. The result will be restricted to the max value passed as parameter too.
     *
     * @param keys vector of double composing the key
     * @param max  define the maximum element to collect elements within the tree
     * @return treeResult object which has to be freed after usage
     */
    ProfileResult queryAround(final double[] keys, final int max);

    /**
     * Query the tree to collect value around (neighborhood) the keys passed as parameter. The result will be restricted to the radius (distance) passed as parameter.
     *
     * @param keys   vector of double composing the key
     * @param radius define the maximum distance to collect elements within the tree
     * @return treeResult object which has to be freed after usage
     */
    ProfileResult queryRadius(final double[] keys, final double radius);

    /**
     * Query the tree to collect value around (neighborhood) the keys passed as parameter. The result will be restricted to the radius (distance) and the max passed as parameters.
     *
     * @param keys   vector of double composing the key
     * @param radius define the maximum distance to collect elements within the tree
     * @param max    define the maximum element to collect elements within the tree
     * @return treeResult object which has to be freed after usage
     */
    ProfileResult queryBoundedRadius(final double[] keys, final double radius, final int max);

    /**
     * Query the tree to collect value within an area define by parameter (min and max for each dimension)
     *
     * @param min vector defining the min for each dimensions of keys
     * @param max vector defining the max for each dimensions of keys
     * @return treeResult object which has to be freed after usage
     */
    ProfileResult queryArea(final double[] min, final double[] max);

}
