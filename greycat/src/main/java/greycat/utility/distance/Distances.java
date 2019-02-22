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
package greycat.utility.distance;

public class Distances {


    public static final int EUCLIDEAN = 0;
    public static final int GEODISTANCE = 1;
    public static final int COSINE = 2;
    public static final int PEARSON = 3;
    public static final int GAUSSIAN = 4;

    public static final int DEFAULT = EUCLIDEAN;

    public static Distance getDistance(int distance, double[] distancearg) {
        switch (distance) {
            case EUCLIDEAN:
                return EuclideanDistance.instance();
            case GEODISTANCE:
                return GeoDistance.instance();
            case COSINE:
                return CosineDistance.instance();
            case PEARSON:
                return PearsonDistance.instance();
            case GAUSSIAN:
                return new GaussianDistance(distancearg);
        }
        return getDistance(DEFAULT,null);
    }

}
