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
package greycatTest.internal.tree;

public class NNL2 {
    /**
     * The maximum priority possible in this priority queue.
     */
    private double maxPriority = Double.MAX_VALUE;

    /**
     * This contains the list of objects in the queue.
     */
    private Object[] data;

    /**
     * This contains the list of prioritys in the queue.
     */
    private double[] value;

    /**
     * Holds the number of elements currently in the queue.
     */
    private int count;

    /**
     * This holds the number elements this queue can have.
     */
    private int capacity;


    // constructor
    public NNL2(int capacity) {
        count = 0;
        this.capacity = capacity;
        this.data = new Object[capacity + 1];
        this.value = new double[capacity + 1];
        this.value[0] = maxPriority;
    }

    public double getMaxPriority() {
        if (count == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return value[1];
    }

    public boolean insert(Object node, double priority) {
        if (count < capacity) {
            add(node, priority);
            return true;
        }
        if (priority > getMaxPriority()) {
            // do not insert - all elements in queue have lower priority
            return false;
        }
        // remove object with highest priority
        remove();
        // add new object
        add(node, priority);
        return true;
    }

    public Object[] getAllNodes() {
        int size = Math.min(capacity, count);
        Object[] nbrs = new Object[size];

        for (int i = 0; i < size; ++i) {
            nbrs[size - i - 1] = remove();
        }
        return nbrs;
    }


    public boolean isCapacityReached() {
        return count >= capacity;
    }

    public Object getHighest() {
        return data[1];
    }

    public double getBestDistance() {
        return value[1];
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public int getSize() {
        return count;
    }


    /**
     * This function adds the given object into the <code>PriorityQueue</code>,
     * its priority is the long priority. The way in which priority can be
     * associated with the elements of the queue is by keeping the priority and
     * the elements array entrys parallel.
     *
     * @param element  is the object that is to be entered into this
     *                 <code>PriorityQueue</code>
     * @param priority this is the priority that the object holds in the
     *                 <code>PriorityQueue</code>
     */
    private void add(Object element, double priority) {
        if (count++ >= capacity) {
            expandCapacity();
        }
        /* put this as the last element */
        value[count] = priority;
        data[count] = element;
        bubbleUp(count);
    }

    /**
     * Remove is a function to remove the element in the queue with the maximum
     * priority. Once the element is removed then it can never be recovered from
     * the queue with further calls. The lowest priority object will leave last.
     *
     * @return the object with the highest priority or if it's empty null
     */
    private Object remove() {
        if (count == 0)
            return 0;
        Object element = data[1];
        /* swap the last element into the first */
        data[1] = data[count];
        value[1] = value[count];
        /* let the GC clean up */
        data[count] = 0;
        value[count] = 0L;
        count--;
        bubbleDown(1);
        return element;
    }


    /**
     * Bubble down is used to put the element at subscript 'pos' into it's
     * rightful place in the heap (i.e heap is another name for
     * <code>PriorityQueue</code>). If the priority of an element at
     * subscript 'pos' is less than it's children then it must be put under one
     * of these children, i.e the ones with the maximum priority must come
     * first.
     *
     * @param pos is the position within the arrays of the element and priority
     */
    private void bubbleDown(int pos) {
        Object element = data[pos];
        double priority = value[pos];
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
                if (value[child] < value[child + 1])
                    child++; /* choose the biggest child */
            /*
             * percolate down the data at 'pos', one level i.e biggest child
             * becomes the parent
             */
            if (priority < value[child]) {
                value[pos] = value[child];
                data[pos] = data[child];
            } else {
                break;
            }
        }
        value[pos] = priority;
        data[pos] = element;
    }

    /**
     * Bubble up is used to place an element relatively low in the queue to it's
     * rightful place higher in the queue, but only if it's priority allows it
     * to do so, similar to bubbleDown only in the other direction this swaps
     * out its parents.
     *
     * @param pos the position in the arrays of the object to be bubbled up
     */

    private void bubbleUp(int pos) {
        Object element = data[pos];
        double priority = value[pos];
        /* when the parent is not less than the child, end */
        int halfpos = (int) Math.floor(pos / 2);
        while (value[halfpos] < priority) {
            /* overwrite the child with the parent */
            value[pos] = value[halfpos];
            data[pos] = data[halfpos];
            pos  = (int) Math.floor(pos / 2);
            halfpos  = (int) Math.floor(pos / 2);
        }
        value[pos] = priority;
        data[pos] = element;
    }

    /**
     * This ensures that there is enough space to keep adding elements to the
     * priority queue. It is however advised to make the capacity of the queue
     * large enough so that this will not be used as it is an expensive method.
     * This will copy across from 0 as 'off' equals 0 is contains some important
     * data.
     */
    private void expandCapacity() {
        capacity = count * 2;
        Object[] elements = new Object[capacity + 1];
        double[] prioritys = new double[capacity + 1];
        System.arraycopy(data, 0, elements, 0, data.length);
        System.arraycopy(value, 0, prioritys, 0, data.length);
        data = elements;
        value = prioritys;
    }
}
