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
package greycat.ml.profiling;

import greycat.struct.EStructArray;
import greycat.struct.EStruct;
import greycat.struct.NDManager;

public class GmmManager implements NDManager {

    EStructArray _backend;
    public GmmManager(EStructArray eStructArray){
        _backend= eStructArray;
    }

    @Override
    public Object get(long id) {
        return _backend.estruct((int) id);
    }

    @Override
    public long updateExistingLeafNode(long oldKey, double[] key, Object valueToInsert) {
        EStruct node= _backend.estruct((int) oldKey);
        GaussianENode gn= new GaussianENode(node);
        gn.learnWithOccurence(key,(int)(long)valueToInsert);
        return oldKey;
    }

    @Override
    public boolean updateParentsOnExisting() {
        return true;
    }

    @Override
    public boolean updateParentsOnNewValue() {
        return true;
    }

    @Override
    public boolean parentsHaveNodes() {
        return true;
    }

    @Override
    public long getNewLeafNode(double[] key, Object valueToInsert) {
        EStruct node= _backend.newEStruct();
        GaussianENode gn= new GaussianENode(node);
        gn.learnWithOccurence(key,(int)(long)valueToInsert);
        return node.id();
    }

    @Override
    public long getNewParentNode() {
        EStruct node= _backend.newEStruct();
        return node.id();
    }

    @Override
    public long updateParent(long parentkey, double[] key, Object valueToInsert) {
        EStruct node= _backend.estruct((int) parentkey);
        GaussianENode gn= new GaussianENode(node);
        gn.learnWithOccurence(key,(int)(long)valueToInsert);
        return parentkey;
    }
}
