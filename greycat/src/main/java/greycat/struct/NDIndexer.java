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

public interface NDIndexer {
    
    void setDistance(int distanceType);

    void setResolution(double[] resolution);

    void setMinBound(double[] min);

    void setMaxBound(double[] max);

    void setBufferSize(int bufferSize);

    void insert(double[] keys, long value);

    ProfileResult queryAround(double[] keys, int max);

    ProfileResult queryRadius(double[] keys, double radius);

    ProfileResult queryBoundedRadius(double[] keys, double radius, int max);

    ProfileResult queryArea(double[] min, double[] max);

    long size();

    long treeSize();
}
