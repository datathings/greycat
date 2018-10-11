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
package org.mwg.mlx.algorithm.clustering;

import org.mwg.Graph;
import org.mwg.plugin.AbstractNode;

import java.util.Arrays;
import java.util.Random;

/**
 * @ignore ts
 */
public class KMeansNode extends AbstractNode {

    public KMeansNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    //TODO That should be configurable
    private static double distance(double[] vector1, double[] vector2) {
        //TODO Skip assertions for now
        double result = 0;
        for (int i = 0; i < vector1.length; i++) {
            result += (vector1[i] - vector2[i]) * (vector1[i] - vector2[i]);
        }
        return result;
    }

    //TODO distanceFromVectorToMatrixRows and closestCentroids can be merged and parallelized by samples
    // Or taken separately and better parallelized (if distance computations are heavy)

    /**
     * @param matrix
     * @param vectorList First index - vector number (i.e. cluster number), second = vector elemebts
     * @return
     */
    private static double[][] distanceFromVectorToMatrixRows(double[][] matrix, double[][] vectorList) {
        //TODO This is blunt implementation. However, it should be very well parallelizeable
        //With CUDA 2D layout we can assign each distance calculation to separate thread
        //TODO Skip assertions for now
        double result[][] = new double[matrix.length][vectorList.length];
        //Parallelizeable on both loops
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < vectorList.length; j++) {
                result[i][j] = distance(matrix[i], vectorList[j]);
            }
        }
        return result;
    }

    /**
     * @param distancesToCentroids First index - sanple, secod - distance to corresponding centroid.
     * @return
     */
    private static int[] closestCentroids(double[][] distancesToCentroids) {
        //TODO Skip checks and assertions
        //TODO - again, it should be very well parallelizeable
        //Effectively, we just run argmin on each row.
        int[] result = new int[distancesToCentroids.length];
        for (int i = 0; i < distancesToCentroids.length; i++) { //Parallelizeable here
            int currentArgMin = 0;
            for (int j = 1; j < distancesToCentroids[i].length; j++) {
                currentArgMin = (distancesToCentroids[i][j] < distancesToCentroids[i][currentArgMin]) ? j : currentArgMin;
            }
            result[i] = currentArgMin;
        }
        return result;
    }

    ;

    /**
     * Initializes centroids between max and min of training set dimension.
     *
     * @param numClusters
     * @param trainingSet
     * @return
     */
    private static double[][] initializeCentroids(int numClusters, double trainingSet[][]) {
        Random rng = new Random();
        //TODO Skip checks for now. Assume training set with at least 1 element.
        double[][] result = new double[numClusters][trainingSet[0].length];
        for (int i = 0; i < trainingSet[0].length; i++) { //Parallelizeable by dimensions
            double dimensionMin = trainingSet[0][i];
            double dimensionMax = trainingSet[0][i];
            for (int j = 1; j < trainingSet.length; j++) {
                dimensionMin = Math.min(dimensionMin, trainingSet[j][i]);
                dimensionMax = Math.max(dimensionMax, trainingSet[j][i]);
            }
            for (int j = 0; j < numClusters; j++) {
                result[j][i] = rng.nextDouble() * (dimensionMax - dimensionMin) + dimensionMin;
            }
        }
        return result;
    }

    private static double[][] recalculateCentroids(double[][] trainingSet, int[] centroidIndices, int k) {
        //TODO Parallelizeable by dimensions
        //TODO skipping tests for now. Assuming at least 1 element in the training set.
        final int dimensions = trainingSet[0].length;
        double result[][] = new double[k][dimensions];
        for (int i = 0; i < dimensions; i++) { //Parallelizeable here
            double dimensionSums[] = new double[k];
            int dimensionCount[] = new int[k];
            for (int j = 0; j < k; j++) {
                dimensionSums[j] = 0;
                dimensionCount[j] = 0;
            }
            for (int j = 0; j < trainingSet.length; j++) {
                dimensionSums[centroidIndices[j]] += trainingSet[j][i];
                dimensionCount[centroidIndices[j]]++;
            }
            for (int j = 0; j < k; j++) {
                //0 count and NaN result? Whatever.
                result[j][i] = dimensionSums[j] / dimensionCount[j];
            }
        }
        return result;
    }

    ;

    //TODO Distance function?

    /**
     * Finds cluster centers using K-means algorithm.
     *
     * @param trainingSet First index - number of training example; second index - dimension within training example ({@code null} disallowed)
     * @param k Number of clusters
     */
    public void learn(double[][] trainingSet, int k) {
        //TODO Skip checks for now. Assume training set with at least 1 element.

        final int dimensions = trainingSet[0].length;

        //Step 1. Randomly intialize cluster centroids.
        double centroids[][] = initializeCentroids(k, trainingSet);

        //Step 2. Repeat until convergence:
        //Step 2.1 Assign each sample to closest centroid.
        //Step 2.2 Recalculate centroids
        boolean converged = false;
        int lastCentroidIndices[] = new int[0]; //Initializing with whatever
        while (!converged) {
            double dist[][] = distanceFromVectorToMatrixRows(trainingSet, centroids);
            int centroidIndices[] = closestCentroids(dist);
            centroids = recalculateCentroids(trainingSet, centroidIndices, k);
            converged = Arrays.equals(centroidIndices, lastCentroidIndices);
            lastCentroidIndices = centroidIndices;
        }

        //TODO Need to output differently:
        System.out.println("Resulting centroid indices:");
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < dimensions; j++) {
                System.out.print(centroids[i][j] + " ");
            }
            System.out.println();
        }

        System.out.println("Cluster assignments: ");
        for (int i = 0; i < trainingSet.length; i++) {
            System.out.print(lastCentroidIndices[i] + " ");
        }
    }

    //TODO To remove it later. Move to tests.
    public static void main(String[] args) {
        KMeansNode kmeansNode = null;// new KMeansNode();

        final double sampleDataset[][] = new double[][]{
                {10.2955108, 9.7138184},
                {10.0791530, 10.3475678},
                {10.0034623, 10.1901753},
                {10.0125541, 9.8906956},
                {9.8966966, 10.0363040},
                {10.0364095, 10.2188401},
                {10.0020150, 10.0465306},
                {9.8335771, 10.8155270},
                {8.8843789, 9.5244977},
                {9.5443927, 9.8229781},
                {-0.2584864, -0.5472838},
                {-0.1011018, 0.2019459},
                {-1.0685223, -0.2057401},
                {0.0586610, -0.5130524},
                {0.1035285, 0.6679819},
                {-0.3699899, -0.6521432},
                {-0.3315273, -0.3317106},
                {-0.1973557, -0.1911614},
                {0.6192540, -0.7801700},
                {0.6920306, 0.3447206},
        };

        kmeansNode.learn(sampleDataset, 2);
    }

}
