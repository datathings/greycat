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
package greycat.internal.custom;

import greycat.Type;
import greycat.base.BaseCustomType;
import greycat.struct.*;
import greycat.utility.distance.Distance;
import greycat.utility.distance.Distances;

public class KDTree extends BaseCustomType implements Tree {

    public static final String NAME = "KDTREE";

    /**
     * public configuration elements
     */
    public static int RESOLUTION = 10;
    public static int DISTANCE = 11;

    /**
     * private keys
     */
    private static int E_SUBTREE_NODES = 0;
    private static int E_KEY = 1;
    private static int E_SUM_KEY = 2;
    private static int E_VALUE = 3;
    private static int E_SUBTREE_VALUES = 4;
    private static int E_RIGHT = 5;
    private static int E_LEFT = 6;
    private static int STRATEGY = 12;
    private static int DIM = 13;

    public KDTree(final EGraph eGraph) {
        super(eGraph);
        if (eGraph.root() == null) {
            ENode root = eGraph.newNode();
            eGraph.setRoot(root);
        }
    }

    private static boolean checkCreateLevels(double[] key1, double[] key2, double[] resolutions) {
        if (resolutions != null) {
            for (int i = 0; i < key2.length; i++) {
                if (Math.abs(key1[i] - key2[i]) > resolutions[i]) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < key2.length; i++) {
                if ((key1[i] != key2[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    // Method rangeSearch translated from 352.range.c of Gonnet & Baeza-Yates
    private static void rangeSearch(final double[] lowk, final double[] uppk, final double[] center, final Distance distance, final ENode node, final int lev, final int dim, final VolatileTreeResult nnl) {

        if (node == null)
            return;

        double[] key = ((DoubleArray) node.getAt(E_KEY)).extract();

        if (lowk[lev] <= key[lev]) {
            rangeSearch(lowk, uppk, center, distance, (ENode) node.getAt(E_LEFT), (lev + 1) % dim, dim, nnl);
        }
        int j;
        j = 0;
        while (j < dim && lowk[j] <= key[j] && uppk[j] >= key[j]) {
            j++;
        }

        if (j == dim) {
            nnl.insert(key, (long) node.getAt(E_VALUE), distance.measure(key, center));
        }
        if (uppk[lev] > key[lev]) {
            rangeSearch(lowk, uppk, center, distance, (ENode) node.getAt(E_RIGHT), (lev + 1) % dim, dim, nnl);
        }
    }

    private static void recursiveTraverse(final ENode node, final VolatileTreeResult nnl, final Distance distance, final double[] target, HRect hr, final int lev, final int dim, double max_dist_sqd, final double radius) {
        // 1. if kd is empty exit.
        if (node == null) {
            return;
        }
        double[] pivot = ((DoubleArray) node.getAt(E_KEY)).extract();
        if (pivot == null) {
            throw new RuntimeException("Key can't be null");
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
        ENode nearer_kd;
        HRect nearer_hr;
        ENode further_kd;
        HRect further_hr;
        // 6. if target-in-left then
        // 6.1. nearer-kd := left field of kd and nearer-hr := left-hr
        // 6.2. further-kd := right field of kd and further-hr := right-hr
        if (target_in_left) {
            nearer_kd = (ENode) node.getAt(E_LEFT);
            nearer_hr = left_hr;
            further_kd = (ENode) node.getAt(E_RIGHT);
            further_hr = right_hr;
        }
        //
        // 7. if not target-in-left then
        // 7.1. nearer-kd := right field of kd and nearer-hr := right-hr
        // 7.2. further-kd := left field of kd and further-hr := left-hr
        else {
            nearer_kd = (ENode) node.getAt(E_RIGHT);
            nearer_hr = right_hr;
            further_kd = (ENode) node.getAt(E_LEFT);
            further_hr = left_hr;
        }
        // 8. Recursively call Nearest Neighbor with paramters
        // (nearer-kd, target, nearer-hr, max-dist-sqd), storing the
        // results in nearest and dist-sqd
        //nnbr(nearer_kd, target, nearer_hr, max_dist_sqd, lev + 1, K, nnl);
        recursiveTraverse(nearer_kd, nnl, distance, target, nearer_hr, lev + 1, dim, max_dist_sqd, radius);
        double dist_sqd;
        if (!nnl.isCapacityReached()) {
            dist_sqd = Double.MAX_VALUE;
        } else {
            dist_sqd = nnl.getWorstDistance();
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
                if (radius > 0) {
                    if (dist_sqd < radius) {
                        nnl.insert(pivot, (Long) node.getAt(E_VALUE), dist_sqd);
                    }
                } else {
                    nnl.insert(pivot, (Long) node.getAt(E_VALUE), dist_sqd);
                }
                // 10.1.3 max-dist-sqd = dist-sqd
                // max_dist_sqd = dist_sqd;
                if (nnl.isCapacityReached()) {
                    max_dist_sqd = nnl.getWorstDistance();
                } else {
                    max_dist_sqd = Double.MAX_VALUE;
                }
            }
            // 10.2 Recursively call Nearest Neighbor with parameters
            // (further-kd, target, further-hr, max-dist_sqd),
            // storing results in temp-nearest and temp-dist-sqd
            //nnbr(further_kd, target, further_hr, max_dist_sqd, lev + 1, K, nnl);
            recursiveTraverse(further_kd, nnl, distance, target, further_hr, lev + 1, dim, max_dist_sqd, radius);
        }
    }

    private static boolean internalInsert(final ENode node, final double[] key, final long value, final int strategyType, final int lev, final double[] resolution) {
        DoubleArray pka = (DoubleArray) node.getAt(E_KEY);
        double[] pKey = null;
        if (pka != null) {
            pKey = pka.extract();
        }
        if (pKey == null) {
            pKey = new double[key.length];
            System.arraycopy(key, 0, pKey, 0, key.length);

            ((DoubleArray) node.getOrCreateAt(E_KEY, Type.DOUBLE_ARRAY)).initWith(pKey);
            node.setAt(E_VALUE, Type.LONG, value);
            if (strategyType == TreeStrategy.PROFILE) {
                node.setAt(E_SUBTREE_VALUES, Type.LONG, value);
                double[] sk = new double[pKey.length];
                for (int i = 0; i < key.length; i++) {
                    sk[i] = pKey[i] * value;
                }
                ((DoubleArray) node.getOrCreateAt(E_SUM_KEY, Type.DOUBLE_ARRAY)).initWith(sk);
            }
            node.setAt(E_SUBTREE_NODES, Type.LONG, 1);
            return true;
        } else if (!checkCreateLevels(key, pKey, resolution)) {
            //Need to replace the key here if indexing
            if (strategyType == TreeStrategy.INDEX) {
                node.setAt(E_VALUE, Type.LONG, value);
            } else if (strategyType == TreeStrategy.PROFILE) {
                //need to update the keys and values of the profiles
                double[] sk = ((DoubleArray) node.getAt(E_SUM_KEY)).extract();
                for (int i = 0; i < pKey.length; i++) {
                    sk[i] = (sk[i] + key[i] * value);
                }
                ((DoubleArray) node.getOrCreateAt(E_SUM_KEY, Type.DOUBLE_ARRAY)).initWith(sk);
                node.setAt(E_VALUE, Type.LONG, (long) node.getOrCreateAt(E_VALUE, Type.LONG) + value);
                node.setAt(E_SUBTREE_VALUES, Type.LONG, (long) node.getOrCreateAt(E_SUBTREE_VALUES, Type.LONG) + value);
            }
            return false;
        } else {
            ENode child;
            if (key[lev] > pKey[lev]) {
                child = (ENode) node.getAt(E_RIGHT);
                if (child == null) {
                    child = createNode(node, true);
                }
            } else {
                child = (ENode) node.getAt(E_LEFT);
                if (child == null) {
                    child = createNode(node, false);
                }
            }
            if (internalInsert(child, key, value, strategyType, (lev + 1) % key.length, resolution)) {
                //update parents recursively
                if (strategyType == TreeStrategy.PROFILE) {
                    node.setAt(E_SUBTREE_VALUES, Type.LONG, (long) node.getAt(E_SUBTREE_VALUES) + value);
                }
                node.setAt(E_SUBTREE_NODES, Type.LONG, (long) node.getAt(E_SUBTREE_NODES) + 1);
                return true;
            } else {
                if (strategyType == TreeStrategy.PROFILE) {
                    node.setAt(E_SUBTREE_VALUES, Type.LONG, (long) node.getAt(E_SUBTREE_VALUES) + value);
                }
                return false;
            }
        }
    }

    private static ENode createNode(final ENode parent, final boolean right) {
        ENode child = parent.egraph().newNode();
        if (right) {
            parent.setAt(E_RIGHT, Type.ENODE, child);
        } else {
            parent.setAt(E_LEFT, Type.ENODE, child);
        }

        return child;
    }

    @Override
    public final void setDistance(final int distanceType) {
        _backend.root().setAt(DISTANCE, Type.INT, distanceType);
    }

    @Override
    public final void setResolution(final double[] resolution) {
        ((DoubleArray) _backend.root().getOrCreateAt(RESOLUTION, Type.DOUBLE_ARRAY)).initWith(resolution);
    }

    @Override
    public final void setMinBound(final double[] min) {
        //Not needed
    }

    @Override
    public final void setMaxBound(final double[] max) {
        //Not needed
    }

    @Override
    public final void insert(final double[] keys, final long value) {
        ENode root = _backend.root();
        int strategy = TreeStrategy.INDEX;
        if (root.getAt(E_KEY) == null) {
            root.setAt(STRATEGY, Type.INT, strategy);
            root.setAt(DIM, Type.INT, keys.length);
        } else {
            if (keys.length != (int) root.getAt(DIM)) {
                throw new RuntimeException("Keys should always be the same length");
            }
        }
        internalInsert(root, keys, value, strategy, 0, ((DoubleArray) root.getAt(RESOLUTION)).extract());
    }

    /*
    @Override
    public final void profile(final double[] keys, final long occurrence) {
        int strategy = TreeStrategy.PROFILE;
        ENode root = eGraph.root();
        if (root.getAt(E_KEY) == null) {
            root.setAt(STRATEGY, Type.INT, strategy);
            root.setAt(DIM, Type.INT, keys.length);
        } else {
            if (keys.length != (int) root.getAt(DIM)) {
                throw new RuntimeException("Keys should always be the same length");
            }
        }
        internalInsert(root, keys, occurrence, strategy, 0, ((DoubleArray)root.getAt(RESOLUTION));
    }*/

    @Override
    public final TreeResult queryAround(final double[] keys, final int max) {
        return queryBoundedRadius(keys, -1, max);
    }

    @Override
    public final TreeResult queryRadius(final double[] keys, final double radius) {
        return queryBoundedRadius(keys, radius, -1);
    }

    @Override
    public final TreeResult queryBoundedRadius(final double[] keys, final double radius, final int max) {
        final ENode root = _backend.root();
        if (root.getAt(E_KEY) == null) {
            return null;
        }
        final Distance distance = Distances.getDistance(root.getAtWithDefault(DISTANCE, Distances.DEFAULT), null);
        if (keys.length != ((DoubleArray) root.getAt(E_KEY)).size()) {
            throw new RuntimeException("Keys are not of the same size");
        }
        EGraph calcZone = _backend.graph().space().newVolatileGraph();
        VolatileTreeResult nnl = new VolatileTreeResult(calcZone.newNode(), max);
        recursiveTraverse(root, nnl, distance, keys, HRect.infiniteHRect(keys.length), 0, keys.length, Double.MAX_VALUE, radius);
        nnl.sort(true);
        return nnl;
    }

    @Override
    public final TreeResult queryArea(double[] min, double[] max) {
        ENode root = _backend.root();
        if (root.getAt(E_KEY) == null) {
            return null;
        }
        final Distance distance = Distances.getDistance(root.getAtWithDefault(DISTANCE, Distances.DEFAULT), null);
        final double[] center = new double[max.length];
        for (int i = 0; i < center.length; i++) {
            center[i] = (min[i] + max[i]) / 2;
        }
        EGraph calcZone = _backend.graph().space().newVolatileGraph();
        VolatileTreeResult nnl = new VolatileTreeResult(calcZone.newNode(), -1);
        rangeSearch(min, max, center, distance, root, 0, min.length, nnl);
        nnl.sort(true);
        return nnl;
    }

    @Override
    public final long size() {
        return (long) _backend.root().getAt(E_SUBTREE_NODES);
    }

    @Override
    public final long treeSize() {
        return (long) _backend.root().getAt(E_SUBTREE_NODES);
    }
}
