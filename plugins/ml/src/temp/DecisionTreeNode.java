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

import java.util.ArrayList;
import java.util.List;

public class DecisionTreeNode {
    private static int ELEMENTS_PER_NODE = 5;
    //TODO Add conditions and splits.
    public double boundary; //For splitting the criterion
    public int classNum; //For leaf
    public DecisionTreeNode left; // <boundary
    public DecisionTreeNode right; //>=boundary
    //TODO Unknown? NaN?
    public int featureNum;

    public static DecisionTreeNode deserializeFromDoubleArray(double decisionTreeArray[]) {
        return deserializeFromDoubleArrayWithRoot(decisionTreeArray, 0);
    }

    protected static DecisionTreeNode deserializeFromDoubleArrayWithRoot(double decisionTreeArray[], int rootIndex) {
        DecisionTreeNode dtr = new DecisionTreeNode();
        dtr.boundary = decisionTreeArray[rootIndex * ELEMENTS_PER_NODE];
        dtr.classNum = (int) decisionTreeArray[rootIndex * ELEMENTS_PER_NODE + 1];
        int leftNum = (int) decisionTreeArray[rootIndex * ELEMENTS_PER_NODE + 2];
        //Stringctly > is correct - 0 is root node, loops are not acceptable
        dtr.left = (leftNum > 0) ? deserializeFromDoubleArrayWithRoot(decisionTreeArray, leftNum) : null;
        int rightNum = (int) decisionTreeArray[rootIndex * ELEMENTS_PER_NODE + 3];
        dtr.right = (rightNum > 0) ? deserializeFromDoubleArrayWithRoot(decisionTreeArray, rightNum) : null;
        dtr.featureNum = (int) decisionTreeArray[rootIndex * ELEMENTS_PER_NODE + 4];
        return dtr;
    }

    /**
     * @return Decision tree (with current node as a root) serialized to double array
     */
    public double[] serializeToDoubleArray() {
        List<Double> resultObjects = serializeToDoubleObjectArray();
        double result[] = new double[resultObjects.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = resultObjects.get(i);
        }
        return result;
    }

    private List<Double> serializeToDoubleObjectArray() {
        List<Double> result = new ArrayList<Double>();
        List<DecisionTreeNode> frontier = new ArrayList<DecisionTreeNode>();
        frontier.add(this);
        int currentNumberToAdd = 0;
        while (frontier.size() > 0) {
            DecisionTreeNode currentNode = frontier.get(0);
            frontier.remove(0);
            result.add(currentNode.boundary);
            result.add((double) currentNode.classNum);
            if (currentNode.left != null) {
                currentNumberToAdd++;
                result.add((double) currentNumberToAdd);
                frontier.add(currentNode.left);
            } else {
                result.add(-1.0);
            }
            if (currentNode.right != null) {
                currentNumberToAdd++;
                result.add((double) currentNumberToAdd);
                frontier.add(currentNode.right);
            } else {
                result.add(-1.0);
            }
            result.add((double) currentNode.featureNum);
        }
        return result;
    }

}