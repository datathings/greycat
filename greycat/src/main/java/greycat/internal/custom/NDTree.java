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
import greycat.plugin.NodeStateCallback;
import greycat.struct.*;
import greycat.utility.distance.Distance;
import greycat.utility.distance.Distances;

public class NDTree extends BaseCustomType implements NDIndexer {

    public static final String NAME = "NDTREE";

    //default values:
    public static int BUFFER_SIZE_DEF = 20;

    //Settings on root node:

    private static int BOUND_MIN = 8;
    private static int BOUND_MAX = 9;
    public static int RESOLUTION = 10;
    public static int BUFFER_SIZE = 11;
    public static int DISTANCE = 12;

    //Setttings on each subspace
    private static int E_TOTAL = 0;
    private static int E_SUBNODES = 1;
    private static int E_BUFFER_KEYS = 2;
    private static int E_BUFFER_VALUES = 3;
    private static int E_KEY = 4;
    private static int E_VALUE = 5;
    private static int E_OFFSET_REL = 16;

    private static int MIN = 0;
    private static int MAX = 1;
    private static int CENTER = 2;

    private final NDManager manager;

    public NDTree(final EStructArray eStructArray, final NDManager manager) {
        super(eStructArray);
        this.manager = manager;
        if (eStructArray.root() == null) {
            EStruct root = eStructArray.newEStruct();
            eStructArray.setRoot(root);
        }
    }

    //From a key to insert, and a parent space with min and max boundaries, get the traverse id of the subspace of child where to insert the key
    private static int getRelationId(double[][] space, double[] keyToInsert) {
        int result = 0;
        for (int i = 0; i < space[MIN].length; i++) {
            if (i != 0) {
                result = result << 1;
            }
            if (keyToInsert[i] > space[CENTER][i]) {
                result += 1;
            }
        }
        return result + E_OFFSET_REL;
    }

    //return a boolean whether with the current settings we can create more sublevels or not
    private static boolean checkCreateLevels(double[][] space, double[] resolutions) {
        if (resolutions != null) {
            for (int i = 0; i < space[MIN].length; i++) {
                if ((space[MAX][i] - space[MIN][i]) > 2 * resolutions[i]) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < space[MIN].length; i++) {
                if ((space[MAX][i] > space[MIN][i])) {
                    return true;
                }
            }
        }
        return false;
    }

    private static double[] getCenter(final double[] min, final double[] max) {
        double[] center = new double[min.length];
        for (int i = 0; i < min.length; i++) {
            center[i] = (max[i] + min[i]) / 2;
        }
        return center;
    }

    //From a traverse id, get the directions in the multidimensional subspace
    //this function is somehow the inverse of getRelationId
    private static boolean[] binaryFromLong(long value, int dim) {
        long tempvalue = value - E_OFFSET_REL;
        long shiftvalue = tempvalue >> 1;
        boolean[] res = new boolean[dim];
        for (int i = 0; i < dim; i++) {
            res[dim - i - 1] = ((tempvalue - (shiftvalue << 1)) == 1);
            tempvalue = shiftvalue;
            shiftvalue = tempvalue >> 1;
        }
        return res;
    }


    private static double[][] getChildSpace(double[][] parentSpace, int index) {
        boolean[] binaries = binaryFromLong(index, parentSpace[MIN].length);
        double[][] childSpace = new double[3][parentSpace[MIN].length];

        for (int i = 0; i < parentSpace[MIN].length; i++) {
            if (!binaries[i]) {
                childSpace[MIN][i] = parentSpace[MIN][i];
                childSpace[MAX][i] = parentSpace[CENTER][i];

            } else {
                childSpace[MIN][i] = parentSpace[CENTER][i];
                childSpace[MAX][i] = parentSpace[MAX][i];
            }
            childSpace[CENTER][i] = (childSpace[MIN][i] + childSpace[MAX][i]) / 2;
        }
        return childSpace;
    }


    private static double[][] getChildMinMax(double[][] parentSpace, int index) {
        boolean[] binaries = binaryFromLong(index, parentSpace[MIN].length);
        double[][] childSpace = new double[2][parentSpace[MIN].length];

        for (int i = 0; i < parentSpace[MIN].length; i++) {
            if (!binaries[i]) {
                childSpace[MIN][i] = parentSpace[MIN][i];
                childSpace[MAX][i] = (parentSpace[MIN][i] + parentSpace[MAX][i]) / 2;

            } else {
                childSpace[MIN][i] = (parentSpace[MIN][i] + parentSpace[MAX][i]) / 2;
                childSpace[MAX][i] = parentSpace[MAX][i];
            }
        }
        return childSpace;
    }


