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
package greycat.ml.common;

/**
 * @ignore ts
 */
public class NDimentionalArray {

    private static int SWITCH = 100000;
    private double[] _data;
    private int[] dimensions;
    private long totalArray;
    private double[] min;
    private double[] max;
    private double[] precisions;
    private double totalProba;

    public NDimentionalArray(double[] min, double[] max, double[] precisions) {
        totalProba = 0;
        totalArray = 1;
        dimensions = new int[min.length];
        for (int i = 0; i < min.length; i++) {
            dimensions[i] = (int) Math.round((max[i] - min[i]) / (precisions[i])) + 1;
            totalArray = totalArray * dimensions[i];
        }
        if (totalArray < SWITCH) {
            int total = (int) totalArray;
            _data = new double[total];
        } else {
            //create hashmap
            throw new RuntimeException("Not implemented yet");
        }
        this.min = min;
        this.max = max;
        this.precisions = precisions;
    }

    public double get(double[] indices) {
        return _data[convertFlat(indices)];
    }

    //Should be synchronized
    private void set(double[] indices, double value) {
        _data[convertFlat(indices)] = value;
    }

    //Should be synchronized
    private void setFromIndex(int index, double value) {
        _data[index] = value;
    }

    private double getFromIndex(int index) {
        return _data[index];
    }


    public int[] convert(double[] indices) {
        int[] positions = new int[indices.length];
        for (int i = 0; i < min.length; i++) {
            positions[i] = (int) Math.round((indices[i] - min[i]) / (precisions[i]));
        }
        return positions;
    }

    public double[] revertIndex(int[] indices) {
        double[] positions = new double[indices.length];
        for (int i = 0; i < min.length; i++) {
            positions[i] = indices[i] * precisions[i] + min[i];
        }
        return positions;
    }

    public int convertFlat(double[] indices) {
        int position = 0;
        int tempMult = 1;
        int tempCalc = 0;
        for (int i = 0; i < min.length; i++) {
            tempCalc = (int) Math.round((indices[i] - min[i]) / (precisions[i]));
            if (tempCalc >= dimensions[i]) {
                tempCalc = dimensions[i] - 1;
            }
            position = position + tempCalc * tempMult;
            tempMult = tempMult * dimensions[i];
        }
        return position;
    }

    public double[] revertFlatIndex(int index) {
        int[] tempindex = new int[dimensions.length];


        for (int i = 0; i < min.length; i++) {
            tempindex[i] = index % dimensions[i];
            index = (index - tempindex[i]) / dimensions[i];
        }
        return revertIndex(tempindex);
    }


    public int[] getDimensions() {
        return dimensions;
    }

    public long getTotalDimension() {
        return totalArray;
    }

    //Need to be synchronized
    public void add(double[] indices, double value) {
        totalProba += value;
        int flatIndex = convertFlat(indices);
        setFromIndex(flatIndex, getFromIndex(flatIndex) + value);
    }

    public void normalize() {
        if (totalProba != 0) {
            for (int i = 0; i < _data.length; i++) {
                _data[i] = _data[i] / totalProba;
            }
        }
        totalProba = 1.0;
    }

    public double getBestPrediction(int index) {
        double sumXproba = 0;
        for (int i = 0; i < _data.length; i++) {
            if (_data[i] == 0) {
                continue;
            } else {
                double[] revert = revertFlatIndex(i);
                sumXproba += revert[index] * _data[i];
            }
        }
        return sumXproba;
    }
}
