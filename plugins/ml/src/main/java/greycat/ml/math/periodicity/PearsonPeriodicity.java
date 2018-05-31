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
package greycat.ml.math.periodicity;

import greycat.utility.distance.Distance;
import greycat.utility.distance.PearsonDistance;

public class PearsonPeriodicity {


    private static double sumup(double[] dataInput, int estimatedPer, int offset) {
        double sum = 0;
        for (int i = 0; i < dataInput.length; i++) {
            if (i % estimatedPer == offset) {
                sum = sum + dataInput[i];
            }
        }
        return sum;
    }

    private static double getAverage(double[] values) {
        double sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum = sum + values[i];
        }
        return sum / values.length;
    }

    public static int getPeriod(double[] values, int minSearch, int maxSearch) {

        //this value is used to compute the confidence at the end
        double sumPearsonCorr = 0;

        int max = 0;
        double value = -1;
        Distance pearsonDist = PearsonDistance.instance();

        for (int estimPer = minSearch; estimPer <= maxSearch; estimPer++) {
            double[] sumResults = new double[estimPer];

            for (int offSet = 0; offSet <= estimPer - 1; offSet++) {
                double currentSum = sumup(values, estimPer, offSet);
                sumResults[offSet] = currentSum;
            }

            //now we got the sums for one estimated period (this is called component with length sumResults.length()), now compare this componends with parts of the origninal curve

            int numberOfSwaps = values.length / sumResults.length;
            int offset = 0;
            double[] pearson = new double[numberOfSwaps];
            for (int swap = 0; swap < numberOfSwaps; swap++) {

                //now cut parts from the current observation and correlate it with the component
                double[] currentOrigComponent = new double[sumResults.length];
                System.arraycopy(values, offset, currentOrigComponent, 0, sumResults.length);
                offset = offset + sumResults.length;

                pearson[swap] = pearsonDist.measure(currentOrigComponent, sumResults);
            }

            //now all parts of the observation curve have been correlated with the component
            //now we have to get the average of the pearson correlations
            double avgPearson = getAverage(pearson);
            if (avgPearson > value) {
                value = avgPearson;
                max = estimPer;
            }

        }
        return max;
    }

}
