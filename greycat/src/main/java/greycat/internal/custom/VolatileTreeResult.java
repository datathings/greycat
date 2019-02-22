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
package greycat.internal.custom;

import greycat.Type;
import greycat.struct.*;

public class VolatileTreeResult implements ProfileResult {

    private static double maxPriority = Double.MAX_VALUE;
    private static int _KEYS = 1;
    private static int _VALUES = 2;
    private static int _DISTANCES = 3;

    private EStruct node;
    private int capacity;
    private int count;
    private double worst;
    private long total=-1;

    private DMatrix _keys;
    private LMatrix _values;
    private DMatrix _distances;

    public VolatileTreeResult(EStruct node, int capacity) {
        this.node = node;
        this.count = 0;
        _keys = (DMatrix) node.getOrCreateAt(_KEYS, Type.DMATRIX);
        _values = (LMatrix) node.getOrCreateAt(_VALUES, Type.LMATRIX);
        _distances = (DMatrix) node.getOrCreateAt(_DISTANCES, Type.DMATRIX);
        this.capacity = capacity;
    }
    
    @Override
    public int size() {
        return count;
    }

    @Override
    public ProfileResult groupBy(double[] resolutions) {
        if (count == 0) {
            return null;
        }

        if ((resolutions.length != _keys.rows())) {
            throw new RuntimeException("Resolutions and keys are not the same size!");
        }
        int count = 0;
        for (int i = 0; i < resolutions.length; i++) {
            if (resolutions[i] < 0) {
                count++;
            }
        }
        if (count == resolutions.length) {
            throw new RuntimeException("All resolutions can't be negative!");
        }

        double[] newmin = new double[resolutions.length];
        double[] newmax = new double[resolutions.length];
        double[] firstkey = keys(0);
        System.arraycopy(firstkey, 0, newmin, 0, firstkey.length);
        System.arraycopy(firstkey, 0, newmax, 0, firstkey.length);

        double d = 0;
        int t = 0;
        int[] indexCollapse = new int[count];

        for (int j = 0; j < firstkey.length; j++) {
            if (resolutions[j] < 0) {
                newmin[j] = 0;
                newmax[j] = 0;
                indexCollapse[t] = j;
                t++;
                continue;
            }
            for (int i = 1; i < count; i++) {
                d = _keys.get(j, i);
                if (d > newmax[j]) {
                    newmax[j] = d;
                }
                if (d < newmin[j]) {
                    newmin[j] = d;
                }
            }
        }

        NDTree tempTree = new NDTree(node.egraph().graph().space().newVolatileGraph(), new IndexManager());
        tempTree.setResolution(resolutions);
        tempTree.setMinBound(newmin);
        tempTree.setMaxBound(newmax);

        double[] k;
        for (int i = 0; i < count; i++) {
            k = _keys.column(i);
            for (t = 0; t < count; t++) {
                k[indexCollapse[t]] = 0;
            }
            tempTree.insert(k, _values.get(0, i));
        }

        return tempTree.queryArea(newmin, newmax);
    }

    @Override
    public ProfileResult sortByProbability(boolean descending) {
        if (count > 1) {
            quickSort(1, count, !descending,true);
        }
        return this;
    }

    @Override
    public long getTotal() {
        if(total==-1){
            total=0;
            for(int i=0;i<size();i++){
                total+=value(i);
            }
            return total;
        }
        else {
            return total;
        }
    }

    @Override
    public boolean insert(double[] key, long value, double distance) {

        if (capacity > 0 && count == capacity) {
            add(key, value, distance, true);
            return true;
        }
        //Only add
        add(key, value, distance, false);
        return true;


    }

    private void add(double[] key, long value, double distance, boolean remove) {

        if (count == 0) {
            _keys.appendColumn(new double[key.length]);
            _values.appendColumn(new long[1]);
            _distances.appendColumn(new double[]{maxPriority});
        }


        if (remove) {
            if (distance > getWorstDistance()) {
                return;
            }
            remove();
            count++;
            //set at last
            for (int i = 0; i < _keys.rows(); i++) {
                _keys.set(i, count, key[i]);
            }
            _values.set(0, count, value);
            _distances.set(0, count, distance);
        } else {
            count++;
            _keys.appendColumn(key);
            _values.appendColumn(new long[]{value});
            _distances.appendColumn(new double[]{distance});
        }
        bubbleUp(count);
        worst = _distances.get(0, 1);
    }


    //value -> _distances
    //data -> _values

    private void remove() {
        if (count == 0)
            return;


        /* swap the last element into the first */
        for (int i = 0; i < _keys.rows(); i++) {
            _keys.set(i, 1, _keys.get(i, count));
            _keys.set(i, count, 0);
        }

        _values.set(0, 1, _values.get(0, count));
        _distances.set(0, 1, _distances.get(0, count));

        _values.set(0, count, 0L);
        _distances.set(0, count, 0);

        count--;
        bubbleDown(1);

    }


    //value -> _distances
    //data -> _values

