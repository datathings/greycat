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
package greycat.utility.distance;

public interface Distance {

    /**
     * Calculates the distance between two instances.
     *
     * @param x the first instance
     * @param y the second instance
     * @return the distance between the two instances
     */
    double measure(double[] x, double[] y);

    /**
     * Returns whether the first distance, similarity or correlation is better
     * than the second distance, similarity or correlation.
     * <p>
     * Both values should be calculated using the same measure.
     * <p>
     * For similarity measures the higher the similarity the better the measure,
     * for distance measures it is the lower the better and for correlation
     * measure the absolute value must be higher.
     *
     * @param x the first distance, similarity or correlation
     * @param y the second distance, similarity or correlation
     * @return true if the first distance is better than the second, false in
     * other cases.
     */
    boolean compare(double x, double y);

    /**
     * Returns the value that this distance metric produces for the lowest
     * distance or highest similarity. This is mainly useful to initialize
     * variables to be used in comparisons with the compare method of this
     * class.
     *
     * @return minimum possible value of the distance metric
     */
    double getMinValue();

    /**
     * Returns the value that this distance metric produces for the highest
     * distance or lowest similarity. This is
     * mainly useful to initialize variables to be used in comparisons with the
     * compare method of this class.
     *
     * @return maximum possible value of the distance metric
     */
    double getMaxValue();
}
