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

public interface Tree {

    /**
     * Configure the function to compute the distance between two multi-dimensional keys
     *
     * @param distanceType integer value from the Dimensions static object
     */
    void setDistance(final int distanceType);

    /**
     * Configure the minimum resolution for each dimensions of keys.
     *
     * @param resolution vector of double which should have exactly the same size than the previous inserted key
     */
    void setResolution(final double[] resolution);

    /**
     * Configure the minimum value for each dimensions of keys.
     *
     * @param min vector of double which should have exactly the same size than the previous inserted key
     */
    void setMinBound(final double[] min);

    /**
     * Configure the maximum value for each dimensions of keys.
     *
     * @param max vector of double which should have exactly the same size than the previous inserted key
     */
    void setMaxBound(final double[] max);

    /**
     * Insert a value in the tree referenced by keys composed by the vector
     *
     * @param keys  vector of double composing the key
     * @param value value to be inserted (can be a node id to create a reference)
     */
    void insert(final double[] keys, final long value);

    /**
     * Query the tree to collect value around (neighborhood) the keys passed as parameter. The result will be restricted to the max value passed as parameter too.
     *
     * @param keys vector of double composing the key
     * @param max  define the maximum element to collect elements within the tree
     * @return treeResult object which has to be freed after usage
     */
    TreeResult queryAround(final double[] keys, final int max);

    /**
     * Query the tree to collect value around (neighborhood) the keys passed as parameter. The result will be restricted to the radius (distance) passed as parameter.
     *
     * @param keys   vector of double composing the key
     * @param radius define the maximum distance to collect elements within the tree
     * @return treeResult object which has to be freed after usage
     */
    TreeResult queryRadius(final double[] keys, final double radius);

    /**
     * Query the tree to collect value around (neighborhood) the keys passed as parameter. The result will be restricted to the radius (distance) and the max passed as parameters.
     *
     * @param keys   vector of double composing the key
     * @param radius define the maximum distance to collect elements within the tree
     * @param max    define the maximum element to collect elements within the tree
     * @return treeResult object which has to be freed after usage
     */
    TreeResult queryBoundedRadius(final double[] keys, final double radius, final int max);

    /**
     * Query the tree to collect value within an area define by parameter (min and max for each dimension)
     *
     * @param min vector defining the min for each dimensions of keys
     * @param max vector defining the max for each dimensions of keys
     * @return treeResult object which has to be freed after usage
     */
    TreeResult queryArea(final double[] min, final double[] max);

    /**
     * Get the number of keys
     *
     * @return the number of keys encoded in the tree
     */
    long size();

    /**
     * Get the size of the tree
     *
     * @return the number of nodes within the tree
     */
    long treeSize();

}