    private static double[][] getRootSpace(EStruct root) {
        double[][] space = new double[3][];
        space[MIN] = ((DoubleArray) root.getAt(BOUND_MIN)).extract();
        space[MAX] = ((DoubleArray) root.getAt(BOUND_MAX)).extract();
        space[CENTER] = getCenter(space[MIN], space[MAX]);
        return space;
    }

    private static double[][] getRootMinMax(EStruct root) {
        double[][] space = new double[2][];
        space[MIN] = ((DoubleArray) root.getAt(BOUND_MIN)).extract();
        space[MAX] = ((DoubleArray) root.getAt(BOUND_MAX)).extract();
        return space;
    }


    private static void check(double[] values, double[] min, double[] max) {
        if (min == null || max == null) {
            throw new RuntimeException("Please set min and max boundary before inserting in the trees");
        }
        if (values.length != min.length) {
            throw new RuntimeException("Values dimension mismatch");
        }
        for (int i = 0; i < min.length; i++) {
            if (values[i] < min[i] || values[i] > max[i]) {
                throw new RuntimeException("Values should be between min, max!");
            }
        }
    }

    private static EStruct createNewNode(final EStruct parent, final EStruct root, final int index) {
        EStruct node = parent.egraph().newEStruct();

        node.setAt(E_TOTAL, Type.LONG, 0);
        node.setAt(E_SUBNODES, Type.LONG, 0);

        parent.setAt(index, Type.ESTRUCT, node);
        parent.setAt(E_SUBNODES, Type.LONG, (long) parent.getAt(E_SUBNODES) + 1);
        parent.setAt(index, Type.ESTRUCT, node);

        return node;
    }

    private static boolean subInsert(final EStruct parent, final double[] key, final Object value, final double[][] space, final double[] resolution, final int buffersize, final EStruct root, boolean bufferupdate, final NDManager manager) {
        int index = getRelationId(space, key);

        EStruct child = (EStruct) parent.getAt(index);
        if (child == null) {
            child = createNewNode(parent, root, index);
        }
        double[][] childSpace = getChildSpace(space, index);
        if (child.getAt(E_VALUE) == null && checkCreateLevels(childSpace, resolution) && manager.parentsHaveNodes()) {
            child.setAt(E_VALUE, Type.LONG, manager.getNewParentNode());
        }

        boolean res = internalInsert(child, key, value, bufferupdate, childSpace, resolution, buffersize, root, manager) && !bufferupdate;
        if (res) {
            parent.setAt(E_TOTAL, Type.LONG, (long) parent.getAt(E_TOTAL) + 1);
            if (manager.parentsHaveNodes()) {
                parent.setAt(E_VALUE, Type.LONG, manager.updateParent((long) parent.getAt(E_VALUE), key, value));
            }
        }
        return res;
    }


    private static boolean internalInsert(final EStruct node, final double[] key, final Object value, final boolean bufferupdate, final double[][] space, final double[] resolution, final int buffersize, final EStruct root, final NDManager manager) {
        if ((long) node.getAt(E_SUBNODES) != 0) {
            return subInsert(node, key, value, space, resolution, buffersize, root, false, manager);
        } else if (checkCreateLevels(space, resolution)) {
            DMatrix buffer = null;
            if (buffersize > 0) {
                buffer = (DMatrix) node.getOrCreateAt(E_BUFFER_KEYS, Type.DMATRIX);
            }
            if (buffer != null) {
                //First step check if it already exists in the buffer
                if (!bufferupdate) {
                    for (int i = 0; i < buffer.columns(); i++) {
                        if (compare(key, buffer.column(i), resolution)) {
                            //call manager interface to get the boolean value here
                            //if the key already exist
                            LongArray bufferValue = (LongArray) node.getOrCreateAt(E_BUFFER_VALUES, Type.LONG_ARRAY);
                            bufferValue.set(i, manager.updateExistingLeafNode(bufferValue.get(i), key, value));

                            if (manager.updateParentsOnExisting() && manager.parentsHaveNodes()) {
                                node.setAt(E_TOTAL, Type.LONG, (long) node.getAt(E_TOTAL) + 1);
                                node.setAt(E_VALUE, Type.LONG, manager.updateParent((long) node.getAt(E_VALUE), key, value));
                            }

                            return manager.updateParentsOnExisting();
                        }
                    }
                }
                //Here it is not in the buffer, we check if we can append
                if (buffer.columns() < buffersize) {
                    buffer.appendColumn(key);
                    LongArray bufferValue = (LongArray) node.getOrCreateAt(E_BUFFER_VALUES, Type.LONG_ARRAY);
                    if (!bufferupdate) {
                        bufferValue.addElement(manager.getNewLeafNode(key, value));
                        node.setAt(E_TOTAL, Type.LONG, (long) node.getAt(E_TOTAL) + 1);
                        if (manager.updateParentsOnNewValue() && manager.parentsHaveNodes()) {
                            node.setAt(E_VALUE, Type.LONG, manager.updateParent((long) node.getAt(E_VALUE), key, value));
                        }
                        return manager.updateParentsOnNewValue();
                    } else {
                        bufferValue.addElement((long) value);
                        return false;
                    }

                }
                //here buffer is full we need to reinsert
                else {
                    //reinsert all children

                    LongArray bufferValue = (LongArray) node.getAt(E_BUFFER_VALUES);
                    for (int i = 0; i < buffer.columns(); i++) {
                        subInsert(node, buffer.column(i), bufferValue.get(i), space, resolution, buffersize, root, true, manager);
                    }


                    //clear the buffer, update the total, and insert the new value
                    node.setAt(E_BUFFER_KEYS, Type.DMATRIX, null);
                    node.setAt(E_BUFFER_VALUES, Type.LONG_ARRAY, null);
                    return subInsert(node, key, value, space, resolution, buffersize, root, bufferupdate, manager);
                }


            } //null buffer means to subinsert as long as we can create levels
            else {
                return subInsert(node, key, value, space, resolution, buffersize, root, bufferupdate, manager);
            }
        }
        //Else we reached here last level of the trees, and the array is full, we need to start a profiler
        else {
            if (bufferupdate) {
                node.setAt(E_VALUE, Type.LONG, value);
                return false;
            } else {
                long evalue = node.getAtWithDefault(E_VALUE, -1l);
                if (evalue > 0) {
                    long newvalue = manager.updateExistingLeafNode(evalue, key, value);
                    node.setAt(E_VALUE, Type.LONG, newvalue);
                    return manager.updateParentsOnExisting();
                } else {
                    node.setAt(E_VALUE, Type.LONG, manager.getNewLeafNode(key, value));
                    return manager.updateParentsOnNewValue();
                }
            }
        }

    }

