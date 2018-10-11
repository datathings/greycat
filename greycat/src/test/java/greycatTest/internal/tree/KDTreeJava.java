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
package greycatTest.internal.tree;

import greycat.Callback;
import greycat.internal.custom.HRect;
import greycat.utility.distance.Distance;

public class KDTreeJava {

    KDTreeJava right;
    KDTreeJava left;
    double[] key;
    Object value;
    private int _id;
    private static int idgen = 0;

    public void setId() {
        this._id = idgen;
        idgen++;
    }

    public KDTreeJava() {
        setId();
    }

    public int getNum() {
        return num;
    }

    private int num;

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    double threshold;
    Distance distance;

    public void setDistance(Distance d) {
        this.distance = d;
    }

    private Distance getDistance() {
        return distance;
    }

    public void insert(final double[] key, final Object value, final Callback<Boolean> callback) {
        final int dim = key.length;

        if (key.length != dim) {
            throw new RuntimeException("Key size should always be the same");
        }

        Distance distance = getDistance();
        internalInsert(this, this, distance, key, 0, dim, threshold, value, callback);
    }

    public void nearest(final double[] key, final Callback<Object> callback) {

        // initial call is with infinite hyper-rectangle and max distance
        HRect hr = HRect.infiniteHRect(key.length);
        double max_dist_sqd = Double.MAX_VALUE;

        NNL2 nnl = new NNL2(1);
        Distance distance = getDistance();
        internalNearest(this, distance, key, hr, max_dist_sqd, 0, key.length, threshold, nnl);

        Object res = nnl.getHighest();
        callback.on(res);
    }

    public void nearestWithinDistance(final double[] key, final Callback<Object> callback) {

        // initial call is with infinite hyper-rectangle and max distance
        HRect hr = HRect.infiniteHRect(key.length);
        double max_dist_sqd = Double.MAX_VALUE;

        NNL2 nnl = new NNL2(1);
        Distance distance = getDistance();
        internalNearest(this, distance, key, hr, max_dist_sqd, 0, key.length, threshold, nnl);

        if (nnl.getBestDistance() <= threshold) {
            Object res = nnl.getHighest();
            callback.on(res);
        } else {
            callback.on(null);
        }
    }


    public void nearestN(final double[] key, final int n, final Callback<Object[]> callback) {

        HRect hr = HRect.infiniteHRect(key.length);
        double max_dist_sqd = Double.MAX_VALUE;

        NNL2 nnl = new NNL2(n);
        Distance distance = getDistance();
        internalNearest(this, distance, key, hr, max_dist_sqd, 0, key.length, threshold, nnl);

        Object[] res = nnl.getAllNodes();
        callback.on(res);


    }


