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
package greycat.ml.profiling;

import greycat.Type;
import greycat.struct.ERelation;
import greycat.struct.EStruct;
import greycat.struct.EStructArray;

public class GaussianSlotsEGraph {

    public static final String NUMBER_OF_SLOTS = "numberOfSlots"; //Number of slots to create in the profile, default is 1

    private static final String SLOTS = "slots";
    private static final String GENERIC_SLOT = "generic_slot";
    private EStructArray backend = null;

    private EStruct root = null;
    private GaussianWrapper[] slots = null;
    private GaussianWrapper generic_slot = null;


    public GaussianSlotsEGraph(EStructArray backend) {
        if (backend == null) {
            throw new RuntimeException("backend can't be null for Gaussian Slot nodes!");
        }
        this.backend = backend;
        if (!load()) {
            root = backend.newEStruct();
            backend.setRoot(root);
            ERelation rel = (ERelation) root.getOrCreate(GENERIC_SLOT, Type.ERELATION);
            rel.add(backend.newEStruct());
            generic_slot = new GaussianWrapper(rel.node(0));
        }

    }

    public void setNumberOfSlots(int number) {
        if (number < 1) {
            throw new RuntimeException("Can't set number of slots <1");
        }
        root.set(NUMBER_OF_SLOTS, Type.INT, number);

        ERelation relation = (ERelation) root.getOrCreate(SLOTS, Type.ERELATION);
        relation.clear();
        EStruct temp;
        slots = new GaussianWrapper[number];
        for (int i = 0; i < number; i++) {
            temp = root.egraph().newEStruct();
            relation.add(temp);
            slots[i] = new GaussianWrapper(temp);
        }
    }

    public void learn(int slot, double[] values) {
        if (slots == null) {
            throw new RuntimeException("Please set the number of slots first!");
        }

        if (slot >= slots.length) {
            throw new RuntimeException("Slot number exceed maximum slots allocated!");
        }
        slots[slot].learn(values);
        generic_slot.learn(values);
    }

    public GaussianWrapper getGaussian(int slot) {
        if (slots == null) {
            throw new RuntimeException("Please set the number of slots first!");
        }
        if (slot >= slots.length) {
            throw new RuntimeException("Slot number exceed maximum slots allocated!");
        }
        return slots[slot];
    }

    public GaussianWrapper getGeneric() {
        return generic_slot;
    }


    public boolean load() {
        if (root != null) {
            return true;
        }

        if (backend.root() == null) {
            return false;
        } else {
            root = backend.root();
            ERelation rel = (ERelation) root.get(SLOTS);
            if (rel != null) {
                slots = new GaussianWrapper[rel.size()];
                for (int i = 0; i < rel.size(); i++) {
                    slots[i] = new GaussianWrapper(rel.node(i));
                }
            }

            rel = (ERelation) root.get(GENERIC_SLOT);
            if (rel != null) {
                generic_slot = new GaussianWrapper(rel.node(0));
            }
            return true;
        }
    }


}