    private static boolean compare(double[] key1, double[] key2, double[] resolution) {
        //todo compare with distance
        if (resolution != null) {
            for (int i = 0; i < key1.length; i++) {
                if (Math.abs(key1[i] - key2[i]) > resolution[i]) {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < key1.length; i++) {
                if (Math.abs(key1[i] - key2[i]) > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void setDistance(int distanceType) {
        _backend.root().setAt(DISTANCE, Type.INT, distanceType);
    }


    @Override
    public void setResolution(double[] resolution) {
        ((DoubleArray) (_backend.root().getOrCreateAt(RESOLUTION, Type.DOUBLE_ARRAY))).initWith(resolution);
    }

    @Override
    public void setMinBound(double[] min) {
        ((DoubleArray) (_backend.root().getOrCreateAt(BOUND_MIN, Type.DOUBLE_ARRAY))).initWith(min);
    }


    @Override
    public void setMaxBound(double[] max) {
        ((DoubleArray) (_backend.root().getOrCreateAt(BOUND_MAX, Type.DOUBLE_ARRAY))).initWith(max);
    }

    @Override
    public void setBufferSize(int bufferSize) {
        if (bufferSize < 0) {
            throw new RuntimeException("Buffer size can't be <0");
        } else {
            _backend.root().setAt(BUFFER_SIZE, Type.INT, bufferSize);
        }

    }

    @Override
    public void insert(final double[] keys, final long value) {
        EStruct root = _backend.root();
        double[][] space = getRootSpace(root);
        check(keys, space[MIN], space[MAX]);
        double[] resolution = ((DoubleArray) root.getAt(RESOLUTION)).extract();


        int buffersize = root.getAtWithDefault(BUFFER_SIZE, BUFFER_SIZE_DEF);
        //Distance distance = Distances.getDistance(state.getWithDefault(DISTANCE, DISTANCE_DEF));
        if (root.getAtWithDefault(E_TOTAL, 0L) == 0) {
            root.setAt(E_TOTAL, Type.LONG, 0);
            root.setAt(E_SUBNODES, Type.LONG, 0);
            if (manager.parentsHaveNodes()) {
                root.setAt(E_VALUE, Type.LONG, manager.getNewParentNode());
            }

        }
        internalInsert(root, keys, value, false, space, resolution, buffersize, root, manager);
    }

    @Override
    public final ProfileResult queryAround(final double[] keys, final int max) {
        return queryBoundedRadius(keys, -1, max);
    }

    @Override
    public final ProfileResult queryRadius(final double[] keys, final double radius) {
        return queryBoundedRadius(keys, radius, -1);
    }

    @Override
    public final ProfileResult queryBoundedRadius(final double[] keys, final double radius, final int max) {
        EStruct root = _backend.root();
        if (root.getAtWithDefault(E_TOTAL, 0L) == 0) {
            return null;
        }
        double[][] space = getRootMinMax(root);

        check(keys, space[MIN], space[MAX]);
        Distance distance = Distances.getDistance(root.getAtWithDefault(DISTANCE, Distances.DEFAULT), null);
        EStructArray calcZone = _backend.graph().space().newVolatileGraph();
        VolatileTreeResult nnl = new VolatileTreeResult(calcZone.newEStruct(), max);
        reccursiveTraverse(root, calcZone, nnl, distance, keys, null, null, radius, space);
        nnl.sort(true);
        return nnl;
    }


    @Override
    public final ProfileResult queryArea(final double[] min, final double[] max) {
        EStruct root = _backend.root();
        if (root.getAtWithDefault(E_TOTAL, 0L) == 0) {
            return null;
        }
        Distance distance = Distances.getDistance(root.getAtWithDefault(DISTANCE, Distances.DEFAULT), null);
        final double[] center = new double[max.length];
        for (int i = 0; i < center.length; i++) {
            center[i] = (min[i] + max[i]) / 2;
        }
        EStructArray calcZone = _backend.graph().space().newVolatileGraph();
        VolatileTreeResult nnl = new VolatileTreeResult(calcZone.newEStruct(), -1);
        double[][] space = getRootMinMax(root);

        reccursiveTraverse(root, calcZone, nnl, distance, center, min, max, -1, space);
        nnl.sort(true);
        return nnl;
    }

    @Override
    public long size() {
        EStruct root = _backend.root();
        return root.getAtWithDefault(E_TOTAL, 0L);
    }

    @Override
    public long treeSize() {
        return _backend.size();
    }


    private static void reccursiveTraverse(final EStruct node, final EStructArray calcZone, final VolatileTreeResult nnl, final Distance distance, final double[] target, final double[] targetmin, final double[] targetmax, final double radius, final double[][] space) {

        if (node.getAtWithDefault(E_SUBNODES, 0L) == 0) {
            //Leave node
            DMatrix buffer = (DMatrix) node.getAt(E_BUFFER_KEYS);
            LongArray bufferValue = (LongArray) node.getAt(E_BUFFER_VALUES);

            if (buffer != null) {
                //Bufferizing node
                //todo think about the manager here
                for (int i = 0; i < buffer.columns(); i++) {
                    TreeHelper.filterAndInsert(buffer.column(i), bufferValue.get(i), target, targetmin, targetmax, distance, radius, nnl);
                }
                return;


            } else {
                //Very End node
                //todo think about the manager here
                double[] key = ((DoubleArray) node.getAt(E_KEY)).extract();
                long value = (long) node.getAt(E_VALUE);
                TreeHelper.filterAndInsert(key, value, target, targetmin, targetmax, distance, radius, nnl);
                return;
            }

        } else {
            //Parent node
            final double worst = nnl.getWorstDistance();
            if (targetmin == null || targetmax == null) {
                if (!nnl.isCapacityReached() || TreeHelper.getclosestDistance(target, space[MIN], space[MAX], distance) <= worst) {
                    final EStruct tempList = calcZone.newEStruct();
                    final VolatileTreeResult childPriority = new VolatileTreeResult(tempList, -1);


                    node.each(new NodeStateCallback() {
                        @Override
                        public void on(int attributeKey, int elemType, Object elem) {
                            if (attributeKey >= E_OFFSET_REL) {
                                double[][] childSpace = getChildMinMax(space, attributeKey);
                                childPriority.insert(childSpace[MIN], attributeKey, TreeHelper.getclosestDistance(target, childSpace[MIN], childSpace[MAX], distance));
                            }
                        }
                    });
                    childPriority.sort(true);

                    for (int i = 0; i < childPriority.size(); i++) {
                        EStruct child = (EStruct) node.getAt((int) childPriority.value(i));
                        double[][] childSpace = getChildMinMax(space, (int) childPriority.value(i));
                        reccursiveTraverse(child, calcZone, nnl, distance, target, targetmin, targetmax, radius, childSpace);
                    }
                    childPriority.free();
                }
            } else {
                node.each(new NodeStateCallback() {
                    @Override
                    public void on(int attributeKey, int elemType, Object elem) {
                        if (attributeKey >= E_OFFSET_REL) {
                            EStruct child = (EStruct) node.getAt(attributeKey);
                            double[][] childSpace = getChildMinMax(space, attributeKey);

                            if (TreeHelper.checkBoundsIntersection(targetmin, targetmax, childSpace[MIN], childSpace[MAX])) {
                                reccursiveTraverse(child, calcZone, nnl, distance, target, targetmin, targetmax, radius, childSpace);
                            }
                        }
                    }
                });
            }
        }
    }
}