    private static void internalNearest(KDTreeJava node, final Distance distance, double[] target, HRect hr, double max_dist_sqd, int lev, int dim, double err, NNL2 nnl) {
        // 1. if kd is empty exit.
        if (node == null) {
            return;
        }

//        System.out.println("J1 "+node.id()+ " lev "+ lev);

        double[] pivot = node.key;
        if (pivot == null) {
            return;
        }


        // 2. s := split field of kd
        int s = lev % dim;

        // 3. pivot := dom-elt field of kd

        double pivot_to_target = distance.measure(pivot, target);

        // 4. Cut hr into to sub-hyperrectangles left-hr and right-hr.
        // The cut plane is through pivot and perpendicular to the s
        // dimension.
        HRect left_hr = hr; // optimize by not cloning
        HRect right_hr = (HRect) hr.clone();
        left_hr.max[s] = pivot[s];
        right_hr.min[s] = pivot[s];

        // 5. target-in-left := target_s <= pivot_s
        boolean target_in_left = target[s] < pivot[s];

        KDTreeJava nearer_kd;
        HRect nearer_hr;
        KDTreeJava further_kd;
        HRect further_hr;

        // 6. if target-in-left then
        // 6.1. nearer-kd := left field of kd and nearer-hr := left-hr
        // 6.2. further-kd := right field of kd and further-hr := right-hr
        if (target_in_left) {
            nearer_kd = node.left;
            nearer_hr = left_hr;
            further_kd = node.right;
            further_hr = right_hr;
        }
        //
        // 7. if not target-in-left then
        // 7.1. nearer-kd := right field of kd and nearer-hr := right-hr
        // 7.2. further-kd := left field of kd and further-hr := left-hr
        else {
            nearer_kd = node.right;
            nearer_hr = right_hr;
            further_kd = node.left;
            further_hr = left_hr;
        }

        // 8. Recursively call Nearest Neighbor with paramters
        // (nearer-kd, target, nearer-hr, max-dist-sqd), storing the
        // results in nearest and dist-sqd
        //nnbr(nearer_kd, target, nearer_hr, max_dist_sqd, lev + 1, K, nnl);
        internalNearest(nearer_kd, distance, target, nearer_hr, max_dist_sqd, lev + 1, dim, err, nnl);

//        System.out.println("J2 "+node.id()+ " lev "+ lev);

        double dist_sqd;

        if (!nnl.isCapacityReached()) {
            dist_sqd = Double.MAX_VALUE;
        } else {
            dist_sqd = nnl.getMaxPriority();
        }

        // 9. max-dist-sqd := minimum of max-dist-sqd and dist-sqd
        max_dist_sqd = Math.min(max_dist_sqd, dist_sqd);

        // 10. A nearer point could only lie in further-kd if there were some
        // part of further-hr within distance sqrt(max-dist-sqd) of
        // target. If this is the case then
        double[] closest = further_hr.closest(target);
        if (distance.measure(closest, target) < max_dist_sqd) {

            // 10.1 if (pivot-target)^2 < dist-sqd then
            if (pivot_to_target < dist_sqd) {

                // 10.1.2 dist-sqd = (pivot-target)^2
                dist_sqd = pivot_to_target;
                nnl.insert(node.value, dist_sqd);

                // 10.1.3 max-dist-sqd = dist-sqd
                // max_dist_sqd = dist_sqd;
                if (nnl.isCapacityReached()) {
                    max_dist_sqd = nnl.getMaxPriority();
                } else {
                    max_dist_sqd = Double.MAX_VALUE;
                }
            }

            // 10.2 Recursively call Nearest Neighbor with parameters
            // (further-kd, target, further-hr, max-dist_sqd),
            // storing results in temp-nearest and temp-dist-sqd
            //nnbr(further_kd, target, further_hr, max_dist_sqd, lev + 1, K, nnl);
            internalNearest(further_kd, distance, target, further_hr, max_dist_sqd, lev + 1, dim, err, nnl);
        }


    }

    public int id() {
        return _id;
    }


    private static void internalInsert(final KDTreeJava node, final KDTreeJava root, final Distance distance, final double[] key, final int lev, final int dim, final double err, final Object value, final Callback<Boolean> callback) {

        double[] tk = node.key;
        if (tk == null) {
            node.key = new double[key.length];
            System.arraycopy(key, 0, node.key, 0, key.length);

            node.value = value;

            if (node == root) {
                node.num = 1;
            }

            if (callback != null) {
                callback.on(true);
            }
            return;

        } else if (distance.measure(key, tk) < err) {
            node.value = value;
            if (callback != null) {
                callback.on(true);
            }
            return;
        } else if (key[lev] > tk[lev]) {
            //check right
            if (node.right == null) {
                KDTreeJava rightNode = new KDTreeJava();
                rightNode.key = new double[key.length];
                System.arraycopy(key, 0, rightNode.key, 0, key.length);

                rightNode.value = value;
                node.right = rightNode;
                root.num = root.num + 1;
                if (callback != null) {
                    callback.on(true);
                }
                return;
            } else {
                internalInsert(node.right, root, distance, key, (lev + 1) % dim, dim, err, value, callback);
                return;
            }

        } else {
            if (node.left == null) {
                KDTreeJava leftNode = new KDTreeJava();
                leftNode.key = new double[key.length];
                System.arraycopy(key, 0, leftNode.key, 0, key.length);
                leftNode.value = value;
                node.left = leftNode;
                root.num = root.num + 1;
                if (callback != null) {
                    callback.on(true);
                }
                return;
            } else {
                internalInsert(node.left, root, distance, key, (lev + 1) % dim, dim, err, value, callback);
                return;
            }
        }
    }
}