    private void bubbleUp(int pos) {

        double[] okey = _keys.column(pos);
        long element = _values.column(pos)[0];
        double priority = _distances.column(pos)[0];

        /* when the parent is not less than the child, end */
        int halfpos = (int) Math.floor(pos / 2);
        while (_distances.column(halfpos)[0] < priority) {
            _distances.set(0, pos, _distances.get(0, halfpos));
            _values.set(0, pos, _values.get(0, halfpos));
            for (int i = 0; i < _keys.rows(); i++) {
                _keys.set(i, pos, _keys.get(i, halfpos));
            }

            /* overwrite the child with the parent */
            pos = (int) Math.floor(pos / 2);
            halfpos = (int) Math.floor(pos / 2);
        }

        _distances.set(0, pos, priority);
        _values.set(0, pos, element);

        for (int i = 0; i < _keys.rows(); i++) {
            _keys.set(i, pos, okey[i]);
        }

    }


    //value -> _distances
    //data -> _values

    private void bubbleDown(int pos) {

        double[] okey = _keys.column(pos);
        long element = _values.column(pos)[0];
        double priority = _distances.column(pos)[0];

        int child;
        /* hole is position '1' */
        for (; pos * 2 <= count; pos = child) {
            child = pos * 2;
            /*
             * if 'child' equals 'count' then there is only one leaf for this
             * parent
             */
            if (child != count)

                /* left_child > right_child */
                if (_distances.get(0, child) < _distances.get(0, child + 1))
                    child++; /* choose the biggest child */
            /*
             * percolate down the data at 'pos', one level i.e biggest child
             * becomes the parent
             */
            if (priority < _distances.get(0, child)) {
                _distances.set(0, pos, _distances.get(0, child));
                _values.set(0, pos, _values.get(0, child));
                for (int i = 0; i < _keys.rows(); i++) {
                    _keys.set(i, pos, _keys.get(i, child));
                }
            } else {
                break;
            }
        }
        _distances.set(0, pos, priority);
        _values.set(0, pos, element);

        for (int i = 0; i < _keys.rows(); i++) {
            _keys.set(i, pos, okey[i]);
        }
    }


    @Override
    public double[] keys(int index) {
        if(index<size()) {
            return _keys.column(index + 1);
        }
        else {
            return null;
        }
    }

    @Override
    public long value(int index) {
        if(index<size()) {
            return _values.get(index + 1, 0);
        }
        else {
            return 0;
        }

    }

    @Override
    public double distance(int index) {
        return _distances.get(index + 1, 0);
    }

    @Override
    public double getWorstDistance() {
        if (count == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return worst;
    }

    @Override
    public void free() {
        EStructArray g = node.egraph();
        node.drop();
        if (g.size() == 0) {
            g.free();
        }
    }

    @Override
    public boolean isCapacityReached() {
        return capacity > 0 && (count == capacity);
    }


    private void swap(int i, int j) {
        double[] tempkey = _keys.column(i);
        long tempvalue = _values.get(0, i);
        double tempdist = _distances.get(0, i);


        _distances.set(0, i, _distances.get(0, j));
        _values.set(0, i, _values.get(0, j));
        for (int k = 0; k < _keys.rows(); k++) {
            _keys.set(k, i, _keys.get(k, j));
        }

        _distances.set(0, j, tempdist);
        _values.set(0, j, tempvalue);
        for (int k = 0; k < _keys.rows(); k++) {
            _keys.set(k, j, tempkey[k]);
        }

    }


    private int partition(int l, int h, boolean ascending, boolean byValue) {
        if(byValue){
            long x = _values.get(0, h);
            int i = (l - 1);

            for (int j = l; j <= h - 1; j++) {
                if (ascending) {
                    if (_values.get(0, j) <= x) {
                        i++;
                        swap(i, j);
                    }
                } else {
                    if (_values.get(0, j) > x) {
                        i++;
                        swap(i, j);
                    }
                }
            }
            // swap arr[i+1] and arr[h]
            swap(i + 1, h);
            return (i + 1);
        }
        else {
            double x = _distances.get(0, h);
            int i = (l - 1);


            for (int j = l; j <= h - 1; j++) {
                if (ascending) {
                    if (_distances.get(0, j) <= x) {
                        i++;
                        swap(i, j);
                    }
                } else {
                    if (_distances.get(0, j) > x) {
                        i++;
                        swap(i, j);
                    }
                }
            }
            // swap arr[i+1] and arr[h]
            swap(i + 1, h);
            return (i + 1);
        }
    }

    private void quickSort(int l, int h, boolean ascending, boolean byValue) {
        // create auxiliary stack
        int stack[] = new int[h - l + 1];

        // initialize top of stack
        int top = -1;

        // push initial values in the stack
        stack[++top] = l;
        stack[++top] = h;

        // keep popping elements until stack is not empty
        while (top >= 0) {
            // pop h and l
            h = stack[top--];
            l = stack[top--];

            // set pivot element at it's proper position
            int p = partition(l, h, ascending, byValue);

            // If there are elements on left side of pivot,
            // then push left side to stack
            if (p - 1 > l) {
                stack[++top] = l;
                stack[++top] = p - 1;
            }

            // If there are elements on right side of pivot,
            // then push right side to stack
            if (p + 1 < h) {
                stack[++top] = p + 1;
                stack[++top] = h;
            }
        }
    }

    @Override
    public ProfileResult sort(boolean ascending) {
        if (count > 1) {
            quickSort(1, count, ascending,false);
        }
        return this;
    }

}
