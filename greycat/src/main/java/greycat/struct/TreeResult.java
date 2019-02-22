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

public interface TreeResult {

    boolean insert(double[] key, long value, double distance);

    double[] keys(int index);

    long value(int index);

    /**
     * Distance of keys result from the requested keys
     *
     * @param index of the result element
     * @return distance of the result keys from the requested keys, in case of QueryArea the distance is measured from the area center
     */
    double distance(int index);

    double getWorstDistance();

    boolean isCapacityReached();

    TreeResult sort(boolean ascending);

    void free();

    int size();

}
